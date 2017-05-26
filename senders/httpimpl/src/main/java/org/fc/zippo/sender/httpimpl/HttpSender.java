package org.fc.zippo.sender.httpimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
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
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.async.FutureSender;
import onight.tfw.async.OFuture;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.CookieBean;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;

@Component
@Instantiate(name = "http")
@Provides(specifications = { ActorService.class })
@Slf4j
@Data
public class HttpSender extends FutureSender implements ActorService, IPacketSender {

	@ServiceProperty(name = "name")
	String name = "http";

	AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

	PropHelper prop = new PropHelper(null);

	String backend = "http://localhost:8000/";

	@Validate
	public void startup() {
		prop = new PropHelper(null);
		backend = prop.get("org.zippo.http.backend.url", "http://localhost:8000/").trim();
		if (!backend.endsWith("/")) {
			backend = backend + "/";
		}
	}

	protected ISerializer jsons = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_JSON);

	public void writePx(FramePacket retpack, OutputStream output) throws IOException {
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
			} else {
				output.write(retpack.getBody());
			}
		}
	}

	public void appendCookie(FramePacket pack, BoundRequestBuilder builder) {
		if (pack.getExtHead() != null && pack.getExtHead().getHiddenkvs() != null)
			for (Entry<String, Object> pair : pack.getExtHead().getHiddenkvs().entrySet()) {
				if (pair.getKey().startsWith(PackHeader.EXT_HIDDEN)
						&& !pair.getKey().startsWith(PackHeader.EXT_IGNORE_RESPONSE)) {
					try {
						builder.addCookie(new Cookie(pair.getKey(), "" + pair.getValue(), true, null, "/",
								Long.MAX_VALUE, false, false));
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
						builder.addCookie(new Cookie(pair.getKey(), "" + pair.getValue(), true, null, "/",
								Long.MAX_VALUE, false, false));
					} catch (Exception e) {
						log.debug("add cookie fail:", e);
					}
				}
			}
		}
		

	}

	@Override
	public void asyncSend(final FramePacket pack, final CallBack<FramePacket> cb) {

		String desturl = pack.getExtStrProp("url");
		if (StringUtils.isBlank(desturl)) {
			desturl = backend + pack.getModule().toLowerCase() + "/pb" + pack.getCMD().toLowerCase() + ".do?fh="
					+ pack.getFixHead().toStrHead() + "&resp=bd&1=1";
		}
		val builder = asyncHttpClient.preparePost(desturl);

		try (ByteArrayOutputStream output = new ByteArrayOutputStream(16 + pack.getFixHead().getTotalSize())) {
			writePx(pack, output);
			builder.setBody(output.toByteArray());
			appendCookie(pack,builder);
			final FramePacket ret = PacketHelper.clonePacket(pack);
			builder.execute(new AsyncCompletionHandler<FramePacket>() {

				@Override
				public void onThrowable(Throwable t) {
					// Something wrong happened.
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

}
