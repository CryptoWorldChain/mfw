package onight.osgi.otransio.nio;

import org.glassfish.grizzly.impl.FutureImpl;

import lombok.AllArgsConstructor;
import lombok.Data;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

@AllArgsConstructor
@Data
public class PacketWriteTask{
	FramePacket pack;
	CompleteHandler handler;
	boolean writed = false;
//	FutureImpl<FramePacket> future;
}