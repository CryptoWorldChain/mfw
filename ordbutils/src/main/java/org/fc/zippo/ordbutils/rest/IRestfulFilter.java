package org.fc.zippo.ordbutils.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fc.zippo.ordbutils.exception.FilterException;

public interface IRestfulFilter {

	public String getSimpleName();

	public void init(FilterConfig filterConfig) throws FilterException;

	public void destroy(FilterConfig filterConfig) throws FilterException;

	public boolean doFilter(HttpServletRequest request, HttpServletResponse response) throws FilterException;

}
