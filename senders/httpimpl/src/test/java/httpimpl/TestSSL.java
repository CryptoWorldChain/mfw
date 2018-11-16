package httpimpl;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.SslEngineFactory;
import org.asynchttpclient.netty.ssl.JsseSslEngineFactory;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.JsonSerializer;

@Slf4j
public class TestSSL {
	static TrustManager[] tm = null;

	private static KeyManager[] createKeyManagers() throws GeneralSecurityException, IOException {

		String trustStoreFile = "/Users/brew/Documents/BC/codes/hd/docs/client.keystore"; // CA证书

		String password = "123456";

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
		tm = tmf.getTrustManagers();

		// Initialize the SSLContext to work with our key managers.
		return kmf.getKeyManagers();
	}

	public static void main(String[] args) {

		System.setProperty("com.ning.http.client.AsyncHttpClientConfig.acceptAnyCertificate", "true");

		try {

			KeyManager[] keyManagers = createKeyManagers();
			SecureRandom secureRandom = new SecureRandom();
			SSLContext sslContext = SSLContext.getInstance("TLS");
			System.out.println("tm==" + tm);
			sslContext.init(keyManagers, tm, secureRandom);
			
			SslEngineFactory factory = new JsseSslEngineFactory(sslContext);

			final AsyncHttpClient asyncHttpClient= asyncHttpClient(config().setSslEngineFactory(factory));
			
//			AsyncHttpClientConfig.Builder configbuilder = new AsyncHttpClientConfig.Builder();
//			configbuilder.setSSLContext(sslContext);
//			final AsyncHttpClient asyncHttpClient = new AsyncHttpClient(configbuilder.build());

			BoundRequestBuilder builder = asyncHttpClient.preparePost("https://58.247.119.27/sh-cupi/restFul/getToken");
			builder.addFormParam("loginId", "bjweijing_test");
			// builder.addFormParam("passWord", "bjweijing1234");
			builder.addFormParam("passWord", "a6619c99244cc7f4d371559f7517b31b11fa932e");
			// builder.addFormParam("loginId", "bjweijing_test");
			log.debug("exec!!");
			builder.execute(new AsyncCompletionHandler<FramePacket>() {

				@Override
				public void onThrowable(Throwable t) {
					// Something wrong happened.
					// ret.setBody(response.getResponseBodyAsBytes());
					log.debug("error", t);
				}

				@Override
				public FramePacket onCompleted(Response response) throws Exception {
					log.debug("ret:" + new String(response.getResponseBodyAsBytes(), "UTF-8"));
					HashMap<String, String> retobj = new HashMap<>();
					retobj = JsonSerializer.getInstance().deserialize(response.getResponseBodyAsBytes(), retobj.getClass());
					String signature = retobj.get("signature");
					if (StringUtils.isNotBlank(signature)) {
						System.out.println("get Signature:" + signature);
						//doValidPhoneLocation(asyncHttpClient,signature);
						doValidCityLocation(asyncHttpClient,signature);
					}

					return null;
				}
			});

			Thread.sleep(60000);

			asyncHttpClient.close();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	static void doValidPhoneLocation(AsyncHttpClient asyncHttpClient, String token) {
		BoundRequestBuilder builder = asyncHttpClient.preparePost("https://58.247.119.27/sh-cupi/restFul/rtLocationCheck");
		builder.addFormParam("token", token);
		builder.addFormParam("loginId", "bjweijing_test");
		builder.addFormParam("cusCode", "200003");
		builder.addFormParam("telephone", "18515590189");
		builder.addFormParam("latitude", "39.9941700000");
		builder.addFormParam("longitude", "116.4527700000");
		log.debug("exec!!");
		builder.execute(new AsyncCompletionHandler<FramePacket>() {

			@Override
			public void onThrowable(Throwable t) {
				// Something wrong happened.
				// ret.setBody(response.getResponseBodyAsBytes());
				log.debug("error", t);
			}

			@Override
			public FramePacket onCompleted(Response response) throws Exception {
				log.debug("ret:" + new String(response.getResponseBodyAsBytes(), "UTF-8"));
				HashMap<String, String> retobj = new HashMap<>();
				retobj = JsonSerializer.getInstance().deserialize(response.getResponseBodyAsBytes(), retobj.getClass());
				String distance = retobj.get("distance");
				System.out.println("get distance:" + distance);
				return null;
			}
		});

	}
	
	
	static void doValidCityLocation(AsyncHttpClient asyncHttpClient, String token) {
		BoundRequestBuilder builder = asyncHttpClient.preparePost("https://58.247.119.27/sh-cupi/restFul/currentCity");
		builder.addFormParam("token", token);
		builder.addFormParam("loginId", "bjweijing_test");
		builder.addFormParam("cusCode", "200003");
		builder.addFormParam("telephone", "18515590189");
		SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
		System.out.println(sdf.format(new Date()));
		builder.addFormParam("userPosition", "V0110001");
		//builder.addFormParam("userPosition",  "100102");
		log.debug("exec!!::"+sdf.format(new Date())+"V0110000");
		builder.execute(new AsyncCompletionHandler<FramePacket>() {

			@Override
			public void onThrowable(Throwable t) {
				// Something wrong happened.
				// ret.setBody(response.getResponseBodyAsBytes());
				log.debug("error", t);
			}

			@Override
			public FramePacket onCompleted(Response response) throws Exception {
				log.debug("ret:" + new String(response.getResponseBodyAsBytes(), "UTF-8"));
				HashMap<String, String> retobj = new HashMap<>();
				retobj = JsonSerializer.getInstance().deserialize(response.getResponseBodyAsBytes(), retobj.getClass());
				String distance = retobj.get("distance");
				System.out.println("get distance:" + distance);
				return null;
			}
		});

	}
}
