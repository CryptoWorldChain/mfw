package onight.osgi.otransio.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.async.CompleteHandler;
import onight.tfw.async.FutureSender;
import onight.tfw.otransio.api.MessageException;
import onight.tfw.otransio.api.beans.FramePacket;

import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

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
	public void asyncSend(FramePacket fp, final CallBack<FramePacket> cb) {
		fp.genBodyBytes();
		osock.routePacket(fp, new CompleteHandler() {
			@Override
			public void onFinished(FramePacket packet) {
				packet.genBodyBytes();
				cb.onSuccess(packet);
			}
		});
	}

	@Override
	public void post(FramePacket fp) {
		osock.routePacket(fp, new CompleteHandler() {
			@Override
			public void onFinished(FramePacket packet) {
			}
		});
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

	 
}
