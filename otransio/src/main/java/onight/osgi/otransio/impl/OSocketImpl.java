package onight.osgi.otransio.impl;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

@Component(immediate = true)
@Instantiate(name = "osocketimpl")
@Provides(specifications = ActorService.class)
@Slf4j
public class OSocketImpl implements Serializable, ActorService {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6301364196672462354L;

	@Getter
	private PropHelper params;

	public static String PACK_FROM = PackHeader.PACK_FROM;
	public static String PACK_TO = PackHeader.PACK_TO;
	public static String PACK_URI = PackHeader.PACK_URI;

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
			if (StringUtils.equals(rmb.getNodeInfo().nodeName, NodeHelper.getCurrNodeName())) {
				log.debug("loop login from local");
				// conn.close();
				return;
			}
			if (node_from != null) {
				mss.addRemoteSession(rmb.getNodeInfo(), conn);
			}
			conn.write(mss.getLocalModulesPacketBack());
		} else if (PackHeader.REMOTE_LOGIN_RET.equals(pack.getGlobalCMD())) {// 来自远端的模块信息返回
		} else if (PackHeader.CMD_HB.equals(pack.getGlobalCMD())) {// 来自远端的心跳线
			log.trace("[HB] From " + conn.getPeerAddress() + " , to " + conn.getLocalAddress());
		} else {
			routePacket(pack, handler);
		}
	}

	public void routePacket(FramePacket pack, final CompleteHandler handler) {
		mss.getAllRCounter().incrementAndGet();
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
		// String destTOIdx = pack.getExtStrProp(PACK_TO_IDX);
		PSession ms = null;
		if (StringUtils.isNotBlank(destTO)) {// 固定给某个节点id的
			ms = mss.byNodeName(destTO);
			if (ms == null) {// not found
				if (destTO.equals(NodeHelper.getCurrNodeName())) {
					ms = mss.getLocalsessionByModule().get(pack.getModule());
					log.debug("message from local:"+pack.getModule());
				} else {
					String uri = pack.getExtStrProp(PACK_URI);
					log.debug("createing new Connection:" + uri + ":name=" + destTO);
					if (StringUtils.isNotBlank(uri)) {
						NodeInfo node = NodeInfo.fromURI(uri);
						try {
							node.setNodeName(destTO);
							ms = osm.createOutgoingSSByURI(node);
						} catch (Exception e) {
							log.error("route ERROR:" + e.getMessage(), e);
							throw new MessageException(e);
						}
					}
				}
			}
		} else {// re
			ms = mss.getLocalsessionByModule().get(pack.getModule());
		}
		if (ms != null) {
			mss.getAllSCounter().incrementAndGet();
			mss.getRecvCounter().incrementAndGet();
			ms.onPacket(pack, handler);
		} else {
			// 没有找到对应的消息
			if (pack.isSync()) {
				handler.onFinished(
						PacketHelper.toPBReturn(pack, new UnknowModuleBody(pack.getModule() + ",to=" + destTO, pack)));
			}
		}

	}

	public void tryDropConnection(String packNameOrId) {
		mss.dropSession(packNameOrId);
	}

}
