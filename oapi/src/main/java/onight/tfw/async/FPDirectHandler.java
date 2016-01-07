package onight.tfw.async;

import onight.tfw.otransio.api.beans.FramePacket;

public abstract class FPDirectHandler implements CallBack<FramePacket> {

	CompleteHandler nextHandler;

	public FPDirectHandler(CompleteHandler nextHandler) {
		super();
		this.nextHandler = nextHandler;
	}

	public abstract FramePacket whenSuccess(FramePacket v);

	@Override
	public void onSuccess(FramePacket v) {
		nextHandler.onFinished(whenSuccess(v));
	}

	public void onFailed(Exception e, FramePacket v) {
		nextHandler.onFinished(v);
	}

}
