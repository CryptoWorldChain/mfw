package org.fc.zippo.scheduler

import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.beans.FramePacket
import com.google.protobuf.Message
import onight.oapi.scala.commons.SessionModules
import onight.tfw.outils.pool.ReusefulLoopPool
import lombok.AllArgsConstructor

@AllArgsConstructor
class Worker(
    var gcmd: String = null,
    var pack: FramePacket = null,
    var pbo: Message = null,
    var handler: CompleteHandler = null,
    var sm: SessionModules[Message] = null, var dcname: String = "default") extends Runnable {
  def run() {
    Thread.currentThread().setName("aworker-" + dcname + "-" + gcmd);
    try {
      sm.onPBPacket(pack, pbo, handler)
    } catch {
      case t: RuntimeException =>
        handler.onFailed(t)
    } finally {
      WorkerObjectPool.returnObj(this)
      Thread.currentThread().setName("aworker-wait-" + dcname);
    }
  }
}

object WorkerObjectPool {
  val pool = new ReusefulLoopPool[Worker]();

  def borrow(gcmd: String, pack: FramePacket, pbo: Message, handler: CompleteHandler, sm: SessionModules[Message], dcname: String): Worker = {
    val r = pool.borrow();
    if (r != null) {
      r.gcmd = gcmd;
      r.handler = handler
      r.pack = pack
      r.pbo = pbo
      r.dcname = dcname
      r.sm = sm;
      r
    } else {
      new Worker(gcmd, pack, pbo, handler, sm, dcname);
    }
  }
  def returnObj(r: Worker) {
    if (pool.size() < DDCConfig.WORKER_OBJECT_POOL_SIZE) {
      pool.retobj(r)
    }
  }

}