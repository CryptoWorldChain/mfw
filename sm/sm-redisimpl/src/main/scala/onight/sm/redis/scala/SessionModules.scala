package onight.sm.redis.scala

import org.osgi.framework.BundleReference
import com.google.protobuf.Message
import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.traits.OLog
import onight.tfw.async.AsyncPBActor
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.outils.serialize.SessionIDGenerator
import onight.tfw.mservice.NodeHelper
import org.apache.commons.lang3.StringUtils

@Slf4j
abstract class SessionModules[T <: Message] extends AsyncPBActor[T] with OLog {

  override def getModule: String = "SSM"

  implicit val ctx =
    classOf[BundleReference].cast(classOf[PBUtils].getClassLoader())
      .getBundle()
      .getBundleContext();

  def service: LService[T] = null

  override def onPBPacket(pack: FramePacket, pbo: T, handler: CompleteHandler) = {

    try {
      service.onPBPacket(pack, pbo, handler)
    } catch {
      case t: Throwable => log.error("fato:", t)
    }
  }
  override def getCmds(): Array[String] = {
    if (service == null)
      Array("")
    else
      Array(service.cmd)

  }
}

object SMIDHelper {
  def cureTimeIdx: String = {
    StringUtils.rightPad(
      SessionIDGenerator.int2Str(((System.currentTimeMillis() / (60 * 1000)) % (24 * 60)).asInstanceOf[Int]), 2, "W");
  }
  val sessionidGenerator = new SessionIDGenerator(NodeHelper.getCurrNodeID);
  def nextSMID(implicit userid: String = ""): String = sessionidGenerator.generate(userid)
  def nextToken(userid: String = "", key: String = "ofw20"): String = sessionidGenerator.genToken(userid, key, cureTimeIdx)
  def checkToken(token: String = "", key: String = "ofw20"): String = sessionidGenerator.checkToken(token, key)
}

