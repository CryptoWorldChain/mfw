package onight.tfw.ojpa.ordb;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import mousio.etcd4j.EtcdClient;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.outils.conf.PropHelper;
import onight.zippo.oparam.etcd.EtcdImpl;

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
	EtcdImpl etcdImpl = new EtcdImpl();
	public URI[] getURI(String urilist){
		try {
			String strarray[]=urilist.split(",");
			URI uriarray[]=new URI[strarray.length];
			int i=0;
			for(String str:strarray){
				uriarray[i++]=new URI(str.trim());
			}
			return uriarray;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public EtcdProvider(BundleContext bundleContext) {
		super();
		params = new PropHelper(bundleContext);
		this.bundleContext = bundleContext;
	}
	EtcdClient etcd=null;

	@Validate
	public synchronized void startup() {
		log.info("启动中...@" + bundleContext);
		log.debug("create:EtcdImpl:");
		String username = params.get("org.zippo.etcd.username", null);
		String passwd = params.get("org.zippo.etcd.passwd", null);
		String uris = params.get("org.zippo.etcd.uris", "http://127.0.0.1:2379");
		String ssluris = params.get("org.zippo.etcd.ssluris", null);
		if (ssluris != null) {
			try {
				SslContext sslContext = SslContextBuilder.forClient().build();
				if(username!=null&&passwd!=null){
					etcd = new EtcdClient(sslContext, getURI(uris));
					log.info("new SSL Etcd"+",Version(cluster,server)=("+etcd.version().getCluster()+","+etcd.version().server+")");
				}else{
					etcd = new EtcdClient(sslContext,username,passwd,getURI(uris));
					log.info("new SSL Etcd with username="+username+",passwd=******"+",Version(cluster,server)=("+etcd.version().getCluster()+","+etcd.version().server+")");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			if(username==null&&passwd==null){
				etcd = new EtcdClient( getURI(uris));
				log.info("new Etcd,Version(cluster,server)=("+etcd.version().getCluster()+","+etcd.version().server+")");
			}else{
				etcd = new EtcdClient(username,passwd,getURI(uris));
				log.info("new Etcd with username="+username+",passwd=******"+",Version(cluster,server)=("+etcd.version().getCluster()+","+etcd.version().server+")");
			}
		}
		if(etcd==null)
		{
			log.warn("cannot connect to etcd!");
		}
		else{
			etcdImpl.setEtcd(etcd);
		}
		log.info("启动完成...");
	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");
		if(etcd!=null){
			try {
				etcd.close();
			} catch (IOException e) {
				log.warn("close etcd error",e);
			}
		}
		log.info("退出完成...");
	}

	@Override
	public DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		return etcdImpl;
	}

	@Override
	public String[] getContextConfigs() {
		return null;
	}

}
