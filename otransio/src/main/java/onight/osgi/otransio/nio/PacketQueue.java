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
import onight.tfw.outils.pool.ReusefulLoopPool;

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
	Executor subexec;
	AtomicBoolean polling = new AtomicBoolean(false);
	AtomicLong packCounter = new AtomicLong(0);
	int maxResendBufferSize = 100000;

	public static String PACK_RESEND_ID = "_PRID";

	ReusefulLoopPool<Connection> greenPool = new ReusefulLoopPool<>();
	ReusefulLoopPool<Connection> pioPool = new ReusefulLoopPool<>();

	public PacketQueue(CKConnPool ckpool, int max_packet_buffer, Executor subexec, PacketTuplePool packPool,
			PacketWriterPool writerPool, ConcurrentHashMap<String, PacketTuple> check_Map, int maxResendBufferSize) {
		this.ckpool = ckpool;
		this.max_packet_buffer = max_packet_buffer;
		this.packPool = packPool;
		this.writerPool = writerPool;
		this.subexec = subexec;
		this.name = ckpool.getNameid() + "/" + ckpool.getIp() + ":" + ckpool.getPort();
		this.check_Map = check_Map;
		new Thread(this).start();
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
		if (polling.get()) {
			if (queuetooffer == green_queue) {
				tryDirectSendPacket(green_queue, greenPool, "green");
			} else if (queuetooffer == pio_queue) {
				tryDirectSendPacket(pio_queue, pioPool, "pio");
			}
		}
	}

	public void offer(PacketTuple pt) {
		LinkedBlockingQueue<PacketTuple> queuetooffer = getQueue(pt.pack);

		while (!queuetooffer.offer(pt))
			;
		if (polling.get()) {
			if (queuetooffer == green_queue) {
				tryDirectSendPacket(green_queue, greenPool, "green");
			} else if (queuetooffer == pio_queue) {
				tryDirectSendPacket(pio_queue, pioPool, "pio");
			}
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
		int cc = 0;
		while (greenPool.size() < ckpool.getCore() / 4 && !isStop && cc < 100) {
			conn = ckpool.ensureConnection();
			if (conn != null && conn.isOpen()) {
				greenPool.addObject(conn);
			} else {
				cc++;
			}
		}
		conn = null;
		cc = 0;
		while (pioPool.size() < ckpool.getCore() / 4 && !isStop && cc < 100) {
			conn = ckpool.ensureConnection();
			if (conn != null&&conn.isOpen()) {
				pioPool.addObject(conn);
			} else {
				cc++;
			}
		}
		conn = null;
		int failedwait = 0;
		while (!isStop) {
			// do {
			try {
				conn = ckpool.ensureConnection();
				writer = null;
				CKConnPool retPut_ckpool = ckpool;
				if (conn != null) {
					writer = writerPool.borrowWriter(name, conn, retPut_ckpool, this);
					boolean hasGreenpack = false;
					boolean firstpoll = true;
					do {
						if (firstpoll) {
							polling.set(true);
							fp = poll(1000);
							polling.set(false);
							firstpoll = false;
						} else {
							fp = poll(1);
						}
						if (fp != null) {
							FramePacket packet = fp.getPack();
							if (packet.getFixHead().getPrio() == '9' || packet.getFixHead().getPrio() == '8') {
								ensurePacketID(packet, fp);
								hasGreenpack = true;
							}
							writer.arrays.add(fp);
						}
					} while (fp != null && writer.arrays.size() < max_packet_buffer && !hasGreenpack);
					if (writer.arrays.size() > 0) {
						subexec.execute(writer);
						writer = null;
					}
					failedwait = 0;
				} else {
					tryDirectSendPacket(green_queue, greenPool, "green");
					tryDirectSendPacket(pio_queue, pioPool, "pio");
					failedwait++;
					if (failedwait > 5) {
						Thread.sleep(2000);// wait for connection
						failedwait = 0;
					}
					failedGetConnection++;
					log.warn("TT1-no more connection for " + name + ",failedcc=" + failedGetConnection + ",failedwait="
							+ failedwait + ",pool=" + ckpool.getActiveObjs().size() + "/" + ckpool.size() + ",conn="
							+ conn + ",queuesize=[" + green_queue.size() + "," + pio_queue.size() + "," + queue.size()
							+ "]");
				}

			} catch (Throwable t) {
				failedGetConnection++;
				log.warn("error in get connection for " + name + ",failedcc=" + failedGetConnection, t);
			} finally {
				if (writer != null) {
					writerPool.retobj(writer);
				}
			}
		}
	}

	public void tryDirectSendPacket(LinkedBlockingQueue<PacketTuple> queue, ReusefulLoopPool<Connection> pool,
			String queuename) {
		PacketWriter writer = null;
		try {
			Thread.currentThread().setName("tryDirectSendPacket--" + queuename);
			// log.error("TTT-tryDirectSendPacket:pool=" +
			// pool.getActiveObjs().size() + "/" + pool.size() + ",queuesize="
			// + queue.size() + ",queuename=" + queuename + ",@" + name);
			long start = System.currentTimeMillis();
			PacketTuple fp = queue.poll();
			if (fp != null) {
				Connection conn = pool.borrow();
				if (conn == null || !conn.isOpen()) {
					if (conn != null) {
						// log.error("TTT-remove not open connection:pool=" +
						// pool.getActiveObjs().size() + "/"
						// + pool.size() + ",queuesize=" + queue.size() +
						// ",queuename=" + queuename + ",@" + name
						// + ",conn=" + conn);
						ckpool.removeObject(conn);
					}
					log.error("TTT-Create one more connection:pool=" + pool.getActiveObjs().size() + "/" + pool.size()
							+ ",queuesize=" + queue.size() + ",queuename=" + queuename + ",@" + name + ",conn=" + conn);
					conn = ckpool.ensureConnection();// try to create new one
				}
				if (conn != null && conn.isOpen()) {
					writer = writerPool.borrowWriter(queuename, conn, pool, this);
					FramePacket packet = fp.getPack();
					ensurePacketID(packet, fp);
					writer.arrays.add(fp);
					writer.run();
					// log.error("TTT-DirectSendPacket " + queuename + "
					// packet:queuesize=" + queue.size() + ",connsize="
					// + pool.getActiveObjs().size() + "/" + pool.size() +
					// ",cost="
					// + (System.currentTimeMillis() - start) + ",@" + name);
				} else {
					if (conn != null) {
						// log.error("TTT-remove not open connection:pool=" +
						// pool.getActiveObjs().size() + "/"
						// + pool.size() + ",queuesize=" + queue.size() +
						// ",queuename=" + queuename + ",@" + name
						// + ",conn=" + conn);

						ckpool.removeObject(conn);
					}
					queue.offer(fp);
					// log.error("TTT-no more connection for " + queuename + "
					// packet:queuesize=" + queue.size()
					// + ",connsize=" + pool.getActiveObjs().size() + "/" +
					// pool.size() + ",@" + name);
				}
			}
		} catch (Throwable e) {
			log.error("TTT-err in send Packet for queue=" + queuename, e);
		} finally {
			Thread.currentThread().setName("transioworker");
		}
	}

}
