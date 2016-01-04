package onight.osgi.otransio.nio;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.beans.ExtHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;

@Slf4j
public class Decoder extends AbstractTransformer<Buffer, FramePacket> {

	protected final Attribute<FixHeader> headerStore;
	protected final Attribute<Long> lastCheckHealthMS;

	public Decoder() {
		headerStore = attributeBuilder.createAttribute("Decoder.FixHeader");
		lastCheckHealthMS = attributeBuilder.createAttribute("Decoder.CheckHealth");
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	protected TransformationResult<Buffer, FramePacket> transformImpl(AttributeStorage storage, Buffer input) throws TransformationException {
		FixHeader header = headerStore.get(storage);
		log.trace("Decoder.getHeader:" + header);
		if (header == null) {
			if (input.remaining() < FixHeader.LENGTH) {
				return TransformationResult.createIncompletedResult(input);
			}
			byte headerbytes[] = new byte[FixHeader.LENGTH];
			input.get(headerbytes, 0, FixHeader.LENGTH);
			try {
				header = FixHeader.parseFrom(headerbytes);
			} catch (Exception e) {
				log.error("parse Head Error:header="+new String(headerbytes));
				throw e;
			}
			headerStore.set(storage, header);
		}

		log.trace("Decoder.getHeader.step2::remain=" + input.remaining() + ",head=" + header);
		// readsize
		if (input.remaining() < header.getTotalSize()) {
			return TransformationResult.createIncompletedResult(input);
		}
		headerStore.remove(storage);
		ExtHeader ext = null;
		if (header.getExtsize() > 0) {
			byte extbytes[] = new byte[header.getExtsize()];
			input.get(extbytes);
			ext = ExtHeader.buildFrom(extbytes);
		}
		byte body[] = new byte[header.getBodysize()];
		input.get(body);
		FramePacket pack = new FramePacket(header, ext, body, header.getCmd() + header.getModule());
		log.trace("Decoder.OKOK::remain=" + input.remaining() + ":totalsize=" + header.getTotalSize() + ".");
		return TransformationResult.createCompletedResult(pack, input);
	}

	@Override
	public void release(AttributeStorage storage) {
		headerStore.remove(storage);
		super.release(storage);
	}

	@Override
	public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
		return input != null && input.hasRemaining();
	}

}
