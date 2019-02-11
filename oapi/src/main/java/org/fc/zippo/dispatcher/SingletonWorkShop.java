package org.fc.zippo.dispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.conf.PropHelper;

@Data
@Slf4j
public abstract class SingletonWorkShop<T> implements Runnable {

	protected boolean finished = false;
	long timeoutMS = 1000L;

	protected PropHelper prop = new PropHelper(null);

	public abstract boolean isRunning();

	public abstract void runBatch(List<T> items);

	protected LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>();

	public String getName() {
		return "default";
	}

	protected int maxQueueSize = prop.get("org.zippo.ddc.singleton." + getName() + ".queue.size", 1000);
	protected int pollMaxWaitMS = prop.get("org.zippo.ddc.singleton." + getName() + "poll.max.wait.ms", 60 * 1000);
	protected int pollMinWaitMS = prop.get("org.zippo.ddc.singleton." + getName() + "poll.min.wait.ms", 50);
	protected int batchSize = prop.get("org.zippo.ddc.singleton" + getName() + ".batch.size", 100);

	protected AtomicLong counter = new AtomicLong(0);

	public void offerMessage(T t) throws QueueOverflowException {
		if (counter.get() < maxQueueSize) {
			counter.incrementAndGet();
			queue.offer(t);
		} else {
			onMessageOverFlow(t);
		}
	}

	public void startup(ExecutorService exec) {
		exec.submit(this);
	}

	public void clearQueue() {
		queue.clear();
	}

	protected void onMessageOverFlow(T t) throws QueueOverflowException {
		throw new QueueOverflowException("Overflow");
	}

	protected int getLoopMaxWaitMS() {
		return pollMaxWaitMS;
	}

	protected int getLoopMinWaitMS() {
		return pollMinWaitMS;
	}

	@Override
	public void run() {
		try {
			ArrayList<T> procList = new ArrayList<>();
			while (isRunning()) {
				T t = queue.poll(getLoopMaxWaitMS(), TimeUnit.MILLISECONDS);
				for (int i = 0; i < batchSize && t != null; i++) {
					procList.add(t);
					counter.decrementAndGet();
					if (i < batchSize - 1) {
						t = queue.poll(getLoopMinWaitMS(), TimeUnit.MILLISECONDS);
					}
				}
				if (procList.size() > 0) {
					try {
						runBatch(procList);
					} catch (Throwable th) {
						log.warn("error in runing batch:" + procList.size(), th);
					} finally {
						procList.clear();
					}
				}
			}
		} catch (java.lang.InterruptedException e) {

		} catch (Throwable t) {
			log.error("error in run timelimit runner:", t);
		} finally {
			finished = true;
		}
	}

}
