package onight.sm.redis.scala

import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.StringUtils
import onight.oapi.scala.traits.OLog
import onight.sm.redis.entity.SMIDSession
import onight.sm.redis.scala.persist.LoginIDRedisLoCache
import onight.sm.redis.scala.persist.RedisDAOs
import onight.sm.redis.scala.persist.SMIDIDRedisLoCache
import onight.tfw.mservice.NodeHelper
import onight.tfw.mservice.ThreadContext
import onight.tfw.ojpa.api.JpaContextConstants
import java.util.concurrent.LinkedBlockingQueue
import onight.sm.redis.entity.LoginResIDSession

object SessionManager extends OLog {
  val exec = new ScheduledThreadPoolExecutor(NodeHelper.getPropInstance.get("sm.check.thread", 50));
  val opexec = new ScheduledThreadPoolExecutor(NodeHelper.getPropInstance.get("sm.op.thread", 5));
  val BATCH_SIZE = NodeHelper.getPropInstance.get("sm.op.batchsize", 1000);
  val runningPool = new ConcurrentHashMap[String, SMIDSession]();
  val TimeOutSec = NodeHelper.getPropInstance.get("sm.log.timeoutsec", 30 * 60) //默认30分钟超时
  val TimeOutMS = TimeOutSec * 1000 //默认30分钟超时
  val OpDelaySec = NodeHelper.getPropInstance.get("sm.op.delaysec", 5) //默认5秒钟延迟操作redis
  val CleanDelaySec = NodeHelper.getPropInstance.get("sm.clean.delaysec", 30) //默认30秒清空redis中的超时缓存

  //  val deleteBox = new LinkedBlockingQueue[SMIDSession]();
  val checkBox = new ConcurrentHashMap[String, SMIDSession]();
  //  val insertBox = new LinkedBlockingQueue[SMIDSession]();
  val cleanBox = new ConcurrentHashMap[String, SMIDSession](); //smid被彻底从redis里面移除

  def watchSMID(session: SMIDSession) = {
    //    ThreadContext.setContext(JpaContextConstants.Cache_Timeout_Second, TimeOutSec)
    try {

      session.setLastUpdateMS(System.currentTimeMillis())
      val logrcsession = LoginResIDSession(session);
      session.relateId = logrcsession.globalID()
      //检查重复登录
      val rcsession = LoginIDRedisLoCache.getAndSet(logrcsession)
      if (rcsession != null && (rcsession.kickout != null)) {
        //same session:insert into redis,
        if (!StringUtils.equals(session.getSmid(), rcsession.getSmid())) {
          log.info("UserSessionRelogin:KickoutOldSession:" + rcsession.getSmid());
          val kickoutsession = SMIDIDRedisLoCache.get(rcsession);
          if (kickoutsession != null) {
            kickoutsession.kickout = "K"
            kickoutsession.relateId = null
          }
          removeSession(kickoutsession)
        } else {
          log.debug("sameSessionRelogin")
        }
      }
      //加入检查队列
      SMIDIDRedisLoCache.insert(session)
      checkBox.put(session.getSmid(), session)
      exec.schedule(CheckRunner, TimeOutSec, TimeUnit.SECONDS); //超时检查

    } finally {
      ThreadContext.cleanContext()
    }
  }

  def checkSMID(session: SMIDSession): Boolean = {
    val current = System.currentTimeMillis();
    if (session.kickout != null) { //已经被踢出去的，不检查了
      false
    }
    val rsession = RedisDAOs.smiddao.selectByPrimaryKey(session);
    if (rsession == null || rsession.kickout != null) {
      log.info("SessionTimeOut:logid:or:notfoundinRedis:logid:" + session.getLoginId() + ":smid:" + session.getSmid())
      //被踢出来的
      removeSession(session)
      false
    } else if (current - rsession.getLastUpdateMS() >= TimeOutMS && current - session.getLastUpdateMS() >= TimeOutMS) {
      //        RedisDAOs.getSmiddao().deleteByPrimaryKey(session)
      removeSession(session)
      log.info("SessionTimeOut:logid:" + session.getLoginId() + ":userid:" + session.getUserId() + ":lastup:" + session.getLastUpdateMS() + ":logtime:" + session.getLoginMS())
      false
    } else { //redis里面被别的集群节点更新过了
      if (rsession.getLastUpdateMS() != session.getLastUpdateMS()) {
        session.setLastUpdateMS(Math.max(rsession.getLastUpdateMS(), session.getLastUpdateMS()))
        val rsessionv = RedisDAOs.smiddao.getAndSet(session);
        if (rsessionv.kickout != null) {
          session.kickout = "K";
          removeSession(session)
          return false;
        }
      }
      true
    }
  }

