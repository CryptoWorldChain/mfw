package onight.tfw.async;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.Message;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.PBActor;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.ExceptionBody;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.pool.ReusefulLoopPool;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;

@Slf4j
public class ActorRunner implements Runnable {

	FramePacket pack;
	HttpServletResponse resp;
	AsyncContext asyncContext;
	PBActor actor;

	public static int actorPoolSize = new PropHelper(null).get("org.zippo.jetty.actor.poolsize", 1000);
	protected static ReusefulLoopPool<ActorRunner> actorPool = new ReusefulLoopPool<>();

	public void reset(FramePacket pack, HttpServletResponse resp, AsyncContext asyncContext, PBActor actor) {
		this.pack = pack;
		this.resp = resp;
		this.asyncContext = asyncContext;
		this.actor = actor;
	}

	protected ISerializer jsons = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_JSON);

	CompleteHandler handler = new CompleteHandler() {
		@Override
		public void onFinished(FramePacket retpack) {
			try {
				if (retpack == null) {
					resp.getOutputStream().write(PacketHelper
							.toJsonBytes(PacketHelper.toPBErrorReturn(pack, ExceptionBody.EC_NOBODYRETURN, "")));
					return;
				}
				if (retpack.getExtProp(PackHeader.EXT_IGNORE_HTTP_RESPONSE + "_sended") != null) {
					return;
				}
				retpack.putHeader(PackHeader.EXT_IGNORE_HTTP_RESPONSE + "_sended", "1");
				resp.setHeader("Content-type", "application/json;charset=UTF-8");
				if (retpack.getFbody() instanceof ExceptionBody) {
					ExceptionBody eb = (ExceptionBody) retpack.getFbody();
					try {
						int errcode = Integer.parseInt(eb.getErrCode());
						resp.sendError(errcode, eb.getErrMsg());
					} catch (Exception e) {
						resp.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, eb.getErrMsg());
					}
					return;
				}
				;
				retpack.getExtHead().buildFor(resp);
				boolean bodyOnly = false;
				if (retpack.getExtStrProp("resp") == null
						|| StringUtils.equalsIgnoreCase("bd", retpack.getExtStrProp("resp"))) {
					bodyOnly = true;
				}

				if (retpack.getFbody() != null && retpack.getFbody() instanceof Message) {
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
							String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
									+ ",\"eh\":"
									+ new String(
											SerializerUtil.toBytes(jsons.serialize(retpack.getExtHead().getVkvs())))
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
								+ ",\"eh\":" + new String((byte[]) (jsons.serialize(retpack.getExtHead().getVkvs())))
								+ "" //
								+ ",\"body\":" + new String((byte[]) jsons.serialize(retpack.getFbody())) + "" + "}";
						resp.getOutputStream().write(ret.getBytes("UTF-8"));
					}
					//
				} else if (retpack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_JSON
						&& retpack.getBody() != null) {
					if (bodyOnly) {
						resp.getOutputStream().write(retpack.getBody());

					} else {
						String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
								+ ",\"eh\":" + new String((byte[]) (jsons.serialize(retpack.getExtHead().getVkvs())))
								+ "" //
								+ ",\"body\":" + new String(retpack.getBody()) + "" + "}";
						resp.getOutputStream().write(ret.getBytes("UTF-8"));
					}

					//
				} else {
					resp.getOutputStream().write(PacketHelper.toJsonBytes(PacketHelper.toPBReturn(pack,
							new ExceptionBody(ExceptionBody.EC_UNKNOW_SERAILTYPE, retpack.getFixHead().toStrHead()))));
				}
			} catch (Exception e) {
				log.error("doweb error:", e);
				try {
					resp.sendError(500, "UNKNOW_ERROR:" + e.getMessage());
				} catch (IOException e1) {
					log.error("error response:", e);
				}
			} finally {
				asyncContext.complete();
				if (actorPool.size() < actorPoolSize) {
					actorPool.retobj(ActorRunner.this);
				}
			}
		}

		@Override
		public void onFailed(Exception e) {
			try {
				resp.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, e.getMessage());
			} catch (IOException e1) {
				log.error("onfailed:", e1);
			} finally {
				asyncContext.complete();
				if (actorPool.size() < actorPoolSize) {
					actorPool.retobj(ActorRunner.this);
				}
			}
		}
	};

	@Override
	public void run() {
		try {
			// doPacketWithFilter(pack
			actor.doPacketWithFilter(pack, handler);
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
}
