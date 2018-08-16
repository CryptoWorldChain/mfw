package onight.osgi.otransio.nio;

import java.util.ArrayList;

import org.glassfish.grizzly.Connection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.util.PacketWriterPool;

@AllArgsConstructor
@Slf4j
@Data
public class PacketWriter implements Runnable {

	String name;
	Connection<?> conn;
	ArrayList<PacketTuple> arrays;
	PacketWriterPool writerPool;
	CKConnPool ckpool;
	PacketQueue queue;

	public void run() {
		try {
			for (PacketTuple pt : arrays) {
				conn.write(pt.pack);
				pt.setWrited(true);
			}
		} catch (Exception e) {
			if (!conn.isOpen()) {
				for (PacketTuple pw : arrays) {
					if (!pw.isWrited()) {
						pw.rewriteTimes++;
						if (pw.rewriteTimes < 3) {
							queue.offer(pw.pack, pw.handler);
						} else {
							pw.handler.onFailed(e);
						}
					}
				}
			} else {
				log.debug("getSend Actor Error:" + e.getMessage() + ",arrays.size=" + arrays.size(), e);
				for (PacketTuple pw : arrays) {
					if (!pw.isWrited()) {
						pw.handler.onFailed(e);
					}
				}
			}
		} finally {
			arrays.clear();
			if (ckpool != null) {
				ckpool.retobj(conn);
			}
			writerPool.retobj(this);
		}
	}
}
