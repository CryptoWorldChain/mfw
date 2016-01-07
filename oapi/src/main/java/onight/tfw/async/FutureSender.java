package onight.tfw.async;

import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.beans.FramePacket;

public abstract class FutureSender implements IPacketSender {

	public FutureSender() {
		super();
	}

	/*
	 * 异步发送
	 */
	public OFuture<FramePacket> asyncSend(FramePacket fp) {
		final OFuture<FramePacket> future = new OFuture<FramePacket>();
		asyncSend(fp, new CallBack<FramePacket>() {
			@Override
			public void onSuccess(FramePacket v) {
				future.result(v);
			}

			@Override
			public void onFailed(Exception e, FramePacket v) {
				future.failed(e, v);
			}
		});
		return future;
	}

}
