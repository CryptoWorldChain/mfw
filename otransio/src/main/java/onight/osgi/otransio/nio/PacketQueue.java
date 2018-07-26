package onight.osgi.otransio.nio;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

@Slf4j
@Data
public class PacketQueue {

	LinkedBlockingQueue<PacketWriteTask> queue = new LinkedBlockingQueue<>();
	LinkedBlockingQueue<PacketWriteTask> green_queue = new LinkedBlockingQueue<>();
	long lastUpdatedMS = System.currentTimeMillis();

	PacketWriteWorker writer;

	boolean isStop = false;

	CKConnPool ckpool;

	public PacketQueue(CKConnPool ckpool, int max_packet_buffer, int writer_thread_count) {
		this.ckpool = ckpool;
		writer = new PacketWriteWorker(ckpool.getNameid() + "/" + ckpool.getIp() + ":" + ckpool.getPort(), this,
				max_packet_buffer);
		for (int i = 0; i < writer_thread_count; i++) {
			new Thread(writer).start();
		}
	}

	public LinkedBlockingQueue<PacketWriteTask> getQueue(FramePacket fp) {
		if (fp.getFixHead().getPrio() == '9') {
			return green_queue;
		} else {
			return queue;
		}
	}

	public void offer(FramePacket fp, final CompleteHandler handler) {
		LinkedBlockingQueue<PacketWriteTask> queuetooffer = getQueue(fp);

		while (!queuetooffer.offer(new PacketWriteTask(fp, handler, false)))
			;
		try {
			synchronized (writer) {
				writer.notifyAll();
			}
		} catch (Exception e) {
			log.warn("error:" + e.getMessage(), e);
		}
	}

	public PacketWriteTask poll(long waitms) throws InterruptedException {
		PacketWriteTask task = green_queue.poll();
		if (task != null ) {
			return task;
		}
		return queue.poll(waitms, TimeUnit.MILLISECONDS);
	}
}
