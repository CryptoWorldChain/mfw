package onight.sm.redis.scala.service

import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.sm.Ssm.PBToken
import onight.sm.Ssm.PBTokenRet
import onight.sm.Ssm.RetCode
import onight.sm.redis.scala.LService
import onight.sm.redis.scala.PBUtils
import onight.sm.redis.scala.SessionModules
import onight.sm.redis.scala.TokensManager
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket

@NActorProvider
object TokenCheckActor extends SessionModules[PBToken] {
  override def service = TokenCheckService
}

object TokenCheckService extends OLog with PBUtils with LService[PBToken] {

  override def cmd: String = "TKC";
// ab -k -r -c 100 -t 60 "http://localhost:8081/ssm/pbtkc.do?fh=VTKNSSC000000J00&bd={%22userid%22:%22aabb%22,%22tokenid%22:%22YHvZ_qvDqR_13urDuQCwAmHZ7bACeM3Kzp1%22}&gcmd=TKNSSC"
// tps: 15k
  
  def onPBPacket(pack: FramePacket, pbo: PBToken, handler: CompleteHandler) = {
    //    log.debug("guava==" + VMDaos.guCache.getIfPresent(pbo.getLogid()));
    val ret = PBTokenRet.newBuilder();
    try {

      val userid = TokensManager.checkToken(pbo.getTokenid, pbo.getUserid)
      log.debug("genTOken:userid:" + pbo.getUserid + ":token:" + userid)
      if (userid != null) {
        if (userid.equals(pbo.getUserid)) {
          ret.setCode("0000").setStatus(RetCode.SUCCESS).setOp(pbo.getOp).setTokenid(pbo.getTokenid);
        } else {
          ret.setCode("0001").setStatus(RetCode.FAILED).setOp(pbo.getOp).setDesc("UserID_Not_Equal");
        }
      } else {
        ret.setCode("0002").setStatus(RetCode.FAILED).setOp(pbo.getOp).setDesc("TOKEN_NOT_FOUND_ERROR");
      }
    } catch {
      case error: Throwable => ret.setCode("0009").setStatus(RetCode.FAILED).setOp(pbo.getOp).setDesc(error.getMessage+"");
    } finally {
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));

    }

  }
}