package onight.tfw.ojpa.ordb;

import java.net.URI;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import io.netty.handler.ssl.SslContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mousio.etcd4j.EtcdClient;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.outils.conf.PropHelper;
import onight.zippo.oparam.etcd.EctdImpl;

@Component(immediate = true)
@Instantiate()
@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
@Slf4j
public class EtcdProvider implements StoreServiceProvider {

	@Override
	public String getProviderid() {
		return ServiceSpec.ETCD_STORE.getTarget();
	}

	BundleContext bundleContext;
	PropHelper params;
	EctdImpl impl = new EctdImpl();

	public EtcdProvider(BundleContext bundleContext) {
		super();
		log.debug("create:EctdImpl:");
		params = new PropHelper(bundleContext);
		String username = params.get("org.zippo.ectd.username", null);
		String passwd = params.get("org.zippo.ectd.passwd", null);
		String uris = params.get("org.zippo.ectd.uris", null);
		String ssluris = params.get("org.zippo.ectd.ssluris", null);

		if (ssluris != null) {
			SslContext sslContext = SslContext.newClientContext();
			
			try (EtcdClient etcd = new EtcdClient(sslContext, URI.create("https://123.45.67.89:8001"),
					URI.create("https://123.45.67.90:8001"))) {
				System.out.println(etcd.getVersion());
			}
		}

		// EtcdClient etcd = new EtcdClient(username, password, baseUri);
		impl.setEtcd(etcd);
		this.bundleContext = bundleContext;
	}

	@AllArgsConstructor
	public class SqlMapperInfo {
		Object sqlmapper;
		String sf;
	}

	@Validate
	public synchronized void startup() {
		log.info("启动中...@" + bundleContext);
		log.info("启动完成...");
	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");
		log.info("退出完成...");
	}

	@Override
	public DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		/*
		 * if (springLoader != null) { return
		 * springLoader.getBeans(dao.getDomainName() + "Dao"); } else {
		 * log.warn("bean dao not found:" + dao.getDomainName()); return null; }
		 */
		return null;
	}

	@Override
	public String[] getContextConfigs() {
		return null;
	}

}
