package onight.zippo.oparam.etcd;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdKeysResponse;

public class FutureWP implements Future<String> {

	EtcdResponsePromise<EtcdKeysResponse> promise;
	boolean isCancelled = false;

	public FutureWP(EtcdResponsePromise<EtcdKeysResponse> promise) {
		super();
		this.promise = promise;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return promise.getNettyPromise().cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return promise.getNettyPromise().isCancelled();
	}

	@Override
	public boolean isDone() {
		return promise.getNettyPromise().isDone();
	}

	@Override
	public String get() throws InterruptedException, ExecutionException {
		try {
			return promise.get().getNode().getValue();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return promise.getNettyPromise().get(timeout, unit).node.value;
	}

}
