package onight.tfw.ntrans.api;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

public class NoneFilterManager implements FilterManager {

	@Override
	public boolean preRouteListner(String module, FramePacket pack, CompleteHandler handler) {
		return false;
	}

	@Override
	public boolean postRouteListner(String module, FramePacket pack, CompleteHandler handler) {
		return false;
	}

}
