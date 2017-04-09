package onight.tfw.oparam.api;

import java.io.IOException;
import java.util.concurrent.Future;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;

@Data
@Slf4j
public class OParam implements OPFace {

	OPFace opimpl;

	public OParam(OPFace opimpl) {
		super();
		this.opimpl = opimpl;
	}

	Class domainClazz,exampleClazz,keyClazz;
	public OParam(ServiceSpec serviceSpec, Class domainClazz, Class exampleClazz, Class keyClazz) {
		this.serviceSpec = serviceSpec;
		this.domainClazz=domainClazz;
		this.exampleClazz=exampleClazz;
		this.keyClazz=keyClazz;
	}

	@Override
	public String getHealth() {
		return opimpl.getHealth();
	}

	@Override
	public Future<OTreeValue> put(String key, String value) throws IOException {
		return opimpl.put(key, value);
	}

	@Override
	public Future<OTreeValue> putDir(String dir) throws IOException {
		return opimpl.putDir(dir);
	}

	@Override
	public Future<OTreeValue> post(String key, String value) throws IOException {
		return opimpl.post(key, value);
	}

	@Override
	public Future<OTreeValue> delete(String key) throws IOException {
		return opimpl.delete(key);
	}

	@Override
	public Future<OTreeValue> deleteDir(String dir) throws IOException {
		return opimpl.deleteDir(dir);
	}

	@Override
	public Future<OTreeValue> get(String key) throws IOException {
		return opimpl.get(key);
	}

	@Override
	public Future<OTreeValue> getDir(String dir) throws IOException {
		return opimpl.getDir(dir);
	}

	@Override
	public Future<OTreeValue> getAll() throws IOException {
		return opimpl.getAll();
	}

	@Override
	public void watchOnce(String key, CallBack<OTreeValue> cb) {
		opimpl.watchOnce(key, cb);
	}

	@Override
	public void watch(String key, CallBack<OTreeValue> cb, boolean always) {
		opimpl.watch(key, cb, always);
	}

	@Override
	public void setDaosupport(DomainDaoSupport support) {
		if (support instanceof OPFace) {
			opimpl = (OPFace) support;
		} else {
			log.warn("cannot set OPFace from support=" + support + ": not a OPFace");
		}
	}

	private ServiceSpec serviceSpec;

	@Override
	public DomainDaoSupport getDaosupport() {
		return this;
	}

	@Override
	public String getDomainName() {
		return "oparam";
	}

	@Override
	public Class<?> getDomainClazz() {
		return domainClazz;
	}

	@Override
	public Future<OTreeValue> compareAndSwap(String key, String value, String compareValue) throws IOException {
		return opimpl.compareAndSwap(key, value, compareValue);
	}

	@Override
	public Future<OTreeValue> compareAndDelete(String key, String compareValue) throws IOException {
		// TODO Auto-generated method stub
		return opimpl.compareAndDelete(key, compareValue);
	}
}
