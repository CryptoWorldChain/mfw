package onight.osgi.otransio.nio;

import java.io.IOException;

import onight.osgi.otransio.impl.OSocketImpl;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.FilterChainContext.State;
import org.glassfish.grizzly.filterchain.NextAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionFilter extends BaseFilter {
	Logger log = LoggerFactory.getLogger(SessionFilter.class);

	protected final AttributeBuilder attributeBuilder = Grizzly.DEFAULT_ATTRIBUTE_BUILDER;

	private OSocketImpl oimpl;

	public SessionFilter(OSocketImpl oimpl, PropHelper prop) {
		super();
		this.oimpl = oimpl;
	}

	@Override
	public NextAction handleRead(final FilterChainContext ctx)
			throws IOException {
		final FramePacket pack = ctx.getMessage();
		if (pack == null) {
			return ctx.getInvokeAction();
		}
		long start = System.currentTimeMillis();
		log.trace("[Message]: " + pack.getGlobalCMD() + ", FROM: "
				+ ctx.getConnection().getPeerAddress() + " HEAD: "
				+ pack.getFixHead() + ",oimpl=" + oimpl);

		CompleteHandler handler = null;
		if (pack.isSync()) {// 需要等待回应的
			final Connection conn = ctx.getConnection();
			handler = new CompleteHandler() {
				@Override
				public void onFinished(FramePacket vpacket) {
					if(conn.isOpen())
					{
						try {
							conn.write(vpacket);
						} catch (Exception e) {
							log.error("write back error:"+vpacket+",pack="+pack+",ctx="+ctx+",filter="+ctx.getFilterChain(),e);
						}
					}
				}
			};
		} else {
			handler = new CompleteHandler() {
				@Override
				public void onFinished(FramePacket packet) {
					log.warn("dropping back mssgage:" + packet);
				}
			};
		}

		oimpl.onPacket(pack, handler, ctx.getConnection());
		log.info("[MSG] " + pack.getCMD() + " " + pack.getModule()//
				+ " , FROM: " + ctx.getConnection().getPeerAddress() //
				+ " , TO: " + ctx.getConnection().getLocalAddress() //
				+ " COST " + (System.currentTimeMillis() - start) + " ms");
		return ctx.getStopAction();
	}

	@Override
	public NextAction handleWrite(FilterChainContext ctx) throws IOException {
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleAccept(FilterChainContext ctx) throws IOException {
		log.info("new connection:" + ctx.getConnection().getPeerAddress());
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleClose(FilterChainContext ctx) throws IOException {
		log.info("close connection:" + ctx.getConnection().getPeerAddress());
		return ctx.getInvokeAction();
	}

}
