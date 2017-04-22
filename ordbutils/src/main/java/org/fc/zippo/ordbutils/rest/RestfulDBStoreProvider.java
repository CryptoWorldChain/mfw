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
import org.fc.zippo.ordbutils.exception.PathException;
import org.fc.zippo.ordbutils.rest.filter.RequestSizeFilter;
import org.osgi.framework.BundleContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.ordb.ORDBProvider;
import onight.tfw.ojpa.ordb.StaticTableDaoSupport;
import onight.tfw.ojpa.ordb.loader.CommonSqlMapper;
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
	protected FilterManager fm = new FilterManager(bundleContext);

	public abstract String[] getCtrlPaths();

	public abstract IRestfulFilter[] getFilters();

	@Validate
	public void startup() {
		super.startup();
		if (StringUtils.equals("true", props.get("org.zippo.rest.filters.sizefilter", "true"))
				|| StringUtils.equals("on", props.get("org.zippo.rest.filters.sizefilter", "on"))
				|| StringUtils.equals("1", props.get("org.zippo.rest.filters.sizefilter", "1"))) {
			fm.addFilter(new RequestSizeFilter());
		}
		if (getFilters() != null && getFilters().length > 0) {
			for (IRestfulFilter rf : getFilters()) {
				fm.addFilter(rf);
			}
		}
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
						ctrl.setDeleteByExampleEnabled(StringHelper.toBool(props.get("org.zippo.rest.deletebyexample","off")));
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
		return uri;
	}

	public BaseRestCtrl tryPath(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json; charset=utf-8");
		if (req.getPathInfo().length() < 2) {
			throw new PathException(
					"{\"status\":\"error\",\"message\":\"path.1 not found:" + req.getPathInfo() + "\"}");
		} else {
			String ctrlpath = req.getPathInfo().substring(1);
			String paths[] = ctrlpath.split("/");
			if (paths.length > 0) {
				BaseRestCtrl ctrl = ctrls.get(paths[0]);
				if (ctrl == null) {
					throw new PathException("{\"status\":\"error\",\"message\":\"Controller not found:" + paths[0]
							+ ",path:" + req.getPathInfo() + "\"}");
				}
				log.debug("tryPath={},ctrl={}", paths[0], ctrl);
				return ctrl;
			}
		}
		throw new PathException("{\"status\":\"error\",\"message\":\"path.2 not found:" + req.getPathInfo() + "\"}");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		try {
			if (!fm.doFilter(req, res))
				return;
			String ret = tryPath(req, res).get(getSafePath(req.getPathInfo().substring(1)), req, res);
			res.getOutputStream().write(ret.getBytes("UTF-8"));
		} catch (PathException e) {
			res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
		} catch (Throwable t) {
			log.debug("unknow Error", t);
			res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (!fm.doFilter(req, res))
			return;
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
		if (bytes == null || bytes.length == 0) {
			res.getWriter().write("{\"status\":\"error\",\"message\":\"POST Body not found\"}");
		} else {
			try {
				String ret = tryPath(req, res).post(bytes, req, res);
				res.getOutputStream().write(ret.getBytes("UTF-8"));
			} catch (PathException e) {
				res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
			} catch (Throwable t) {
				log.debug("unknow Error", t);
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
			}
		}
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (!fm.doFilter(req, res))
			return;

		byte bytes[] = HttpHelper.getRequestContentBytes(req);
		if (bytes == null || bytes.length == 0) {
			res.getWriter().write("{\"status\":\"error\",\"message\":\"PUT Body not found\"}");
		} else {
			try {
				String ret = tryPath(req, res).put(getSafePath(req.getPathInfo().substring(1)), bytes, req, res);
				res.getOutputStream().write(ret.getBytes("UTF-8"));
			} catch (PathException e) {
				res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
			} catch (Throwable t) {
				log.debug("unknow Error", t);
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
			}
		}
	}

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (!fm.doFilter(req, res))
			return;
		byte bytes[] = HttpHelper.getRequestContentBytes(req);
		if (bytes == null || bytes.length == 0) {
			res.getWriter().write("{\"status\":\"error\",\"message\":\"DELETE Body not found\"}");
		} else {
			try {
				String ret = tryPath(req, res).delete(getSafePath(req.getPathInfo().substring(1)), bytes, req, res);
				res.getOutputStream().write(ret.getBytes("UTF-8"));
			} catch (PathException e) {
				res.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, e.getMessage());
			} catch (Throwable t) {
				log.debug("unknow Error", t);
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknow Error:" + t.getMessage());
			}
		}
	}

}