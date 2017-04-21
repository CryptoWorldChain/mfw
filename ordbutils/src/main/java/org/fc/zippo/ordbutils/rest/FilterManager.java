package org.fc.zippo.ordbutils.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fc.zippo.ordbutils.exception.FilterException;
import org.fc.zippo.ordbutils.exception.NoAuthorizedException;
import org.osgi.framework.BundleContext;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.conf.PropHelper;

@Slf4j
public class FilterManager {

	BundleContext bctx;

	FilterConfig config;

	public FilterManager(BundleContext bctx) {
		super();
		this.bctx = bctx;
		config = new FilterConfig(bctx, new PropHelper(bctx));
	}

	protected List<IRestfulFilter> filters = new ArrayList<>();

	public void addFilter(IRestfulFilter filter) {
		filter.init(config);
		filters.add(filter);
	}

	public void removeFilter(IRestfulFilter filter) {
		filter.destroy(config);
		filters.remove(filter);
	}

	public synchronized void shutdown() {
		for (IRestfulFilter filter : filters) {
			filter.destroy(config);
		}
		filters.clear();
	}

	public boolean doFilter(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (filters.size() > 0) {
			try {
				for (IRestfulFilter filter : filters) {
					if (!filter.doFilter(request, response)) {
						response.sendError(HttpServletResponse.SC_BAD_REQUEST,
								"FilterNotAccepted:" + filter.getSimpleName());
						return false;
					}
				}
			} catch (NoAuthorizedException e) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
				return false;
			} catch (FilterException e) {
				log.debug("filterException:", e);
				response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, e.getMessage());
				return false;
			}
		}
		return true;
	}

}
