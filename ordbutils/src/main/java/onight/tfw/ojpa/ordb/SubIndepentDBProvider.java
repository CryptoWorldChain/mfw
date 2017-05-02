package onight.tfw.ojpa.ordb;

import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.ordb.loader.SubSpringContextLoader;

@Slf4j
@Provides(specifications = { StoreServiceProvider.class, ActorService.class }, strategy = "SINGLETON")
public abstract class SubIndepentDBProvider extends ORDBProvider implements ActorService {

	@ActorRequire(scope = "global",name=".superProName")
	@Getter
	ORDBProvider orclProvider;

	@Getter
	@Setter
	String superProName;

	public void setOrclProvider(ORDBProvider orclProvider) {
		this.orclProvider = orclProvider;
		reloadContx();
	}

	public SubIndepentDBProvider(String superProName, BundleContext bundleContext) {
		super(bundleContext);
		this.superProName = superProName;
	}

	@Override
	public String[] getContextConfigs() {
		return new String[] {  };
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
