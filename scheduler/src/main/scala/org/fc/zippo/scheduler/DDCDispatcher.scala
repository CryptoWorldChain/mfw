package org.fc.zippo.scheduler

import java.util.concurrent.ForkJoinPool
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import onight.oapi.scala.traits.OLog
import java.util.concurrent.atomic.AtomicBoolean

case class DDCDispatcher(name: String, q: LinkedBlockingQueue[Worker], threadPool: ForkJoinPool, running: AtomicBoolean = new AtomicBoolean(true)) extends Runnable with OLog {

  def run() {
    Thread.currentThread().setName("DDC-Dispatcher-" + name)
    while (DDCInstance.running.get && running.get) {
      log.info("DDC-RT:" + name + ",tp[A=" + threadPool.getActiveThreadCount + ",Q=" + threadPool.getQueuedTaskCount + ",C=" + threadPool.getPoolSize
        + ",M=" + threadPool.getParallelism
        + ",S=" + threadPool.getStealCount + ",F=" + threadPool.getRunningThreadCount
        + "],defQ.size=" + q.size());
      val task = q.poll(DDCConfig.DEFAULT_DISPATCHER_QUEUE_WAIT_MS, TimeUnit.MILLISECONDS);
      if (task != null) {
        threadPool.submit(task);
      }
    }
  }
}