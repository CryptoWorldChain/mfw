package onight.osgi.otransio.util;

import java.util.ArrayList;

import org.glassfish.grizzly.Connection;

import lombok.AllArgsConstructor;
import onight.osgi.otransio.ck.CKConnPool;
import onight.osgi.otransio.nio.PacketQueue;
import onight.osgi.otransio.nio.PacketTuple;
import onight.osgi.otransio.nio.PacketWriter;
import onight.tfw.outils.pool.ReusefulLoopPool;

@AllArgsConstructor
public class PacketWriterPool extends ReusefulLoopPool<PacketWriter> {

	int maxObjectSize = 1000;

	public PacketWriter borrowWriter(String name, Connection<?> conn, ReusefulLoopPool ckpool, PacketQueue queue) {
		PacketWriter writer = super.borrow();
		if (writer != null) {
			writer.setName(name);
			writer.setConn(conn);
			writer.setCkpool(ckpool);
			writer.setQueue(queue);
		} else {
			writer = new PacketWriter(name, conn, new ArrayList<PacketTuple>(10), this, ckpool, queue);
		}
		return writer;
	}

	@Override
	public void retobj(PacketWriter t) {
		if (t != null) {
			t.release();
			if (super.size() < maxObjectSize) {
				super.retobj(t);
			}
		}
	}

}
