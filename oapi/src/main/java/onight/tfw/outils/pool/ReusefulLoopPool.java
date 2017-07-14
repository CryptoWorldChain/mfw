package onight.tfw.outils.pool;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.Getter;

public class ReusefulLoopPool<T> {

	@Getter
	ConcurrentLinkedQueue<T> allObjs = new ConcurrentLinkedQueue<>();

	ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

	public boolean addObject(T t) {
		rwlock.writeLock().lock();
		try {
			if (!allObjs.contains(t)) {
				allObjs.add(t);
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
		return	allObjs.remove(t);
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	public Iterator<T> iterator(){
		return allObjs.iterator();
	}

	public T get() {
		rwlock.writeLock().lock();
		try {
			T t = allObjs.poll();
			return t;
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	public int size(){
		return allObjs.size();
	}

}
