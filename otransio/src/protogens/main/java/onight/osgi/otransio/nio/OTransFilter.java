package onight.osgi.otransio.nio;


import onight.tfw.otransio.api.beans.FramePacket;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

public class OTransFilter extends AbstractCodecFilter<Buffer, FramePacket> {
	
	public OTransFilter() {
		super(new Decoder(), new Encoder());
	}

}
