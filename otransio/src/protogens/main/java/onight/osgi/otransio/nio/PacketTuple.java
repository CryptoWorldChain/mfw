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

	long writeTime = -1;
	boolean responsed = false;
	PacketQueue packQ;
	public void reset(FramePacket pack, CompleteHandler handler,PacketQueue packQ) {
		this.pack = pack;
		this.handler = handler;
		writed = false;
		responsed = false;
		writeTime = -1;
		rewriteTimes = 0;
		this.packQ = packQ;
	}
	
	public void reset() {
		reset(null,null,null);

	}
	// FutureImpl<FramePacket> future;
}