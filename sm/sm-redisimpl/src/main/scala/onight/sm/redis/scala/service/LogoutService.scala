package onight.sm.redis.scala.service

import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.lang3.StringUtils
import com.github.mauricio.async.db.RowData
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.sm.Ssm.PBSSO
import onight.sm.Ssm.PBSSORet
import onight.sm.redis.scala.LService
import onight.sm.redis.scala.PBUtils
import onight.sm.redis.scala.SessionModules
import onight.sm.redis.scala.persist.MysqlDAOs
import onight.sm.redis.scala.persist.MysqlDAOs.KOLoginUser
import onight.sm.redis.scala.persist.MysqlDAOs.KOLoginUser
import onight.sm.redis.scala.persist.VMDaos
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket
import onight.async.mysql.commons.Range
import scala.concurrent.duration.DurationInt
import scala.None
import scala.None
import scala.concurrent.Await
import onight.sm.redis.scala.SessionManager
import onight.sm.Ssm.RetCode
import onight.tfw.otransio.api.beans.ExtHeader
import onight.tfw.otransio.api.beans.CookieBean
import onight.sm.Ssm.PBCommand

@NActorProvider
object LogoutActor extends SessionModules[PBSSO] {
  override def service = LogoutService
}

object LogoutService extends OLog with PBUtils with LService[PBSSO] {

  override def cmd: String = PBCommand.OUT.name();
  def onPBPacket(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler) = {
    // ！！检查用户是否已经登录
    val ret = PBSSORet.newBuilder();
    if (pbo == null) {
      ret.setDesc("Packet_Error").setBizcode("0003") setRetcode (RetCode.FAILED);
    } else {
      val session = SessionManager.logout(pbo.getSmid, pbo.getLoginId, pbo.getResId)
      if (session._1 != null) {
        ret.setBizcode("0000").setRetcode(RetCode.SUCCESS) setLoginId (session._1.getLoginId());
        pack.getExtHead.append(ExtHeader.SESSIONID, new CookieBean(pbo.getSmid, 0))

      } else {
        //      log.debug("result error: session not found")
        ret.setDesc(session._2).setBizcode("0001").setLoginId(pbo.getLoginId) setRetcode (RetCode.FAILED);
      }
    }
    handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
  }
}