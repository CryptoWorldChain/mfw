package onight.tfw.async;

import onight.tfw.otransio.api.beans.FramePacket;

public interface CompleteHandler {

	public void onFinished(FramePacket packet);
	public void onFailed(Exception e);
	
	
}
