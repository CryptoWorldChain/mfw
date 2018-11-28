package org.fc.zippo.scheduler

import onight.tfw.outils.conf.PropHelper

object DDCConfig {
  val prop = new PropHelper(null);
  val PREFIX = "org.zippo.ddc."

  val DAEMON_WORKER_THREAD_COUNT = prop.get(PREFIX + "daemon.actor.thread.count", Runtime.getRuntime().availableProcessors());
  val DEFAULT_WORKER_QUEUE_MAXSIZE = prop.get(PREFIX + "default.actor.queue.maxsize", 10);
  val DEFAULT_WORKER_THREAD_COUNT = prop.get(PREFIX + "default.actor.thread.count", Runtime.getRuntime().availableProcessors() * 4);
  val DEFAULT_DISPATCHER_QUEUE_WAIT_MS = prop.get(PREFIX + "default.dispatcher.queue.wait.ms", 60000);
  val DEFAULT_DISPATCHER_COUNT = prop.get(PREFIX + "default.dispatcher.count", 1 + Runtime.getRuntime().availableProcessors() / 2);

  val WORKER_OBJECT_POOL_SIZE = prop.get(PREFIX + "runner.object.pool.size", 1024);

  val SPEC_ACTOR_DEFAULT_DISPATCHER = prop.get(PREFIX + "spec.actor.default.dispatcher", 1);
  val SPEC_ACTOR_DEFAULT_WORKER = prop.get(PREFIX + "spec.actor.default.worker", Runtime.getRuntime().availableProcessors());

  val SPEC_ACTOR_PREDEF = prop.get(PREFIX + "spec.actor.predef", "JINDOB[1->],LOCAL[2->100]");

  def parseInt(str: String, default: Int = 0): Int = {
    try {
      str.toInt
    } catch {
      case t: Throwable =>
        default
    }
  }
  def spectActors(): Iterable[(String, Int, Int)] = {
    SPEC_ACTOR_PREDEF.split(",").map { da =>
      val d = da.split("\\[|->|\\]")
      if (d.length == 3) {
        (d(0), parseInt(d(1), SPEC_ACTOR_DEFAULT_DISPATCHER), parseInt(d(2), SPEC_ACTOR_DEFAULT_WORKER))
      } else {
        (d(0), SPEC_ACTOR_DEFAULT_DISPATCHER, SPEC_ACTOR_DEFAULT_WORKER)
      }
    }
  }
  def main(args: Array[String]): Unit = {
    println(spectActors)
    spectActors.map { x => 
      println("x==>"+x._1);  
    }
  }
}