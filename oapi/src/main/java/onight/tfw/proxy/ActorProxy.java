package onight.tfw.proxy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.Message;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.orouter.api.IQClient;
import onight.tfw.orouter.api.IRecievier;
import onight.tfw.orouter.api.NoneQService;
import onight.tfw.orouter.api.QService;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PSender;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.HttpHelper;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;

@iPojoBean
@Slf4j
@Deprecated 
public abstract class ActorProxy<T extends Message> implements IActor, IJPAClient, IQClient,PSenderService {

	private QService qService = new NoneQService();

	@Getter
	@Setter
	@PSender
	private IPacketSender oSocket;
	
	
	@Setter
	@Getter
	private int daoAutoWareCount;
	
	
	@Override
	public String[] getWebPaths() {
		String cmd = getCMD();
		if (cmd != null) {
			return new String[]{"/pb" + cmd + ".do"};
		}
		return null;
	}

	private String getCMD() {
		return null;
	}

	/**
	 * 创建队列消息
	 */
	@Override
	public final void sendMessage(final String ex, final Object wmsg) {
		qService.sendMessage(ex, wmsg);
	}

	ISerializer pbserializer = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_PROTOBUF);

	/**
	 * 发送消息
	 */
	public final FramePacket sendPBMsg(String cmd, FramePacket pack) {
		return oSocket.send(pack,60*1000);
	}

	/**
	 * 发送消息
	 */
	public final void postPBMsg(String cmd, FramePacket pack) {
		oSocket.post(pack);
	}

	public FramePacket genOPacket(String cmd, Message load, String from, String to) {
//		return FramePacket.genPBPack(cmd, load, from, to);
		return null;
	}

	public final void createMessageListener(String ex, IRecievier reciever) {
		log.debug("创建队列" + ex + "::" + qService);
		qService.createMessageListener(this, ex, reciever, 0, 0);
	}

	/**
	 * 获取Q队列数据后处理
	 */

	@Override
	public void onQServiceReady() {

	}

	/**
	 * DAO注册时回调
	 */
	@Override
	public void onDaoServiceReady(DomainDaoSupport dao) {
		
	}


	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String reqstr = req.getParameter("data");
		doWeb(req, reqstr, resp);
	}

	public void doWeb(HttpServletRequest req, String reqstr, HttpServletResponse resp) throws IOException {
		String cmd = getCMD();
		if (!StringUtils.isBlank(reqstr) && !StringUtils.isBlank(cmd)) {
//			!!TODO 从json过来的字符转换
//			FramePacket pack = FramePacket(cmd, reqstr);
//			pack.putHeader(PackHeader.PEER_IP, getIpAddr(req));
//			PackHeader.buildHeaderFromHttp(pack, req);
//			FramePacket retpack = onCMD(pack);
//			if (retpack != null && retpack.getRawdatas() != null) {
//				resp.getOutputStream().write(retpack.getRawdatas());
//			}
		} else {
			resp.getWriter().write("POST NOT SUPPORT");
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doWeb(req, HttpHelper.getRequestContent(req), resp);
	}

	
	public final QService getQService() {
		return qService;
	}

	public final void setQService(QService qService) {
		this.qService = qService;
	}

	
	

//	public Builder getPBBuilder() {
//		String type = getCMD();
//		if (type != null)
//			try {
//				Method method = type.getMclazz().getMethod("newBuilder", null);
//				return (Builder) method.invoke(null, null);
//			} catch (Exception e) {
//				log.warn("cannot found pb builder for class" + type.getMclazz(), e);
//			}
//		return null;
//	}

	@Override
	public void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("PUT NOT SUPPORT");
		
	}

	@Override
	public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("DELETE NOT SUPPORT");
		
	}

}
