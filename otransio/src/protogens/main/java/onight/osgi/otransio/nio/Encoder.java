
package onight.osgi.otransio.nio;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import onight.tfw.otransio.api.beans.FramePacket;

public class Encoder extends AbstractTransformer<FramePacket, Buffer> {

	Logger log = LoggerFactory.getLogger(Encoder.class);

	protected final Attribute<Long> lastCheckHealthMS = Grizzly.DEFAULT_ATTRIBUTE_BUILDER
			.createAttribute("Decoder.CheckHealth");

	public static String LOG_TIME_SENT = "T__LOG_SENT";

	@Override
	public String getName() {
		return "";
	}

	@Override
	public boolean hasInputRemaining(AttributeStorage storage, FramePacket input) {
		return input != null;
	}

	// Buffer HB_BUFFER = HeapBuffer.wrap(new byte[] { '0', '0', '0', '2', '\n',
	// '\n' });

	@Override
	public void release(AttributeStorage storage) {
//		lastCheckHealthMS.remove(storage);
		super.release(storage);
	}

	@Override
	protected TransformationResult<FramePacket, Buffer> transformImpl(AttributeStorage storage, FramePacket input)
			throws TransformationException {
		byte[] bodyb = input.genBodyBytes();
		long senttime = System.currentTimeMillis();
		input.putHeader(LOG_TIME_SENT, "" + senttime);
		byte[] extb = input.genExtBytes();
		input.getFixHead().setExtsize(extb.length);
		input.getFixHead().setBodysize(bodyb.length);

		Buffer output = obtainMemoryManager(storage).allocate(16 + input.getFixHead().getTotalSize());
	    output.allowBufferDispose(true);

		output.put(input.getFixHead().genBytes());
		if (extb.length > 0) {
			output.put(extb);
		}
		if (bodyb.length > 0) {
			output.put(bodyb);
		}
		output.flip();
		//output.allowBufferDispose(true);
		// log.trace("encode:" + input.getFixHead().toStrHead() + ",extsize=" +
		// extb.length + ",bodysize=" + bodyb.length);
		log.debug("transio send {}{},bodysize [{}]b,sent@{},resp={},sync={},prio={}", input.getFixHead().getCmd(),
				input.getFixHead().getModule(), input.getFixHead().getBodysize(), senttime, input.getFixHead().isResp(),
				input.getFixHead().isSync(),input.getFixHead().getPrio());

		if (bodyb.length > 0 && input.getFixHead().toStrHead().endsWith("400000T00")) {
			log.error("unknow input::");
			return TransformationResult.createCompletedResult(null, null);
		}
		lastCheckHealthMS.set(storage.getAttributes(), System.currentTimeMillis());
		return TransformationResult.createCompletedResult(output, null);
		
	}
}
