package onight.tfw.async;

import onight.tfw.otransio.api.beans.FramePacket;

public class NoneCallBack implements CallBack<FramePacket> {

	public void onSuccess(FramePacket v) {

	}

	public void onFailed(Exception e, FramePacket v) {

	}
	
	public final static NoneCallBack cb=new NoneCallBack();
}
