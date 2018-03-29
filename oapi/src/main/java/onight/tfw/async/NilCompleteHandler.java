package onight.tfw.async;

import onight.tfw.otransio.api.beans.FramePacket;

public class NilCompleteHandler implements CompleteHandler {

	public void onFinished(FramePacket packet) {

	}
	
	public static final NilCompleteHandler handler=new NilCompleteHandler();

	@Override
	public void onFailed(Exception e) {
		// TODO Auto-generated method stub
		
	}

	
	
	
	

}
