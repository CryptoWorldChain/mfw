package onight.tfw.otransio.api;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

/**
 * 
 * @author brew
 *
 */
public interface PacketFilter {

	public boolean preRoute(FramePacket pack, CompleteHandler handler);

	public boolean postRoute(FramePacket pack, CompleteHandler handler);
	
}
