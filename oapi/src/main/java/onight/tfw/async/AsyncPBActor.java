package onight.tfw.async;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.PBActor;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.ExceptionBody;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

@Slf4j
public abstract class AsyncPBActor<T extends Message> extends PBActor<T> {

	@Override
	public void doWeb(final HttpServletRequest req, final HttpServletResponse resp, final FramePacket pack) throws IOException {
		final AsyncContext asyncContext = req.startAsync();
		asyncContext.start(new Runnable() {
			@Override
			public void run() {
				doPacketWithFilter(pack, new CompleteHandler() {
					@Override
					public void onFinished(FramePacket retpack) {
						try {
							if (retpack == null) {
								resp.getOutputStream().write(PacketHelper.toJsonBytes(PacketHelper.toPBReturn(pack, new ExceptionBody("", pack))));
								return;
							}
							retpack.getExtHead().buildFor(resp);
							if (retpack.getFbody() != null & retpack.getFbody() instanceof Message) {
								Message msg = (Message) retpack.getFbody();
								String str = JsonFormat.printToString(msg);
								retpack.getFixHead().genBytes();
								String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
										+ ",\"eh\":" + new String(SerializerUtil.toBytes(jsons.serialize(retpack.getExtHead().getVkvs()))) + "" //
										+ ",\"body\":" + str + "" + "}";

								resp.getOutputStream().write(ret.getBytes("UTF-8"));

							} else if (retpack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_JSON && retpack.getBody() != null) {
								String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
										+ ",\"eh\":" + new String(SerializerUtil.toBytes(jsons.serialize(retpack.getExtHead().getVkvs()))) + "" //
										+ ",\"body\":" + new String(retpack.getBody()) + "" + "}";

								resp.getOutputStream().write(ret.getBytes("UTF-8"));

								//
							} else {
								resp.getOutputStream().write(PacketHelper.toJsonBytes(PacketHelper.toPBReturn(pack, new ExceptionBody("", pack))));
							}
						} catch (IOException e) {
							log.debug("doweb error:", e);
						} finally {
							asyncContext.complete();
						}
					}
				});

			}
		});

	}

}