  def removeSession(session: SMIDSession) {
    session.kickout = "K";
    ThreadContext.setContext(JpaContextConstants.Cache_Timeout_Second, CleanDelaySec)
    RedisDAOs.smiddao.insert(session)
    if (session.relateId != null) {
      RedisDAOs.logiddao.insert(session)
    }
    cleanBox.put(session.smid, session);
    exec.schedule(CleanRunner, CleanDelaySec, TimeUnit.SECONDS); //超时检查
  }

  object CheckRunner extends Runnable {
    def run() {
      var size = checkBox.size()
      val it = checkBox.values().iterator();
      while (size > 0 && it.hasNext()) {
        val obj = it.next();
        size = size - 1;
        if (obj != null) {
          if (checkSMID(obj)) {
            exec.schedule(CheckRunner,
              Math.max(1, TimeOutSec - (System.currentTimeMillis() - obj.getLastUpdateMS()) / 1000), TimeUnit.SECONDS); //timeout的
          } else {
            checkBox.remove(obj.smid)
          }
        } else {
          size = 0;
        }
      }
    }
  }

  object CleanRunner extends Runnable {
    def run() {
      val tmpList = new ArrayList[SMIDSession]();
      val idDelList = new ArrayList[LoginResIDSession]();

      var size = cleanBox.size()
      log.debug("CleanRunner.do,size=:" + size)
      val it = cleanBox.values().iterator()
      while (size > 0 && it.hasNext()) {
        val obj = it.next()
        size = size - 1;
        if (obj != null) {
          log.debug("CleanSMID:" + obj.smid + ":globalid:" + obj.globalID);
          tmpList.add(obj);
          if (obj.relateId != null) {
            idDelList.add(LoginResIDSession(obj))
            LoginIDRedisLoCache.redisLocalCache.invalidate(obj.relateId)
          }
          SMIDIDRedisLoCache.redisLocalCache.invalidate(obj.smid)
        } else {
          size = 0;
        }
        if (tmpList.size() > BATCH_SIZE) {
          RedisDAOs.smiddao.batchDelete(tmpList.asInstanceOf[java.util.List[Object]])
          if (idDelList.size() > 0) {
            RedisDAOs.logiddao.batchDelete(idDelList.asInstanceOf[java.util.List[Object]])
          }
          tmpList.clear();
        }
      }
      if (tmpList.size() > 0) {
        RedisDAOs.smiddao.batchDelete(tmpList.asInstanceOf[java.util.List[Object]])
        if (idDelList.size() > 0) {
          RedisDAOs.logiddao.batchDelete(idDelList.asInstanceOf[java.util.List[Object]])
        }
        tmpList.clear();
      }

    }
  }

  //  object InsertRunner extends Runnable {
  //    def run() {
  //      while (insertBox.size() > 0) {
  //        val tmpList = new ArrayList[SMIDSession]();
  //        insertBox.drainTo(tmpList, BATCH_SIZE);
  //        RedisDAOs.smiddao.batchInsert(tmpList.asInstanceOf[java.util.List[Object]])
  //      }
  //    }
  //  }

  //检查是否登录
  def checkAndUpdateSession(smid: String): SMIDSession = {
    val session = SMIDIDRedisLoCache.get(SMIDSession(smid));
    if (session != null) {
      if (session.kickout != null) {
        return null;
      }
      if (System.currentTimeMillis() - session.lastUpdateMS > TimeOutMS) {
        removeSession(session);
        return null;
      }
      session.lastUpdateMS = System.currentTimeMillis();
      checkBox.put(session.smid, session)
      opexec.schedule(CheckRunner, OpDelaySec, TimeUnit.SECONDS); //timeout的
      return session;
    }
    null
  }

  // 登出
  def logout(smid: String): SMIDSession = {
    val session = SMIDIDRedisLoCache.get(SMIDSession(smid));
    if (session != null) {
      if (session.kickout != null) {
        return null;
      }
      val idcachesession = LoginIDRedisLoCache.get(LoginResIDSession(session));
      if (idcachesession != null) {
        idcachesession.kickout = "K";
      }
      removeSession(session)
      return session;
    }
    null
  }
}