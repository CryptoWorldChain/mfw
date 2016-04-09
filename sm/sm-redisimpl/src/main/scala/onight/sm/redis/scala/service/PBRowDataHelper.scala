package onight.sm.redis.scala.service

import scala.collection.JavaConversions.asScalaBuffer

import com.github.mauricio.async.db.RowData

import onight.oapi.scala.traits.OLog
import onight.sm.Ssm.PBSSORet

object PBRowDataHelper extends OLog {
  def fieldValue(row: RowData, name: String): Any = {
    try {
      val ret = row(name) match {
        case v: String => v
        case n @ null => {
          return null;
        }
        case v: org.joda.time.LocalDateTime => {
          v.toDateTime().getMillis
        }
        case Some(v) => return v
        case v: scala.math.BigDecimal => {
          //        log.debug("change bigDe{}",v.doubleValue());
          return Double.box(v.doubleValue());
        }
        case v @ _ => {
          log.warn("unknow type:" + v.getClass() + ":" + row(name))
          return v
        }
      }
      return ret;
    } catch {
      case e: Throwable => {

      }
      case n: java.util.NoSuchElementException =>
        {

        }
    }
    return null;
  }
  def copyFields(builder: PBSSORet.Builder, row: RowData) = {

    builder.getDescriptorForType.getFields.foreach { fd =>
      //      println("fd==:" + fd + ",,name==" + fd.getName)
      //      println("fd==:" + fd + ",,name==" + fd.getName + "::rowcon=" + row("" + fd.getName.toUpperCase()))

      val v = fieldValue(row, fd.getName.toUpperCase())
      if (v != null) {
        try {
          builder.setField(fd, v)
        } catch {
          case a: Throwable => {
            if (fd.getType == com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING) {
              try { builder.setField(fd, String.valueOf(v)) } catch {
                case aa: Throwable => {
                  log.error("cannot set v:" + fd.getName + ",v=" + v + "," + aa.getMessage)
                }
              }
            }

          }
        }
      }
    }
  }

  def copyMFields(builder: PBSSORet.Builder, retfields: Map[String, Any]):collection.mutable.Map[String, Any] = {

    val mmp = collection.mutable.Map[String, Any]() ++= retfields

    builder.getDescriptorForType.getFields.foreach { fd =>
      retfields.get(fd.getName.toUpperCase()) match {
        case Some(v) =>
          {
            try {
              builder.setField(fd, v)
              mmp.remove(fd.getName.toUpperCase());
            } catch {
              case a: Throwable => {
                if (fd.getType == com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING) {
                  try {
                    builder.setField(fd, String.valueOf(v))
                    mmp.remove(fd.getName.toUpperCase());
                  } catch {
                    case aa: Throwable => {
                      log.error("cannot set v:" + fd.getName + ",v=" + v + "," + aa.getMessage, aa)
                    }
                  }
                }
              }
            }
          }
        case None =>
      }

    }
    return mmp;

  }

}