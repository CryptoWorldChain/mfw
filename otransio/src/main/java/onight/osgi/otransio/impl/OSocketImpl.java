package onight.osgi.otransio.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;
import org.osgi.framework.BundleContext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.nio.OServer;
import onight.osgi.otransio.sm.MSessionSets;
import onight.osgi.otransio.sm.OutgoingSessionManager;
import onight.osgi.otransio.sm.RemoteModuleBean;
import onight.osgi.otransio.sm.RemoteModuleSession;
import onight.tfw.async.CompleteHandler;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.beans.LoopPackBody;
import onight.tfw.otransio.api.beans.UnknowModuleBody;
import onight.tfw.otransio.api.session.CMDService;
import onight.tfw.otransio.api.session.LocalModuleSession;
import onight.tfw.otransio.api.session.PSession;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.proxy.IActor;

@Component(immediate = true)
@Instantiate(name = "osocketimpl")
@Provides(specifications = { ActorService.class, IActor.class })
@Slf4j
public class OSocketImpl implements Serializable, ActorService, IActor {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6301364196672462354L;

	@Getter
	private PropHelper params;

	public static String PACK_FROM = PackHeader.PACK_FROM;
	public static String PACK_TO = PackHeader.PACK_TO;
	public static String PACK_URI = PackHeader.PACK_URI;

	public static String DROP_CONN = "DROP**";

	BundleContext context;

	public OSocketImpl(BundleContext context) {
		this.context = context;
		params = new PropHelper(context);
		mss = new MSessionSets();
		osm = new OutgoingSessionManager(this, params, mss);
		mss.setOsm(osm);
	}

	MSessionSets mss;

	OTransSender sender = new OTransSender(this);

	OServer server = new OServer();

	OutgoingSessionManager osm;
	ThreadPoolExecutor localPool = new ThreadPoolExecutor(10, 100, 120l, TimeUnit.SECONDS, new LinkedBlockingQueue());

