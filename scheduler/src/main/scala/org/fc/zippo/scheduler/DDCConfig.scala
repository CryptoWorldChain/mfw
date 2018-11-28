package org.fc.zippo.scheduler

import onight.tfw.outils.conf.PropHelper

object DDCConfig {
  val prop = new PropHelper(null);
  val PREFIX = "org.zippo.ddc."

  val DAEMON_WORKER_THREAD_COUNT = prop.get(PREFIX + "daemon.actor.thread.count", Runtime.getRuntime().availableProcessors());
  val DEFAULT_WORKER_QUEUE_MAXSIZE = prop.get(PREFIX + "default.actor.queue.maxsize", 102400);
  val DEFAULT_WORKER_THREAD_COUNT = prop.get(PREFIX + "default.actor.thread.count", Runtime.getRuntime().availableProcessors() * 4);
  val DEFAULT_DISPATCHER_QUEUE_WAIT_MS = prop.get(PREFIX + "default.dispatcher.queue.wait.ms", 60000);
  val DEFAULT_DISPATCHER_COUNT = prop.get(PREFIX + "default.dispatcher.count", 1); // + Runtime.getRuntime().availableProcessors() / 2);
  val LOGINFO_DISPATCHER_TIMESEC = prop.get(PREFIX + "loginfo.dispatcher.timesec", 60);

  val WORKER_OBJECT_POOL_SIZE = prop.get(PREFIX + "runner.object.pool.size", 1024);

  val SPEC_DISPATCHER_COUNT = prop.get(PREFIX + "spec.dispatcher.count", 1);
  val SPEC_DISPATCHER_WORKER = prop.get(PREFIX + "spec.dispatcher.worker", Runtime.getRuntime().availableProcessors());
  val SPEC_DISPATCHERS = prop.get(PREFIX + "spec.dispatchers", "transio[1->100],dob[1->100]");
  val SPEC_ACTOR_PREDEF = prop.get(PREFIX + "spec.actor.predef", "TRANSIO->transio,JINDOB->transio");

  def parseInt(str: String, default: Int = 0): Int = {
    try {
      str.toInt
    } catch {
      case t: Throwable =>
        default
    }
  }
  def specDispatchers(): Iterable[(String, Int, Int)] = {
    SPEC_DISPATCHERS.split(",").map { da =>
      val d = da.split("\\[|->|\\]")
      if (d.length == 3) {
        (d(0).trim(), parseInt(d(1), SPEC_DISPATCHER_COUNT), parseInt(d(2), SPEC_DISPATCHER_WORKER))
      } else {
        (d(0).trim(), SPEC_DISPATCHER_COUNT, SPEC_DISPATCHER_WORKER)
      }
    }
  }
  def spectActors(): Iterable[(String, String)] = {
    SPEC_ACTOR_PREDEF.split(",").map { da =>
      val d = da.split("->")
      if (d.length == 2) {
        (d(0).trim(), d(1).trim())
      } else {
        (d(0).trim(), "default")
      }
    }
  }
  def main(args: Array[String]): Unit = {
    println(spectActors)
    spectActors.map { x =>
      println("x==>" + x._1);
    }
  }
}