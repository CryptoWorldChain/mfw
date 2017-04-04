package onight.tfw.oparam.api;

import java.io.IOException;
import java.util.concurrent.Future;

import onight.tfw.async.CallBack;
import onight.tfw.ojpa.api.DomainDaoSupport;

public interface OPFace extends DomainDaoSupport{

	String getHealth();

	Future<String> put(String key, String value) throws IOException;

	Future<String> putDir(String dir) throws IOException;

	Future<String> post(String key, String value) throws IOException;

	Future<String> delete(String key) throws IOException;

	Future<String> deleteDir(String dir) throws IOException;

	Future<String> get(String key) throws IOException;

	Future<String> getDir(String dir) throws IOException;

	Future<String> getAll() throws IOException;

	void watchOnce(String key, CallBack<String> cb);

	void watch(String key, CallBack<String> cb, boolean always);

}