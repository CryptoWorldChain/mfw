package onight.zippo.ojpa.ordb;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.oparam.api.HashParam;
import onight.tfw.oparam.api.OPFace;
import onight.tfw.outils.conf.PropHelper;
import onight.zippo.oparam.bdb.OBDBImpl;

@Component(immediate = true)
@Instantiate()
@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
@Slf4j
public class BDBOParamProvider implements StoreServiceProvider {

	@Override
	public String getProviderid() {
		return "obdb";
	}

	BundleContext bundleContext;
	PropHelper params;
	OPFace bdbImpl = new HashParam();

	public BDBOParamProvider(BundleContext bundleContext) {
		super();
		params = new PropHelper(bundleContext);
		this.bundleContext = bundleContext;
	}

	@Validate
	public synchronized void startup() {
		log.info("启动中...@" + bundleContext);
		log.debug("create: bdb oparam impl:");
		String dir = params.get(""
				+ "", "odb."+Math.abs(NodeHelper.getCurrNodeListenOutPort()-5100));
		bdbImpl = new OBDBImpl(dir);
		log.info("启动完成...");
	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");
		if (bdbImpl instanceof OBDBImpl) {
			OBDBImpl obdb = (OBDBImpl) bdbImpl;
			obdb.close();
		}
		log.info("退出完成...");
	}

	@Override
	public synchronized DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		return bdbImpl;
	}

	@Override
	public String[] getContextConfigs() {
		return null;
	}

}
