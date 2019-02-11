package onight.tfw.servlet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.osgi.IPolicy;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.proxy.IActor;

@iPojoBean
@Slf4j
public class ActorManager implements IPolicy {

	BundleContext bundleContext;

	PropHelper prop;

	public ActorManager(BundleContext context) {
		bundleContext = context;
		prop = new PropHelper(bundleContext);
	}

	@Validate
	public void start() {

	}

	private LinkedList<HttpService> services = new LinkedList<HttpService>();
	private Map<String, Servlet> servlets = new HashMap<String, Servlet>();

	@Bind(id = "actor.http")
	public void bindHttp(HttpService service,final Map<?, ?> aServiceProperties) {
		log.debug("binHttpService::" + service+",prop="+aServiceProperties);
		services.add(service);
	}
	@Unbind
	public void unbindHttp(HttpService http) {
		System.out.println("unbindHttp::" + http);

		for (String ctx : servlets.keySet()) {
			http.unregister(ctx);
			log.debug("Method unbindHttp  execute  ctx[" + ctx + "]...");
		}
		services.remove(http);
	}

	@Bind
	public void bindActor(IActor actor) {
		log.info("bindActor:" + actor);
		if (actor == null)
			return;
		String rootpath = prop.get("ofw.actor.rootpath", "");
		if (rootpath.endsWith("/")) {
			rootpath = rootpath.substring(0, rootpath.length() - 1);
		}
		String[] ctxpaths = actor.getWebPaths();
		if (ctxpaths != null) {
			for (String ctxpath : ctxpaths) {
				HttpServlet servlet = new AsyncServlet(actor);
				for (String spath : ctxpath.split(",")) {
					servlets.put(rootpath + spath, servlet);
					for (HttpService s : services) {
						try {
							if (prop.get("org.zippo.servlet.default", "/bca/pbver.do").equals(spath)) {
								try {
									s.registerServlet("/", servlet, null, null);
									s.registerServlet(rootpath, servlet, null, null);
									s.registerServlet(rootpath + spath, servlet, null, null);
								} catch (Exception e) {
								}
							} else {
								s.registerServlet(rootpath + spath, servlet, null, null);
								log.info("register servlet:" + rootpath + spath);
							}
						} catch (Exception e) {
							log.warn("Failed in register servlet:" + rootpath + spath, e);
						}
					}
				}
			}
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindActor(IActor actor) {
		log.debug("unbind actor" + actor);
		if (actor == null)
			return;
		String[] ctxpaths = actor.getWebPaths();
		String rootpath = prop.get("ofw.actor.rootpath", "");
		if (rootpath.endsWith("/")) {
			rootpath = rootpath.substring(0, rootpath.length() - 1);
		}
		if (ctxpaths != null) {
			for (String ctxpath : ctxpaths) {
				servlets.remove(rootpath + ctxpath);
				for (HttpService s : services) {
					try {
						s.unregister(rootpath + ctxpath);
					} catch (Exception e) {
					}
					log.info("unbind servlet :" + ctxpath);
				}
			}
		}

	}

}
