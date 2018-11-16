package org.fc.zippo.sender.httpimpl;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.SslEngineFactory;
import org.asynchttpclient.netty.ssl.JsseSslEngineFactory;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.conf.PropHelper.IFinder;

@Slf4j
public class AsyncSenderPool {

	ConcurrentHashMap<String, AsyncHttpClient> clientByHostPort = new ConcurrentHashMap<>();
	PropHelper prop;

	AsyncSenderPool(PropHelper prop) {
		this.prop = prop;
	}

	AsyncHttpClient createSSLClient(final String url) throws GeneralSecurityException, IOException {

		final StringBuffer ca_alias = new StringBuffer();

		prop.findMatch("org.zippo.http.ca.url.*", new IFinder() {
			@Override
			public void onMatch(String key, String value) {
				if (url.startsWith(value)) {
					ca_alias.append(key.substring("org.zippo.http.ca.url".length()));
				}
			}
		});
		String trustStoreFile = prop.get("org.zippo.http.ca.file" + ca_alias.toString(), "data/ca/client.keystore");
		String password = prop.get("org.zippo.http.ca.passwd" + ca_alias.toString(), "123456");

		// "/Users/brew/Documents/BC/codes/hd/docs/client.keystore"; // CA证书
		// String password = "123456";
		KeyStore ks = KeyStore.getInstance("JKS");
		try (InputStream keyStoreStream = new FileInputStream(new File(trustStoreFile))) {
			char[] keyStorePassword = password.toCharArray();
			ks.load(keyStoreStream, keyStorePassword);
		}
		// Set up key manager factory to use our key store
		char[] certificatePassword = password.toCharArray();
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, certificatePassword);

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		// tm = tmf.getTrustManagers();

		KeyManager[] keyManagers = kmf.getKeyManagers();
		SecureRandom secureRandom = new SecureRandom();

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagers, tmf.getTrustManagers(), secureRandom);
		SslEngineFactory factory = new JsseSslEngineFactory(sslContext);
		return asyncHttpClient(config().setSslEngineFactory(factory));
	}

	public AsyncHttpClient getAyncClientBy(String strurl) {
		try {
			URL url = new URL(strurl);
			String urlhead = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
			if (url.getPort() == -1 || url.getPort() == 80) {
				urlhead = url.getProtocol() + "://" + url.getHost();
			}

			AsyncHttpClient ret = clientByHostPort.get(urlhead);
			if (ret != null) {
				return ret;
			}
			synchronized (clientByHostPort) {
				ret = clientByHostPort.get(urlhead);
				if (ret != null) {
					return ret;
				}
				if (url.getProtocol().endsWith("s")) {// context https
					ret = createSSLClient(urlhead);
				} else {
					ret = asyncHttpClient();
				}
				clientByHostPort.put(urlhead, ret);
			}
			return ret;
		} catch (Exception e) {
			log.debug("getAyncClientException: " + strurl, e);
		}
		return null;
	}
}
