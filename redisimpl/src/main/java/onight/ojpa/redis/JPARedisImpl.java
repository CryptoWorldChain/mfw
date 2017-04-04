package onight.ojpa.redis;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import lombok.extern.slf4j.Slf4j;
import onight.ojpa.redis.loader.BatchDao;
import onight.ojpa.redis.loader.DaoRedisImpl;
import onight.ojpa.redis.loader.RedisConnector;
import onight.tfw.ojpa.api.BatchExecutor;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.outils.conf.PropHelper;

@Component(immediate = true)
@Instantiate(name = "redisimpl")
@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
@Slf4j
public class JPARedisImpl implements StoreServiceProvider {

	@Override
	public String getProviderid() {
		return ServiceSpec.REDIS_STORE.getTarget();
	}

	BundleContext bundleContext;
	PropHelper props;

	public JPARedisImpl(BundleContext bundleContext) {
		super();
		log.debug("create:JPARedisImpl:");
		this.bundleContext = bundleContext;
		redis = new RedisConnector();
	}

	@Validate
	public void startup() {
		props = new PropHelper(bundleContext);
		log.info("启动中...@" + bundleContext);
		redis.onStart(props.get("ofw.redis.addr", "172.30.12.44"), props.get("ofw.redis.port", 6379)
				, props.get("ofw.redis.poolsize", 20));
		log.info("启动完成...");
	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");
		redis.onDestory();
		log.info("退出完成...");
	}

	RedisConnector redis;

	@Override
	public DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		if (BatchExecutor.class.equals(dao.getDomainClazz())) {
			return new BatchDao(redis, (OJpaDAO)dao);
		}
		return new DaoRedisImpl(redis, (OJpaDAO)dao);
	}

	@Override
	public String[] getContextConfigs() {
		return null;
	}

}
