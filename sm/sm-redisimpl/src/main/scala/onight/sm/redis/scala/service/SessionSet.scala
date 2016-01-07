package onight.sm.redis.scala.service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import com.github.mauricio.async.db.RowData
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.sm.Ssm.PBSSO
import onight.sm.Ssm.PBSSORet
import onight.sm.Ssm.RetCode
import onight.sm.redis.scala.LService
import onight.sm.redis.scala.PBUtils
import onight.sm.redis.scala.SessionManager
import onight.sm.redis.scala.SessionModules
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.ExtHeader
import onight.tfw.otransio.api.beans.FramePacket
import onight.sm.Ssm.PBCommand
import onight.sm.redis.scala.PBUtils
import onight.sm.Ssm.PBSession
import onight.sm.redis.entity.LoginResIDSession

@NActorProvider
object SessionSet extends SessionModules[PBSSO] {
  override def service = SessionSetService
}

//http://localhost:8081/ssm/pbset.do?fh=VSETSSM000000J00&bd={"smid":"VWo1Z0trd0EycmltemU6YWJjL2FuZHJvaWQ5","session":{"kvs":{"a":"bbb"}}}&gcmd=SETSSM

object SessionSetService extends OLog with PBUtils with LService[PBSSO] {

  override def cmd: String = PBCommand.SET.name();
  def onPBPacket(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler) = {
    // ！！检查用户是否已经登录
    val ret = PBSSORet.newBuilder();
    val session = SessionManager.checkAndUpdateSession(pbo.getSmid,pbBeanUtil.copyFromPB(pbo.getSession, new LoginResIDSession))
    if (session._1 != null) {
      ret.setCode("0000").setStatus(RetCode.SUCCESS) setLoginId (session._1.getLoginId());
      ret.setSession(pbBeanUtil.toPB[PBSession](PBSession.newBuilder(), session));
      pack.putHeader(ExtHeader.SESSIONID, pbo.getSmid);
    } else {
      //      log.debug("result error: session not found")
      ret.setDesc(session._2).setCode("0001").setLoginId(pbo.getLoginId) setStatus (RetCode.FAILED);
      pack.getExtHead().remove(ExtHeader.SESSIONID)
    }
    handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
  }
}