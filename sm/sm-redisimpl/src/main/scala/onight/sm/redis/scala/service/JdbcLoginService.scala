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
import onight.sm.redis.scala.persist.JDBCDaos

import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket
import onight.sm.redis.scala.persist.LoginIDRedisLoCache
import onight.sm.redis.entity.LoginResIDSession
import onight.tfw.otransio.api.PackHeader
import onight.tfw.otransio.api.beans.ExtHeader
import onight.sm.Ssm.PBCommand

@NActorProvider
object JdbcLoginActor extends SessionModules[PBSSO] {
  override def service = JdbcLoginService
}

object JdbcLoginService extends OLog with PBUtils with LService[PBSSO] {

  override def cmd: String = PBCommand.OIN.name();

  //http://localhost:18080/ssm/pboin.do?fh=VOINSSM000000J00&bd={%22login_id%22:%22abc%22,%22password%22:%22000000%22,%22op%22:0,%22res_id%22:%22android%22}&gcmd=OINSSM

//ab -k -r -c 2000 -t 60 "http://localhost:18080/ssm/pboin.do?fh=VOINSSM000000J00&bd={%22login_id%22:%22abc%22,%22password%22:%22000000%22,%22op%22:0,%22res_id%22:%22android%22}&gcmd=OINSSM"
//Concurrency Level:      2000
//Time taken for tests:   3.893 seconds
//Complete requests:      50000
//Failed requests:        20390
//   (Connect: 2, Receive: 3, Length: 20385, Exceptions: 0)
//Keep-Alive requests:    49997
//Total transferred:      21304930 bytes
//HTML transferred:       6827465 bytes
//Requests per second:    12843.02 [#/sec] (mean)
//Time per request:       155.727 [ms] (mean)
//Time per request:       0.078 [ms] (mean, across all concurrent requests)
//Transfer rate:          5344.13 [Kbytes/sec] received
  def resultfunc(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler, loginId: String,passwd:String)(implicit errorCode: String = "0002", errorMessage: String = "Unknow Error"): Unit = {
    val ret = PBSSORet.newBuilder();

    if (passwd != null) {
      val loginId = pbo.getLoginId; //+Math.abs((Math.random()*100)%100).asInstanceOf[Int];
      VMDaos.pwdCache.put(loginId, passwd); 
      //        log.debug("db.row=" + row + ",gua size=" + VMDaos.guCache.size());
      ret.setLoginId(pbo.getLoginId) setStatus (RetCode.FAILED)
      if (StringUtils.equals(passwd, pbo.getPassword)) {

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
      val cachepwd = VMDaos.pwdCache.getIfPresent(pbo.getLoginId);
      if (cachepwd != null) {
        resultfunc(pack, pbo, handler,pbo.getLoginId, cachepwd)
      } else {
        val pwd= JDBCDaos.doSelectByLoginID(pbo.getLoginId)
        resultfunc(pack, pbo, handler,pbo.getLoginId, pwd)("0002", "User not found!")
      }
    }

  }
}