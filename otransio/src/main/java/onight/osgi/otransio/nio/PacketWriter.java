package onight.osgi.otransio.nio;

import java.util.ArrayList;

import org.glassfish.grizzly.Connection;
import org.slf4j.MDC;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.util.PacketWriterPool;
import onight.tfw.outils.pool.ReusefulLoopPool;

@AllArgsConstructor
@Slf4j
@Data
public class PacketWriter implements Runnable {

	String name;
	Connection<?> conn;
	ArrayList<PacketTuple> arrays;
	PacketWriterPool writerPool;
	ReusefulLoopPool ckpool;
	PacketQueue queue;

	public void run() {
		try {
			MDC.put("BCUID", name);
			long writeTime = System.currentTimeMillis();
			if (!conn.isOpen()) {
				for (PacketTuple pw : arrays) {
					queue.offer(pw.pack, pw.handler);
				}
			}
			for (PacketTuple pt : arrays) {
				if (!pt.isResponsed() || !pt.isWrited()) {
					pt.setWriteTime(writeTime);
					conn.write(pt.pack);
					pt.setWrited(true);
				}
			}
		} catch (Exception e) {
			log.error("error in writing packet:", e);
			if (!conn.isOpen()) {
				for (PacketTuple pw : arrays) {
					if (!pw.isWrited()) {
						pw.rewriteTimes++;
						if (pw.rewriteTimes < 5) {
							queue.offer(pw.pack, pw.handler);
						} else {
							pw.handler.onFailed(e);
						}
					}
				}
			} else {
				log.error("getSend Actor Error:" + e.getMessage() + ",arrays.size=" + arrays.size(), e);
				for (PacketTuple pw : arrays) {
					if (!pw.isWrited()) {
						pw.handler.onFailed(e);
					}
				}
			}
		} finally {
			writerPool.retobj(this);
		}
	}

	public void release() {
		arrays.clear();
		if (ckpool != null && conn != null) {
			if (conn.isOpen()) {
				ckpool.retobj(conn);
			} else {
				ckpool.removeObject(conn);
			}
		}
	}
}
