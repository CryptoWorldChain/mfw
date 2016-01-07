package onight.tfw.async;

public class FutureScheduler {

	public void observed(OFuture future, CallBack cb) {
		future.cbs.add(cb);
	}
}
