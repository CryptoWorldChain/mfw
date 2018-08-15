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
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.PSession;
import onight.tfw.outils.conf.PropHelper;

public class SessionFilter extends BaseFilter {
	Logger log = LoggerFactory.getLogger(SessionFilter.class);

	protected final AttributeBuilder attributeBuilder = Grizzly.DEFAULT_ATTRIBUTE_BUILDER;

	private OSocketImpl oimpl;

	public SessionFilter(OSocketImpl oimpl, PropHelper prop) {
		super();
		this.oimpl = oimpl;
	}

	NilCompleteHandler nch = new NilCompleteHandler();

	@Override
	public NextAction handleRead(final FilterChainContext ctx) throws IOException {
		final FramePacket pack = ctx.getMessage();
		if (pack == null) {
			return ctx.getInvokeAction();
		}
		// long start = System.currentTimeMillis();
		// // log.trace("[Actor]: " + pack.getGlobalCMD() + ", FROM: " +
		// // ctx.getConnection().getPeerAddress() + " HEAD: "
		// // + pack.getFixHead() + ",oimpl=" + oimpl);String sendtime =
		// (String) ext.get(Encoder.LOG_TIME_SENT);
		String sendtime = (String) pack.getExtStrProp(Encoder.LOG_TIME_SENT);
		if (StringUtils.isNumeric(sendtime)) {
			FixHeader header = pack.getFixHead();
			log.debug("transio session recv " + header.getCmd() + "" + header.getModule() + " bodysize ["
					+ header.getBodysize() + "]b cost[" + (System.currentTimeMillis() - Long.parseLong(sendtime))
					+ "]ms sent@=" + sendtime + " resp=" + header.isResp() + ",sync=" + header.isSync()+" local="+
					ctx.getConnection().getLocalAddress()+" peer="+ctx.getConnection().getPeerAddress());
		}

		CompleteHandler handler = null;
		if (pack.isSync() && !pack.isResp()) {// 需要等待回应的
			final Connection conn = ctx.getConnection();
			handler = new CompleteHandler() {
				@Override
				public void onFinished(FramePacket vpacket) {
					vpacket.getExtHead().reset();
					vpacket.getExtHead().genBytes();
					try {
						if (conn.isOpen()) {
							conn.write(vpacket);
						} else {

							String packfrom = vpacket.getExtStrProp(OSocketImpl.PACK_FROM);
							// log.debug("get Pack callback from :" + packfrom);
							vpacket.putHeader(OSocketImpl.PACK_TO, packfrom);
							vpacket.getFixHead().setSync(false);
							vpacket.getFixHead().setResp(true);
							PSession session = oimpl.getMss().byNodeName(packfrom);
							if (session != null && session instanceof RemoteModuleSession) {
								RemoteModuleSession rms = (RemoteModuleSession) session;
								rms.onPacket(vpacket, new CompleteHandler() {
									@Override
									public void onFinished(FramePacket arg0) {
									}

									@Override
									public void onFailed(Exception arg0) {
									}
								});
							} else {
								log.debug("drop response packet:" + pack.getGlobalCMD() + ",packfrom=" + packfrom);
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
					conn.write(PacketHelper.toPBErrorReturn(pack, error.getLocalizedMessage(), error.getMessage()));
				}
			};
		} else {
			handler = nch;
		}
		oimpl.onPacket(pack, handler, ctx.getConnection());
		/*
		 * log.debug("[MSG] " + pack.getCMD() + " " + pack.getModule()// +
		 * " , FROM: " + ctx.getConnection().getPeerAddress() // + " , TO: " +
		 * ctx.getConnection().getLocalAddress() // + " COST " +
		 * (System.currentTimeMillis() - start) + " ms");
		 */
		return ctx.getStopAction();
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
		log.debug("close connection:" + ctx.getConnection().getPeerAddress());
		return ctx.getInvokeAction();
	}

}
