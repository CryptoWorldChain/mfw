package onight.tfw.ojpa.ordb;

import java.util.concurrent.LinkedBlockingDeque;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.SqlMapper;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.ordb.loader.SpringContextLoader;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

//@Component(immediate = true)
//@Instantiate(name = "mysqlimpl")
@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
@Slf4j
public abstract class ORDBProvider implements StoreServiceProvider {

	// @Override
	// public String getProviderid() {
	// return ServiceSpec.MYSQL_STORE.getTarget();
	// }

	BundleContext bundleContext;

	SpringContextLoader springLoader;

	public ORDBProvider(BundleContext bundleContext) {
		super();
		log.debug("create:JPAORDBImpl:");
		this.bundleContext = bundleContext;
	}

	@AllArgsConstructor
	public class SqlMapperInfo {
		Object sqlmapper;
		String sf;
	}

	LinkedBlockingDeque<SqlMapperInfo> wishlist = new LinkedBlockingDeque<SqlMapperInfo>();

	@Validate
	public synchronized void startup() {
		log.info("启动中...@" + bundleContext);
		if (springLoader == null) {
			springLoader = new SpringContextLoader();
		}
		springLoader.init(bundleContext, getContextConfigs());
		springLoader.registerDaoBeans();
		for (SqlMapperInfo mapper : wishlist) {
			springLoader.registerMapper(mapper.sqlmapper, mapper.sf);
		}
		wishlist.clear();
		log.info("启动完成...");
	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");
		if (springLoader != null) {
			springLoader.destory();
		}
		log.info("退出完成...");
	}

	@Bind(aggregate = true, optional = true)
	public synchronized void bindMapper(SqlMapper mapper, ServiceReference sf) {
		if (springLoader != null) {
			springLoader.registerMapper(mapper, String.valueOf(sf.getBundle().getBundleId()));
		} else {
			wishlist.add(new SqlMapperInfo(mapper, String.valueOf(sf.getBundle().getBundleId())));
		}
	}

	@Unbind(aggregate = true, optional = true)
	public synchronized void unbindMapper(SqlMapper mapper, ServiceReference sf) {
		wishlist.remove(mapper);
		if (springLoader != null) {
			springLoader.unregisterMapper(mapper, String.valueOf(sf.getBundle().getBundleId()));
		}
	}

	@Override
	public DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		if (springLoader != null) {
			return springLoader.getBeans(dao.getDomainName() + "Dao");
		} else {
			log.warn("bean dao not found:" + dao.getDomainName());
			return null;
		}
	}

}
