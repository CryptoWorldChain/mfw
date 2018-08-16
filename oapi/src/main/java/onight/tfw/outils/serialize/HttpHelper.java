package onight.tfw.outils.serialize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import onight.tfw.outils.pool.ReusefulLoopPool;

public class HttpHelper {

	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forrwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Prroxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Prroxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	public static String getRequestContent(HttpServletRequest request) throws IOException {
		return new String(getRequestContentBytes(request));
	}

	public static byte[] getRequestContentBytes(HttpServletRequest request) throws IOException {
		InputStream is = null;
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()){
			is = request.getInputStream();
			byte[] data = new byte[8192];
			int rsize = 0;
			do {
				rsize = is.read(data);
				if (rsize > 0) {
					bout.write(data, 0, rsize);
				}
			} while (rsize > 0);
			return bout.toByteArray();
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

}
