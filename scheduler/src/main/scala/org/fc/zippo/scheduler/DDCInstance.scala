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

object DDCInstance extends OLog {
  val prop = new PropHelper(null);

  val daemonsWTC = new ScheduledThreadPoolExecutor(DDCConfig.DAEMON_WORKER_THREAD_COUNT);

  val timeoutSCH = new ScheduledThreadPoolExecutor(DDCConfig.TIMEOUT_CHECK_THREAD_COUNT);
  val defaultWTC = new ForkJoinPool(DDCConfig.DEFAULT_WORKER_THREAD_COUNT);
  val defaultQ = new LinkedBlockingDeque[Worker]
  val specQDW = new ConcurrentHashMap[String, (LinkedBlockingDeque[Worker], Iterable[DDCDispatcher], ForkJoinPool)];
  val specQ = new ConcurrentHashMap[String, (String, LinkedBlockingDeque[Worker])];

  val running = new AtomicBoolean(true);
  @Validate
  def init(): Unit = {
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
      val newQ = new LinkedBlockingDeque[Worker]
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
    log.info("DDC-Startup[OK]: defaultWTC=" + DDCConfig.DEFAULT_WORKER_THREAD_COUNT + ",daemonsWTC=" + DDCConfig.DAEMON_WORKER_THREAD_COUNT);
  }
  @Invalidate
  def destroy() {
    running.set(false)
    daemonsWTC.shutdown();
    defaultWTC.shutdown()
    val nullWorker = new SessionModules[Message] {
      override def onPBPacket(pack: FramePacket, pbo: Message, handler: CompleteHandler) = {
      }
    };
    defaultQ.offer(WorkerObjectPool.borrow("quit", null, null, nullWorker, "default"))
    val ele = specQDW.elements();
    while (ele.hasMoreElements()) {
      val f = ele.nextElement()
      val q = f._1;
      val d = f._2;
      val w = f._3;
      q.offer(WorkerObjectPool.borrow("quit", null, null, nullWorker, "default"));
      d.map { x => x.running.set(false) }
      w.shutdown();
    }
  }
  /**
   * run in seconds at fix delays
   */
  def scheduleWithFixedDelaySecond(run: Runnable, initialDelay: Long, period: Long) = {
    daemonsWTC.scheduleWithFixedDelay(run, initialDelay, period, TimeUnit.SECONDS)
  }

  def post(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message]) = {
    val (dname, q) = if (specQ.containsKey(pack.getModuleAndCMD)) specQ.get(pack.getModuleAndCMD)
    else {
      ("default", defaultQ)
    }
    if (q.size() < DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE) {
      q.offer(WorkerObjectPool.borrow(pack.getModuleAndCMD, pack, handler, sm, dname));
    } else {
      log.error("drop actor exec for pool size exceed:" + q.size() + "==>" + DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE);
    }
  }

  def post(pack: FramePacket, runner: Runnable) = {
    val (dname, q) = if (specQ.containsKey(pack.getModuleAndCMD)) specQ.get(pack.getModuleAndCMD)
    else {
      ("default", defaultQ)
    }
    if (q.size() < DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE) {
      q.offer(WorkerObjectPool.borrow(pack.getModuleAndCMD, runner, dname));
    } else {
      log.error("drop actor exec for pool size exceed:" + q.size() + "==>" + DDCConfig.DEFAULT_WORKER_QUEUE_MAXSIZE);
    }
  }
  def executeNow(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message]): Unit = {
    val (dname, q) = if (specQ.containsKey(pack.getModuleAndCMD)) specQ.get(pack.getModuleAndCMD)
    else {
      ("default", defaultQ)
    }
    q.addFirst(WorkerObjectPool.borrow(pack.getModuleAndCMD, pack, handler, sm, dname));

  }
  def executeNow(pack: FramePacket, runner: Runnable): Unit = {
    val (dname, q) = if (specQ.containsKey(pack.getModuleAndCMD)) specQ.get(pack.getModuleAndCMD)
    else {
      ("default", defaultQ)
    }
    q.addFirst(WorkerObjectPool.borrow(pack.getModuleAndCMD, runner, dname));
  }

  def postWithTimeout(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message], timeoutMS: Long): Unit = {
    val runner = new TimeLimitRunner(timeoutSCH, timeoutMS, handler) {
      @Override
      def runOnce() = {
        sm.onPacket(pack, handler);
      }
    };
    post(pack, handler, sm);
  }

  def postWithTimeout(pack: FramePacket, runner: Runnable, timeoutMS: Long, handler: CompleteHandler): Unit = {
    val tlrunner = new TimeLimitRunner(timeoutSCH, timeoutMS, handler) {
      def runOnce() = {
        runner.run();
      }
    };
    post(pack, tlrunner);
  }

  def executeNowWithTimeout(pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message], timeoutMS: Long): Unit = {
    val runner = new TimeLimitRunner(timeoutSCH, timeoutMS, handler) {
      @Override
      def runOnce() = {
        sm.onPacket(pack, handler);
      }
    };
    executeNow(pack, handler, sm);
  }

  def executeNowWithTimeout(pack: FramePacket, runner: Runnable, timeoutMS: Long, handler: CompleteHandler): Unit = {
    val tlrunner = new TimeLimitRunner(timeoutSCH, timeoutMS, handler) {
      def runOnce() = {
        runner.run();
      }
    };
    executeNow(pack, tlrunner);
  }

  def getExecutorService(poolname: String): ExecutorService = {
    val v = specQDW.get(poolname)
    if (v != null) {
      v._3;
    } else {
      defaultWTC
    }
  }

}