package onight.async.mysql.commons

import java.lang.reflect.Field

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Failure
import scala.util.Success

import org.slf4j.LoggerFactory

import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.QueryResult
import com.github.mauricio.async.db.ResultSet

//import akka.util.Timeout

case class DBResult(t: Any)
case class Range(offset: Int, limit: Int)

class QueryResultWithArray(
  val seqs: Seq[Any],
  override val rowsAffected: Long, override val statusMessage: String, override val rows: Option[ResultSet] = None)
  extends QueryResult(rowsAffected, statusMessage, rows) {

}

class NoneQueryResult()
  extends QueryResult(0, null) {

}
trait SimpleDAO[T] extends AsyncDB {
  val log = LoggerFactory.getLogger("SimpleDAO")
  //  implicit val timeout = Timeout(60000)
  //  def mapToBean(row: RowData): T;
  val ttag: ClassTag[T];
  val tablename: String;

  val keyname: String;

  lazy val fields = ttag.runtimeClass.getDeclaredFields().filter({ field =>
    field.setAccessible(true); true
  });
  lazy val constructor = ttag.runtimeClass.getConstructors()(0);

  def resultRow(queryResult: QueryResult): Any = {
    //    println(UpdateString)
    //    val fields = ttag.runtimeClass.getDeclaredFields();
    val ret: ListBuffer[T] = ListBuffer.empty;
    queryResult.rows match {
      case Some(rs) =>
        for (row <- queryResult.rows.head) ret.append({
          val map = fields.map({ field =>
            //            println("FF:" + field.getName() + ",=>" + row(field.getName()))
            if (row(field.getName()) == null) {
              null
            } else {
              println("FF:" + field.getName() + "(" + field.getType() + ")" + ",=>" + row(field.getName()) + ",type=" + row(field.getName()).getClass)
              if (field.getType() == classOf[String]) {
                row(field.getName()).asInstanceOf[String]
              } else if (field.getType() == classOf[Option[Int]] && row(field.getName()).isInstanceOf[java.lang.Integer]) {
                row(field.getName()) match {
                  case str: String =>
                    Some(str.toLong)
                  case a @ _ =>
                    Some(a.asInstanceOf[Int])
                }

              } else if (field.getType() == classOf[Option[Float]] && row(field.getName()).isInstanceOf[java.lang.Float]) {
                Option(row(field.getName()).asInstanceOf[Float])
              } else if (field.getType() == classOf[Option[Long]] && row(field.getName()).isInstanceOf[scala.math.BigDecimal]) {
                Option(row(field.getName()).asInstanceOf[scala.math.BigDecimal].toLong)
              } else if (field.getType() == classOf[Option[Long]] && row(field.getName()).isInstanceOf[java.lang.String]) {
                Option(row(field.getName()).asInstanceOf[String].toLong)
              } else if (field.getType() == classOf[Option[Long]] && row(field.getName()).isInstanceOf[java.lang.Long]) {
                Option(row(field.getName()).asInstanceOf[Long])
              } else {

                row(field.getName()).asInstanceOf[String]
                //          row(field.getName()).asInstanceOf[String]
              }
            }
          })
          val instance = constructor.newInstance(map.toArray[AnyRef]: _*)
          instance.asInstanceOf[T]
        })
      case _ => return queryResult
    }
    //    println(queryResult.rows)

    return ret.toList;
  }

  def beanValue(bean: T, filter: Field => Boolean): Seq[Any] = {
    fields.filter(filter).map(_.get(bean)).map { v =>
      v match {
        case opt: Option[Any] => opt.getOrElse(null)
        case list: List[Any] => {
          list.mkString(",")
        }
        case _ => v
      }
    }
  }

  def bean2Array(bean: T): Seq[Any] = {
    fields.map(_.get(bean)).map { v =>
      v match {
        case opt: Option[Any] => opt.getOrElse(null)
        case list: List[Any] => {
          list.mkString(",")
        }
        case _ => v
      }
    }
  }
  def folder(v: Any): String = {
    v match {
      case opt: Option[Any] => '"' + opt.getOrElse("null").toString + '"'
      case list: List[Any] => {
        (list.mkString(","))
      }
      case v if v != null => '"' + v.toString + '"'
      case _ => "null"
    }
  }
  def beans2Values(beans: List[T]): String = {
    val list = (for (bean <- beans) yield fields.map(_.get(bean)).map(folder(_)).mkString("(", ",", ")"))
    list.mkString("values", ",", "")
  }
  def beans2Prepare(beans: List[T]): String = {
    val list = (for (bean <- beans) yield fields.map(f => "?").mkString("(", ",", ")"))
    list.mkString("values", ",", "")
  }

  def beans2Array(beans: List[T]): Seq[Any] = {
    val seqs: ArrayBuffer[Any] = ArrayBuffer.empty;
    for (bean <- beans) yield fields.map(_.get(bean)).map { v =>
      v match {
        case opt: Option[Any] => seqs.+=(opt.getOrElse(null))
        case list: List[Any] => {
          seqs.+=(list.mkString(","))
        }
        case _ => seqs.+=(v)
      }
    }
    seqs
  }
  def bean2KeyLastArray(bean: T): Seq[Any] = beanValue(bean, _.getName() != keyname) ++ beanValue(bean, _.getName() == keyname)

