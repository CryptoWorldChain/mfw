package onight.tfw.ntrans.osgi;

import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@iPojoBean
@Slf4j
public class ActorAutoWare implements IPolicy{

	BundleContext context;

	public ActorAutoWare(BundleContext context) {
//		log.debug("ActorAutoWare::"+context);
		this.context = context;
	}

	@Validate
	public void start() {
	}

	public ConcurrentHashMap<Long, BundleAutoWare> bundleAutoByBundleID = new ConcurrentHashMap<>();

	@Bind(optional=true,aggregate=true)
	public void bindActor(ActorService service, ServiceReference ref) {
		log.debug("bindActorService:"+service+",ref="+ref);
		BundleAutoWare baw = bundleAutoByBundleID.get(ref.getBundle().getBundleId());
		if (baw == null) {
			baw = new BundleAutoWare();
			bundleAutoByBundleID.put(ref.getBundle().getBundleId(), baw);
		}
		baw.bindActor(service, ref);
		for(BundleAutoWare bawexist:bundleAutoByBundleID.values()){
			if(bawexist==baw)continue;
			baw.checkWared(baw.requireList,bawexist.serviceByName,true);
			bawexist.checkWared(bawexist.requireList, baw.serviceByName, true);
		}
		
	}

	@Unbind(optional=true,aggregate=true)
	public void unbindActor(ActorService service, ServiceReference ref) {
		log.debug("unbindActorService:"+service+",ref="+ref);
		
		BundleAutoWare baw = bundleAutoByBundleID.get(ref.getBundle().getBundleId());
		if (baw != null&&service!=null) {
			baw.unbindActor(service, ref);
		}

	}
}
