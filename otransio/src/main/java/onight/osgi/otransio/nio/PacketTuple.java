package onight.osgi.otransio.nio;

import lombok.AllArgsConstructor;
import lombok.Data;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

@AllArgsConstructor
@Data
public class PacketTuple {
	FramePacket pack;
	CompleteHandler handler;
	boolean writed = false;
	int rewriteTimes = 0;

	public void reset(FramePacket pack, CompleteHandler handler) {
		this.pack = pack;
		this.handler = handler;
		writed = false;
		rewriteTimes = 0;
	}
	
	public void reset() {
		this.pack = null;
		this.handler = null;
	}
	// FutureImpl<FramePacket> future;
}