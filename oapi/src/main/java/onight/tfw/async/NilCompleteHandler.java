package onight.tfw.async;

import onight.tfw.otransio.api.beans.FramePacket;

public class NilCompleteHandler implements CompleteHandler {

	public void onFinished(FramePacket packet) {

	}
	
	public static final NilCompleteHandler handler=new NilCompleteHandler();

	
	
	
	

}
