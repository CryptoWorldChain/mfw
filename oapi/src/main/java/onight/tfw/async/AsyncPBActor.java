
package onight.tfw.async;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.PBActor;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.ExceptionBody;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;

import com.google.protobuf.Message;

@Slf4j
public abstract class AsyncPBActor<T extends Message> extends PBActor<T> {

	long def_timeout = new PropHelper(null).get("tfw.async.timeout", 60000);

	@Override
	public void doWeb(final HttpServletRequest req, final HttpServletResponse resp, final FramePacket pack)
			throws IOException {
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Content-type", "application/json;charset=UTF-8");
		pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_REQUEST, req);
		pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_RESPONSE, resp);

		final AsyncContext asyncContext = req.startAsync();
		String timeout = req.getHeader("tfw_timeout");
		if (StringUtils.isNumeric(timeout)) {
			asyncContext.setTimeout(Integer.parseInt(timeout));
		} else {
			asyncContext.setTimeout(def_timeout);
		}
		asyncContext.start(new Runnable() {
			@Override
			public void run() {
				try {
					doPacketWithFilter(pack, new CompleteHandler() {
						@Override
						public void onFinished(FramePacket retpack) {
							try {
								if (retpack == null) {
									resp.getOutputStream().write(PacketHelper.toJsonBytes(
											PacketHelper.toPBErrorReturn(pack, ExceptionBody.EC_NOBODYRETURN, "")));
									return;
								}
								retpack.getExtHead().buildFor(resp);
								boolean bodyOnly = false;
								if (StringUtils.equalsIgnoreCase("bd", retpack.getExtStrProp("resp"))) {
									bodyOnly = true;
								}

								if (retpack.getFbody() != null & retpack.getFbody() instanceof Message) {
									if (retpack.getFixHead().getEnctype() == 'P') {
										byte[] bodyb = retpack.genBodyBytes();
										byte[] extb = retpack.genExtBytes();
										retpack.getFixHead().setExtsize(extb.length);
										retpack.getFixHead().setBodysize(bodyb.length);
										if (!bodyOnly) {
											resp.getOutputStream().write(retpack.getFixHead().genBytes());
											resp.getOutputStream().write(extb);
										}
										resp.getOutputStream().write(bodyb);
									} else {
										Message msg = (Message) retpack.getFbody();
										String str = new JsonPBFormat().printToString(msg);
										if (bodyOnly) {
											resp.getOutputStream().write(str.getBytes("UTF-8"));
										} else {
											retpack.getFixHead().genBytes();

											String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes()))
													+ "\""//
													+ ",\"eh\":"
													+ new String(SerializerUtil
															.toBytes(jsons.serialize(retpack.getExtHead().getVkvs())))
													+ "" //
													+ ",\"body\":" + str + "" + "}";
											resp.getOutputStream().write(ret.getBytes("UTF-8"));

										}

									}

								} else if (retpack.getFbody() != null
										&& retpack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_JSON) {

									if (bodyOnly) {
										resp.getOutputStream().write((byte[]) jsons.serialize(retpack.getFbody()));
									} else {
										String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
												+ ",\"eh\":"
												+ new String((byte[]) (jsons.serialize(retpack.getExtHead().getVkvs())))
												+ "" //
												+ ",\"body\":"
												+ new String((byte[]) jsons.serialize(retpack.getFbody())) + "" + "}";

										resp.getOutputStream().write(ret.getBytes("UTF-8"));
									}

									//
								} else if (retpack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_JSON
										&& retpack.getBody() != null) {
									if (bodyOnly) {
										resp.getOutputStream().write(retpack.getBody());

									} else {
										String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
												+ ",\"eh\":"
												+ new String((byte[]) (jsons.serialize(retpack.getExtHead().getVkvs())))
												+ "" //
												+ ",\"body\":" + new String(retpack.getBody()) + "" + "}";
										resp.getOutputStream().write(ret.getBytes("UTF-8"));
									}

									//
								} else {
									resp.getOutputStream()
									.write(PacketHelper.toJsonBytes(PacketHelper.toPBReturn(pack,
											new ExceptionBody(ExceptionBody.EC_UNKNOW_SERAILTYPE,
													retpack.getFixHead().toStrHead()))));
								}
							} catch (Exception e) {
								log.debug("doweb error:", e);
								try {
									FramePacket fp = PacketHelper.toPBErrorReturn(pack, ExceptionBody.EC_SERVICE_EXCEPTION,
											"UNKNOW_ERROR:" + e.getMessage());
									resp.getOutputStream().write(PacketHelper.toJsonBytes(fp));
								} catch (IOException e1) {
									// e1.printStackTrace();
									log.debug("error response:", e);
								}
							} finally {
								asyncContext.complete();
							}

						}
					});
				} catch (Exception e) {
					log.debug("doweb error:", e);
					try {
						resp.getOutputStream().write(PacketHelper.toJsonBytes(PacketHelper.toPBErrorReturn(pack,
								ExceptionBody.EC_SERVICE_EXCEPTION, "UNKNOW_ERROR:" + e.getMessage())));
					} catch (IOException e1) {
						// e1.printStackTrace();
						log.debug("error response:", e);
					} finally {
						asyncContext.complete();
					}
				}
			}
		});

	}
}
