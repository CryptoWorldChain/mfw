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
object TokenGenActor extends SessionModules[PBToken] {
  override def service = TokenGenService
}

object TokenGenService extends OLog with PBUtils with LService[PBToken] {

  override def cmd: String = "TKN";
  //http://localhost:8081/ssm/pbtkn.do?fh=VTKNSSM000000J00&bd={%22userid%22:%22aabb%22}&gcmd=TKNSSM
  //压力测试，tps在14k左右，
  //ab -k -n 1000000 -r -c 1000 -t 60  "http://localhost:8081/ssm/pbtkn.do?fh=VTKNSSM000000J00&bd={%22userid%22:%22aabb%22}&gcmd=TKNSSM"
  def onPBPacket(pack: FramePacket, pbo: PBToken, handler: CompleteHandler) = {
    //    log.debug("guava==" + VMDaos.guCache.getIfPresent(pbo.getLogid()));
    val ret = PBTokenRet.newBuilder();
    try {
      val token = TokensManager.genToken(pbo.getUserid)
      log.debug("genTOken:userid:" + pbo.getUserid + ":token:" + token)
      if (token != null) {
        ret.setCode("0000").setStatus(RetCode.SUCCESS).setOp(pbo.getOp).setTokenid(token);
      } else {
        ret.setCode("0001").setStatus(RetCode.FAILED).setOp(pbo.getOp);
      }
    } catch {
      case error: Throwable => {
        log.error("GENERROR", error)
        ret.setCode("0009").setStatus(RetCode.FAILED).setOp(pbo.getOp).setDesc(error.getMessage + "");
      }
    } finally {
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));

    }

  }
}