	public String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
		return "localhost";
	}

	@Validate
	public void start() {
		server.startServer(this, params);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					/**
					 * 等2秒主要是为了等注册
					 */
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				// 建立外联服务
				log.info("trying to init remote session.");
				osm.init();
				log.info("init remote session.[success] ");
			}
		}).start();
	}

	@Invalidate
	public void stop() {
		log.info("nio stoping");
		server.stop();
		log.info("nio stopped ... OK");
	}

	@Bind(aggregate = true, optional = true)
	public void bindPSender(PSenderService pl) {
		log.info("Register PSender::" + pl + ",sender=" + sender);
		SenderPolicy.bindPSender(pl, sender);
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindPSender(PSenderService pl) {
		log.info("Remove PSender::" + pl);
	}

	@Bind(aggregate = true, optional = true)
	public void bindCMDService(CMDService service) {
		log.info("Register CMDService::" + service);
		LocalModuleSession ms = mss.addLocalMoudle(service.getModule());
		for (String cmd : service.getCmds()) {
			ms.registerService(cmd, service);
		}

	}

	@Unbind(aggregate = true, optional = true)
	public void unbindCMDService(CMDService service) {
		log.info("Remove ModuleSession::" + service);
	}

	public void onPacket(FramePacket pack, final CompleteHandler handler, Connection<?> conn) {
		if (PackHeader.REMOTE_LOGIN.equals(pack.getGlobalCMD())) {// 来自远端的登录
			RemoteModuleBean rmb = pack.parseBO(RemoteModuleBean.class);
			String node_from = pack.getExtStrProp(PACK_FROM);
			// if (StringUtils.equals(rmb.getNodeInfo().nodeName,
			// NodeHelper.getCurrNodeName())) {
			// log.debug("loop login from local");
			// // conn.close();
			// return;
			// }
			if (node_from == null) {
				node_from = rmb.getNodeInfo().getNodeName();
			}
			if (node_from != null) {
				PSession session = mss.byNodeName(node_from);
				if (session != null && session instanceof RemoteModuleSession) {
					RemoteModuleSession rms = (RemoteModuleSession) session;
					rms.addConnection(conn);
				}
				rmb.getNodeInfo().setNodeName(node_from);
				// mss.addRemoteSession(rmb.getNodeInfo(), conn);
			} else {
				log.debug("unknow node id_from:" + node_from);
			}
			// conn.write(mss.getLocalModulesPacketBack());
		} else if (DROP_CONN.equals(pack.getGlobalCMD())) {// 来自远端的模块信息返回
			conn.closeSilently();
		} else if (PackHeader.CMD_HB.equals(pack.getGlobalCMD())) {// 来自远端的心跳线
			log.trace("[HB] From " + conn.getPeerAddress() + " , to " + conn.getLocalAddress());
		} else {
			routePacket(pack, handler, conn);
		}
	}

	public void routePacket(FramePacket pack, final CompleteHandler handler) {
		// from local
		routePacket(pack, handler, null);
	}

	public void routePacket(FramePacket pack, final CompleteHandler handler, Connection<?> conn) {
		if (pack.isResp() && pack.getExtHead().isExist(mss.getPackIDKey())) {
			// 检查是否为响应包
			String expackid = pack.getExtStrProp(mss.getPackIDKey());
			FutureImpl<FramePacket> future = mss.getPackMaps().remove(expackid);
			if (future != null) {
				Object opackid = pack.getExtHead().remove(mss.getPackIDKey());
				Object ofrom = pack.getExtHead().remove(OSocketImpl.PACK_FROM);
				Object oto = pack.getExtHead().remove(OSocketImpl.PACK_TO);
				log.debug("oldfrom = " + ofrom + ",oto=" + oto + ",opackid=" + opackid);
				future.result(pack);
			} else {
				log.warn("unknow ack:" + expackid + ",packid=" + pack.getExtProp(mss.getPackIDKey()));
				handler.onFinished(PacketHelper.toPBReturn(pack, new LoopPackBody(mss.getPackIDKey(), pack)));
			}
			return;
		}
		String destTO = pack.getExtStrProp(PACK_TO);
		String from = pack.getExtStrProp(PACK_FROM);

		PSession ms = null;

		if (StringUtils.isNotBlank(destTO) && conn == null) {// 固定给某个节点id的
			ms = mss.byNodeName(destTO);
			if (ms == null) {// not found
				String uri = pack.getExtStrProp(PACK_URI);
				log.debug("creating new Connection:" + uri + ":name=" + destTO);
				if (StringUtils.isNotBlank(uri)) {
					NodeInfo node = NodeInfo.fromURI(uri, destTO);
					try {
						ms = osm.createOutgoingSSByURI(node);
					} catch (Exception e) {
						log.error("route ERROR:" + e.getMessage(), e);
						throw new MessageException(e);
					}
				}
			} else {
				String uri = pack.getExtStrProp(PACK_URI);
				if (StringUtils.isNotBlank(uri) && ms instanceof RemoteModuleSession) {
					RemoteModuleSession rms = (RemoteModuleSession) ms;
					rms.getConnsPool().setAliasURI(uri);
				}
				log.debug("using exist session:" + ms);
			}
			mss.getSendCounter().incrementAndGet();
		} else {// re
			if (conn != null) {
				mss.getRecvCounter().incrementAndGet();
				if (StringUtils.isNotBlank(from)) {
					RemoteModuleSession rms = osm.addIncomming(from, conn);
					if (rms != null) {
						rms.getRecvCounter().incrementAndGet();
					}
				}
			}
			ms = mss.getLocalsessionByModule().get(pack.getModule());
		}
		if (ms != null) {
			if (ms instanceof LocalModuleSession) {
				route2Local(pack, handler, ms);
			} else {
				ms.onPacket(pack, handler);
			}
		} else {
			// 没有找到对应的消息
			if (pack.isSync()) {
				handler.onFinished(
						PacketHelper.toPBReturn(pack, new UnknowModuleBody(pack.getModule() + ",to=" + destTO, pack)));
			}
		}

	}

	public void route2Local(final FramePacket pack, final CompleteHandler handler, final PSession ms) {
		String packid = null;
		if (pack.isSync()) {
			final FutureImpl<String> future = Futures.createSafeFuture();
			localPool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						ms.onPacket(pack, handler);
					} finally {
						future.result("F");
					}
				}
			});
			try {
				future.get(60, TimeUnit.SECONDS);
			} catch (Throwable e) {
				log.error("route Failed:" + e.getMessage(), e);
				handler.onFailed(new RuntimeException(e));
			}
		} else {
			localPool.execute(new Runnable() {
				@Override
				public void run() {
					ms.onPacket(pack, handler);
				}
			});
		}
	}

	public void tryDropConnection(String packNameOrId) {
		mss.dropSession(packNameOrId);
	}

	public void renameSession(String oldname, String newname) {
		mss.renameSession(oldname, newname);
	}

	@Override
	public void doDelete(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		doSomething(arg0, arg1);
	}

	@Override
	public void doGet(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		doSomething(arg0, arg1);
	}

	@Override
	public void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		doSomething(arg0, arg1);
	}

	@Override
	public void doPut(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		doSomething(arg0, arg1);
	}

	public void doSomething(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Content-type", "application/json;charset=UTF-8");
		resp.getWriter().write(mss.getJsonInfo());
	}

	@Override
	public String[] getWebPaths() {
		return new String[] { "/nio/stat" };
	}

}