  def beans2KeyLastArray(beans: List[T]): Seq[Any] =
    {
      //     for (bean <- beans) yield beanValue(bean, _.getName() != keyname) ++ beanValue(bean, _.getName() == keyname)
      val seqs: ArrayBuffer[Any] = ArrayBuffer.empty;

      beans.map { bean =>
        seqs ++= bean2KeyLastArray(bean)
      }
      seqs
    }

  def bean2NoKeyArray(bean: T): Seq[Any] = beanValue(bean, _.getName() != keyname)

  def bean2KeyArray(bean: T): Seq[Any] = beanValue(bean, _.getName() == keyname)

  def fieldValueIsNotNull(fv:Any) =  fv != null && fv!=None 
  
  
  def bean2SelectiveArray(bean: T): Seq[Any] = beanValue(bean, { v =>
       val vv=v.get(bean);
        vv != null && vv!=None 
  })

  def bean2NoKeySelectiveArray(bean: T): Seq[Any] = beanValue(bean, { field =>
    field.get(bean) != null && field.getName() != keyname
  })

  def getInt(queryResultO: Option[QueryResult]): Int = {
    queryResultO match {
      case Some(queryResult) =>
        queryResult.rows match {
          case Some(rs) =>
            queryResult.rows.head(0).asInstanceOf[Int]
          case _ =>
            -1
        }
      case _ => -1
    }

  }
  def exec(query: String, values: Seq[Any] = List())(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = {
    log.info("exec:" + query + ",obj=" + values)

    if (noexec) return Future { new QueryResultWithArray(values, 0L, query) }

    val result = pool.sendPreparedStatement(query, values)
    if (f != null) {
      result.onComplete {
        case Success(result) => {
          val dbresults = DBResult(resultRow(result.asInstanceOf[QueryResult]))
          f(dbresults)
        }
        case Failure(failure) => f(DBResult(failure))
      }
    } else {
      //      val ff = Await.result(result, timeout.duration);

    }
    return result
  }
  def execQuery(query: String)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = {
    log.info("exec:" + query)
    if (noexec) return Future { new QueryResult(0L, query) }
    val result = pool.sendQuery(query)
    if (f != null) {
      result.onComplete {
        case Success(result) => {
          val dbresults = DBResult(resultRow(result.asInstanceOf[QueryResult]))
          f(dbresults)
        }
        case Failure(failure) => f(DBResult(failure))
      }
    } else {
      //      val ff = Await.result(result, timeout.duration);

    }
    return result
  }
  private lazy val InsertString: String = {
    ("INSERT INTO " + tablename + " (") + fields.map({ _.getName() }).mkString(",") + ") values(" + "?," * (fields.size - 1) + "?)"
  }

  private lazy val UpdateString: String = {
    ("UPDATE " + tablename + " SET ") + fields.filter(_.getName() != keyname).map({ _.getName() }).mkString("=(?) , ") + " =(?) WHERE " + keyname + " = (?)"
  }

  private def SelectString: String = {
    ("SELECT ") + fields.map({ _.getName() }).mkString(",") + " from " + tablename;
  }
  private def CountString: String = {
    "SELECT COUNT(1)  as __count FROM " + tablename;
  }

  private def rangeStr(range: Range): String = {
    if (range == null) ""
    else
      " LIMIT " + range.offset + "," + range.limit + " "
  }

  private def SelectOneString: String = {
    ("SELECT ") + fields.map({ _.getName() }).mkString(",") + " from " + tablename + " WHERE " + keyname + " = (?)";
  }

  private def SelectByCond(bean: T)(implicit condition: String = "AND"): String = {
    ("SELECT ") + fields.map({ _.getName() }).mkString(",") + " from " + tablename + " WHERE " +
      fields.filter({ v=>
        val vv=v.get(bean);
        vv != null && vv!=None 
      }).map({ _.getName() }).mkString("", "=(?) " + condition + " ", "=(?) ")
  }
  private def CountByCond(cond: String): String = {
    "SELECT COUNT(1) as __count from " + tablename + " WHERE " + cond
  }

  private lazy val InsertOrUpdate: String = {
    InsertString + " ON DUPLICATE KEY UPDATE " + fields.filter(_.getName() != keyname).map(_.getName()).mkString("", "=(?) , ", "=(?)")
  }

  //  private def InsertBatchString(beans: List[T]): String = {
  //    ("INSERT INTO " + tablename + " (") + fields.map({ _.getName() }).mkString(",") + ") " +  beans2Values(beans)
  //  }

  private def InsertBatchString(beans: List[T]): String = {
    ("INSERT INTO " + tablename + " (") + fields.map({ _.getName() }).mkString(",") + ") " + beans2Prepare(beans)
  }

  private def UpdateBatchString(beans: List[T]): String = {

    (("UPDATE " + tablename + " SET ") + fields.filter({ field =>
      field.getName() != keyname
    }).map({ _.getName() }).mkString("=(?) , ") + " =(?) WHERE " + keyname + " = (?);") * beans.size
  }

  private def UpdateSelectiveString(bean: T): String = {
    ("UPDATE " + tablename + " SET ") + fields.filter({ field =>
      field.get(bean) != null && field.getName() != keyname
    }).map({ _.getName() }).mkString("=(?) , ") + " =(?) WHERE " + keyname + " = (?)"
  }

  private def UpdateCondString(beanset: T, beanfilter: T): String = {
    ("UPDATE " + tablename + " SET ") + fields.filter(_.get(beanset) != null).map({ _.getName() }).mkString("=(?) , ") + " =(?) WHERE " +
      fields.filter(_.get(beanfilter) != null).map { _.getName() }.mkString(" ", "=(?) AND ", "=(?) ")
  }

  private lazy val DeleteOneString: String = {
    "DELETE FROM " + tablename + " WHERE " + keyname + " = (?)";
  }

  private def DeleteCondString(bean: T): String = {
    val selectFields = fields.filter(_.get(bean) != null)
    "DELETE FROM " + tablename + " WHERE " + selectFields.map({ _.getName() }).mkString("=(?) AND ") + " = (?)"
  }

  private def InsertSelectiveString(bean: T): String = {
    val selectFields = fields.filter(_.get(bean) != null)
    ("INSERT INTO " + tablename + " (") + selectFields.map({ _.getName() }).mkString(",") + ") values(" + "?," * (selectFields.size - 1) + "?)"
  }

  def insertSelective(bean: T)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(InsertSelectiveString(bean), bean2SelectiveArray(bean))(f, noexec)

  def insert(bean: T)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(InsertString, bean2Array(bean))(f, noexec)

  def insertBatch(beans: List[T])(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(InsertBatchString(beans), beans2Array(beans))(f, noexec)

  def insertOrUpdate(bean: T)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(InsertOrUpdate, bean2Array(bean) ++ bean2NoKeyArray(bean))(f, noexec)

  def update(bean: T)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(UpdateString, bean2KeyLastArray(bean))(f, noexec)

  //  def updateBatch(beans: List[T])(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(UpdateBatchString(beans), beans2KeyLastArray(beans))(f,noexec)

  def updateSelective(bean: T)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(UpdateSelectiveString(bean), bean2NoKeySelectiveArray(bean) ++ bean2KeyArray(bean))(f, noexec)

  def updateByCond(beanset: T, beanfilter: T)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(UpdateCondString(beanset, beanfilter),
    bean2SelectiveArray(beanset) ++ bean2SelectiveArray(beanfilter))(f, noexec)

  def delete(keyy: String)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(DeleteOneString, Array(keyy))(f, noexec)

  def deleteByCond(bean: T)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(DeleteCondString(bean), bean2SelectiveArray(bean))(f, noexec)

  def findByKey(keyy: String)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = if (keyy != null && keyy.length() > 0) exec(SelectOneString, Array(keyy))(f, noexec) else exec(SelectString)(f, noexec)

  def findByCondPrepare(bean: T, range: Range = null)(implicit condition: String = "AND", f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(SelectByCond(bean)(condition) + rangeStr(range), bean2SelectiveArray(bean))(f, noexec)

  def findByCond(cond: String, range: Range = null)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = execQuery(SelectString + " WHERE " + cond + rangeStr(range))(f, noexec)

  //  def findByCondPrepare(cond: String, range: Range = null)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(SelectString + " WHERE " + cond, )(f, noexec)

  def findAll(range: Range = null)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[QueryResult] = exec(SelectString + rangeStr(range))(f, noexec)

  def countAll(): Future[Option[Long]] = exec(CountString).map({ result =>
    result.rows.map({ row =>
      row.head(0).asInstanceOf[Long]
    })
  })

  def countByCond(cond: String)(implicit f: DBResult => Unit = null, noexec: Boolean = false): Future[Option[Long]] = exec(CountByCond(cond))(f, noexec).map({ result =>
    result.rows.map({ row =>
      println("count==" + row.head(0).asInstanceOf[Long])
      row.head(0).asInstanceOf[Long]
    })
  })

  def hb() = exec("SELECT " + keyname + " FROM " + tablename + " LIMIT 1")

  //  import reflect.runtime.universe._

  // then only keep the ones with a MyProperty annotation
  //  fields.flatMap(f => f.annotations.find(_.tpe =:= typeOf[MyProperty]).
  //    map((f, _))).toList

  def execListSub(qr: QueryResult, c: Connection): Future[QueryResult] = {
    qr match {
      case qrw: QueryResultWithArray => c.sendPreparedStatement(qrw.statusMessage, qrw.seqs)
      case _ => c.sendPreparedStatement(qr.statusMessage)
    }
  }
  def execList(fs: List[QueryResult], i: Int, c: Connection): Future[QueryResult] = {
    if (i < fs.size - 1)
      execListSub(fs(i), c).flatMap(r => execList(fs, i + 1, c))
    else
      execListSub(fs(i), c)
  }

}

