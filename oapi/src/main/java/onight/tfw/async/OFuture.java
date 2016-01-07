package onight.tfw.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.Getter;
import onight.tfw.otransio.api.beans.FramePacket;

public class OFuture<V> {

	@Getter
	V result;
	boolean failed;
	Exception e;
	ReadWriteLock rwlock = new ReentrantReadWriteLock();
	List<CallBack<V>> cbs;

	public OFuture(V result) {
		super();
		this.result = result;
		cbs = new ArrayList<CallBack<V>>();
	}

	public OFuture() {
		super();
		cbs = new ArrayList<CallBack<V>>();
	}

	public void result(V v) {
		rwlock.writeLock().lock();
		try {
			failed = false;
			result = v;
			if (cbs != null) {
				for (CallBack<V> cb : cbs) {
					cb.onSuccess(v);
				}
				cbs.clear();
			}
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	public void failed(Exception e, V v) {
		rwlock.writeLock().lock();
		try {
			this.e = e;
			failed = true;
			result = v;
			for (CallBack<V> cb : cbs) {
				cb.onFailed(e, v);
			}
			cbs.clear();
		} finally {
			rwlock.writeLock().unlock();
		}

	}

	public void cancel(V v) {
		failed(new CancelException("cancel"), v);
	}

	public OFuture<V> whenDone(CallBack<V> cb) {
		if (result != null) {
			if (failed) {
				failed(e, result);
			} else {
				cb.onSuccess(result);
			}
		} else {
			rwlock.writeLock().lock();
			if (cbs != null) {
				try {
					cbs.add(cb);
				} finally {
					rwlock.writeLock().unlock();
				}
			}
		}
		return this;
	}

	public OFuture<V> withComplete(final CompleteHandler handler) {
		whenDone(new CallBack<V>() {
			@Override
			public void onSuccess(V v) {
				handler.onFinished((FramePacket) v);
			}

			@Override
			public void onFailed(Exception e, V v) {
				handler.onFinished((FramePacket) v);
			}
		});
		return this;
	}

}
