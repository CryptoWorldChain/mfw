package org.fc.zippo.scheduler

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import onight.oapi.scala.traits.OLog
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.LinkedBlockingDeque

case class DDCDispatcher(name: String, q: LinkedBlockingDeque[Worker], threadPool: ForkJoinPool, running: AtomicBoolean = new AtomicBoolean(true)) extends Runnable with OLog {

  def run() {
    Thread.currentThread().setName("DDC-Dispatcher-" + name)
    var lastlogTime: Long = 0;

    while (running.get) {
      try {
        if (System.currentTimeMillis() - lastlogTime > DDCConfig.LOGINFO_DISPATCHER_TIMESEC*1000) {
          log.info("DDC-Dispatcher:" + name + ",tp[A=" + threadPool.getActiveThreadCount + ",Q=" + threadPool.getQueuedTaskCount + ",C=" + threadPool.getPoolSize
            + ",M=" + threadPool.getParallelism
            + ",S=" + threadPool.getStealCount + ",F=" + threadPool.getRunningThreadCount
            + "],defQ.size=" + q.size());
          lastlogTime = System.currentTimeMillis();
        }
        val task = q.poll(DDCConfig.DEFAULT_DISPATCHER_QUEUE_WAIT_MS, TimeUnit.MILLISECONDS);
        threadPool.submit(task);
      } catch {
        case npe: NullPointerException =>
        case reject: RejectedExecutionException =>
        case t: Throwable =>
          log.error("error in dispatching:" + name + ",q.size=" + q.size() + t, t)
      }
    }

    log.info("DDC-Dispatcher [ Quit ]:" + name + ",tp[A=" + threadPool.getActiveThreadCount + ",Q=" + threadPool.getQueuedTaskCount + ",C=" + threadPool.getPoolSize
      + ",M=" + threadPool.getParallelism
      + ",S=" + threadPool.getStealCount + ",F=" + threadPool.getRunningThreadCount
      + "],defQ.size=" + q.size());

  }
}