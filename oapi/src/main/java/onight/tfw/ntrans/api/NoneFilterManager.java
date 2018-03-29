package onight.tfw.ntrans.api;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

public class NoneFilterManager implements FilterManager {

	@Override
	public boolean preRouteListner(ActWrapper actor, FramePacket pack, CompleteHandler handler) {
		return true;
	}

	@Override
	public boolean postRouteListner(ActWrapper actor, FramePacket pack, CompleteHandler handler) {
		return true;
	}

	@Override
	public boolean onCompleteListner(ActWrapper actor, FramePacket pack) {
		return false;
	}

	@Override
	public boolean onErrorListner(ActWrapper actor, Exception e) {
		// TODO Auto-generated method stub
		return false;
	}

}
