package onight.osgi.otransio.nio;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class PacketWriteWorker implements Runnable {

	PacketQueue queue;
	int max_packet_buffer = 10;

	public void run() {
		ArrayList<PacketWriteTask> arrays = new ArrayList<>(10);
		log.debug("PacketWriteWorker    .... [Start]");
		Thread.currentThread().setName("PacketWriteWorker.");
		while (!queue.isStop) {
			PacketWriteTask fp = null;
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
					try {
						queue.getCkpool().sendMessage(arrays);
						
					} catch (Exception e) {
						log.debug("getSend Message Error:",e);
						for(PacketWriteTask pw:arrays){
							if(!pw.isWrited())
							{
								if(pw.getFuture()!=null){
									pw.getFuture().failure(e);
								}else{
									pw.handler.onFailed(e);
								}
							}
						}
					}finally{
						arrays.clear();
					}
				}
			} catch (Throwable t) {

			} finally {

			}

		}

		log.debug("PacketWriteWorker   ......  [STOP]");

	}
}
