package onight.tfw.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.PacketFilter;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.proxy.IActor;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Unbind;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

@Component(name = "actorman")
@iPojoBean
@Slf4j
public class ActorManager {

	BundleContext bundleContext;

	PropHelper prop;

	public ActorManager(BundleContext bundleContext) {
		super();
		prop = new PropHelper(bundleContext);
		log.debug("create:ActorManager:");
		this.bundleContext = bundleContext;
	}

	private LinkedList<HttpService> services = new LinkedList<HttpService>();
	private Map<String, Servlet> servlets = new HashMap<String, Servlet>();

	@Bind(aggregate = true)
	public void bindHttp(HttpService service) {
		log.info("bindHttp::" + service);
		services.add(service);
	}

	@Unbind(aggregate = true)
	public void unbindHttp(HttpService http) {
		for (String ctx : servlets.keySet()) {
			http.unregister(ctx);
			log.info("Method unbindHttp  execute  ctx[" + ctx + "]...");
		}
		services.remove(http);
	}

	@WebServlet(asyncSupported = true)
	public class AsyncServlet extends HttpServlet {
		IActor factor;

		public AsyncServlet(IActor factor) {
			super();
			this.factor = factor;
		}

		protected void doGet(HttpServletRequest req, final HttpServletResponse resp)
				throws ServletException, IOException {
			// FramePacket pack = PacketHelper.buildHeaderFromHttpGet(req);
			// CompleteHandler handler = new CompleteHandler() {
			// @Override
			// public void onFinished(FramePacket packet) {
			// try {
			// postRouteListner(packet, null);
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// }
			// };
			// if (preRouteListner(pack, handler)) {
			// return;
			// }
			factor.doGet(req, resp);
		}

		@Override
		protected void doPost(HttpServletRequest req, final HttpServletResponse resp)
				throws ServletException, IOException {
			// FramePacket pack = PacketHelper.buildHeaderFromHttpPost(req);
			// CompleteHandler handler = new CompleteHandler() {
			// @Override
			// public void onFinished(FramePacket packet) {
			// try {
			// if (packet != null) {
			// resp.getOutputStream().write(PacketHelper.toTransBytes(packet));
			// }
			// postRouteListner(packet, null);
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
			// }
			// };
			// if (preRouteListner(pack, handler)) {
			// return;
			// }
			factor.doPost(req, resp);

		}

		@Override
		protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			factor.doPut(req, resp);
		}

		@Override
		protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			factor.doDelete(req, resp);
		}
	}

	@Bind(aggregate = true, optional = true)
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
							s.registerServlet(rootpath+spath, servlet, null, null);
							log.info("注册servlet成功:" + rootpath+spath);
						} catch (Exception e) {
							log.warn("注册servlet失败", e);
						}
					}
				}
			}
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindActor(IActor actor) {
		log.info("注销actor" + actor);
		if (actor == null)
			return;
		String[] ctxpaths = actor.getWebPaths();
		if (ctxpaths != null) {
			for (String ctxpath : ctxpaths) {
				servlets.remove(ctxpath);
				for (HttpService s : services) {
					try {
						s.unregister(ctxpath);
					} catch (Exception e) {
					}
					log.info("注销servlet成功" + ctxpath);
				}
			}
		}

	}

}
