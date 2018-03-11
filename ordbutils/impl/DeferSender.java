package org.fc.hzq.orcl.impl;

import lombok.Data;
import onight.tfw.async.CallBack;
import onight.tfw.async.OFuture;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.beans.FramePacket;

@Data
public class DeferSender implements IPacketSender {

	IPacketSender sender;

	public DeferSender(IPacketSender sender) {
		super();
		this.sender = sender;
	}

	@Override
	public OFuture<FramePacket> asyncSend(FramePacket arg0) {
		// TODO Auto-generated method stub
		return sender.asyncSend(arg0);
	}

	@Override
	public void asyncSend(FramePacket arg0, CallBack<FramePacket> arg1) {
		sender.asyncSend(arg0, arg1);
	}

	@Override
	public void post(FramePacket arg0) {
		sender.post(arg0);
	}

	@Override
	public FramePacket send(FramePacket arg0, long arg1) {
		return sender.send(arg0, arg1);
	}

}
