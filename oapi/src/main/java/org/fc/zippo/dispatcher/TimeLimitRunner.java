package org.fc.zippo.dispatcher;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.Data;
import onight.tfw.async.CompleteHandler;

@Data
public abstract class TimeLimitRunner implements Runnable {

	protected boolean finished = false;
	ScheduledThreadPoolExecutor sch;
	long timeoutMS = 1L;

	@Override
	public void run() {
		try {
			if (!finished) {
				runOnce();
			}
		} catch (Throwable t) {
			finished = true;
		}
	}

	public abstract void runOnce();

	public TimeLimitRunner(ScheduledThreadPoolExecutor sch, final long timeoutMS, final CompleteHandler ch) {
		this(sch, timeoutMS, ch, true);
	}

	public TimeLimitRunner(ScheduledThreadPoolExecutor sch, final long timeoutMS, final CompleteHandler ch,
			final boolean norunWhenTimeout) {
		super();
		this.sch = sch;
		this.timeoutMS = Math.max(1L, timeoutMS);
		sch.schedule(new Runnable() {
			@Override
			public void run() {
				if (!finished) {
					if (norunWhenTimeout) {
						finished = true;// timeout,就不处理了
					}
					ch.onFailed(new TimeoutException("Timeout-" + timeoutMS));
				}
			}
		}, this.timeoutMS, TimeUnit.MILLISECONDS);
	}
}
