package onight.tfw.oparam.api;

import java.io.IOException;
import java.util.concurrent.Future;

import onight.tfw.async.CallBack;
import onight.tfw.ojpa.api.DomainDaoSupport;

public interface OPFace extends DomainDaoSupport{

	String getHealth();

	Future<OTreeValue> put(String key, String value) throws IOException;

	Future<OTreeValue> putDir(String dir) throws IOException;

	Future<OTreeValue> post(String key, String value) throws IOException;

	Future<OTreeValue> delete(String key) throws IOException;

	Future<OTreeValue> deleteDir(String dir) throws IOException;

	Future<OTreeValue> get(String key) throws IOException;

	Future<OTreeValue> getDir(String dir) throws IOException;

	Future<OTreeValue> getAll() throws IOException;

	void watchOnce(String key, CallBack<OTreeValue> cb);

	void watch(String key, CallBack<OTreeValue> cb, boolean always);

}