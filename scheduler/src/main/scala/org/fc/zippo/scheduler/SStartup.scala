package org.fc.zippo.scheduler

import org.apache.felix.ipojo.annotations.Invalidate
import org.apache.felix.ipojo.annotations.Validate
import org.fc.zippo.scheduler.pbgens.Schedule.PModule

import com.google.protobuf.Message

import onight.oapi.scala.commons.SessionModules
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.async.CompleteHandler

abstract class PMDDC[T <: Message] extends SessionModules[T] with OLog {
  override def getModule: String = PModule.DDC.name()
}

@NActorProvider
class SStartup extends PMDDC[Message] {

  override def getCmds: Array[String] = Array("SSS");

  @Validate
  def init() {
    //abc
    DDCInstance.init();
  }

  @Invalidate
  def destory() {
    DDCInstance.shutdown();
  }

  def post(pack: FramePacket, pbo: Message, handler: CompleteHandler, sm: SessionModules[Message]): Unit = {
    DDCInstance.post(pack, pbo, handler, sm);
  }
  def scheduleWithFixedDelaySecond(run: Runnable, initialDelay: Long, period: Long): Unit = {
    DDCInstance.scheduleWithFixedDelaySecond(run, initialDelay, period);
  }
}

