
package org.fc.zippo.dispatcher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;

import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.PBActor;
import onight.tfw.otransio.api.beans.FramePacket;

/**
 * for actor scheduler and dispatcher central
 * 
 * @author brew
 *
 */

public interface IActorDispatcher {

	public ExecutorService getExecutorService(String gcmd);

	public void post(FramePacket pack, CompleteHandler handler, PBActor<Message> sm);

	public void postWithTimeout(FramePacket pack, CompleteHandler handler, PBActor<Message> sm, long timeoutMS);

	public void scheduleWithFixedDelaySecond(Runnable run, long initialDelay, long period);

	public void scheduleWithFixedDelay(Runnable run, long initialDelay, long period, TimeUnit timeunit);

	public void destroy();

	public boolean isRunning();
	
	public void post(FramePacket pack, Runnable runner);

	/**
	 * 超时通知
	 * 
	 * @param pack
	 * @param runner
	 * @param timeoutMS
	 * @param timeoutCB
	 */
	public void postWithTimeout(FramePacket pack, Runnable runner, long timeoutMS, CompleteHandler timeoutCB);

	/*
	 * 尽快执行这个
	 */
	public void executeNow(FramePacket pack, CompleteHandler handler, PBActor<Message> sm);

	public void executeNowWithTimeout(FramePacket pack, CompleteHandler handler, PBActor<Message> sm, long timeoutMS);

	/*
	 * 尽快执行这个
	 */
	public void executeNow(FramePacket pack, Runnable runner);

	/**
	 * 加上超时控制
	 * 
	 * @param pack
	 * @param runner
	 * @param timeoutMS
	 * @param timeoutCB
	 */
	public void executeNowWithTimeout(FramePacket pack, Runnable runner, long timeoutMS, CompleteHandler timeoutCB);

	public void init();
}
