package onight.sm.redis.entity

import java.util.HashMap

import scala.beans.BeanProperty

import org.apache.commons.lang3.StringUtils

class LoginResIDSession {
  @BeanProperty var smid: String = null
  @BeanProperty var userId: String = null;
  @BeanProperty var loginId: String = null
  @BeanProperty var password: String = null
  @BeanProperty var resId: String = null
  @BeanProperty var lastUpdateMS: Long = System.currentTimeMillis();
  @BeanProperty var loginMS: Long = System.currentTimeMillis();
  @BeanProperty var maxInactiveInterval: Long = 0;
  @BeanProperty var status: String = null;
  @BeanProperty var kvs = new HashMap[String, Object]();

  def globalID(): String = { loginId + "/" + resId }
  def isKickout(): Boolean = {
    status != null && StringUtils.equals(status, "K")
  }
  def kickout(kick: Boolean) = {
    if (kick) status = "K";
    else status = "N"
  }
}

//class LoginResIDSession extends SMIDSession {
//}
object LoginResIDSession {
  //  def apply(session: LoginResIDSession) = {
  //    val ret = new LoginResIDSession();
  //    ret.smid = session.smid
  //    ret.userId = session.userId
  //    ret.loginId = session.loginId
  //    ret.password = session.password
  //    ret.resId = session.resId
  //    ret.ext = session.ext;
  //    ret
  //  }
  def apply(session: LoginResIDSession, update: Boolean) = {
    val ret = new LoginResIDSession();
    ret.loginId = session.loginId
    ret.resId = session.resId
    ret.lastUpdateMS = session.lastUpdateMS;
    ret.kvs = session.kvs;
    ret.status = session.status;
    ret
  }
  def apply(login_res: String) = {
    val arrs = login_res.split("/");
    if (arrs.length > 2) {
      null;
    } else {
      val ret = new LoginResIDSession()
      ret.loginId = arrs(0)
      if (arrs.length == 1) {
        ret.resId = "";
      } else {

        ret.resId = arrs(1)
      }
      ret
    }
  }

  def apply(loginId: String, resId: String) = {
    val ret = new LoginResIDSession();
    ret.loginId = loginId
    ret.resId = resId
    ret
  }
  def apply(smid: String, userId: String, loginId: String, password: String, resId: String, kvs: HashMap[String, Object]) = {
    val ret = new LoginResIDSession();
    ret.smid = smid
    ret.userId = userId
    ret.loginId = loginId
    ret.password = password
    ret.resId = resId
    ret.kvs = kvs;
    ret
  }
}
//object SMIDSession {
//  def apply(smid: String) = {
//    val ret = new SMIDSession();
//    ret.smid = smid
//    ret
//  }
//  def apply(smid: String, userId: String, loginId: String, password: String, resId: String, ext: String) = {
//    val ret = new SMIDSession();
//    ret.smid = smid
//    ret.userId = userId
//    ret.loginId = loginId
//    ret.password = password
//    ret.resId = resId
//    ret.ext = ext;
//    ret
//  }
//}

/*
 * 保存token的地方
 */
class TokenEncKeys {
  @BeanProperty var timeIdx: String = null;
  @BeanProperty var enckey: String = null;
  @BeanProperty var gentime: Long = System.currentTimeMillis();
}
object TokenEncKeys {
  def apply(timeIdx: String) = {
    val ret = new TokenEncKeys();
    ret.timeIdx = timeIdx;
    ret
  }
}
  