package onight.tfw.ntrans.osgi;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.osgi.framework.ServiceReference;

@Slf4j
public class BundleAutoWare {

	public ConcurrentHashMap<String, Object> serviceByName = new ConcurrentHashMap<>();

	@AllArgsConstructor
	public static class WaredInfo {
		Method setMethod;
		Object destObj;
	};

	public ConcurrentHashMap<String, List<WaredInfo>> requireList = new ConcurrentHashMap<>();

	public void bindActor(ActorService service, ServiceReference ref) {

		Class clazz = service.getClass();
		Instantiate inst = (Instantiate) clazz.getAnnotation(Instantiate.class);
		String name = clazz.getName();
		if (inst != null) {
			name = inst.name();
			log.debug("Register ActorService:" + name + "," + service + ",clazz=" + clazz);
		}
		serviceByName.put(name, service);

		// serviceByName.put(service.getClass(), value)
		// int serviceid=System.identityHashCode(service);

		ArrayList<WaredInfo> wares = new ArrayList<WaredInfo>();

		for (Field field : clazz.getDeclaredFields()) {
			ActorRequire anno = field.getAnnotation(ActorRequire.class);
			if (anno != null) {
				String beanname = anno.name();
				if (StringUtils.isBlank(beanname)) {
					beanname = field.getType().getName();
				}
				log.debug("Service(" + service + "),require.field=" + field.getName() + ",bean=" + beanname + ",type=" + field.getType());
				try {
					PropertyDescriptor pd = new PropertyDescriptor(field.getName(), clazz);
					List<WaredInfo> rq = requireList.get(beanname);
					if (rq == null) {
						rq = new ArrayList<WaredInfo>();
						requireList.put(beanname, rq);
					}
					rq.add(new WaredInfo(pd.getWriteMethod(), service));
				} catch (Exception e) {
					log.error("error in auto ware Service..", e);
					e.printStackTrace();
				}
			}
		}
		checkWared();
	}

	public void unbindActor(ActorService service, ServiceReference ref) {
		Class clazz = service.getClass();
		Instantiate inst = (Instantiate) clazz.getAnnotation(Instantiate.class);
		String name = clazz.getName();
		if (inst != null) {
			name = inst.name();
			log.debug("Register ActorService:" + name + "," + service + ",clazz=" + clazz);
		}
		serviceByName.remove(name, service);

	}

	public void checkWared() {
		for (Entry<String, List<WaredInfo>> hunted : requireList.entrySet()) {
			Object result = serviceByName.get(hunted.getKey());
			if (result != null) {
				for (WaredInfo wi : hunted.getValue()) {
					try {
						wi.setMethod.invoke(wi.destObj, result);
						log.debug("AutoWared Success " + hunted.getKey() + " for class " + wi.setMethod);

					} catch (Exception e) {
						log.error("error in auto ware Service..:" + hunted.getKey() + ",wi=" + wi, e);
					}
				}
				hunted.getValue().clear();
			}
		}
	}
}
