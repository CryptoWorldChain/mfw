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
  val specQDW = new ConcurrentHashMap[String, (LinkedBlockingQueue[Worker], Iterable[DDCDispatcher], ForkJoinPool)];
  val specQ = new ConcurrentHashMap[String, (String, LinkedBlockingQueue[Worker])];

  val running = new AtomicBoolean(true);

  def init() {
    running.set(true)
    log.info("DDC-Startup: defaultWTC=" + DDCConfig.DEFAULT_WORKER_THREAD_COUNT + ",daemonsWTC=" + DDCConfig.DAEMON_WORKER_THREAD_COUNT);
    for (i <- 1 to DDCConfig.DEFAULT_DISPATCHER_COUNT) {
      new Thread(new DDCDispatcher("default(" + i + ")", defaultQ, defaultWTC)).start()
    }
    //init specify dispatcher -- thread pools 
    DDCConfig.specDispatchers().map { x =>
      val dcname = x._1
      val ddc = x._2;
      val wc = x._3;
      val newQ = new LinkedBlockingQueue[Worker]
      val newWTC = new ForkJoinPool(wc);
      val incr = new AtomicInteger(0);
      val newDP = Array.fill(ddc)(0).map { x => new DDCDispatcher(dcname + "(" + incr.incrementAndGet() + ")", newQ, newWTC); }
      newDP.map { f => new Thread(f).start() }
      specQDW.put(dcname, (newQ, newDP, newWTC))
    }

    DDCConfig.spectActors().map { x =>
      val gcmd = x._1;
      val dcname = x._2;
      val dqw = specQDW.get(dcname)
      if (dqw != null) {
        specQ.put(gcmd, (dcname, dqw._1))
      }
    }
  }

  /**
   * run in seconds at fix delays
   */
  def scheduleWithFixedDelaySecond(run: Runnable, initialDelay: Long, period: Long) = {
    daemonsWTC.scheduleWithFixedDelay(run, initialDelay, period, TimeUnit.SECONDS)
  }

  def post(pack: FramePacket, pbo: Message, handler: CompleteHandler, sm: SessionModules[Message]) = {
    val (dname, q) = specQ.getOrDefault(pack.getModuleAndCMD, ("default", defaultQ))
    if (q.size() < DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE) {
      q.offer(WorkerObjectPool.borrow(pack.getModuleAndCMD, pack, pbo, handler, sm, dname));
    } else {
      log.error("drop actor exec for pool size exceed:" + q.size() + "==>" + DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE);
    }
  }

  def shutdown() {
    running.set(false)
    daemonsWTC.shutdown();
    defaultWTC.shutdown()
    val nullWorker = new SessionModules[Message] {
      override def onPBPacket(pack: FramePacket, pbo: Message, handler: CompleteHandler) = {
      }
    };
    defaultQ.offer(WorkerObjectPool.borrow("quit", null, null, null, nullWorker, "default"))
    specQDW.map(f =>
      {
        val q = f._2._1;
        val d = f._2._2;
        val w = f._2._3;
        q.offer(WorkerObjectPool.borrow("quit", null, null, null, nullWorker, "default"));
        d.map { x => x.running.set(false) }
        w.shutdown();
      })
  }
}