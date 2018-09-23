package onight.osgi.otransio.nio;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.glassfish.grizzly.Connection;
import org.slf4j.MDC;

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
	LinkedBlockingQueue<PacketTuple> pio_queue = new LinkedBlockingQueue<>();
	ConcurrentHashMap<String, PacketTuple> check_Map;

	long lastUpdatedMS = System.currentTimeMillis();

	boolean isStop = false;

	CKConnPool ckpool;

	PacketTuplePool packPool;
	PacketWriterPool writerPool;
	int max_packet_buffer = 10;
	Executor exec;
	Executor subexec;
	AtomicBoolean running = new AtomicBoolean(false);
	AtomicLong packCounter = new AtomicLong(0);
	int maxResendBufferSize = 100000;

	public static String PACK_RESEND_ID = "_PRID";

	public PacketQueue(CKConnPool ckpool, int max_packet_buffer, int writer_thread_count, Executor exec,
			Executor subexec, PacketTuplePool packPool, PacketWriterPool writerPool,
			ConcurrentHashMap<String, PacketTuple> check_Map, int maxResendBufferSize) {
		this.ckpool = ckpool;
		this.max_packet_buffer = max_packet_buffer;
		this.packPool = packPool;
		this.writerPool = writerPool;
		this.exec = exec;
		this.subexec = subexec;
		this.name = ckpool.getNameid() + "/" + ckpool.getIp() + ":" + ckpool.getPort();
		this.check_Map = check_Map;
		this.maxResendBufferSize = maxResendBufferSize;
	}

	public void ensurePacketID(FramePacket fp, PacketTuple pt) {
		if (fp.getExtProp(PACK_RESEND_ID) == null) {
			String packid = System.currentTimeMillis() % 100000000 + "" + packCounter.incrementAndGet();
			fp.putHeader(PACK_RESEND_ID, packid);
			if (check_Map.size() < maxResendBufferSize) {
				check_Map.put(packid, pt);
			}
		}
	}

	public LinkedBlockingQueue<PacketTuple> getQueue(FramePacket fp) {
		if (fp.getFixHead().getPrio() == '9') {
			return green_queue;
		} else if (fp.getFixHead().getPrio() == '8') {
			return pio_queue;
		} else {
			return queue;
		}
	}

	public void offer(FramePacket fp, final CompleteHandler handler) {
		LinkedBlockingQueue<PacketTuple> queuetooffer = getQueue(fp);

		while (!queuetooffer.offer(packPool.borrowTuple(fp, handler, this)))
			;
		if (running.compareAndSet(false, true)) {
			exec.execute(this);
		}
	}

	public void offer(PacketTuple pt) {
		LinkedBlockingQueue<PacketTuple> queuetooffer = getQueue(pt.pack);

		while (!queuetooffer.offer(pt))
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
		task = pio_queue.poll();
		if (task != null) {
			return task;
		}
		return queue.poll(waitms, TimeUnit.MILLISECONDS);
	}

	String name;

	@Override
	public void run() {
		// log.debug("PacketQueue {} .... running,", name);
		Thread.currentThread().setName(name);

		PacketTuple fp = null;
		Connection<?> conn = null;
		PacketWriter writer = null;
		int failedGetConnection = 0;
		if (isStop) {
			return;
		}
		try {
			do {
				try {
					conn = ckpool.ensureConnection();
					CKConnPool retPut_ckpool = ckpool;
					if (conn == null && failedGetConnection >= 5) {
						Iterator<Connection> it = ckpool.iterator();
						if (it != null && it.hasNext()) {
							conn = it.next();
							retPut_ckpool = null;
						}
					}
					if (conn != null) {
						writer = writerPool.borrowWriter(name, conn, retPut_ckpool, this);
						do {
							fp = poll(1);
							if (fp != null) {
								FramePacket packet = fp.getPack();
								if (packet.getFixHead().getPrio() == '9' || packet.getFixHead().getPrio() == '8') {
									ensurePacketID(packet, fp);
								}
								writer.arrays.add(fp);
							}
						} while (fp != null && writer.arrays.size() < max_packet_buffer);
						if (writer.arrays.size() > 0) {
							subexec.execute(writer);
							writer = null;
						}
					} else {
						failedGetConnection++;
						// log.warn("no more connection for " + name +
						// ",failedcc=" + failedGetConnection);
					}

				} catch (Throwable t) {
					failedGetConnection++;
				} finally {
					if (writer != null) {
						writerPool.retobj(writer);
					}
				}
			} while (!isStop && failedGetConnection < ckpool.getCore()
					&& (queue.size() > 0 || green_queue.size() > 0 || pio_queue.size() > 0));

			if (!isStop && failedGetConnection >= ckpool.getCore()
					&& (queue.size() > 0 || green_queue.size() > 0 || pio_queue.size() > 0)) {
				MDC.put("BCUID", name);
				log.warn("no more connection for " + name + ",failedcc=" + failedGetConnection + ",qsize="
						+ queue.size() + ",green_qsize=" + green_queue.size() + ",pio_qsize=" + pio_queue.size());
			}
		} finally {
			running.set(false);
		}
	}

	public void resendBacklogs() {
		if ((queue.size() > 0 || green_queue.size() > 0 || pio_queue.size() > 0)
				&& running.compareAndSet(false, true)) {
			exec.execute(this);
		}
	}
}
