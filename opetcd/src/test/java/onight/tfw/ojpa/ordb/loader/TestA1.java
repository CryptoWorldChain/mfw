package onight.tfw.ojpa.ordb.loader;

import java.io.IOException;
import java.util.concurrent.Future;

import mousio.etcd4j.EtcdClient;
import onight.zippo.oparam.etcd.FutureWP;

public class TestA1 {
	EtcdClient etcd = new EtcdClient();

	public static void main(String[] args) {
		System.out.println("hello:");
		// EtcdClient etcd = new EtcdClient();/
	}

	public String getHealth() {
		return etcd.getHealth().getHealth();
	}

	public Future<String> put(String key, String value) throws IOException {
		return new FutureWP(etcd.put(key, value).send());
	}

	public Future<String> putDir(String dir) throws IOException {
		return new FutureWP(etcd.putDir(dir).send());
	}

	public Future<String> post(String key, String value) throws IOException {
		return new FutureWP(etcd.post(key, value).send());
	}

	public Future<String> delete(String key) throws IOException {
		// TODO Auto-generated method stub
		return new FutureWP(etcd.delete(key).send());
	}

	public Future<String> deleteDir(String dir) throws IOException {
		return new FutureWP(etcd.deleteDir(dir).send());
	}

	public Future<String> get(String key) throws IOException {
		return new FutureWP(etcd.get(key).send());
	}

	public Future<String> getDir(String dir) throws IOException {
		return new FutureWP(etcd.getDir(dir).send());
	}

	public Future<String> getAll() throws IOException {
		return new FutureWP(etcd.getAll().send());
	}

}
