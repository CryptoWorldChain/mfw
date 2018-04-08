package onight.osgi.otransio.nio;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.beans.FramePacket;

@AllArgsConstructor
@Slf4j
public class PacketWriteWorker implements Runnable {

	PacketQueue queue;
	int max_packet_buffer = 10;

	public void run() {
		ArrayList<FramePacket> arrays = new ArrayList<>(10);
		log.debug("PacketWriteWorker         [Start]");
		Thread.currentThread().setName("PacketWriteWorker.");
		while (!queue.isStop) {
			FramePacket fp = null;
			try {
				do {
					fp = queue.poll();
					if (fp != null) {
						arrays.add(fp);
					}
				} while (fp != null && arrays.size() < max_packet_buffer);

				if (fp == null && arrays.size() <= 0) {
					try {
						synchronized (this) {
							this.wait();
						}
					} catch (InterruptedException e) {
						log.debug("wait up.");
					}
				} else {
					queue.getCkpool().sendMessage(arrays);
					arrays.clear();
				}
			} catch (Throwable t) {

			} finally {

			}

		}
		
		
		log.debug("PacketWriteWorker   ......  [STOP]");

	}
}
