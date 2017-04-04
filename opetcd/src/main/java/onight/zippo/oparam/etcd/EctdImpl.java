package onight.zippo.oparam.etcd;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Provides;

import lombok.extern.slf4j.Slf4j;
import mousio.client.promises.ResponsePromise;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.responses.EtcdKeysResponse;
import onight.tfw.async.CallBack;
import onight.tfw.ojpa.api.StoreServiceProvider;

@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
@Slf4j
public class EctdImpl implements OPFace {
	EtcdClient etcd;

	public EtcdClient getEtcd() {
		return etcd;
	}

	public void setEtcd(EtcdClient etcd) {
		this.etcd = etcd;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#getHealth()
	 */
	@Override
	public String getHealth() {
		return etcd.getHealth().getHealth();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#put(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Future<String> put(String key, String value) throws IOException {
		return new FutureWP(etcd.put(key, value).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#putDir(java.lang.String)
	 */
	@Override
	public Future<String> putDir(String dir) throws IOException {
		return new FutureWP(etcd.putDir(dir).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#post(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public Future<String> post(String key, String value) throws IOException {
		return new FutureWP(etcd.post(key, value).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#delete(java.lang.String)
	 */
	@Override
	public Future<String> delete(String key) throws IOException {
		// TODO Auto-generated method stub
		return new FutureWP(etcd.delete(key).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#deleteDir(java.lang.String)
	 */
	@Override
	public Future<String> deleteDir(String dir) throws IOException {
		return new FutureWP(etcd.deleteDir(dir).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#get(java.lang.String)
	 */
	@Override
	public Future<String> get(String key) throws IOException {
		return new FutureWP(etcd.get(key).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#getDir(java.lang.String)
	 */
	@Override
	public Future<String> getDir(String dir) throws IOException {
		return new FutureWP(etcd.getDir(dir).send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#getAll()
	 */
	@Override
	public Future<String> getAll() throws IOException {
		return new FutureWP(etcd.getAll().send());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#watchOnce(java.lang.String,
	 * onight.tfw.async.CallBack)
	 */
	@Override
	public void watchOnce(final String key, final CallBack<String> cb) {
		watch(key, cb, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.etcd.OPFace#watch(java.lang.String,
	 * onight.tfw.async.CallBack, boolean)
	 */
	@Override
	public void watch(final String key, final CallBack<String> cb, final boolean always) {

		EtcdKeyGetRequest getRequest = etcd.get(key).waitForChange().timeout(60, TimeUnit.SECONDS);
		try {

			EtcdResponsePromise<EtcdKeysResponse> promise = getRequest.send();
			promise.addListener(new ResponsePromise.IsSimplePromiseResponseHandler<EtcdKeysResponse>() {
				@Override
				public void onResponse(ResponsePromise<EtcdKeysResponse> response) {
					try {
						cb.onSuccess(response.get().getNode().value);
					} catch (Exception e) {
						cb.onFailed(e, key);
					} finally {
						if (always) {
							// still watch
							EctdImpl.this.watch(key, cb, always);
						}
					}
				}

			});
		} catch (Exception e) {
			cb.onFailed(e, key);
		}
	}

}
