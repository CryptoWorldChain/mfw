package onight.tfw.ntrans.api;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

public interface FilterManager {

	public abstract boolean preRouteListner(ActWrapper actor, FramePacket pack, CompleteHandler handler);

	public abstract boolean postRouteListner(ActWrapper actor, FramePacket pack, CompleteHandler handler);
	
	public abstract boolean onCompleteListner(ActWrapper actor, FramePacket pack);

}