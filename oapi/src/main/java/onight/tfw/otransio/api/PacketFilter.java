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
public interface PacketFilter {

	public String[] modules();

	public int getPriority();

	public String getSimpleName();

	public void init(FilterConfig filterConfig) throws FilterException;

	public void destroy(FilterConfig filterConfig) throws FilterException;

	public boolean preRoute(ActWrapper actor, FramePacket pack, CompleteHandler handler) throws FilterException;

	public boolean postRoute(ActWrapper actor, FramePacket pack, CompleteHandler handler) throws FilterException;

	public boolean onComplete(ActWrapper actor, FramePacket completepack) throws FilterException;

}
