package onight.tfw.ojpa.ordb;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.outils.conf.PropHelper;
import onight.zippo.oparam.etcd.EtcdBrewImpl;
import onight.zippo.oparam.etcd.HttpRequestor;

@Component(immediate = true)
@Instantiate()
@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
@Slf4j
public class BrewEtcdProvider implements StoreServiceProvider {

	@Override
	public String getProviderid() {
		return ServiceSpec.ETCD_STORE.getTarget();
	}

	BundleContext bundleContext;
	PropHelper params;
	EtcdBrewImpl etcdImpl = new EtcdBrewImpl();

	public URI[] getURI(String urilist) {
		try {
			String strarray[] = urilist.split(",");
			URI uriarray[] = new URI[strarray.length];
			int i = 0;
			for (String str : strarray) {
				uriarray[i++] = new URI(str.trim());
			}
			return uriarray;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public BrewEtcdProvider(BundleContext bundleContext) {
		super();
		params = new PropHelper(bundleContext);
		this.bundleContext = bundleContext;

	}

	@Validate
	public synchronized void startup() {
		log.info("启动中...@" + bundleContext);
		log.debug("create:EtcdImpl:");
		String username = params.get("org.zippo.etcd.username", null);
		String passwd = params.get("org.zippo.etcd.passwd", null);
		String uris = params.get("org.zippo.etcd.uris", "http://127.0.0.1:2379");
		String ssluris = params.get("org.zippo.etcd.ssluris", null);
		try {
			HttpRequestor req = new HttpRequestor();
			req.setUrlbase(uris);
			req.reload();

			req.changeMaxPerRoute(params.get("org.zippo.etcd.maxpreroute", 100));
			req.changeMaxTotal(params.get("org.zippo.etcd.maxtotal", 100));

			etcdImpl.setReq(req);
			String rootpath = params.get("org.zippo.bc.org", "fbs");
			if (rootpath.endsWith("/")) {
				rootpath = rootpath.substring(0, rootpath.length() - 1);
			}
			if (!rootpath.startsWith("/")) {
				rootpath = "/" + rootpath;
			}
			etcdImpl.setRootPath(rootpath);
			etcdImpl.setDefault_ttl(params.get("org.zippo.etcd.ttl", 99999999));
		} catch (Exception e) {
			log.warn("consensus start error", e);
		}
		log.info("启动完成...");
	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");

		if (etcdImpl.getReq() != null) {
			try {
				etcdImpl.getReq().destroy();
			} catch (Exception e) {
				log.warn("close etcd error", e);
			}
		}
		log.info("退出完成...");
	}

	@Override
	public synchronized DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		return etcdImpl;
	}

	@Override
	public String[] getContextConfigs() {
		return null;
	}

}
