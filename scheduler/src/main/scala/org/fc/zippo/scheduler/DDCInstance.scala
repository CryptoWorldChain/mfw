package org.fc.zippo.scheduler

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.JavaConversions._

import com.google.protobuf.Message

import onight.oapi.scala.commons.SessionModules
import onight.oapi.scala.traits.OLog
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.outils.conf.PropHelper
import onight.tfw.outils.pool.ReusefulLoopPool
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger

object DDCInstance extends OLog {

  val prop = new PropHelper(null);

  val daemonsWTC = new ScheduledThreadPoolExecutor(DDCConfig.DAEMON_WORKER_THREAD_COUNT);
  val defaultWTC = new ForkJoinPool(DDCConfig.DEFAULT_WORKER_THREAD_COUNT);
  val defaultQ = new LinkedBlockingQueue[Worker]
  val specQ = new ConcurrentHashMap[String, LinkedBlockingQueue[Worker]];
  val specDPW = new ConcurrentHashMap[String, (Iterable[DDCDispatcher], ForkJoinPool)];
  val running = new AtomicBoolean(true);

  def init() {
    running.set(true)
    log.info("DDC-Startup: defaultWTC=" + DDCConfig.DEFAULT_WORKER_THREAD_COUNT + ",daemonsWTC=" + DDCConfig.DAEMON_WORKER_THREAD_COUNT);
    for (i <- 1 to DDCConfig.DEFAULT_DISPATCHER_COUNT) {
      new Thread(new DDCDispatcher("default(" + i + ")", defaultQ, defaultWTC)).start()
    }
    //init specify actors
    DDCConfig.spectActors().map { x =>
      val gcmd = x._1
      val ddc = x._2;
      val wc = x._3;
      //drop exist
      val existQ = specQ.get(gcmd)
      if (existQ != null) {
        existQ.clear();
      }
      val existDP = specDPW.get(gcmd)
      if (existDP != null) {
        existDP._1.map { dp => dp.running.set(false) }
        existDP._2.shutdown();
      }
      //create new one
      val newQ = new LinkedBlockingQueue[Worker]
      val newWTC = new ForkJoinPool(wc);
      val incr = new AtomicInteger(0);
      val newDP = Array.fill(ddc)(0).map { x => new DDCDispatcher(gcmd + "(" + incr.incrementAndGet() + ")", newQ, newWTC); }
      newDP.map { f => new Thread(f).start() }
      specQ.put(gcmd, newQ)
      specDPW.put(gcmd, (newDP, newWTC))

    }
  }

  /**
   * run in seconds at fix delays
   */
  def scheduleWithFixedDelaySecond(run: Runnable, initialDelay: Long, period: Long) = {
    daemonsWTC.scheduleWithFixedDelay(run, initialDelay, period, TimeUnit.SECONDS)
  }

  def registActor(gcmd: String, sm: SessionModules[Message]) {

  }
  def post(pack: FramePacket, pbo: Message, handler: CompleteHandler, sm: SessionModules[Message]) = {
    if (defaultQ.size() < DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE) {
      defaultQ.offer(WorkerObjectPool.borrow(pack, pbo, handler, sm));
    } else {
      log.error("drop actor exec for pool size exceed:" + defaultQ.size() + "==>" + DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE);
    }
  }

  def shutdown() {
    running.set(false)
    daemonsWTC.shutdown();
    defaultWTC.shutdown()
  }
}