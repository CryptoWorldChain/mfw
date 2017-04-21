package org.fc.zippo.ordbutils.rest.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.fc.zippo.ordbutils.exception.FilterException;
import org.fc.zippo.ordbutils.rest.FilterConfig;
import org.fc.zippo.ordbutils.rest.IRestfulFilter;

public class RequestSizeFilter implements IRestfulFilter {

	@Override
	public String getSimpleName() {
		return "sizefilter";
	}

	int maxsize = -1;

	@Override
	public void init(FilterConfig filterConfig) throws FilterException {
		maxsize = -1;// 1M
		String strsize = filterConfig.getStrProp("org.zippo.rest.req.maxsize", "1M");
		if (StringUtils.endsWithIgnoreCase(strsize, "M")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1)) * 1024 * 1024;
		} else if (StringUtils.endsWithIgnoreCase(strsize, "K")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1)) * 1024;
		} else if (StringUtils.endsWithIgnoreCase(strsize, "G")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1)) * 1024 * 1024 * 1024;
		} else if (StringUtils.endsWithIgnoreCase(strsize, "B")) {
			maxsize = Integer.parseInt(strsize.trim().substring(0, strsize.length() - 1));
		} else {
			maxsize = Integer.parseInt(strsize.trim());
		}
	}

	@Override
	public void destroy(FilterConfig filterConfig) throws FilterException {

	}

	@Override
	public boolean doFilter(HttpServletRequest request, HttpServletResponse response) throws FilterException {
		return maxsize > 0 && request.getContentLength() < maxsize;
	}

}
