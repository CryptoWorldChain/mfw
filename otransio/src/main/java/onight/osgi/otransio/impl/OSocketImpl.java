package onight.osgi.otransio.impl;

import java.beans.Transient;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;
import org.osgi.framework.BundleContext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.ck.NewConnCheckHealth;
import onight.osgi.otransio.exception.NoneServerException;
import onight.osgi.otransio.exception.TransIOException;
import onight.osgi.otransio.exception.UnAuthorizedConnectionException;
import onight.osgi.otransio.nio.OServer;
import onight.osgi.otransio.nio.PacketQueue;
import onight.osgi.otransio.sm.MSessionSets;
import onight.osgi.otransio.sm.OutgoingSessionManager;
import onight.osgi.otransio.sm.RemoteModuleBean;
import onight.osgi.otransio.sm.RemoteModuleSession;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
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
	private transient PropHelper params;

	public static String PACK_FROM = PackHeader.PACK_FROM;
	public static String PACK_TO = PackHeader.PACK_TO;
	public static String PACK_URI = PackHeader.PACK_URI;

	public static String DROP_CONN = "DROP**";

	BundleContext context;

	public OSocketImpl(BundleContext context) {
		this.context = context;
		params = new PropHelper(context);
		mss = new MSessionSets(params);
		osm = new OutgoingSessionManager(this, params, mss);
		mss.setOsm(osm);
		localProcessor.exec = mss.getReaderexec();
		localProcessor.poolSize = params.get("org.zippo.otransio.maxrunnerbuffer", 1000);
	}

	@Getter
	transient MSessionSets mss;

	transient OTransSender sender = new OTransSender(this);

	transient OServer server = new OServer();

	@Getter
	transient OutgoingSessionManager osm;
	// transient ThreadPoolExecutor localPool;

	transient ConcurrentHashMap<String, PacketQueue> queueBybcuid = new ConcurrentHashMap<>();
	transient LocalMessageProcessor localProcessor = new LocalMessageProcessor();

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
				osm.init();
			}
		}).start();
	}

	@Invalidate
	public void stop() {
		log.debug("nio stoping");
		server.stop();
		log.debug("nio stopped ... OK");
	}

	@Bind(aggregate = true, optional = true)
	public void bindPSender(PSenderService pl) {
		// log.debug("Register PSender::" + pl + ",sender=" + sender);
		SenderPolicy.bindPSender(pl, sender);
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindPSender(PSenderService pl) {
		// log.debug("Remove PSender::" + pl);
	}

	@Bind(aggregate = true, optional = true)
	public void bindCMDService(CMDService service) {
		// log.debug("Register CMDService::" + service.getModule());
		LocalModuleSession ms = mss.addLocalMoudle(service.getModule());
		for (String cmd : service.getCmds()) {
			ms.registerService(cmd, service);
		}

	}

	@Unbind(aggregate = true, optional = true)
	public void unbindCMDService(CMDService service) {
		// log.debug("Remove ModuleSession::" + service);
	}

	public void onPacket(FramePacket pack, final CompleteHandler handler, Connection<?> conn) throws TransIOException {
		if (PackHeader.REMOTE_LOGIN.equals(pack.getGlobalCMD())) {// 来自远端的登录
			RemoteModuleBean rmb = pack.parseBO(RemoteModuleBean.class);
			String node_from = pack.getExtStrProp(PACK_FROM);
			if (StringUtils.isBlank(node_from)) {
				node_from = rmb.getNodeInfo().getNodeName();
			} else {
				rmb.getNodeInfo().setNodeName(node_from);
			}
			log.debug("Get New Login Connection From:" + rmb.getNodeInfo().getUname() + ",nodeid=" + node_from);
			if (node_from != null) {
				PSession session = mss.byNodeName(node_from);
				if (session != null && session instanceof RemoteModuleSession) {
					RemoteModuleSession rms = (RemoteModuleSession) session;
					rms.addConnection(conn);
					CKConnPool ckpool = rms.getConnsPool();
					ckpool.setIp(rmb.getNodeInfo().getAddr());
					ckpool.setPort(rmb.getNodeInfo().getPort());
				} else {
					try {
						osm.createOutgoingSSByURI(rmb.getNodeInfo(), node_from);
					} catch (NoneServerException e) {
					}
				}
				conn.getAttributes().setAttribute(NewConnCheckHealth.CONN_AUTH_INFO, rmb);
			} else {
				log.debug("unknow node id_from:" + node_from);
			}
			// conn.write(mss.getLocalModulesPacketBack());
		} else if (DROP_CONN.equals(pack.getGlobalCMD())) {// 来自远端的模块信息返回
			log.debug("get drop connection message");
			osm.dropSessionByRemote(conn);
			conn.closeSilently();
		} else if (PackHeader.CMD_HB.equals(pack.getGlobalCMD())) {// 来自远端的心跳线
			log.trace("[HB] From " + conn.getPeerAddress() + " , to " + conn.getLocalAddress());
		} else {
			routePacket(pack, handler, conn);
		}
	}

	public void routePacket(FramePacket pack, final CompleteHandler handler) throws TransIOException {
		// from local
		routePacket(pack, handler, null);
	}

	public void routePacket(FramePacket pack, final CompleteHandler handler, Connection<?> conn)
			throws TransIOException {
		if (pack.isResp() && pack.getExtHead().isExist(mss.getPackIDKey())) {
			// 检查是否为响应包
			String expackid = pack.getExtStrProp(mss.getPackIDKey());
			CompleteHandler future_handler = mss.getPackMaps().remove(expackid);
			if (future_handler != null) {
				Object opackid = pack.getExtHead().remove(mss.getPackIDKey());
				Object ofrom = pack.getExtHead().remove(OSocketImpl.PACK_FROM);
				Object oto = pack.getExtHead().remove(OSocketImpl.PACK_TO);
				log.debug("response from = " + ofrom + ",oto=" + oto + ",opackid=" + opackid);
				future_handler.onFinished(pack);
			} else {
				log.warn("unknow ack:" + expackid + ",packid=" + pack.getExtProp(mss.getPackIDKey()));
				// handler.onFinished(PacketHelper.toPBReturn(pack, new
				// LoopPackBody(mss.getPackIDKey(), pack)));
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
				log.debug("creating new Connection:" + uri + ":name=" + destTO + ",from=" + from);
				if (StringUtils.isNotBlank(uri)) {
					NodeInfo node = NodeInfo.fromURI(uri, destTO);
					try {
						ms = osm.createOutgoingSSByURI(node, from);
					} catch (Exception e) {
						log.error("route ERROR:" + e.getMessage(), e);
						throw new MessageException(e);
					}
				}
			}
		} else {// re
			if (conn != null) {
				if (StringUtils.isNotBlank(from)) {
					try {
						osm.addIncomming(from, conn);
					} catch (UnAuthorizedConnectionException e) {
						// conn.close();
					}
				}
				ms = mss.getLocalsessionByModule().get(pack.getModule());
			} else {
				ms = mss.getLocalsessionByModule().get(pack.getModule());
			}
		}
		if (ms != null) {
			if (ms instanceof LocalModuleSession) {
				localProcessor.route2Local(pack, handler, ms);
			} else {
				ms.onPacket(pack, handler);
			}
		} else {
			// 没有找到对应的消息
			log.debug("UnknowModule:" + pack.getModule() + ",CMD=" + pack.getCMD() + ",from=" + from + ",conn=" + conn
					+ ",destTO=" + destTO);
			if (pack.isSync()) {
				handler.onFinished(
						PacketHelper.toPBReturn(pack, new UnknowModuleBody(pack.getModule() + ",to=" + destTO, pack)));
			}
		}

	}

	public synchronized void tryDropConnection(String packNameOrId) {
		mss.dropSession(packNameOrId, true);
		PacketQueue queue = queueBybcuid.remove(packNameOrId);
		if (queue != null) {
			queue.setStop(true);
		}
	}

	public synchronized void renameSession(String oldname, String newname) {
		mss.renameSession(oldname, newname);
		PacketQueue queue = queueBybcuid.remove(oldname);
		if (queue != null) {
			queueBybcuid.put(newname, queue);
		}
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
