package org.fc.zippo.scheduler

import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.beans.FramePacket
import com.google.protobuf.Message
import onight.oapi.scala.commons.SessionModules
import onight.tfw.outils.pool.ReusefulLoopPool
import lombok.AllArgsConstructor
import onight.tfw.ntrans.api.PBActor

class Worker(
    var gcmd: String = null,
    var pack: FramePacket = null,
    var handler: CompleteHandler = null,
    var sm: PBActor[Message] = null, var dcname: String = "default") extends Runnable {
  def run() {
    Thread.currentThread().setName("aworker-" + dcname + "-" + gcmd);
    try {
      sm.onPacket(pack, handler)
    } catch {
      case t: RuntimeException =>
        handler.onFailed(t)
    } finally {
      WorkerObjectPool.returnWorker(this)
      Thread.currentThread().setName("aworker-wait-" + dcname);
    }
  }
}

class RunnableWorker(
    var rgcmd: String = null,
    var runner: Runnable, var rdcname: String = "default") extends Worker {
  override def run() {
    Thread.currentThread().setName("aworker-" + rdcname + "-" + rgcmd);
    try {
      runner.run();
    } finally {
      WorkerObjectPool.returnRunnable(this)
      Thread.currentThread().setName("aworker-wait-" + rdcname);
    }
  }
}

object WorkerObjectPool {
  val pool = new ReusefulLoopPool[Worker]();
  val runnerPool = new ReusefulLoopPool[RunnableWorker]();

  def borrow(gcmd: String, pack: FramePacket, handler: CompleteHandler, sm: PBActor[Message], dcname: String): Worker = {
    val r = pool.borrow();
    if (r != null) {
      r.gcmd = gcmd;
      r.handler = handler
      r.pack = pack
      r.dcname = dcname
      r.sm = sm;
      r
    } else {
      new Worker(gcmd, pack, handler, sm, dcname);
    }
  }

  def borrow(gcmd: String, runner: Runnable, dcname: String): RunnableWorker = {
    val r = runnerPool.borrow();
    if (r != null) {
      r.gcmd = gcmd;
      r.dcname = dcname
      r.runner = runner;
      r
    } else {
      new RunnableWorker(gcmd, runner, dcname);
    }
  }
  def returnWorker(r: Worker) {
    if (pool.size() < DDCConfig.WORKER_OBJECT_POOL_SIZE) {
      pool.retobj(r)
    }
  }
  def returnRunnable(r: RunnableWorker) {
    if (runnerPool.size() < DDCConfig.WORKER_OBJECT_POOL_SIZE) {
      runnerPool.retobj(r)
    }
  }

}