package onight.osgi.otransio.util;

import lombok.AllArgsConstructor;
import onight.osgi.otransio.nio.PacketTuple;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.pool.ReusefulLoopPool;

@AllArgsConstructor
public class PacketTuplePool extends ReusefulLoopPool<PacketTuple> {

	int maxObjectSize = 1000;

	public PacketTuple borrowTuple(FramePacket fp, final CompleteHandler handler) {
		PacketTuple task = super.borrow();
		if (task != null) {
			task.reset(fp, handler);
		} else {
			task = new PacketTuple(fp, handler, false,0);
		}
		return task;
	}

	@Override
	public void retobj(PacketTuple t) {
		if (super.size() < maxObjectSize) {
			t.reset();
			super.retobj(t);
		}
	}

}
