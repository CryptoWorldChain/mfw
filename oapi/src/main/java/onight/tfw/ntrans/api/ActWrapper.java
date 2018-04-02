package onight.tfw.ntrans.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.fc.zippo.filter.exception.FilterException;

import com.google.protobuf.Message;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.orouter.api.IQClient;
import onight.tfw.orouter.api.IRecievier;
import onight.tfw.orouter.api.NoneQService;
import onight.tfw.orouter.api.QService;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.ExceptionBody;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.beans.SendFailedBody;
import onight.tfw.otransio.api.beans.UnknowCMDBody;
import onight.tfw.otransio.api.session.CMDService;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;
import onight.tfw.proxy.IActor;

@iPojoBean
@Slf4j
public class ActWrapper implements IActor, IJPAClient, IQClient, PSenderService, CMDService {

	@Setter
	@Getter
	private QService qService = new NoneQService();

	public ActWrapper() {

	}

	public String getModule() {
		return "";
	}

	// @Getter
	// @Setter
	// @PSender
	// protected IPacketSender sender;

	@Override
	final public void sendMessage(String ex, Object wmsg) {
		qService.sendMessage(ex, SerializerUtil.toBytes(SerializerUtil.serialize(wmsg)));
	}

	private boolean qReady = false;
	private boolean daoReady = false;

	@Override
	final public void onQServiceReady() {
		qReady = true;
		registerMQ();
	}

	@Override
	final public void createMessageListener(String ex, IRecievier reciever) {
		qService.createMessageListener(this, ex, reciever, 0, 0);
	}

	@Override
	public void onDaoServiceReady(DomainDaoSupport dao) {

	}

	@Override
	final public String[] getWebPaths() {
		List<String> ret = new ArrayList<>();
		for (String cmd : getCmds()) {
			ret.add(("/" + getModule() + "/pb" + cmd + ".do").toLowerCase());
		}
		return ret.toArray(new String[] {});
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		FramePacket pack = PacketHelper.buildHeaderFromHttpGet(req);
		doWeb(req, resp, pack);
	}

