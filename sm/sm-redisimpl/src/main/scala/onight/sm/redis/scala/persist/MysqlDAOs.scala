package onight.sm.redis.scala.persist

import onight.async.mysql.commons.SimpleDAO
import scala.reflect.classTag
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors

object MysqlDAOs {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  case class KOLoginUser(val LOGIN_ID: String = null,
    val EMAIL: String = null,
    val MOBILE: String = null,
    val THIRD_LOGINID1: String = null,
    val THIRD_LOGINID2: String = null,
    val USER_ID: String = null,
    val PASSWORD: String = null, val TRADE_PASSWORD: String = null, val STATUS: Option[Int] = None)

  object ALoginUserDAO extends SimpleDAO[KOLoginUser] {
    val ttag = classTag[KOLoginUser];
    val tablename = "TFG_LOGIN_USER";
    val keyname = "USER_ID"
  }

}


