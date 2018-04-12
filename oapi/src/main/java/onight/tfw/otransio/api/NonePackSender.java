package onight.tfw.otransio.api;

import onight.tfw.async.CallBack;
import onight.tfw.async.OFuture;
import onight.tfw.otransio.api.beans.FramePacket;

public class NonePackSender implements IPacketSender {

	@Override
	public FramePacket send(FramePacket fp, long timeoutMS) {
		return fp;
	}

	@Override
	public void asyncSend(FramePacket fp, CallBack<FramePacket> cb) {
		// do nothing..
		cb.onSuccess(fp);
	}

	@Override
	public void post(FramePacket fp) {
		//do nothing..
//		throw new MessageException("No Sender Found");
	}

	@Override
	public OFuture<FramePacket> asyncSend(FramePacket fp) {
//		throw new MessageException("No Sender Found");
		return new OFuture<FramePacket>(fp);
	}

	@Override
	public void tryDropConnection(String dest) {
//		throw new MessageException("No Sender Found");
	}

	@Override
	public void changeNodeName(String oldname, String newname) {
//		throw new MessageException("No Sender Found");
	}

	@Override
	public void setCurrentNodeName(String name) {
//		throw new MessageException("No Sender Found");
		
	}

	@Override
	public void setDestURI(String dest, String uri) {
		
	}

}
