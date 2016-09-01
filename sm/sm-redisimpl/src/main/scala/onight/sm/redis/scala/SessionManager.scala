package onight.sm.redis.scala

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.StringUtils
import onight.oapi.scala.traits.OLog
import onight.sm.redis.entity.LoginResIDSession
import onight.sm.redis.entity.LoginResIDSession
import onight.sm.redis.scala.persist.LoginIDRedisLoCache
import onight.sm.redis.scala.persist.RedisDAOs
import onight.tfw.mservice.NodeHelper
import onight.tfw.mservice.ThreadContext
import onight.tfw.ojpa.api.JpaContextConstants
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.HashMap

object SessionManager extends OLog {
  val exec = new ScheduledThreadPoolExecutor(NodeHelper.getPropInstance.get("sm.check.thread", 3));
  val opexec = new ScheduledThreadPoolExecutor(NodeHelper.getPropInstance.get("sm.op.thread", 100));
  val BATCH_SIZE = NodeHelper.getPropInstance.get("sm.op.batchsize", 1000);
  val runningPool = new ConcurrentHashMap[String, LoginResIDSession]();
  val TimeOutSec = NodeHelper.getPropInstance.get("sm.log.timeoutsec", 30 * 60) //默认30分钟超时
  val TimeOutMS = TimeOutSec * 1000 //默认30分钟超时
  val OpDelaySec = NodeHelper.getPropInstance.get("sm.op.delaysec", 10) //默认5秒钟延迟操作redis
  val InsertDelaySec = NodeHelper.getPropInstance.get("sm.insert.delaysec", 2) //默认5秒钟延迟操作redis

  //  val CleanDelaySec = NodeHelper.getPropInstance.get("sm.clean.delaysec", 30) //默认30秒清空redis中的超时缓存

  //  val deleteBox = new LinkedBlockingQueue[LoginResIDSession]();
  val checkBox = new ConcurrentHashMap[String, LoginResIDSession]();
  val insertBox = new ConcurrentHashMap[String, LoginResIDSession]();
  //  val cleanBox = new ConcurrentHashMap[String, LoginResIDSession](); //smid被彻底从redis里面移除

  val NONE_RELATEID = "__";

  exec.scheduleAtFixedRate(CheckRunner, OpDelaySec, OpDelaySec, TimeUnit.SECONDS)
  exec.scheduleAtFixedRate(InsertRunner, OpDelaySec, InsertDelaySec, TimeUnit.SECONDS)

  def watchSMID(session: LoginResIDSession) = {
    try {
      session.setLastUpdateMS(System.currentTimeMillis())
      //检查重复登录
      //      val exists = LoginIDRedisLoCache.redisLocalCache.getIfPresent(session.globalID());
      //      if (exists != null) exists.kickout(true)
//      LoginIDRedisLoCache.redisLocalCache.put(session.globalID(), session)
//      insertBox.put(session.globalID(), session)
      
       val maxTimeOutMS = session.getMaxInactiveInterval() match {
        case f if f > 0 => f*1000
        case _ => TimeOutMS
      }

      ThreadContext.setContext(JpaContextConstants.Cache_Timeout_Second, maxTimeOutMS/1000)

      
            LoginIDRedisLoCache.insert(session);
//            if (rcsession != null) {
//              //same session:insert into redis,
//              if (!StringUtils.equals(session.getSmid(), rcsession.getSmid())) {
//                log.info("UserSessionRelogin:KickoutOldSession:" + rcsession.getSmid());
//      //          rcsession.kickout(true);
//              } else {
//                log.debug("sameSessionRelogin")
//              }
//              //        logrcsession.kickout(false)
//      //        val example = new KVExample();
//              //        example.getCriterias.add(logrcsession);
//              //        example.setSelectCol("status")
//              //        LoginIDRedisLoCache.dao.deleteByExample(example);
//            }
      //加入检查队列


    } finally {
      //      ThreadContext.cleanContext()
    }
  }

  def checkSMID(session: LoginResIDSession): Boolean = {
    val current = System.currentTimeMillis();
    if (session.isKickout()) { //已经被踢出去的，不检查了
      false
    }
    val rsession = RedisDAOs.logiddao.selectByPrimaryKey(session);

    if (rsession == null || rsession.isKickout()) {
      log.info("SessionTimeOut:logid:or:notfoundinRedis:logid:" + session.getLoginId() + ":smid:" + session.getSmid())
      //被踢出来的
      removeSession(session)
      return false
    }
    val maxTimeOutMS = rsession.getMaxInactiveInterval() match {
      case f if f > 0 => f*1000
      case _ => TimeOutMS
    }

    if (current - rsession.getLastUpdateMS() >= maxTimeOutMS && current - session.getLastUpdateMS() >= maxTimeOutMS) {
      //        RedisDAOs.getSmiddao().deleteByPrimaryKey(session)
      removeSession(session)
      log.info("SessionTimeOut:logid:" + session.getLoginId() + ":userid:" + session.getUserId() + ":lastup:" + session.getLastUpdateMS() + ":logtime:" + session.getLoginMS())
      false
    } else { //redis里面被别的集群节点更新过了
      if (!rsession.getLastUpdateMS().equals(session.getLastUpdateMS())) {
        ThreadContext.setContext(JpaContextConstants.Cache_Timeout_Second, maxTimeOutMS/1000)
        log.debug("update Session:" + session.smid + ":gid:" + session.globalID() + ",rsession=" + rsession + ",session = " + session)
        val upsession = LoginResIDSession(session, true);
        val rsessionv = RedisDAOs.logiddao.getAndSet(upsession);
        if (rsessionv.isKickout()) {
          session.kickout(true)
          removeSession(session)
          return false;
        }
      }
      true
    }
  }

