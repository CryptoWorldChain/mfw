package onight.async.mysql.commons

import org.osgi.framework.BundleContext

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.github.mauricio.async.db.pool.ConnectionPool
import com.github.mauricio.async.db.pool.PoolConfiguration

import onight.tfw.outils.conf.PropHelper

class DBPool()(implicit val ctx: BundleContext=null) {

  val pconfig: PropHelper = new PropHelper(ctx);

  val dbUsername = pconfig.get("tcp-async.db.username", "root")
  val dbPassword = pconfig.get("tcp-async.db.password", "000000")
  val dbPort = pconfig.get("tcp-async.db.port", 3306)
  val dbHost = pconfig.get("tcp-async.db.host", "localhost")

  val dbName = pconfig.get("tcp-async.db.name", "TFG")
  val dbPoolMaxObjects = pconfig.get("tcp-async.db.pool.maxObjects", 10)
  val dbPoolMaxIdle = pconfig.get("tcp-async.db.pool.maxIdle", 1000)
  val dbPoolMaxQueueSize = pconfig.get("tcp-async.db.pool.maxQueueSize", 10000)

  val configuration = new Configuration(username = dbUsername,
    port = dbPort,
    host = dbHost,
    password = Some(dbPassword),
    database = Some(dbName))

  val factory = new MySQLConnectionFactory(configuration)
  val pool = new ConnectionPool(factory, new PoolConfiguration(dbPoolMaxObjects, dbPoolMaxIdle, dbPoolMaxQueueSize))

}

object DBPool {
  val pool = new DBPool().pool
  //  def apply(val ctx: BundleContext) = new DBPool(ctx)
}


