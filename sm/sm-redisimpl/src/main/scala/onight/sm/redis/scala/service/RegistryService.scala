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
import onight.tfw.outils.conf.PropHelper
import onight.sm.Ssm.PBSSORet
import scala.collection.JavaConversions._
import org.ethereum.crypto.ECKey
import java.security.SecureRandom
import org.spongycastle.util.encoders.Hex

@NActorProvider
object RegistryActor extends SessionModules[PBSSO] {
  override def service = RegistryService
}

object RegistryService extends OLog with PBUtils with LService[PBSSO] {

  override def cmd: String = PBCommand.REG.name();

  //http://localhost:8000/ssm/pbreg.do?fh=VSINSSM000000J00&bd={"user_name":"abc","password":"000000","trade_no":"00000001","metadata":"abcdef"}&gcmd=SINSSM
  //Requests per second:    16584.16 [#/sec] (mean)
  //Time per request:       60.298 [ms] (mean)
  //Time per request:       0.060 [ms] (mean, across all concurrent requests)
  //Transfer rate:          4262.07 [Kbytes/sec] received
  //ab -k -r -c 1000 -t 60 "http://localhost:8081/ssm/pbsin.do?fh=VSINSSM000000J00&bd={%22login_id%22:%22abc%22,%22password%22:%22000000%22,%22op%22:0,%22res_id%22:%22android%22}&gcmd=SINSSM"

  def resultfunc(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler, rowcount: Int)(implicit errorCode: String = "0002", errorMessage: String = "Unknow Error"): Unit = {
    val ret = PBSSORet.newBuilder();

    if (rowcount != null&&rowcount>0) {
      val loginId = pbo.getLoginId; //+Math.abs((Math.random()*100)%100).asInstanceOf[Int];
        ret.setBizcode("0000").setRetcode(RetCode.SUCCESS) setLoginId (loginId)
    } else {
      log.debug("result error:" + errorMessage)
      ret.setDesc(errorMessage).setBizcode(errorCode);
      pack.getExtHead().remove(ExtHeader.SESSIONID)
    }
    handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
  }

  def onPBPacket(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler) = {
    //    log.debug("guava==" + VMDaos.guCache.getIfPresent(pbo.getLogid()));

    if (pbo == null) {
      val ret = PBSSORet.newBuilder();
      ret.setDesc("Packet_Error").setBizcode("0003") setRetcode (RetCode.FAILED);
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()));
    } else {
      val row = VMDaos.dbCache.getIfPresent(pbo.getLoginId);
      if (row != null) {
        resultfunc(pack, pbo, handler, -1)("0003", "InsertError,LoginIdExist!")
      } else {
        val example = new KOLoginUser( StringUtils.trimToNull(pbo.getUserName),
          StringUtils.trimToNull(pbo.getEmail),
          StringUtils.trimToNull(pbo.getMobile),
          StringUtils.trimToNull(pbo.getThirdLoginid1),
          StringUtils.trimToNull(pbo.getThirdLoginid2),
          StringUtils.trimToNull(pbo.getUserName),
          StringUtils.trimToNull(pbo.getPassword),
          StringUtils.trimToNull(pbo.getTradeNo),
          StringUtils.trimToNull("bc_"+SMIDHelper.nextSMID(pbo.getUserName)),
          StringUtils.trimToNull("bubipkipkipki"),
          StringUtils.trimToNull(pbo.getMetadata),
          StringUtils.trimToNull(pbo.getPassword),
          Some(1));
        
        val ran=new SecureRandom();
        //ran.generateSeed(System.currentTimeMillis().asInstanceOf[Int])
        val eckey = new ECKey(ran);
        log.debug("new eckey:"+eckey);
        log.debug("pub:"+Hex.toHexString(eckey.getPubKey));
        log.debug("pri:"+Hex.toHexString(eckey.getPrivKeyBytes));
        log.debug("nodeid:"+Hex.toHexString(eckey.getNodeId));
        //log.debug("address":+eckey.getAddress());//+",pki="+eckey.getPrivKeyBytes()+",pub="+eckey.getPubKey());
        log.debug("Register DB:" + pbo.getUserName + ",nn::"+Hex.toHexString(eckey.getPubKey))
        eckey.getNodeId
        val ff = MysqlDAOs.ALoginUserDAO.insertSelective(example)
        ff onSuccess {
          case result @ _ => {
            if (result.rowsAffected > 0) {
              resultfunc(pack, pbo, handler,result.rowsAffected.asInstanceOf[Int])
            } else {
              resultfunc(pack, pbo, handler, 0)("0002", "InsertError")
            }
          }
        }
        ff onFailure ({
          case t @ _ => log.error("error in run RegistryService：", t); resultfunc(pack, pbo, handler, 0)("0003", t.getMessage)
        })
      }
    }

  }
}