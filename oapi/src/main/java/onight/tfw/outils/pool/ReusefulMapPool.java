package onight.tfw.outils.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.Getter;

public class ReusefulMapPool<K, T> {

	@Getter
	ConcurrentHashMap<K, T> allObjMaps = new ConcurrentHashMap<>();
	@Getter
	ConcurrentLinkedQueue<T> allObjs = new ConcurrentLinkedQueue<>();

	ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();

	public void addObject(K key, T value) {
		rwlock.writeLock().lock();
		try {
			if (!allObjMaps.contains(key)) {
				allObjMaps.put(key, value);
				allObjs.add(value);
			}
		} finally {
			rwlock.writeLock().unlock();
		}
	}
	
	public T removeByKey(K key) {
		rwlock.writeLock().lock();
		try {
			T v = allObjMaps.get(key);
			if (v != null) {
				allObjMaps.remove(key);
				allObjs.remove(v);
			}
			return v;
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	public T get() {
		rwlock.writeLock().lock();
		try {
			T t = allObjs.poll();
			if (t != null) {
				allObjs.offer(t);
			}
			return t;
		} finally {
			rwlock.writeLock().unlock();
		}
	}

	public T getByKey(String key) {
		rwlock.readLock().lock();
		try {
			return allObjMaps.get(key);
		} finally {
			rwlock.readLock().unlock();
		}
	}

	public int size() {
		return allObjs.size();
	}

}
