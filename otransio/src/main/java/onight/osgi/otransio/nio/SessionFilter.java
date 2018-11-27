package onight.osgi.otransio.nio;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.exception.NoneServerException;
import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.sm.RemoteModuleSession;
import onight.tfw.async.CompleteHandler;
import onight.tfw.async.NilCompleteHandler;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.ExtHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.PSession;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.SerializerFactory;

public class SessionFilter extends BaseFilter {
	Logger log = LoggerFactory.getLogger(SessionFilter.class);

	private OSocketImpl oimpl;

	public SessionFilter(OSocketImpl oimpl, PropHelper prop) {
		super();
		this.oimpl = oimpl;
	}

	NilCompleteHandler nch = new NilCompleteHandler();
	public static String GCMD_ECHOS = "ECHO**";

	@Override
	public NextAction handleRead(final FilterChainContext ctx) throws IOException {
		final FramePacket pack = ctx.getMessage();
		if (pack == null) {
			return ctx.getInvokeAction();
		}
		// long start = System.currentTimeMillis();
		// // log.trace("[Actor]: " + pack.getModuleAndCMD() + ", FROM: " +
		// // ctx.getConnection().getPeerAddress() + " HEAD: "
		// // + pack.getFixHead() + ",oimpl=" + oimpl);String sendtime =
		// (String) ext.get(Encoder.LOG_TIME_SENT);
		String sendtime = (String) pack.getExtStrProp(Encoder.LOG_TIME_SENT);
		if (StringUtils.isNumeric(sendtime)) {
			FixHeader header = pack.getFixHead();
			log.debug("transio session recv " + header.getCmd() + "" + header.getModule() + " bodysize ["
					+ header.getBodysize() + "]b cost[" + (System.currentTimeMillis() - Long.parseLong(sendtime))
					+ "]ms sent@=" + sendtime + " resp=" + header.isResp() + ",sync=" + header.isSync() + " local="
					+ ctx.getConnection().getLocalAddress() + " peer=" + ctx.getConnection().getPeerAddress());
		}
		if (pack.getFixHead().getPrio() == '8' || pack.getFixHead().getPrio() == '9') {
			String packid = pack.getExtStrProp(PacketQueue.PACK_RESEND_ID);
			if (packid != null) {
				if (GCMD_ECHOS.equals(pack.getModuleAndCMD())) {
//					log.error("get echo for packid==>" + packid);
					PacketTuple pt = oimpl.getMss().getResendMap().remove(packid);
					if (pt != null) {
						pt.setResponsed(true);
					}
					return ctx.getInvokeAction();
				}
				FramePacket resp = new FramePacket();//PacketHelper.toPBReturn(pack, null);
				resp.setFixHead(new FixHeader());
				resp.setExtHead(new ExtHeader());
				resp.getFixHead().setSync(false);
				resp.getFixHead().setCmd("ECH");
				resp.getFixHead().setModule("O**");
				resp.getFixHead().setResp(true);
				resp.getFixHead().setPrio((byte)'9');
				resp.getFixHead().genBytes();
				resp.putHeader(PacketQueue.PACK_RESEND_ID, packid);
//				log.error("send echo for packid==>" + packid + ",gcmd=" + pack.getModuleAndCMD() + ",resp.gcmd="
//						+ resp.getModuleAndCMD());
				ctx.write(resp);
				if (oimpl.getMss().getDuplicateCheckMap().containsKey(packid)) {
					log.debug("duplicate message:{}", packid);
					return ctx.getInvokeAction();
				} else {
					if (oimpl.getMss().getDuplicateCheckMap().size() < oimpl.getMss().getResendBufferSize()) {
						oimpl.getMss().getDuplicateCheckMap().put(packid, System.currentTimeMillis());
					}
				}
			}
		}

		CompleteHandler handler = null;
		if (pack.isSync() && !pack.isResp()) {// 需要等待回应的
			final Connection conn = ctx.getConnection();
			final String packfrom = pack.getExtStrProp(OSocketImpl.PACK_FROM);

			handler = new CompleteHandler() {
				@Override
				public void onFinished(FramePacket vpacket) {
					vpacket.getExtHead().reset();
					vpacket.getExtHead().genBytes();
					vpacket.getFixHead().setResp(true);
					try {
						if (conn.isOpen()) {
							log.debug("sync message response to conn=" + conn + ",bcuid=" + packfrom + ",packgcmd="
									+ vpacket.getModuleAndCMD() + "/" + pack.getModuleAndCMD());
							vpacket.putHeader(OSocketImpl.PACK_FROM,
									oimpl.getMss().getRmb().getNodeInfo().getNodeName());
							conn.write(vpacket);
						} else {
							log.error("sync message response to new conn=" + conn + ",bcuid=" + packfrom + ",packgcmd="
									+ vpacket.getModuleAndCMD() + "/" + pack.getModuleAndCMD());
							// log.debug("get Pack callback from :" + packfrom);
							vpacket.putHeader(OSocketImpl.PACK_TO, packfrom);
							vpacket.getFixHead().setSync(false);
							vpacket.getFixHead().setResp(true);
							PSession session = oimpl.getMss().byNodeName(packfrom);
							if (session != null && session instanceof RemoteModuleSession) {
								RemoteModuleSession rms = (RemoteModuleSession) session;
								rms.getWriterQ().offer(vpacket, null);
							} else {
								log.error("drop response packet:" + pack.getModuleAndCMD() + ",packfrom=" + packfrom);
							}
						}
						//
					} catch (Exception e) {
						log.error("write back error:" + vpacket + ",pack=" + pack + ",ctx=" + ctx + ",filter="
								+ ctx.getFilterChain(), e);
					}
				}

				@Override
				public void onFailed(Exception error) {
					log.error("sync callback error:" + pack, error);
					conn.write(PacketHelper.toPBErrorReturn(pack, error.getLocalizedMessage(), error.getMessage()));
				}
			};
		} else {
			handler = nch;
		}
		try {
			oimpl.onPacket(pack, handler, ctx.getConnection());
		} catch (Throwable t) {
			log.error("error in process pack:" + pack.getCMD() + "" + pack.getModule() + ",conn=" + ctx.getConnection()
					+ ":pack=" + pack, t);
		}
		/*
		 * log.debug("[MSG] " + pack.getCMD() + " " + pack.getModule()// +
		 * " , FROM: " + ctx.getConnection().getPeerAddress() // + " , TO: " +
		 * ctx.getConnection().getLocalAddress() // + " COST " +
		 * (System.currentTimeMillis() - start) + " ms");
		 */
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleWrite(FilterChainContext ctx) throws IOException {
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleAccept(FilterChainContext ctx) throws IOException {
		log.debug("new connection:" + ctx.getConnection().getPeerAddress());
		oimpl.getOsm().getNck().addCheckHealth(ctx.getConnection());
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleClose(FilterChainContext ctx) throws IOException {
		if (ctx.getConnection().getCloseReason() != null) {
			Connection conn = ctx.getConnection();
			log.debug("close connection:" + ctx.getConnection().getPeerAddress() + ",reason="
					+ conn.getCloseReason().getType() + ":" + conn.getCloseReason().getCause());
		} else {
			log.debug("close connection:" + ctx.getConnection().getPeerAddress() + ",closereson="
					+ ctx.getConnection().getCloseReason());
		}
		return ctx.getInvokeAction();
	}

}