  def removeSession(session: LoginResIDSession) {
    session.kickout(true)
    LoginIDRedisLoCache.delete(session)
  }

  abstract class Runner extends Runnable {
    var isRunner = false;
    def doSomething();
    def run() {
      isRunner = true;
      ThreadContext.setContext(JpaContextConstants.Cache_Timeout_Second, TimeOutSec)
      try {
        doSomething()
      } finally {
        isRunner = false;
        ThreadContext.cleanContext()
      }
    }
  }
  object CheckRunner extends Runner {
    def doSomething() {
      var size = checkBox.size()
      val it = checkBox.values().iterator();
      while (size > 0 && it.hasNext()) {
        val obj = it.next();
        size = size - 1;
        if (obj != null) {
          if (checkSMID(obj)) {
            //            exec.schedule(CheckRunner,
            //              Math.max(1, TimeOutSec - (System.currentTimeMillis() - obj.getLastUpdateMS()) / 1000), TimeUnit.SECONDS); //timeout的
          } else {
            checkBox.remove(obj.globalID())
          }
        } else {
          size = 0;
        }
      }
    }
  }

  object InsertRunner extends Runner {
    def doSomething() {
      var size = insertBox.size()
      val it = insertBox.values().iterator();
      val tmpList = new ArrayList[LoginResIDSession]();
      while (size > 0 && it.hasNext()) {
        val obj = it.next();
        size = size - 1;
        if (obj != null && !obj.isKickout()) {
          tmpList.add(obj);
          checkBox.put(obj.globalID(), obj)
        }
        //        it.remove();
        insertBox.remove(obj.globalID())
      }
      RedisDAOs.logiddao.batchInsert(tmpList.asInstanceOf[java.util.List[Object]]);
    }
  }

  //检查是否登录
  def checkAndUpdateSession(smid: String, newsession: LoginResIDSession = null): Tuple2[LoginResIDSession, String] = {
    val tkgid = SMIDHelper.fetchUID(smid);
    if (StringUtils.isBlank(tkgid)) {
      return (null, "smid_error_0")
    }
    val searchSession = LoginResIDSession(tkgid);
    if (searchSession == null) {
      return (null, "smid_error_1")
    }
    var session = LoginIDRedisLoCache.get(searchSession);
    if (session != null) {
      if (session.isKickout()) {
        return (null, "smid_error_2");
      }
       if( !StringUtils.equals(session.getSmid(), smid)){
         session =  LoginIDRedisLoCache.getFromDb(searchSession)
         if( !StringUtils.equals(session.getSmid(), smid)){
            return (null, "smid_error_3")
         }
      }
       

      val maxTimeOutMS = session.getMaxInactiveInterval() match {
        case f if f > 0 => f*1000
        case _ => TimeOutMS
      }

      if (System.currentTimeMillis() - session.lastUpdateMS > maxTimeOutMS) {
        removeSession(session);
        return (null, "session_timeout");
      }
      if (newsession != null) {
        //        if (!StringUtils.equals(session.smid, newsession.smid) ||
        //          !StringUtils.equals(session.loginId, newsession.loginId) ||
        //          !StringUtils.equals(session.resId, newsession.resId)) {
        //          return (null, "smid_error_3:not_the_same_session");
        //        }
        if (session.kvs == null) {
          log.debug("none for kvs:" + session);
          session.kvs = new HashMap();
        }
        if (newsession.getKvs() == null) {
          log.debug("new session.kvs is null:")
        } else {
          session.kvs.putAll(newsession.getKvs());
        }
      }
            
      ThreadContext.setContext(JpaContextConstants.Cache_Timeout_Second, maxTimeOutMS/1000)

      session.lastUpdateMS = System.currentTimeMillis();
      
        val rsessionv = RedisDAOs.logiddao.getAndSet(session);
        if (rsessionv.isKickout()) {
          session.kickout(true)
          removeSession(session)
          return  (null, "session_kickout");
        }

//      checkBox.put(session.globalID, session)
            
//      opexec.schedule(CheckRunner, Math.min(OpDelaySec, Math.max(1, (TimeOutMS - (System.currentTimeMillis() - lastup)) / 100)), TimeUnit.SECONDS); //timeout的
      return (session, "OK");
    }
    (null, "not_login")
  }

  // 登出
  def logout(smid: String, loginId: String, resId: String): Tuple2[LoginResIDSession, String] = {
    val tkgid = SMIDHelper.fetchUID(smid);
    val searchSession = LoginResIDSession(loginId, resId);
    if (!StringUtils.equals(searchSession.globalID(), tkgid)) {
      return (null, "smid_error_1")
    }
    var session = LoginIDRedisLoCache.get(searchSession);
    if (session != null) {
      if (session.isKickout()) {
        return (null, "smid_error_2")
      }
      if( !StringUtils.equals(session.getSmid(), smid)){
         session =  LoginIDRedisLoCache.getFromDb(searchSession)
         if( !StringUtils.equals(session.getSmid(), smid)){
            return (null, "smid_error_3")
         }
      }
      
      if (System.currentTimeMillis() - session.lastUpdateMS > TimeOutMS) {
        removeSession(session);
        return (null, "session_timeout");
      }
      removeSession(session)
      return (session, "OK");
    }
    (null, "not_login")

  }
}