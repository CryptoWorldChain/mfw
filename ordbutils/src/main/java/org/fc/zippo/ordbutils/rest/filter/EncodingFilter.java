package org.fc.zippo.ordbutils.rest.filter;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.fc.zippo.ordbutils.exception.FilterException;
import org.fc.zippo.ordbutils.rest.FilterConfig;
import org.fc.zippo.ordbutils.rest.IRestfulFilter;

public class EncodingFilter implements IRestfulFilter {

	@Override
	public String getSimpleName() {
		return "sizefilter";
	}

	String defaultCharset = "utf-8";

	@Override
	public void init(FilterConfig filterConfig) throws FilterException {
		defaultCharset = filterConfig.getStrProp("org.zippo.rest.charset", "utf-8");
	}

	@Override
	public void destroy(FilterConfig filterConfig) throws FilterException {

	}

	@Override
	public boolean doFilter(HttpServletRequest req, HttpServletResponse res) throws FilterException {
		try {
			req.setCharacterEncoding(defaultCharset);
			res.setContentType("application/json; charset=" + defaultCharset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

}
