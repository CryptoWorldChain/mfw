package onight.osgi.otransio.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.sm.RemoteModuleSession;
import onight.tfw.async.CallBack;
import onight.tfw.async.CompleteHandler;
import onight.tfw.async.FutureSender;
import onight.tfw.async.NilCompleteHandler;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.PSession;

@Slf4j
public class OTransSender extends FutureSender {

	OSocketImpl osock;

	public OTransSender(OSocketImpl osock) {
		super();
		this.osock = osock;
	}

	@Override
	public FramePacket send(FramePacket fp, long timeoutMS) throws MessageException {
		final FutureImpl<FramePacket> future = Futures.createSafeFuture();

		osock.routePacket(fp, new CompleteHandler() {
			@Override
			public void onFinished(FramePacket packet) {
				future.result(packet);
			}

			@Override
			public void onFailed(Exception error) {
				future.failure(error);
			}
		});
		try {
			FramePacket ret = future.get(timeoutMS, TimeUnit.MILLISECONDS);
			return ret;
		} catch (InterruptedException e) {
			log.warn("send InterruptedException:" + fp, e);
			throw new MessageException(e);
		} catch (ExecutionException e) {
			log.warn("send ExecutionException:" + fp, e);
			throw new MessageException(e);
		} catch (TimeoutException e) {
			log.warn("send TimeoutException:" + fp, e);
			throw new MessageException(e);
		} catch (Exception e) {
			throw new MessageException(e);
		}
	}

	@Override
	public void asyncSend(final FramePacket fp, final CallBack<FramePacket> cb) {
		fp.genBodyBytes();
		osock.routePacket(fp, new CompleteHandler() {
			@Override
			public void onFailed(Exception e) {
				cb.onFailed(e, fp);
			}

			@Override
			public void onFinished(FramePacket rfp) {
				rfp.genBodyBytes();
				cb.onSuccess(rfp);
			}
		});
	}

	NilCompleteHandler hh = new NilCompleteHandler();

	@Override
	public void post(FramePacket fp) {
		osock.routePacket(fp, hh);
	}

	@Override
	public void tryDropConnection(String pack_to) {
		osock.tryDropConnection(pack_to);
	}

	@Override
	public void changeNodeName(String oldname, String newname) {
		osock.renameSession(oldname, newname);
	}

	@Override
	public void setCurrentNodeName(String name) {
		osock.mss.getRmb().getNodeInfo().setNodeName(name);
	}

	@Override
	public void setDestURI(String dest, String uri) {
		PSession session = osock.mss.byNodeName(dest);
		if (session != null && session instanceof RemoteModuleSession) {
			RemoteModuleSession rms = (RemoteModuleSession) session;
			rms.getConnsPool().parseURI(uri);
		}
	}

}
