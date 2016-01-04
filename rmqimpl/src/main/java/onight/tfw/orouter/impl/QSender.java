package onight.tfw.orouter.impl;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.orouter.api.NoneQService;
import onight.tfw.orouter.api.QService;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.UUIDGenerator;

/**
 * 处理节点的
 * 
 * @author brew
 *
 */
@Slf4j
public class QSender implements IPacketSender {

	@Setter
	@PSender
	private QService qService = new NoneQService();

	public QSender() {
		super();
	}

	@Override
	public FramePacket send(FramePacket fp, long timeoutMS) {
		
		fp.getFixHead().setSync(true);
		String tmpQName = UUIDGenerator.generate();
		fp.putHeader(fp.getGlobalCMD()+".QID",tmpQName);
		Object obj = qService.syncSendMessage(fp.getGlobalCMD(),tmpQName,PacketHelper.toTransBytes(fp));
		if (obj != null) {
			return PacketHelper.buildPacketFromTransBytes((byte[]) obj);
		}
		return null;
	}

	@Override
	public void asyncSend(FramePacket fp, CallBack<FramePacket> cb) {
		qService.sendMessage(fp.getGlobalCMD(), PacketHelper.toTransBytes(fp));
		cb.onSuccess(fp);
	}

	@Override
	public void post(FramePacket fp) {
		qService.sendMessage(fp.getGlobalCMD(), PacketHelper.toTransBytes(fp));
	}

}
