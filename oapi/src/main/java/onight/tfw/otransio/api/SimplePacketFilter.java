package onight.tfw.otransio.api;

import org.fc.zippo.filter.FilterConfig;
import org.fc.zippo.filter.exception.FilterException;

import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.otransio.api.beans.FramePacket;

/**
 * 
 * @author brew
 *
 */
public class SimplePacketFilter implements PacketFilter {

	@Override
	public String[] modules() {
		return null;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public String getSimpleName() {
		return null;
	}

	@Override
	public void init(FilterConfig filterConfig) throws FilterException {

	}

	@Override
	public void destroy(FilterConfig filterConfig) throws FilterException {

	}

	@Override
	public boolean preRoute(ActWrapper actor, FramePacket pack, CompleteHandler handler) throws FilterException {
		return true;
	}

	@Override
	public boolean postRoute(ActWrapper actor, FramePacket pack, CompleteHandler handler) throws FilterException {
		return true;
	}

}
