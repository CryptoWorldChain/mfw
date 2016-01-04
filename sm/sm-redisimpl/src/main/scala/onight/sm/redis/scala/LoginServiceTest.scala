package onight.sm.redis.scala

import scala.concurrent.ExecutionContext.Implicits.global

import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.sm.Ssm.PBSSO
import onight.sm.redis.scala.persist.MysqlDAOs
import onight.sm.redis.scala.persist.RedisDAOs
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket

@NActorProvider
object SMLogin extends SessionModules[PBSSO] {
  override def service = LoginServiceTest 
}

object LoginServiceTest extends OLog with PBUtils with LService[PBSSO] {

  override def cmd: String = "tst";

  def onPBPacket(pack: FramePacket, pbo: PBSSO, handler: CompleteHandler) = {
    MysqlDAOs.ALoginUserDAO.countByCond("1=1") map { result =>
      val count = result.get
      log.debug("Get Login:count=" + count);
//      val record = pbBeanUtil.copyFromPB(pbo, new LogSessionByID())
//      println("record===" + record)
//      val rss = RedisDAOs.logiddao.insertIfNoExist(record);
//      
//      println("insertifnot exist:" + rss);
//      val rss2 = RedisDAOs.logiddao.insert(record)
//      println("insert ff:" + rss2) 
//      val dbresult = RedisDAOs.logiddao.selectByPrimaryKey(record);
//      log.debug("dbresult==" + dbresult);
//      if (dbresult != null) {
//        log.debug("sid=pwd=" + dbresult.password)
//      }
////      Thread.sleep(5000);
      handler.onFinished(PacketHelper.toPBReturn(pack, pbo));
//      log.debug("finsised!!");

    }
    //    val count = Await.ready(DAOs.ALoginUserDAO.countByCond("1=1"), 60 seconds)

  }
}