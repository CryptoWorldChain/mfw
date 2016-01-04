package onight.tfw.proxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IActor {

	public abstract String[] getWebPaths();

	public abstract void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

	public abstract void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

	public abstract void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

	public abstract void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

}