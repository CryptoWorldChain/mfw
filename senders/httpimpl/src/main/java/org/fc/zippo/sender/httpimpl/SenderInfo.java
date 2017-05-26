package org.fc.zippo.sender.httpimpl;

import org.fc.zippo.ordbutils.pbgens.Ordb.PRetQueryBySQL;
import org.fc.zippo.ordbutils.pbgens.Ordb.PSQueryBySQL;
import org.fc.zippo.sender.httpimpl.pbs.Httpsender.PCommand;
import org.fc.zippo.sender.httpimpl.pbs.Httpsender.PModule;
import org.fc.zippo.sender.httpimpl.pbs.Httpsender.PSCheck;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.PBUtils;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.AsyncPBActor;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.bean.BeanPBUtil;
import onight.tfw.outils.bean.JsonPBUtil;
import onight.tfw.proxy.IActor;

@NActorProvider
@Slf4j
public class SenderInfo extends AsyncPBActor<PSCheck> implements ActorService, IActor {
	String name = "sender";

	@ActorRequire(name = "http", scope = "global")
	IPacketSender sender;

	public IPacketSender getSender() {
		return sender;
	}

	public void setSender(IPacketSender sender) {
		this.sender = sender;
	}

	@Override
	public String[] getCmds() {
		return new String[] { PCommand.INF.name() };
	}

	BeanPBUtil pbutil = new BeanPBUtil();
	@Override
	public void onPBPacket(FramePacket pack, PSCheck arg1, CompleteHandler handler) {
		log.debug("pack::" + pack + "::sender==" + sender + ":body=" + new String(pack.getBody()));
		/**
		 * 构造发送的类
		 */
		PSQueryBySQL.Builder pbody = PSQueryBySQL.newBuilder();
		pbody.setCols("*").setFroms("gasSysMenu").setLimit(4).setPage(true);
		FramePacket pp = PacketHelper.buildFromBody(pbody.build(), "SELDBC");
		// 同步发送
		pp = PacketHelper.buildJsonFromStr("{\"cols\":\"*\",\"froms\":\"gasSysMenu\",\"limit\":3}", "SELDBC");

		val ret = sender.send(pp, 10);
		try {
			// 接受到请求
			//PRetQueryBySQL.Builder builder = PRetQueryBySQL.newBuilder().mergeFrom(ret.getBody());
			//log.debug("OK:" + builder.getLimit());
			//json
			log.debug("ret=="+new String(ret.getBody()));
			PRetQueryBySQL.Builder builder = PRetQueryBySQL.newBuilder();
			JsonPBUtil.json2PB(ret.getBody(), builder);
			//
			handler.onFinished(PacketHelper.toPBReturn(pack, builder.build()));
		} catch (Exception e) {
			e.printStackTrace();
			handler.onFinished(PacketHelper.toPBReturn(pack, "error:" + ret));
		}

	}

	@Override
	public String getModule() {
		return PModule.S01.name();
	}

}
