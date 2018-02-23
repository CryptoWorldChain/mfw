package onight.tfw.outils.pool;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.Getter;

public class ReusefulLoopPool<T> {

	@Getter
	ConcurrentHashMap<T, T> allObjs = new ConcurrentHashMap<>();

	@Getter
	ConcurrentLinkedQueue<T> activeObjs = new ConcurrentLinkedQueue<>();

	ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

	public boolean addObject(T t) {
		rwlock.writeLock().lock();
		try {
			if (!allObjs.containsKey(t)) {
				allObjs.put(t, t);
				activeObjs.offer(t);
				return true;
			}
		} finally {
			rwlock.writeLock().unlock();
		}
		return false;
	}

	public boolean removeObject(T t) {
		rwlock.writeLock().lock();
		try {
			allObjs.remove(t);
			return activeObjs.remove(t);
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	public Iterator<T> iterator() {
		return allObjs.values().iterator();
	}

	public T borrow() {
		rwlock.writeLock().lock();
		try {
			T t = activeObjs.poll();
			return t;
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	public void retobj(T t) {
		rwlock.writeLock().lock();
		try {
			activeObjs.offer(t);
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	public int size() {
		return allObjs.size();
	}

}