	protected ISerializer jsons = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_JSON);

	public void doWeb(final HttpServletRequest req, final HttpServletResponse resp, final FramePacket pack)
			throws IOException {
		try {
			resp.setCharacterEncoding("UTF-8");
			resp.setHeader("Content-type", "application/json;charset=UTF-8");
			pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_REQUEST, req);
			pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_RESPONSE, resp);

			doPacketWithFilter(pack, new CompleteHandler() {
				@Override
				public void onFinished(FramePacket retpack) {
					try {
						if (retpack == null) {
							resp.getOutputStream().write(PacketHelper.toJsonBytes(
									PacketHelper.toPBErrorReturn(pack, ExceptionBody.EC_NOBODYRETURN, "")));
							return;
						}
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

						if (retpack.getFbody() != null & retpack.getFbody() instanceof Message) {
							Message msg = (Message) retpack.getFbody();
							String str = new JsonPBFormat().printToString(msg);
							retpack.getFixHead().genBytes();
							String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
									+ ",\"eh\":"
									+ new String(
											SerializerUtil.toBytes(jsons.serialize(retpack.getExtHead().getVkvs())))
									+ "" //
									+ ",\"body\":" + str + "" + "}";

							resp.getOutputStream().write(ret.getBytes("UTF-8"));

						} else if (retpack.getFixHead().getEnctype() == SerializerFactory.SERIALIZER_JSON
								&& retpack.getBody() != null) {
							String ret = "{\"fh\":\"" + (new String(retpack.getFixHead().genBytes())) + "\""//
									+ ",\"eh\":"
									+ new String(
											SerializerUtil.toBytes(jsons.serialize(retpack.getExtHead().getVkvs())))
									+ "" //
									+ ",\"body\":" + new String(retpack.getBody()) + "" + "}";
							resp.getOutputStream().write(ret.getBytes("UTF-8"));
							//
						} else {
							resp.getOutputStream().write(PacketHelper.toJsonBytes(
									PacketHelper.toPBReturn(pack, new ExceptionBody(ExceptionBody.EC_UNKNOW_SERAILTYPE,
											retpack.getFixHead().toStrHead()))));
						}
					} catch (Exception e) {
						log.debug("doweb error:", e);
						try {
							// FramePacket fp =
							// PacketHelper.toPBErrorReturn(pack,
							// ExceptionBody.EC_SERVICE_EXCEPTION,
							// "UNKNOW_ERROR:" + e.getMessage());
							resp.sendError(500, "UNKNOW_ERROR:" + e.getMessage());

							// resp.getOutputStream().write(PacketHelper.toJsonBytes(fp));
						} catch (IOException e1) {
							// e1.printStackTrace();
							log.debug("error response:", e);
						}
					} finally {
						try {
							CompleteHandler handler = (CompleteHandler) req.getAttribute("__POST_HANDLER");
							if (handler != null) {
								handler.onFinished(retpack);
							}
						} catch (Exception e) {
							log.debug("doweb filter error:", e);
						}
					}
				}

				@Override
				public void onFailed(Exception e) {
					try {
						resp.sendError(500, "UNKNOW_ERROR:" + e.getMessage());
					} catch (IOException e1) {
						log.error("unknow error:" + e.getMessage(), e1);
					}
				}
			});
		} catch (Exception e) {
			log.debug("doweb error:", e);
			try {
				resp.sendError(500, "UNKNOW_ERROR:" + e.getMessage());
			} catch (IOException e1) {
				log.error("unknow error:" + e.getMessage(), e1);
			}
		}

	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		FramePacket pack = PacketHelper.buildHeaderFromHttpPost(req);
		doWeb(req, resp, pack);
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		FramePacket pack = PacketHelper.buildHeaderFromHttpPost(req);
		doWeb(req, resp, pack);
	}

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		FramePacket pack = PacketHelper.buildHeaderFromHttpPost(req);
		doWeb(req, resp, pack);
	}

	@Override
	public String[] getCmds() {
		return new String[] {};
	}

	@ActorRequire(name = "filterManager", scope = "global")
	@Setter
	@Getter
	public FilterManager fm = new NoneFilterManager();

	@Override
	final public void doPacketWithFilter(FramePacket pack, final CompleteHandler handler) {
		try {
			if (!fm.preRouteListner(this, pack, handler)) {
				throw new FilterException("FilterBlock!");
			}
			onPacket(pack, new CompleteHandler() {

				@Override
				public void onFinished(FramePacket endpack) {
					try {
						handler.onFinished(endpack);
					} finally {
						fm.onCompleteListner(ActWrapper.this, endpack);
					}

				}

				@Override
				public void onFailed(Exception e) {
					try {
						handler.onFailed(e);
					} finally {
						fm.onErrorListner(ActWrapper.this, null);
					}
				}
			});
		} catch (FilterException e) {
			handler.onFinished(PacketHelper.toPBErrorReturn(pack, ExceptionBody.EC_FILTER_EXCEPTION,
					"FilterBlocked:" + e.getMessage()));
		} catch (Throwable e) {
			e.printStackTrace();
			log.debug("doPacketWithFilterError:", e);
			handler.onFinished(PacketHelper.toPBErrorReturn(pack, ExceptionBody.EC_SERVICE_EXCEPTION, e.getMessage()));
		} finally {
			fm.postRouteListner(this, pack, handler);
		}
	}

	public void onPacket(FramePacket pack, CompleteHandler handler) {
		if (handler != null) {
			handler.onFinished(PacketHelper.toPBReturn(pack, new UnknowCMDBody("NOT IMPL", pack)));
		}
	}

	@Override
	public void onDaoServiceAllReady() {
		daoReady = true;
		registerMQ();
	}

	public boolean isResourceReady() {
		return daoReady && qReady;
	}

	public void registerMQ() {
		if (!isResourceReady()) {
			return;
		}
		for (String cmd : getCmds()) {
			createMessageListener(cmd + "" + getModule(), new IRecievier() {
				@Override
				public boolean onMessage(final String ex, Serializable wmsg) {
					final FramePacket pack = PacketHelper.buildPacketFromTransBytes((byte[]) wmsg);
					doPacketWithFilter(pack, new CompleteHandler() {
						@Override
						public void onFinished(FramePacket packet) {
							if (pack.isSync()) {
								String qid = pack.getExtStrProp(ex + ".QID");
								if (StringUtils.isNotBlank(qid)) {
									qService.sendMessage(qid, PacketHelper.toTransBytes(packet));
								}
							}
						}

						@Override
						public void onFailed(Exception e) {
							if (pack.isSync()) {
								String qid = pack.getExtStrProp(ex + ".QID");
								if (StringUtils.isNotBlank(qid)) {
									qService.sendMessage(qid, PacketHelper.toTransBytes(PacketHelper
											.toPBErrorReturn(pack, e.getMessage(), e.getCause().getMessage())));
								}
							}
						}
					});

					return true;
				}
			});
		}
	}
}
