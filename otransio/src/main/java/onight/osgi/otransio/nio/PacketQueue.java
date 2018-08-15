package onight.osgi.otransio.nio;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.grizzly.Connection;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.util.PacketTuplePool;
import onight.osgi.otransio.util.PacketWriterPool;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

@Slf4j
@Data
public class PacketQueue implements Runnable {

	LinkedBlockingQueue<PacketTuple> queue = new LinkedBlockingQueue<>();
	LinkedBlockingQueue<PacketTuple> green_queue = new LinkedBlockingQueue<>();
	long lastUpdatedMS = System.currentTimeMillis();

	boolean isStop = false;

	CKConnPool ckpool;

	PacketTuplePool packPool;
	PacketWriterPool writerPool;
	int max_packet_buffer = 10;
	Executor exec;
	Executor subexec;
	AtomicBoolean running = new AtomicBoolean(false);

	public PacketQueue(CKConnPool ckpool, int max_packet_buffer, int writer_thread_count, Executor exec,
			Executor subexec, PacketTuplePool packPool, PacketWriterPool writerPool) {
		this.ckpool = ckpool;
		this.max_packet_buffer = max_packet_buffer;
		this.packPool = packPool;
		this.writerPool = writerPool;
		this.exec = exec;
		this.subexec = subexec;
		this.name = ckpool.getNameid() + "/" + ckpool.getIp() + ":" + ckpool.getPort();
	}

	public LinkedBlockingQueue<PacketTuple> getQueue(FramePacket fp) {
		if (fp.getFixHead().getPrio() == '9') {
			return green_queue;
		} else {
			return queue;
		}
	}

	public void offer(FramePacket fp, final CompleteHandler handler) {
		LinkedBlockingQueue<PacketTuple> queuetooffer = getQueue(fp);

		while (!queuetooffer.offer(packPool.borrowTuple(fp, handler)))
			;
		if (running.compareAndSet(false, true)) {
			exec.execute(this);
		}
	}

	public PacketTuple poll(long waitms) throws InterruptedException {
		PacketTuple task = green_queue.poll();
		if (task != null) {
			return task;
		}
		return queue.poll(waitms, TimeUnit.MILLISECONDS);
	}

	String name;

	@Override
	public void run() {
		log.debug("PacketQueue {}  .... running,", name);
		Thread.currentThread().setName(name);

		PacketTuple fp = null;
		Connection<?> conn = null;
		PacketWriter writer = null;
		int failedGetConnection = 0;
		if(isStop){
			return;
		}
		try {
			do {
				try {
					conn = ckpool.ensureConnection();
					CKConnPool retPut_ckpool = ckpool;
					if (conn == null && failedGetConnection >= 5) {
						conn = ckpool.iterator().next();
						retPut_ckpool = null;
					}
					if (conn != null) {
						writer = writerPool.borrowWriter(name, conn, retPut_ckpool,this);
						do {
							fp = poll(1);
							if (fp != null) {
								writer.arrays.add(fp);
							}
						} while (fp != null && writer.arrays.size() < max_packet_buffer);
						if (writer.arrays.size() > 0) {
							subexec.execute(writer);
							writer = null;
						}
					} else {
						failedGetConnection++;
						log.warn("no more connection for " + name + ",failedcc=" + failedGetConnection);
					}

				} catch (Throwable t) {
					log.debug("get error:in running Queue:" + name + ":" + t.getMessage(), t);
				} finally {
					if (writer != null) {
						writerPool.retobj(writer);
					}
				}
			} while (!isStop && failedGetConnection < 10 && (queue.size() > 0 || green_queue.size() > 0));

		} finally {
			running.set(false);
		}
	}
}
