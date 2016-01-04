package onight.tfw.otransio.api;

import onight.tfw.async.CallBack;
import onight.tfw.otransio.api.beans.FramePacket;


public interface IPacketSender {

	/*
	 * 发送一个消息出去,同步等待
	 */
	public FramePacket send(FramePacket fp,long timeoutMS);
	
	/* 
	 * 异步发送
	 */
	public void asyncSend(FramePacket fp,CallBack<FramePacket> cb);

	
	public void post(FramePacket fp);

}
