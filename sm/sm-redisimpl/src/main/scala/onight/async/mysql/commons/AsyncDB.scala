package onight.async.mysql.commons

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.RowData

trait AsyncDB {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  val pool=DBPool.pool
  def execute(query: String, values: Any*): Future[QueryResult] = {
    if (values.size > 0)
      pool.sendPreparedStatement(query, values)
    else
      pool.sendQuery(query)
  }

  def fetch(query: String, values: Any*): Future[Option[Seq[RowData]]] =
    execute(query, values: _*).map(_.rows)

}