package onight.tfw.ntrans.api;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

public interface FilterManager {

	public abstract boolean preRouteListner(String module, FramePacket pack, CompleteHandler handler);

	public abstract boolean postRouteListner(String module, FramePacket pack, CompleteHandler handler);

}