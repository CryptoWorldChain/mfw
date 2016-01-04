package onight.tfw.otransio.api;

import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

/**
 * 
 * @author brew
 *
 */
public interface PacketFilter {

	public String[] modules();

	public boolean preRoute(String module, FramePacket pack, CompleteHandler handler);

	public boolean postRoute(String module, FramePacket pack, CompleteHandler handler);

}
