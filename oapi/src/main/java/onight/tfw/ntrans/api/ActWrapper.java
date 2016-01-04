package onight.tfw.ntrans.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.orouter.api.IQClient;
import onight.tfw.orouter.api.IRecievier;
import onight.tfw.orouter.api.NoneQService;
import onight.tfw.orouter.api.QService;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.ExceptionBody;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.otransio.api.beans.UnknowCMDBody;
import onight.tfw.otransio.api.session.CMDService;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;
import onight.tfw.proxy.IActor;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

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
	public void onDaoServiceReady(OJpaDAO<?> dao) {

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
		FramePacket pack = PacketHelper.buildHeaderFromHttpGet(req);
		doWeb(req, resp, pack);
	}

	protected ISerializer jsons = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_JSON);

	public void doWeb(HttpServletRequest req, final HttpServletResponse resp, final FramePacket pack) throws IOException {
		onPacket(pack, new CompleteHandler() {
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
				}
			}
		});

	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		FramePacket pack = PacketHelper.buildHeaderFromHttpPost(req);
		doWeb(req, resp, pack);
	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("PUT NOT SUPPORT");

	}

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("DELETE NOT SUPPORT");
	}

	@Override
	public String[] getCmds() {
		return new String[] {};
	}

	@Override
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
					onPacket(pack, new CompleteHandler() {
						@Override
						public void onFinished(FramePacket packet) {
							if (pack.isSync()) {
								String qid = pack.getExtStrProp(ex + ".QID");
								if (StringUtils.isNotBlank(qid)) {
									qService.sendMessage(qid, PacketHelper.toTransBytes(packet));
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
