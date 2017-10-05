package onight.zippo.oparam.etcd;

import static org.apache.http.HttpVersion.HTTP_1_1;
import static org.apache.http.params.CoreConnectionPNames.SO_TIMEOUT;
import static org.apache.http.params.CoreConnectionPNames.STALE_CONNECTION_CHECK;
import static org.apache.http.params.CoreProtocolPNames.HTTP_CONTENT_CHARSET;
import static org.apache.http.params.CoreProtocolPNames.HTTP_ELEMENT_CHARSET;
import static org.apache.http.params.CoreProtocolPNames.PROTOCOL_VERSION;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.mservice.ThreadContext;

@Slf4j
public class HttpRequestor {

	@Setter
	@Getter
	public String urlbase = "http://localhost:2379/";

	public void reload() {
		lock.writeLock().lock();
		try {
			cm = new ThreadSafeClientConnManager();
			httpclient = new DefaultHttpClient(cm);
			httpclient.getParams().setParameter(PROTOCOL_VERSION, HTTP_1_1);
			httpclient.getParams().setParameter(HTTP_CONTENT_CHARSET, "UTF-8");
			httpclient.getParams().setParameter(HTTP_ELEMENT_CHARSET, "UTF-8");
			httpclient.getParams().setParameter(STALE_CONNECTION_CHECK, true);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void changeMaxTotal(int size) {
		lock.writeLock().lock();
		try {
			cm.setMaxTotal(size);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void changeMaxPerRoute(int size) {
		lock.writeLock().lock();
		try {
			cm.setDefaultMaxPerRoute(size);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public String post(String xml, String address) throws ClientProtocolException, IOException {
		lock.readLock().lock();
		try {
			address = format(address);
			log.debug("httppost:" + urlbase + address + ",data=" + xml);

			httpclient.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			StringEntity entity = new StringEntity("value=" + xml, "UTF-8");
			HttpPost httppost = new HttpPost(urlbase + address);
			httppost.setEntity(entity);
			// httppost.addHeader("Keep-Alive", "" +
			// ThreadContext.getContextInt("wait.timeout", 60));
			httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");

			httppost.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String result = httpclient.execute(httppost, responseHandler);

			log.debug("httpresult:" + address + ",result=" + result);
			return result;
		} catch (SocketTimeoutException so) {
			throw so;
		} catch (HttpResponseException he) {
			if (he.getStatusCode() == 404) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			} else if (he.getStatusCode() == 412) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			}
			throw he;
		} catch (Exception e) {
			log.trace("error:" + e + ":" + urlbase + address, e);
			throw e;
		} finally {

			lock.readLock().unlock();
		}
	}

	public String put(String xml, String address) throws ClientProtocolException, IOException {
		lock.readLock().lock();
		try {
			address = format(address);
			log.debug("httppost:" + urlbase + address + ",data=" + xml);

			httpclient.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			StringEntity entity = new StringEntity("value=" + xml, "UTF-8");
			HttpPut httppost = new HttpPut(urlbase + address);
			httppost.setEntity(entity);
			// httppost.addHeader("Keep-Alive", "" +
			// ThreadContext.getContextInt("wait.timeout", 60));
			httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");
			httppost.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String result = httpclient.execute(httppost, responseHandler);

			log.debug("httpresult:" + address + ",result=" + result);
			return result;
		} catch (SocketTimeoutException so) {
			throw so;
		} catch (HttpResponseException he) {
			if (he.getStatusCode() == 404) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			} else if (he.getStatusCode() == 412) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			}
			throw he;
		} catch (Exception e) {
			log.trace("error:" + e + ":" + urlbase + address, e);
			throw e;
		} finally {
			lock.readLock().unlock();
		}
	}

	public String get(String address) throws ClientProtocolException, IOException {
		lock.readLock().lock();
		try {
			address = format(address);
			log.debug("httpget:" + urlbase + address);
			httpclient.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			HttpGet get = new HttpGet(urlbase + address);
			// get.addHeader("Keep-Alive", "" +
			// ThreadContext.getContextInt("wait.timeout", 60));
			get.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String result = httpclient.execute(get, responseHandler);

			log.debug("httpresult:" + address + ",result=" + result);
			return result;
		} catch (SocketTimeoutException so) {
			throw so;
		} catch (HttpResponseException he) {
			if (he.getStatusCode() == 404) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			} else if (he.getStatusCode() == 412) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			}
			throw he;
		} catch (Exception e) {
			log.trace("error:" + e + ":" + urlbase + address, e);
			throw e;
		} finally {
			lock.readLock().unlock();
		}
	}

	public String format(String address) {
		int ttl = ThreadContext.getContextInt("ttl", 0);
		if (ttl > 0) {
			if (address.contains("?")) {
				address = address + "&";
			} else {
				address = address + "?";
			}
			address += "ttl=" + ttl;
		}
		return address;
	}

	public String delete(String address) throws ClientProtocolException, IOException {
		lock.readLock().lock();
		try {
			address = format(address);
			log.debug("httpget:" + urlbase + address);
			httpclient.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			HttpDelete get = new HttpDelete(urlbase + address);
			// get.addHeader("Keep-Alive", "" +
			// ThreadContext.getContextInt("wait.timeout", 60));
			get.getParams().setParameter(SO_TIMEOUT, defaultHttpTimeoutMillis);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			String result = httpclient.execute(get, responseHandler);

			log.debug("httpresult:" + address + ",result=" + result);
			return result;
		} catch (SocketTimeoutException so) {
			throw so;
		} catch (HttpResponseException he) {
			if (he.getStatusCode() == 404) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			} else if (he.getStatusCode() == 412) {
				return "{\"errorCode\":100,\"message\":\"Key not found\"}";
			}
			throw he;
		} catch (Exception e) {
			log.trace("error:" + e + ":" + urlbase + address, e);
			throw e;

		} finally {
			lock.readLock().unlock();
		}
	}

	public void destroy() {
		lock.writeLock().lock();
		try {
			cm.shutdown();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public int defaultHttpTimeoutMillis = 10000;// http超时时间
	private int httpKeepAlivesSecs = 60;
	private ThreadSafeClientConnManager cm;
	private DefaultHttpClient httpclient;
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

}
