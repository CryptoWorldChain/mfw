package onight.sm.redis.scala.persist

import onight.oapi.scala.traits.OLog
import onight.tfw.outils.conf.PropHelper
import scalikejdbc._
import java.sql.DriverManager

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

  // ad-hoc session provider on the REPL
  implicit val session = AutoSession

  def doSelectByLoginID(login_id: String): String = {
    try {
      val passwd = SQL(Sel_SQL).bind(login_id).map { rs =>
        rs.string("PASSWORD")
      }.single().apply()
      passwd match {
        case Some(str) =>
          log.debug("selectResult:" + login_id + ",pwd=:" + str); str
        case None => log.debug("not Found:" + login_id); null
      }
    } catch {
      case e: Exception => log.warn("SELECT_PWD_ERROR:" + Sel_SQL+",login_id="+login_id, e);null
    }

  }

}