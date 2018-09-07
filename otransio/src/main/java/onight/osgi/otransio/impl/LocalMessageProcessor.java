package onight.osgi.otransio.impl;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.FilterManager;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.session.PSession;
import onight.tfw.outils.pool.ReusefulLoopPool;

@Slf4j
@Data
public class LocalMessageProcessor {

	ForkJoinPool exec;
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
			// if (fm != null && pack != null) {
			// try {
			// fm.preRouteListner(null, pack, handler);
			// } catch (Exception e) {
			// log.error("error in prerouter message:" +
			// pack.getModuleAndCMD());
			// }
			// }
			try {
				ms.onPacket(pack, handler);
			} catch (Exception e) {
				log.debug("error in runing local onPacket:" + e.getMessage(), e);
			} finally {
				if (future != null) {
					future.result("F");
				}
				// if (fm != null && pack != null) {
				// try {
				// fm.postRouteListner(null, pack, handler);
				// } catch (Exception e) {
				// log.error("error in prerouter message:" +
				// pack.getModuleAndCMD() + ",erro=" + e.getMessage(),
				// e);
				// }
				// }

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
			long startTime = System.currentTimeMillis();
			FutureImpl<String> future = Futures.createSafeFuture();
			runner.reset(pack, handler, ms, future);
			if (pack.getFixHead().getPrio() == '8' || pack.getFixHead().getPrio() == '9') {
				runner.run();
				runner = null;
			} else {
				exec.execute(runner);
				try {
					future.get(60, TimeUnit.SECONDS);
				} catch (Throwable e) {
					log.error("route Failed:" + e.getMessage() + ",GCMD=" + pack.getFixHead().getCmd()
							+ pack.getFixHead().getModule() + ",realcost=" + (System.currentTimeMillis() - startTime)
							+ ",queue=" + exec.getQueuedTaskCount() + ",running=" + exec.getRunningThreadCount()
							+ ",active=" + exec.getActiveThreadCount() + ",poolsize=" + runnerPool.size()
							+ ",activepoolsize=" + runnerPool.getActiveObjs().size(), e);
					handler.onFailed(new RuntimeException(e));
				}
			}
		} else {
			runner.reset(pack, handler, ms, null);
			if (pack.getFixHead().getPrio() == '9') {
				// green
				runner.run();
				runner = null;
			} else {
				exec.execute(runner);
			}
		}
	}
}
