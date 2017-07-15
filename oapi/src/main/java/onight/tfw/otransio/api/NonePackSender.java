package onight.tfw.otransio.api;

import onight.tfw.async.CallBack;
import onight.tfw.async.OFuture;
import onight.tfw.otransio.api.beans.FramePacket;

public class NonePackSender implements IPacketSender {

	@Override
	public FramePacket send(FramePacket fp, long timeoutMS) {
		throw new MessageException("No Sender Found");
	}

	@Override
	public void asyncSend(FramePacket fp, CallBack<FramePacket> cb) {
		throw new MessageException("No Sender Found");
	}

	@Override
	public void post(FramePacket fp) {
		throw new MessageException("No Sender Found");
	}

	@Override
	public OFuture<FramePacket> asyncSend(FramePacket fp) {
		throw new MessageException("No Sender Found");
	}

}
