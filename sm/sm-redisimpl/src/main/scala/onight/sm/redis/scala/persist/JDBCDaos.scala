package onight.sm.redis.scala.persist

import onight.oapi.scala.traits.OLog
import onight.tfw.outils.conf.PropHelper
import scalikejdbc._
import java.sql.DriverManager
import onight.tfw.outils.conf.PropHelper.IFinder
import java.util.HashMap
import onight.sm.Ssm.PBSSO
import org.apache.commons.lang3.StringUtils

object JDBCDaos extends OLog {

  val pconfig: PropHelper = new PropHelper(null);
  try {

    DriverManager.registerDriver(Class.forName(pconfig.get("sm.jdbc.driver", "com.mysql.jdbc.Driver")).newInstance().asInstanceOf[java.sql.Driver]);
  } catch {
    case e: Exception => log.error("Driver_Load_Error:" + pconfig.get("sm.jdbc.driver", "com.mysql.jdbc.Driver"), e)
  }
  GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
    enabled = true,
    singleLineMode = true,
    logLevel = 'DEBUG)

  ConnectionPool.singleton(
    pconfig.get("sm.jdbc.url", "jdbc:mysql://127.0.0.1:3306/TFG?autoReconnect=true"),
    pconfig.get("sm.jdbc.usr", "root"),
    pconfig.get("sm.jdbc.pwd", "000000"),
    ConnectionPoolSettings(initialSize = pconfig.get("sm.jdbc.initsize", 10), maxSize = pconfig.get("sm.jdbc.maxsize", 100)))

  val Sel_SQL = pconfig.get("sm.jdbc.sql.select.loginid", "SELECT LOGIN_ID,STATUS,PASSWORD FROM TFG_LOGIN_USER WHERE LOGIN_ID=? AND STATUS=1");
  val SQLMap = new HashMap[String, String];
  pconfig.get("sm.jdbc.sql.select", "").split(";").foreach { x =>
    pconfig.findMatch("sm.jdbc.sql.select.*", new IFinder {
      def onMatch(key: String, v: String) = {
        SQLMap.put(key.substring("sm.jdbc.sql.select.".length()), v)
      }
    });
  }
  // ad-hoc session provider on the REPL
  implicit val session = AutoSession

  def doSelectByLoginID(login_id: String): Map[String, Any] = {
    try {

      val passwd = SQL(Sel_SQL).bind(login_id).map { rs =>
        //        rs.string("PASSWORD")
        rs.toMap()
      }.single().apply()
      passwd match {
        case Some(m: Map[String, Any]) =>
          log.debug("selectResult:" + login_id + ",pwd=:" + m); m
        case None => log.debug("not Found:" + login_id); null
      }
    } catch {
      case e: Exception => log.warn("SELECT_PWD_ERROR:" + Sel_SQL + ",login_id=" + login_id, e); null
    }

  }

  def getLoginType(pbo: PBSSO): (String, String) = {
    if (StringUtils.isNoneBlank(pbo.getLoginId)) {
      ("login_id", pbo.getLoginId)
    } else if (StringUtils.isNoneBlank(pbo.getUserId)) {
      ("user_id", pbo.getUserId)
    } else if (StringUtils.isNoneBlank(pbo.getMobile)) {
      ("mobile", pbo.getMobile)
    } else if (StringUtils.isNoneBlank(pbo.getEmail)) {
      ("email", pbo.getEmail)
    } else if (StringUtils.isNoneBlank(pbo.getThirdLoginid1)) {
      ("third_loginid1", pbo.getThirdLoginid1)
    } else if (StringUtils.isNoneBlank(pbo.getThirdLoginid2)) {
      ("third_loginid2", pbo.getThirdLoginid2)
    }
    else null
  }

  def doSelectByOthers(loginType:(String, String),sso: PBSSO): Map[String, Any] = {
    try {
      if (loginType == null) return null;

      val sql = SQLMap.get(loginType._1);
      if (sql == null) return null;

      val passwd = SQL(sql).bind(loginType._2).map { rs =>
        //        rs.string("PASSWORD")
        rs.toMap()
      }.single().apply()
      passwd match {
        case Some(m: Map[String, Any]) =>
          log.debug("selectResult:" + loginType._1 + ",v2=:" + loginType._2); return m
        case None => log.debug("not Found:" + loginType._1); null
      }
      return null;
    } catch {
      case e: Exception => log.warn("SELECT_PWD_ERROR:" + Sel_SQL + ",=" + sso, e); null
    }

  }

}