package org.fc.zippo.ordbutils.rest;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.fc.zippo.filter.exception.FilterException;
import org.fc.zippo.filter.exception.PathException;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.ntrans.api.FilterManager;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.ordb.ORDBProvider;
import onight.tfw.ojpa.ordb.StaticTableDaoSupport;
import onight.tfw.ojpa.ordb.loader.CommonSqlMapper;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketFilter;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.HttpHelper;
import onight.tfw.proxy.IActor;

@Slf4j
public abstract class RestfulDBStoreProvider extends ORDBProvider implements IActor {

	public RestfulDBStoreProvider(BundleContext bundleContext) {
		super(bundleContext);
	}

	PropHelper props = new PropHelper(bundleContext);

	HashMap<String, BaseRestCtrl> ctrls = new HashMap<>();

	public abstract String[] getCtrlPaths();

	@ActorRequire(name = "filterManager", scope = "global")
	FilterManager fm;

	public FilterManager getFm() {
		return fm;
	}

	public void setFm(FilterManager fm) {
		this.fm = fm;
	}

	public abstract PacketFilter[] getFilters();

	protected ActWrapper actwapper = new ActWrapper() {

		@Override
		public String getModule() {
			return "rest";
		}

	};

	@Validate
	public void startup() {
		super.startup();
		for (String path : getCtrlPaths()) {
			Enumeration<URL> en = bundleContext.getBundle().findEntries(path.replaceAll("\\.", "/"), "*.class", true);
			while (en.hasMoreElements()) {
				URL url = en.nextElement();
				try {
					Class clazz = bundleContext.getBundle().loadClass(
							url.getPath().replaceAll("\\.class", "").replaceAll("/", ".").replaceFirst("\\.", ""));
					if (BaseRestCtrl.class.equals(clazz.getGenericSuperclass())) {
						Constructor<BaseRestCtrl> construct = clazz.getConstructor(StaticTableDaoSupport.class,
								CommonSqlMapper.class);
						String ctrlname = clazz.getSimpleName().replaceAll("Ctrl", "").toLowerCase();
						BaseRestCtrl ctrl = construct.newInstance(getStaticDao(ctrlname), getCommonSqlMapper());
						ctrl.setDeleteByExampleEnabled(
								StringHelper.toBool(props.get("org.zippo.rest.deletebyexample", "off")));
						log.debug("Registry Ctrl:path=" + ctrlname + ":ctrl" + ctrl + ",dao=" + getStaticDao(ctrlname));
						ctrls.put(ctrlname, ctrl);
					}

				} catch (Exception e) {
					log.debug("cannot load clazz:{}", url);
				}
			}
		}

		log.debug("startup RestfulDBStoreProvider");

	}

	public Resource[] loadResource(String locationPattern) {
		List<Resource> ret = new ArrayList<Resource>();
		if (locationPattern.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
			int rootstartidx = locationPattern.indexOf(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)
					+ ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX.length();
			int rootendidx = locationPattern.lastIndexOf("/");
			String rootpath = locationPattern.substring(rootstartidx, rootendidx);
			String filePattern = locationPattern.substring(rootendidx + 1);
			Enumeration<URL> en = bundleContext.getBundle().findEntries(rootpath, filePattern, true);
			while (en.hasMoreElements()) {
				URL url = en.nextElement();
				log.debug("find Resource:" + url);
				ret.add(new UrlResource(url));
			}
		}
		return ret.toArray(new Resource[] {});
	}

	@Invalidate
	public void shutdown() {
		super.shutdown();
	}

	public String getSafePath(String uri) {
		int index = uri.indexOf("?");
		if (index >= 0) {
			uri = uri.substring(0, index);
		}
		index = uri.indexOf("/");
		if (index >= 0) {
			uri = uri.substring(index);
		}
		if (uri.startsWith("/"))
			return uri.substring(1);
		return "";
	}

	public BaseRestCtrl tryPath(String path) throws IOException {
		if (path.length() < 2) {
			throw new PathException("{\"status\":\"error\",\"message\":\"path.1 not found:" + path + "\"}");
		} else {
			String ctrlpath = path.substring(1);
			String paths[] = ctrlpath.split("/");
			if (paths.length > 0) {
				BaseRestCtrl ctrl = ctrls.get(paths[0]);
				if (ctrl == null) {
					throw new PathException("{\"status\":\"error\",\"message\":\"Controller not found:" + paths[0]
							+ ",path:" + path + "\"}");
				}
				log.debug("tryPath={},ctrl={}", paths[0], ctrl);
				return ctrl;
			}
		}
		throw new PathException("{\"status\":\"error\",\"message\":\"path.2 not found:" + path + "\"}");
	}

