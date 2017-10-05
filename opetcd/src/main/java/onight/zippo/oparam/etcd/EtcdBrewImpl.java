package onight.zippo.oparam.etcd;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.Future;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.zippo.oparam.etcd.EtcdMembersResponse;
import onight.tfw.async.CallBack;
import onight.tfw.mservice.ThreadContext;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.oparam.api.OPFace;
import onight.tfw.oparam.api.OTreeValue;
import onight.tfw.outils.serialize.JsonSerializer;

@Slf4j
public class EtcdBrewImpl implements OPFace, DomainDaoSupport {

	@Setter
	@Getter
	HttpRequestor req;

	@Setter
	@Getter
	int default_ttl = 99999999;

	@Setter
	@Getter
	String rootPath = "fbs";

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#getHealth()
	 */
	@Override
	public String getHealth() {
		Object obj = ThreadContext.getContext("iscluster");
		if (obj != null && obj instanceof Boolean) {
			if ((Boolean) obj) {
				try {
					return JsonSerializer.formatToString(JsonSerializer.getInstance()
							.deserialize(req.get("/v2/members"), EtcdMembersResponse.class).getMembers());
				} catch (Exception e) {
				}
			}
		}

		return "{\"health\": \"true\"}";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#put(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Future<OTreeValue> put(String key, String value) throws IOException {
		return new FutureBrew(req.put(URLEncoder.encode(value, "UTF-8"), "/v2/keys" + rootPath + key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#putDir(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> putDir(String dir) throws IOException {
		return new FutureBrew(req.put("dir=true", "/v2/keys" + rootPath + dir));

		// return new
		// FutureWP(etcd.putDir(dir).timeout(ThreadContext.getContextInt("wait.timeout",
		// 60), TimeUnit.SECONDS)
		// .ttl(ThreadContext.getContextInt("ttl", default_ttl)).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#post(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Future<OTreeValue> post(String key, String value) throws IOException {
		return new FutureBrew(req.post(URLEncoder.encode(value, "UTF-8"), "/v2/keys" + rootPath + key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#delete(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> delete(String key) throws IOException {
		return new FutureBrew(req.delete("/v2/keys" + rootPath + key + "?recursive=true"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#deleteDir(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> deleteDir(String dir) throws IOException {
		return new FutureBrew(req.delete("/v2/keys" + rootPath + dir + "?recursive=true&dir=true"));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#get(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> get(String key) throws IOException {
		if (ThreadContext.getContextInt("sorted", 0) == 1) {
			return new FutureBrew(req.get("/v2/keys" + rootPath + key + "?recursive=true&sorted=true"));
		}
		return new FutureBrew(req.get("/v2/keys" + rootPath + key + "?recursive=true"));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#getDir(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> getDir(String dir) throws IOException {
		return get(dir);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#getAll()
	 */
	@Override
	public Future<OTreeValue> getAll() throws IOException {
		return get("/v2/keys/" + rootPath + "?recursive=true");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#watchOnce(java.lang.String,
	 * onight.tfw.async.CallBack)
	 */
	@Override
	public void watchOnce(final String key, final CallBack<OTreeValue> cb) {
		watch(key, cb, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#watch(java.lang.String,
	 * onight.tfw.async.CallBack, boolean)
	 */
	boolean shutdown = false;

	@Override
	public void watch(final String key, final CallBack<OTreeValue> cb, final boolean always) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!shutdown) {
					try {
						try {
							Future<OTreeValue> ret;
							if (ThreadContext.getContextInt("sorted", 0) == 1) {
								ret = new FutureBrew(
										req.get("/v2/keys" + rootPath + key + "?recursive=true&sorted=true&wait=true"));
							}
							ret = new FutureBrew(req.get("/v2/keys" + rootPath + key + "?recursive=true&wait=true"));

							cb.onSuccess(ret.get());
						} catch (Exception e) {
							cb.onFailed(e, null);
						}
					} catch (Throwable e) {
					} finally {

					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

	}

	@Override
	public DomainDaoSupport getDaosupport() {
		return this;
	}

	@Override
	public Class<?> getDomainClazz() {
		return Object.class;
	}

	@Override
	public String getDomainName() {
		return "etcd";
	}

	@Override
	public ServiceSpec getServiceSpec() {
		return ServiceSpec.ETCD_STORE;
	}

	@Override
	public void setDaosupport(DomainDaoSupport dao) {
		log.trace("setDaosupport::dao=" + dao);
	}

	@Override
	public Future<OTreeValue> compareAndDelete(String key, String value) throws IOException {
		return new FutureBrew(req.delete(
				"/v2/keys" + rootPath + key + "?recursive=true&prevValue=" + URLEncoder.encode(value, "UTF-8")));
	}

	@Override
	public Future<OTreeValue> compareAndSwap(String key, String newvalue, String comparevalue) throws IOException {
		if (comparevalue == null) {
			return new FutureBrew(req.put(URLEncoder.encode(newvalue, "UTF-8")+"", "/v2/keys" + rootPath + key+"?prevExist=false"));
		}
		return new FutureBrew(req.put(URLEncoder.encode(newvalue, "UTF-8"),
				"/v2/keys" + rootPath + key + "?prevValue=" + URLEncoder.encode(comparevalue, "UTF-8")));
	}

}
