package onight.sm.redis.scala.service

import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.lang3.StringUtils
import com.github.mauricio.async.db.RowData
import lombok.extern.slf4j.Slf4j
import onight.async.mysql.commons.Range
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.sm.Ssm.PBSSO
import onight.sm.Ssm.PBSSORet
import onight.sm.Ssm.RetCode
import onight.sm.redis.scala.LService
import onight.sm.redis.scala.PBUtils
import onight.sm.redis.scala.SMIDHelper
import onight.sm.redis.scala.SessionManager
import onight.sm.redis.scala.SessionModules
import onight.sm.redis.scala.persist.MysqlDAOs
import onight.sm.redis.scala.persist.MysqlDAOs.KOLoginUser
import onight.sm.redis.scala.persist.VMDaos
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket
import onight.sm.redis.scala.persist.LoginIDRedisLoCache
import onight.sm.redis.entity.LoginResIDSession
import onight.tfw.otransio.api.PackHeader
import onight.tfw.otransio.api.beans.ExtHeader
import onight.sm.Ssm.PBCommand

@NActorProvider
object LoginActor extends SessionModules[PBSSO] {
  override def service = LoginService
}

object LoginService extends OLog with PBUtils with LService[PBSSO] {

  override def cmd: String = PBCommand.SIN.name();

  //http://localhost:8081/ssm/pbsin.do?fh=VSINSSM000000J00&bd={%22login_id%22:%22abc%22,%22password%22:%22000000%22,%22op%22:0,%22res_id%22:%22android%22}&gcmd=SINSSM
  //Requests per second:    16584.16 [#/sec] (mean)
  //Time per request:       60.298 [ms] (mean)
  //Time per request:       0.060 [ms] (mean, across all concurrent requests)
  //Transfer rate:          4262.07 [Kbytes/sec] received
  //ab -k -r -c 1000 -t 60 "http://localhost:8081/ssm/pbsin.do?fh=VSINSSM000000J00&bd={%22login_id%22:%22abc%22,%22password%22:%22000000%22,%22op%22:0,%22res_id%22:%22android%22}&gcmd=SINSSM"

  def resultfunc(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler, row: RowData)(implicit errorCode: String = "0002", errorMessage: String = "Unknow Error"): Unit = {
    val ret = PBSSORet.newBuilder();

    if (row != null) {
      val loginId = pbo.getLoginId; //+Math.abs((Math.random()*100)%100).asInstanceOf[Int];
      VMDaos.dbCache.put(row("LOGIN_ID").asInstanceOf[String], row);
      //        log.debug("db.row=" + row + ",gua size=" + VMDaos.guCache.size());
      ret.setLoginId(pbo.getLoginId) setStatus (RetCode.FAILED)
      if (StringUtils.equals(row("PASSWORD").asInstanceOf[String], pbo.getPassword)) {

        val smid = SMIDHelper.nextSMID(loginId + "/" + pbo.getResId)
        ret.setCode("0000").setStatus(RetCode.SUCCESS) setLoginId (loginId) setSmid (smid)
        val session = LoginResIDSession(smid, pbo.getUserId, loginId, pbo.getPassword, pbo.getResId, null);
        SessionManager.watchSMID(session)
        pack.putHeader(ExtHeader.SESSIONID, smid);
      } else {
        ret.setDesc("Password error").setCode("0002");
        pack.getExtHead().remove(ExtHeader.SESSIONID)

      }
    } else {
      log.debug("result error:" + errorMessage)
      ret.setDesc(errorMessage).setCode(errorCode);
      pack.getExtHead().remove(ExtHeader.SESSIONID)

    }
    handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
  }

  def onPBPacket(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler) = {
    //    log.debug("guava==" + VMDaos.guCache.getIfPresent(pbo.getLogid()));
    if (pbo == null) {
      val ret = PBSSORet.newBuilder();
      ret.setDesc("Packet_Error").setCode("0003") setStatus (RetCode.FAILED);
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
    } else {
      val row = VMDaos.dbCache.getIfPresent(pbo.getLoginId);
      if (row != null) {
        resultfunc(pack, pbo, handler, row)
      } else {
        val example = new KOLoginUser(StringUtils.trimToNull(pbo.getLoginId), StringUtils.trimToNull(pbo.getEmail),
          StringUtils.trimToNull(pbo.getMobile), StringUtils.trimToNull(pbo.getThirdLoginid1), StringUtils.trimToNull(pbo.getThirdLoginid2));
        log.debug("LoginFrom DB:" + pbo.getLoginId + ",nn")
        val ff = MysqlDAOs.ALoginUserDAO.findByCondPrepare(example, Range(0, 1))
        ff onSuccess {
          case result @ _ => {
            if (result.rowsAffected > 0) {
              resultfunc(pack, pbo, handler, result.rows.head(0))
            } else {
              resultfunc(pack, pbo, handler, null)("0002", "User not found!")
            }
          }
        }
        ff onFailure ({
          case t @ _ => log.error("error in run LoginService：", t); resultfunc(pack, pbo, handler, null)("0003", t.getMessage)
        })
      }
    }

  }
}