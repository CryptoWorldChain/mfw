package onight.osgi.otransio.nio;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PacketWriteWorker implements Runnable {

	String name = "PacketWriteWorker";
	PacketQueue queue;
	int max_packet_buffer = 10;

	public void run() {
		ArrayList<PacketWriteTask> arrays = new ArrayList<>(10);
		log.debug("PacketWriteWorker " + name + "  .... [Start]");
		Thread.currentThread().setName(name);
		while (!queue.isStop) {
			PacketWriteTask fp = null;
			try {
				do {
					try {
						if (arrays.size() == 0) {
							fp = queue.poll(10000);
						} else {
							fp = queue.poll(1);
						}
					} catch (Exception e) {
					}
					if (fp != null) {
						arrays.add(fp);
					}
				} while (fp != null && arrays.size() < max_packet_buffer);

				if (fp == null && arrays.size() <= 0) {
					try {
						synchronized (PacketWriteWorker.this) {
							PacketWriteWorker.this.wait(10);
						}
					} catch (InterruptedException e) {
						log.debug("wait up.");
					}
				} else {
					try {
						queue.getCkpool().sendMessage(arrays);
					} catch (Exception e) {
						log.debug("getSend Message Error:" + e.getMessage()+",arrays.size="+arrays.size(), e);
						for (PacketWriteTask pw : arrays) {
							if (!pw.isWrited()) {
								pw.handler.onFailed(e);
							}
						}
					} finally {
						arrays.clear();
					}
				}
			} catch (Throwable t) {

			} finally {

			}

		}

		log.debug("PacketWriteWorker " + name + "  ......  [STOP]");

	}
}
