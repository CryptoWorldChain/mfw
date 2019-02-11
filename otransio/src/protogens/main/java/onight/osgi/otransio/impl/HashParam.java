package onight.osgi.otransio.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import onight.tfw.async.CallBack;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.oparam.api.OPFace;
import onight.tfw.oparam.api.OParam;
import onight.tfw.oparam.api.OTreeValue;

public class HashParam extends OParam {

	HashMap<String, String> map = new HashMap<>();

	public HashParam() {
		super(null);
	}

	@Override
	public void setDaosupport(DomainDaoSupport support) {

	}

	@Override
	public DomainDaoSupport getDaosupport() {
		return null;
	}

	ServiceSpec ss = new ServiceSpec("localparam");

	@Override
	public ServiceSpec getServiceSpec() {
		return ss;
	}

	@Override
	public String getDomainName() {
		return "localparam";
	}

	@Override
	public Class<?> getDomainClazz() {
		return String.class;
	}

	@Override
	public String getHealth() {
		return "OK";
	}

	@Override
	public Future<OTreeValue> put(String key, String value) throws IOException {
		OTreeValue v = new OTreeValue(key, value, null, 0, 0);
		map.put(key, value);
		return ConcurrentUtils.constantFuture(v);
	}

	@Override
	public Future<OTreeValue> compareAndSwap(String key, String value, String compareValue) throws IOException {
		synchronized (map) {
			String v = map.get(key);
			if (StringUtils.equals(v, compareValue))
				return put(key, value);
		}
		return null;
	}

	@Override
	public Future<OTreeValue> compareAndDelete(String key, String compareValue) throws IOException {
		synchronized (map) {
			String v = map.get(key);
			if (StringUtils.equals(v, compareValue)) {
				map.remove(key);
				return ConcurrentUtils.constantFuture(new OTreeValue(key, v, null, 0, 0));
			}
		}
		return null;
	}

	@Override
	public Future<OTreeValue> putDir(String dir) throws IOException {
		return put(dir, "");
	}

	@Override
	public Future<OTreeValue> post(String key, String value) throws IOException {
		return put(key, value);
	}

	@Override
	public Future<OTreeValue> delete(String key) throws IOException {
		synchronized (map) {
			String v = map.get(key);
			if (v != null) {
				map.remove(key);
				return ConcurrentUtils.constantFuture(new OTreeValue(key, v, null, 0, 0));
			}
		}
		return null;
	}

	@Override
	public Future<OTreeValue> deleteDir(String dir) throws IOException {
		return delete(dir);
	}

	@Override
	public Future<OTreeValue> get(String key) throws IOException {
		synchronized (map) {
			String v = map.get(key);
			if (v != null) {
				return ConcurrentUtils.constantFuture(new OTreeValue(key, v, null, 0, 0));
			}
		}
		return null;
	}

	@Override
	public Future<OTreeValue> getDir(String dir) throws IOException {
		return get(dir);
	}

	@Override
	public Future<OTreeValue> getAll() throws IOException {
		throw new RuntimeException("NOT_SUPPORT");
	}

	@Override
	public void watchOnce(String key, CallBack<OTreeValue> cb) {
		throw new RuntimeException("NOT_SUPPORT");
	}

	@Override
	public void watch(String key, CallBack<OTreeValue> cb, boolean always) {
		throw new RuntimeException("NOT_SUPPORT");
	}

}
