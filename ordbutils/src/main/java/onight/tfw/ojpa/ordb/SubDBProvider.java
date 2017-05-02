package onight.tfw.ojpa.ordb;

import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.ordb.loader.SubSpringContextLoader;
import onight.tfw.outils.reflect.TypeHelper;

@Slf4j
@Provides(specifications = { StoreServiceProvider.class, ActorService.class }, strategy = "SINGLETON")
public abstract class SubDBProvider<T extends ORDBProvider> extends ORDBProvider implements ActorService {

	@ActorRequire(scope = "global")
	@Getter
	T orclProvider;

	public void setOrclProvider(T orclProvider) {
		this.orclProvider = orclProvider;
		reloadContx();
	}

	public SubDBProvider(BundleContext bundleContext) {
		super(bundleContext);
	}

	@Override
	public String[] getContextConfigs() {
		return new String[] { "/SpringContext-daoConfig-sys.xml" };
	}

	@Validate
	public void startup() {
		// super.startup();
		log.info("启动中...@" + bundleContext);
		reloadContx();
		log.debug("startup LOGINDB");
	}

	public void reloadContx() {
		if (orclProvider != null) {
			springLoader = new SubSpringContextLoader(orclProvider.getApplicationCtx());
			super.startup();
		}
	}

	@Override
	public String getProviderid() {
		return null;
	}
	


}
