package onight.osgi.otransio.impl;

import java.util.concurrent.TimeUnit;

import org.fc.zippo.dispatcher.IActorDispatcher;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

import com.google.protobuf.Message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.PBActor;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.PSession;
import onight.tfw.outils.pool.ReusefulLoopPool;

@Slf4j
@Data
public class LocalMessageProcessor {

	// ForkJoinPool exec;
	IActorDispatcher dispatcher;
	int poolSize = 1000;

	class Runner implements Runnable {
		FramePacket pack;
		CompleteHandler handler;
		PSession ms;
		FutureImpl<String> future;

		public void reset(FramePacket pack, CompleteHandler handler, PSession ms, FutureImpl<String> future) {
			this.pack = pack;
			this.handler = handler;
			this.ms = ms;
			this.future = future;
		}

		public void release() {

		}

		@Override
		public void run() {
			try {
				ms.onPacket(pack, handler);
			} catch (Exception e) {
				log.debug("error in runing local onPacket:" + e.getMessage(), e);
				handler.onFailed(e);
			} finally {
				if (future != null) {
					future.result("F");
				}
				if (runnerPool.size() < poolSize) {
					this.reset(null, null, null, null);
					runnerPool.retobj(this);
				}
			}
		}

	}

	ReusefulLoopPool<Runner> runnerPool = new ReusefulLoopPool<>();

	public void route2Local(FramePacket pack, CompleteHandler handler, PSession ms) {
		// String packid = null;
		Runner runner = runnerPool.borrow();
		if (runner == null) {
			runner = new Runner();
		}
		if (pack.isSync()) {
			FutureImpl<String> future = Futures.createSafeFuture();
			runner.reset(pack, handler, ms, future);
			if (pack.getFixHead().getPrio() == '8' || pack.getFixHead().getPrio() == '9') {
				dispatcher.executeNow(runner.pack, runner);
				runner = null;
			} else {
				dispatcher.postWithTimeout(runner.pack, runner, 60 * 1000, runner.handler);
			}
		} else {
			runner.reset(pack, handler, ms, null);
			if (pack.getFixHead().getPrio() == '8' || pack.getFixHead().getPrio() == '9') {
				dispatcher.executeNow(runner.pack, runner);
			} else {
				dispatcher.post(runner.pack, runner);
			}
		}
	}
}
