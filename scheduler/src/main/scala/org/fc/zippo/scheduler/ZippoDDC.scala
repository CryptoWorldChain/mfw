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
import org.apache.felix.ipojo.annotations.Component
import onight.osgi.annotation.iPojoBean
import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import org.fc.zippo.dispatcher.IActorDispatcher
import onight.tfw.ntrans.api.PBActor
import onight.tfw.async.ActorRunner
import onight.tfw.async.CallBack
import java.util.concurrent.ExecutorService
import org.fc.zippo.dispatcher.TimeLimitRunner
import onight.tfw.outils.conf.PropHelper
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

abstract class PMDDC[T <: Message] extends SessionModules[T] with OLog {
  override def getModule: String = PModule.DDC.name()
}

@NActorProvider
@Instantiate(name = "zippo.ddc")
@Provides(specifications = Array(classOf[ActorService]), strategy = "SINGLETON")
class ZippoDDC extends PMDDC[Message] with IActorDispatcher {
  override def getCmds: Array[String] = Array("SSS");
  @Validate
  def init():Unit =  {
    DDCInstance.init();
  }
  @Invalidate
  def destroy() {
    DDCInstance.destroy()
  }
  /**
   * run in seconds at fix delays
   */
  def scheduleWithFixedDelaySecond(run: Runnable, initialDelay: Long, period: Long) = {
    DDCInstance.scheduleWithFixedDelaySecond(run, initialDelay, period)
  }

  def post(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message]) = {
    DDCInstance.post(pack, handler, sm)
  }

  def post(pack: FramePacket, runner: Runnable) = {
    DDCInstance.post(pack, runner)
  }
  def executeNow(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message]): Unit = {
   DDCInstance.executeNow(pack, handler, sm)

  }
  def executeNow(pack: FramePacket, runner: Runnable): Unit = {
   DDCInstance.executeNow(pack, runner)
  }

  def postWithTimeout(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message], timeoutMS: Long): Unit = {
   DDCInstance.postWithTimeout(pack, handler, sm, timeoutMS)
  }

  def postWithTimeout(pack: FramePacket, runner: Runnable, timeoutMS: Long, handler: CompleteHandler): Unit = {
   DDCInstance.postWithTimeout(pack, runner, timeoutMS, handler)
  }

  def executeNowWithTimeout(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message], timeoutMS: Long): Unit = {
    DDCInstance.executeNowWithTimeout(pack, handler, sm, timeoutMS)
  }

  def executeNowWithTimeout(pack: FramePacket, runner: Runnable, timeoutMS: Long, handler: CompleteHandler): Unit = {
   DDCInstance.executeNowWithTimeout(pack, runner, timeoutMS, handler)
  }

  def getExecutorService(poolname: String): ExecutorService = {
    DDCInstance.getExecutorService(poolname)
  }

}

