package org.fc.zippo.dispatcher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;

import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.PBActor;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;

public class ForkJoinDispatcher implements IActorDispatcher {

	static PropHelper prop = new PropHelper(null);

	static ForkJoinPool defaultPool = new ForkJoinPool(
			prop.get("org.zippo.threadpool.exec.size", Runtime.getRuntime().availableProcessors() * 4));
	static ScheduledThreadPoolExecutor sch = new ScheduledThreadPoolExecutor(
			prop.get("org.zippo.threadpool.scheduler.size", Runtime.getRuntime().availableProcessors()));

	@Override
	public void scheduleWithFixedDelaySecond(Runnable run, long initialDelay, long period) {
		sch.scheduleWithFixedDelay(run, initialDelay, period, TimeUnit.SECONDS);
	}

	@Override
	public void post(final FramePacket pack, final CompleteHandler handler, final PBActor<Message> sm) {
		defaultPool.execute(new Runnable() {
			@Override
			public void run() {
				sm.onPacket(pack, handler);
			}
		});
	}

	@Override
	public void destroy() {
		defaultPool.shutdown();
		sch.shutdown();
	}

	@Override
	public void init() {

	}

	@Override
	public void post(FramePacket pack, Runnable runner) {
		defaultPool.submit(runner);
	}

	@Override
	public void executeNow(final FramePacket pack, final CompleteHandler handler, final PBActor<Message> sm) {
		defaultPool.execute(new Runnable() {
			@Override
			public void run() {
				sm.onPacket(pack, handler);
			}
		});
	}

	@Override
	public void executeNow(FramePacket pack, Runnable runner) {
		defaultPool.execute(runner);

	}

	@Override
	public void postWithTimeout(final FramePacket pack, final CompleteHandler handler, final PBActor<Message> sm,
			final long timeoutMS) {
		TimeLimitRunner runner = new TimeLimitRunner(sch, timeoutMS, handler) {
			@Override
			public void runOnce() {
				sm.onPacket(pack, handler);
			}
		};
		defaultPool.submit(runner);

	}

	@Override
	public void postWithTimeout(FramePacket pack, final Runnable runner, long timeoutMS, CompleteHandler timeoutCB) {
		TimeLimitRunner tlrunner = new TimeLimitRunner(sch, timeoutMS, timeoutCB) {
			@Override
			public void runOnce() {
				runner.run();
			}
		};
		defaultPool.submit(tlrunner);
	}

	@Override
	public void executeNowWithTimeout(final FramePacket pack, final CompleteHandler handler, final PBActor<Message> sm,
			long timeoutMS) {
		TimeLimitRunner runner = new TimeLimitRunner(sch, timeoutMS, handler) {
			@Override
			public void runOnce() {
				sm.onPacket(pack, handler);
			}
		};
		defaultPool.execute(runner);
	}

	@Override
	public void executeNowWithTimeout(FramePacket pack, final Runnable runner, long timeoutMS,
			CompleteHandler timeoutCB) {
		TimeLimitRunner tlrunner = new TimeLimitRunner(sch, timeoutMS, timeoutCB) {
			@Override
			public void runOnce() {
				runner.run();
			}
		};
		defaultPool.execute(tlrunner);
	}

	@Override
	public ExecutorService getExecutorService(String gcmd) {
		return defaultPool;
	}

}
