package onight.tfw.otransio.api;

import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.otransio.api.beans.FramePacket;

/**
 * 
 * @author brew
 *
 */
public interface PacketFilter {

	public String[] modules();

	public boolean preRoute(ActWrapper actor, FramePacket pack, CompleteHandler handler);

	public boolean postRoute(ActWrapper actor, FramePacket pack, CompleteHandler handler);

}