	FramePacket getFakeFramePack(HttpServletRequest req, HttpServletResponse res) {
		FramePacket pack = PacketHelper.buildHeaderFromHttpGet(req);// new
		pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_REQUEST, req);
		pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_RESPONSE, res);
		return pack;
	}

	public boolean doPreFilter(FramePacket packet, HttpServletRequest req, HttpServletResponse res) {
		if (fm != null) {
			if (!fm.preRouteListner(actwapper, packet, new CompleteHandler() {
				@Override
				public void onFinished(FramePacket arg0) {

				}
			})) {
				throw new FilterException("FilterCannotMatch");
			}
		}
		return true;

	}

	public boolean doPostFilter(FramePacket pack, HttpServletRequest req, HttpServletResponse res) {
		try {
			if (pack != null) {
				try {
					pack.getExtHead().buildFor(res);
				} catch (Exception e) {
					log.debug("error for Build for res:",e);
				}
			}
			if (fm != null) {
				return fm.postRouteListner(actwapper, pack, new CompleteHandler() {
					@Override
					public void onFinished(FramePacket arg0) {

					}
				});
			}
		} catch (Throwable e) {
			log.debug("doPostFilterError,", e);
		}
		return true;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		FramePacket packet = getFakeFramePack(req, res);
		try {
			doPreFilter(packet, req, res);
			String ret = tryPath(req.getPathInfo()).get(getSafePath(req.getPathInfo().substring(1)), req, res);
			doPostFilter(packet, req, res);
			res.getOutputStream().write(ret.getBytes("UTF-8"));
		} catch (PathException e) {
			doPostFilter(packet, req, res);
			res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
		} catch (Throwable t) {
			log.debug("unknow Error", t);
			doPostFilter(packet, req, res);
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
		} finally {

		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		String method = req.getParameter("_method");
		if (StringUtils.isNotBlank(method)) {
			if (method.equals("del")) {
				doDelete(req, res);
				return;
			}
			if (method.equals("put") || method.equals("mod")) {
				doPut(req, res);
				return;
			}
			if (method.equals("get")) {
				doGet(req, res);
				return;
			}
		}
		byte bytes[] = HttpHelper.getRequestContentBytes(req);
		if (bytes == null) {
			res.getWriter().write("{\"status\":\"error\",\"message\":\"POST Body not found\"}");
		} else {
			FramePacket packet = getFakeFramePack(req, res);

			try {
				doPreFilter(packet, req, res);
				String ret = tryPath(req.getPathInfo()).post(bytes, req, res);
				doPostFilter(packet,req, res);
				res.getOutputStream().write(ret.getBytes("UTF-8"));
			} catch (PathException e) {
				doPostFilter(packet,req, res);
				res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
			} catch (Throwable t) {
				log.debug("unknow Error", t);
				doPostFilter(packet,req, res);
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
			}
		}
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		byte bytes[] = HttpHelper.getRequestContentBytes(req);
		if (bytes == null) {
			res.getWriter().write("{\"status\":\"error\",\"message\":\"PUT Body not found\"}");
		} else {
			FramePacket packet = getFakeFramePack(req, res);

			try {
				doPreFilter(packet, req, res);
				String ret = tryPath(req.getPathInfo()).put(getSafePath(req.getPathInfo().substring(1)), bytes, req,
						res);
				doPostFilter(packet,req, res);

				res.getOutputStream().write(ret.getBytes("UTF-8"));
			} catch (PathException e) {
				doPostFilter(packet,req, res);
				res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
			} catch (Throwable t) {
				log.debug("unknow Error", t);
				doPostFilter(packet,req, res);
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
			}
		}
	}

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		byte bytes[] = HttpHelper.getRequestContentBytes(req);
		if (bytes == null || bytes.length == 0) {
			res.getWriter().write("{\"status\":\"error\",\"message\":\"DELETE Body not found\"}");
		} else {
			FramePacket packet = getFakeFramePack(req, res);
			try {
				doPreFilter(packet, req, res);
				String ret = tryPath(req.getPathInfo()).delete(getSafePath(req.getPathInfo().substring(1)), bytes, req,
						res);
				doPostFilter(packet,req, res);
				res.getOutputStream().write(ret.getBytes("UTF-8"));
			} catch (PathException e) {
				doPostFilter(packet,req, res);
				res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
			} catch (Throwable t) {
				log.debug("unknow Error", t);
				doPostFilter(packet,req, res);
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
			}
		}
	}

}