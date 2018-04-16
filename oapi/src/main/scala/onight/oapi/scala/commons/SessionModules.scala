package onight.oapi.scala.commons

import org.osgi.framework.BundleReference
import com.google.protobuf.Message
import onight.tfw.async.AsyncPBActor
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.beans.FramePacket
import onight.oapi.scala.traits.OLog
import onight.tfw.outils.conf.PropHelper

//import onight.tfw.outils.serialize.SessionIDGenerator
//import onight.tfw.mservice.NodeHelper
//import org.apache.commons.lang3.StringUtils
//import onight.act.ordbgens.act.pbo.PBModule

abstract class SessionModules[T <: Message] extends AsyncPBActor[T] with OLog{

//  implicit val ctx =
//    classOf[BundleReference].cast(classOf[PBUtils].getClassLoader())
//      .getBundle()
//      .getBundleContext();
  implicit val props:PropHelper=new PropHelper(null);
  
  def service: LService[T] = null

  override def onPBPacket(pack: FramePacket, pbo: T, handler: CompleteHandler) = {

    try {
      service.onPBPacket(pack, pbo, handler)
    } catch {
      case t: Throwable => { log.error("fato:", t); throw new RuntimeException(t); }
    }
  }
  override def getCmds(): Array[String] = {
    if (service == null)
      Array("")
    else
      Array(service.cmd)

  }
}

