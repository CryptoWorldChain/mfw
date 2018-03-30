package org.fc.zippo.sender.httpimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;

import com.google.protobuf.Message;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.cookie.Cookie;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.async.FutureSender;
import onight.tfw.async.OFuture;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FormParamBody;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.FormDataSerializer;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;

@Component
@Instantiate(name = "http")
@Provides(specifications = { ActorService.class })
@Slf4j
@Data
public class HttpSender extends FutureSender implements ActorService, IPacketSender {

	@ServiceProperty(name = "name")
	String name = "http";

	PropHelper prop = new PropHelper(null);

	AsyncSenderPool clientPool = new AsyncSenderPool(prop);

	String backend = "http://localhost:8000/";

	@Validate
	public void startup() {
		prop = new PropHelper(null);
		clientPool.prop = prop;
		backend = prop.get("org.zippo.http.backend.url", "http://localhost:8000/").trim();
		if (!backend.endsWith("/")) {
			backend = backend + "/";
		}

		if (StringUtils.isBlank(System.getProperty("com.ning.http.client.AsyncHttpClientConfig.acceptAnyCertificate"))) {
			System.setProperty("com.ning.http.client.AsyncHttpClientConfig.acceptAnyCertificate", "true");
		}
	}

	protected ISerializer jsons = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_JSON);

	public void writePx(FramePacket retpack, OutputStream output, BoundRequestBuilder builder) throws IOException {
		if (retpack.getFbody() != null) {
			if (retpack.getFixHead().getEnctype() == 'P') {
				if (retpack.getFbody() instanceof Message) {
					byte[] bodyb = retpack.genBodyBytes();
					output.write(bodyb);
				} else if (retpack.getFbody() instanceof String) {
					output.write(((String) retpack.getFbody()).getBytes("UTF-8"));
				} else if (retpack.getFbody() instanceof byte[]) {
					output.write((byte[]) retpack.getFbody());
				} else {
					output.write((byte[]) jsons.serialize(retpack.getFbody()));
				}
			} else if (retpack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_JSON) {
				builder.setHeader("Content-Type", "application/json");
				if (retpack.getFbody() instanceof Message) {
					Message msg = (Message) retpack.getFbody();
					String str = new JsonPBFormat().printToString(msg);
					output.write(str.getBytes("UTF-8"));
				} else if (retpack.getFbody() instanceof String) {
					output.write(((String) retpack.getFbody()).getBytes("UTF-8"));
				} else if (retpack.getFbody() instanceof byte[]) {
					output.write((byte[]) retpack.getFbody());
				} else {
					output.write((byte[]) jsons.serialize(retpack.getFbody()));
				}

			} else if (retpack.getFbody() instanceof FormParamBody) {
				FormParamBody fpb = (FormParamBody) retpack.getFbody();
				for (Entry<String, String> entry : fpb.getItems().entrySet()) {
					builder.addFormParam(entry.getKey(), entry.getValue());
				}
			} else if (retpack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_FORMDATA) {
				String formstr = "";
				if (retpack.getFbody() instanceof String)
					formstr = (String) retpack.getFbody();
				else if (retpack.getFbody() instanceof byte[])
					formstr = new String((byte[]) retpack.getFbody());
				else
					formstr = (String)FormDataSerializer.getInstance().serialize(retpack.getFbody());
				for (String kvs : formstr.split("&")) {
					String pair[] = kvs.split("=");
					if(pair.length==2)
					{
						builder.addFormParam(pair[0], pair[1]);
					}
				}
			} else {
				output.write(retpack.getBody());
			}
		}
	}

	public void appendCookie(FramePacket pack, BoundRequestBuilder builder) {
		if (pack.getExtHead() != null && pack.getExtHead().getHiddenkvs() != null)
			for (Entry<String, Object> pair : pack.getExtHead().getHiddenkvs().entrySet()) {
				if (pair.getKey().startsWith(PackHeader.EXT_HIDDEN) && !pair.getKey().startsWith(PackHeader.EXT_IGNORE_RESPONSE)) {
					try {
						builder.addCookie(new Cookie(pair.getKey(), "" + pair.getValue(), true, null, "/", Long.MAX_VALUE, false, false));
						// addCookie(res, pair.getKey(), pair.getValue());
					} catch (Exception e) {
						log.debug("add cookie fail:", e);
					}
				}
			}
		if (pack.getExtHead() != null && pack.getExtHead().getVkvs() != null) {
			Map<String, Object> cookies = (Map<String, Object>) pack.getExtHead().getVkvs().get(PackHeader.Set_COOKIE);
			if (cookies != null && cookies.size() > 0) {
				for (Entry<String, Object> pair : cookies.entrySet()) {
					try {
						builder.addCookie(new Cookie(pair.getKey(), "" + pair.getValue(), true, null, "/", Long.MAX_VALUE, false, false));
					} catch (Exception e) {
						log.debug("add cookie fail:", e);
					}
				}
			}
		}
	}

	public void appendHeader(FramePacket pack, BoundRequestBuilder builder) {
		if (pack.getExtHead() != null && pack.getExtHead().getVkvs() != null) {
			for (Entry<String, Object> pair : pack.getExtHead().getVkvs().entrySet()) {
				if (!pair.getKey().startsWith(PackHeader.EXT_HIDDEN)) {
					try {
						builder.setHeader(pair.getKey(), pair.getValue() + "");
					} catch (Exception e) {
						log.debug("add cookie fail:", e);
					}
				}
			}
		}
	}

	@Override
	public void asyncSend(final FramePacket pack, final CallBack<FramePacket> cb) {

		String desturl = pack.getExtStrProp(PackHeader.FORWORD_URL);
		if (StringUtils.isBlank(desturl)) {
			desturl = backend + pack.getModule().toLowerCase() + "/pb" + pack.getCMD().toLowerCase() + ".do?fh=" + pack.getFixHead().toStrHead() + "&resp=bd&1=1";
		}
		String method = pack.getExtStrProp(PackHeader.FORWORD_METHOD);
		BoundRequestBuilder builder = null;
		AsyncHttpClient asyncHttpClient = clientPool.getAyncClientBy(desturl);
		if (method == null || StringUtils.equalsIgnoreCase(method, "get")) {
			builder = asyncHttpClient.prepareGet(desturl);
		} else if (StringUtils.equalsIgnoreCase(method, "delete")) {
			builder = asyncHttpClient.prepareDelete(desturl);
		} else if (StringUtils.equalsIgnoreCase(method, "put")) {
			builder = asyncHttpClient.preparePut(desturl);
		} else {
			builder = asyncHttpClient.preparePost(desturl);
		}

		try (ByteArrayOutputStream output = new ByteArrayOutputStream(16 + pack.getFixHead().getTotalSize())) {
			writePx(pack, output, builder);
			byte body[] = output.toByteArray();
			if (body != null && body.length > 0) {
				builder.setBody(body);
			}
			appendCookie(pack, builder);
			appendHeader(pack, builder);
			final FramePacket ret = PacketHelper.clonePacket(pack);
			final String url = desturl;
			builder.execute(new AsyncCompletionHandler<FramePacket>() {

				@Override
				public void onThrowable(Throwable t) {
					log.debug("http send error:url="+url, t);
					cb.onFailed(new RuntimeException(t), pack);
				}

				@Override
				public FramePacket onCompleted(com.ning.http.client.Response response) throws Exception {
					ret.setBody(response.getResponseBodyAsBytes());
					log.debug("ret:" + new String(ret.getBody(), "UTF-8"));
					cb.onSuccess(ret);
					return ret;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}

	}

	@Override
	public void post(FramePacket pack) {
		asyncSend(pack, new CallBack<FramePacket>() {

			@Override
			public void onFailed(Exception arg0, FramePacket arg1) {

			}

			@Override
			public void onSuccess(FramePacket arg0) {

			}
		});
	}

	@Override
	public FramePacket send(FramePacket pack, long waitsecond) {
		final OFuture<FramePacket> future = new OFuture<FramePacket>();
		final CountDownLatch ll = new CountDownLatch(1);
		asyncSend(pack, new CallBack<FramePacket>() {

			@Override
			public void onFailed(Exception arg0, FramePacket arg1) {
				future.result(arg1);
				ll.countDown();
			}

			@Override
			public void onSuccess(FramePacket arg0) {
				future.result(arg0);
				ll.countDown();
			}
		});
		try {
			ll.await(waitsecond, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return future.getResult();
	}

	@Override
	public void changeNodeName(String arg0, String arg1) {
		
	}

	@Override
	public void setCurrentNodeName(String arg0) {
		
	}

	@Override
	public void tryDropConnection(String arg0) {
		
	}

}
