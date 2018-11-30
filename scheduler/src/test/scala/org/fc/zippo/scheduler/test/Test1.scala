package org.fc.zippo.scheduler.test

import org.fc.zippo.scheduler.ZippoDDC
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.otransio.api.PacketHelper
import org.fc.zippo.scheduler.pbgens.Schedule.PSThreadsInfo
import onight.oapi.scala.commons.SessionModules
import com.google.protobuf.Message
import onight.tfw.async.CompleteHandler
import java.util.concurrent.CountDownLatch
import com.googlecode.protobuf.format.util.HexUtils
import org.apache.commons.codec.binary.Hex

object Test1 {

  def main(args: Array[String]): Unit = {
    val ddc = new ZippoDDC()
    ddc.init()
    val tx = new String(Hex.decodeHex("47555344".toCharArray()))
    println("tx="+tx)
    val pack = PacketHelper.genASyncPBPack("JIN", "DOB", "hello");
    val pack2 = PacketHelper.genASyncPBPack("TRAN", "SIO", "hello");
    val pbo = PSThreadsInfo.newBuilder().addGcmds("JINDOB").build()
    val n = 10000;
    val cdl = new CountDownLatch(n * 2);
    val sm = new SessionModules[Message] {
      override def onPBPacket(pack: FramePacket, pbo: Message, handler: CompleteHandler) = {
        //        println("onpacket:")
        Thread.sleep(100)
        cdl.countDown()
      }
    }
    val start = System.currentTimeMillis();
    for (i <- 1 to n) {
      ddc.post(pack,  null, sm)
      ddc.post(pack2,  null, sm)
    }
    println("cost.1=" + (System.currentTimeMillis() - start));
    cdl.await()
    println("cost.2=" + (System.currentTimeMillis() - start));

    ddc.destroy();
    println("END!")

  }

}