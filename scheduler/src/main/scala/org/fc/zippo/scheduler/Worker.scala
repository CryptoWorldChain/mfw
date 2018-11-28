package org.fc.zippo.scheduler

import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.beans.FramePacket
import com.google.protobuf.Message
import onight.oapi.scala.commons.SessionModules
import onight.tfw.outils.pool.ReusefulLoopPool
import lombok.AllArgsConstructor

@AllArgsConstructor
class Worker(
  var pack:    FramePacket             = null,
  var pbo:     Message                 = null,
  var handler: CompleteHandler         = null,
  var sm:      SessionModules[Message] = null) extends Runnable {
  def run() {
    try {
      sm.onPBPacket(pack, pbo, handler)
    } catch {
      case t: RuntimeException =>
        handler.onFailed(t)
    } finally {
      WorkerObjectPool.returnObj(this)
    }
  }
}

object WorkerObjectPool {
  val pool = new ReusefulLoopPool[Worker]();

  def borrow(pack: FramePacket, pbo: Message, handler: CompleteHandler, sm: SessionModules[Message]): Worker = {
    val r = pool.borrow();
    if (r != null) {
      r.handler = handler
      r.pack = pack
      r.pbo = pbo
      r.sm = sm;
      r
    } else {
      new Worker(pack, pbo, handler, sm);
    }
  }
  def returnObj(r: Worker) {
    if (pool.size() < DDCConfig.WORKER_OBJECT_POOL_SIZE) {
      pool.retobj(r)
    }
  }

}