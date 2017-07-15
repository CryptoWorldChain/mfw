package onight.tfw.ojpa.opm.proxy;

import onight.osgi.annotation.iPojoBean;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.NoneDomainDao;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.StoreServiceProvider;

import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;

@iPojoBean
@Provides(specifications = StoreServiceProvider.class)
public class NoneStoreService implements StoreServiceProvider{

	@Validate 
	public void start() {
	}

	@Invalidate
	public void destory() {
	}

	@Override
	public DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		return new NoneDomainDao();
	}

	@Override
	public String getProviderid() {
		return "";
	}

	@Override
	public String[] getContextConfigs() {
		return new String[]{};
	}

	
}
