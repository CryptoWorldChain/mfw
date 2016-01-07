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

@NActorProvider
object ValidateActor extends SessionModules[PBSSO] {
  override def service = ValidateService
}

//http://localhost:8081/ssm/pbvld.do?fh=VSINSSM000000J00&bd={%22login_id%22:%22abc%22,%22password%22:%22000000%22,%22op%22:0,%22smid%22:%22WEp3azdLSGJ6ZV9hYmMF%22}&gcmd=VLDSSM

object ValidateService extends OLog with PBUtils with LService[PBSSO] {

  override def cmd: String = PBCommand.VLD.name();
  def onPBPacket(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler) = {
    // ！！检查用户是否已经登录
    val ret = PBSSORet.newBuilder();
    val session = SessionManager.checkAndUpdateSession(pbo.getSmid)
    if (session._1 != null) {
      ret.setCode("0000").setStatus(RetCode.SUCCESS) setLoginId (session._1.getLoginId());
      pack.putHeader(ExtHeader.SESSIONID, pbo.getSmid);
    } else {
      //      log.debug("result error: session not found")
      ret.setDesc(session._2).setCode("0001").setLoginId(pbo.getLoginId) setStatus (RetCode.FAILED);
      pack.getExtHead().remove(ExtHeader.SESSIONID)
    }
    handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
  }
}