package onight.osgi.otransio.nio;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.beans.ExtHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;

@Slf4j
public class Decoder extends AbstractTransformer<Buffer, FramePacket> {

	protected final Attribute<FixHeader> headerStore;
	protected final Attribute<AtomicLong> blankHeaderCount;
	protected final Attribute<Long> lastCheckHealthMS;
	protected final Attribute<Byte> firstByte;

	public static final int MAX_BLANK_COUNT = 128;

	public Decoder() {
		headerStore = attributeBuilder.createAttribute("Decoder.FixHeader");
		lastCheckHealthMS = attributeBuilder.createAttribute("Decoder.CheckHealth");
		blankHeaderCount = attributeBuilder.createAttribute("Decoder.blankcount");
		firstByte = attributeBuilder.createAttribute("Decoder.firstByte");
	}

	@Override
	public String getName() {
		return "";
	}

	public void clean(AttributeStorage storage) {
//		log.error("clean storage:"+storage);
		headerStore.remove(storage);
//		lastCheckHealthMS.remove(storage);
		blankHeaderCount.remove(storage);
		firstByte.remove(storage);
	}

	@Override
	protected TransformationResult<Buffer, FramePacket> transformImpl(AttributeStorage storage, Buffer input)
			throws TransformationException {
		FixHeader header = headerStore.get(storage);
		log.trace("Decoder.getHeader:{}", header);
		if (header == null) {
			byte first = '\n';
			int blankcount = 0;
			if (firstByte.get(storage) != null) {
				first = firstByte.get(storage);
			}

			while (input.hasRemaining() && (first == '\n' || first == '\r' || first == ' ' || first == '\t')
					&& blankcount < MAX_BLANK_COUNT) {
				first = input.get();
				blankcount++;
			}
			AtomicLong ll = blankHeaderCount.get(storage);
			if (ll == null) {
				ll = new AtomicLong(0);
				blankHeaderCount.set(storage, ll);
			}
			if (blankcount > 0) {
				if (ll.addAndGet(blankcount) >= MAX_BLANK_COUNT) {
					log.error("too many blank bytes {}", ll.get());
					clean(storage);
					
					throw new TransformationException("too many blank bytes");
				}
			}
			if (input.remaining() < FixHeader.LENGTH - 1) {
				// input.position(input.position() - 1);//reput... for bugs
				firstByte.set(storage, first);
				return TransformationResult.createIncompletedResult(input);
			}
			ll.set(0);
			firstByte.remove(storage);
			byte headerbytes[] = new byte[FixHeader.LENGTH];
			headerbytes[0] = first;
			input.get(headerbytes, 1, FixHeader.LENGTH - 1);
			try {
				header = FixHeader.parseFrom(headerbytes);
				if (header.getTotalSize() > 1024 * 1024 * 128 || header.getExtsize() < 0 || header.getBodysize() < 0) {
					log.error("packet Size too large {},storage={}", header.getTotalSize(), storage);
					throw new TransformationException();
				}
			} catch (Exception e) {
				log.error("parse Head Error:header=" + Hex.encodeHexString(headerbytes) + ",@" + storage);
				clean(storage);
				throw e;
			}
			headerStore.set(storage, header);
		}

		log.trace("Decoder.getHeader.step2::remain={},header={}", input.remaining(), header);
		// readsize
		if (input.remaining() < header.getTotalSize()) {
			return TransformationResult.createIncompletedResult(input);
		}
		headerStore.remove(storage);
		ExtHeader ext = new ExtHeader();
		if (header.getExtsize() > 0) {
			byte extbytes[] = new byte[header.getExtsize()];
			input.get(extbytes);
			ext = ExtHeader.buildFrom(extbytes);
			String sendtime = (String) ext.get(Encoder.LOG_TIME_SENT);
			if (StringUtils.isNumeric(sendtime)) {
				log.debug("transio recv {}{},bodysize:{},cost:{} ms,sent={},resp={},sync={},pio={}", header.getCmd(),
						header.getModule(), header.getBodysize(),
						(System.currentTimeMillis() - Long.parseLong(sendtime)), sendtime, header.isResp(),
						header.isSync(), header.getPrio());
			}
		}
		byte body[] = new byte[header.getBodysize()];
		input.get(body);
		FramePacket pack = new FramePacket(header, ext, body, header.getCmd() + header.getModule());
		log.trace("Decoder.OKOK::remain={},totalsize={}", input.remaining(), header.getTotalSize());
		return TransformationResult.createCompletedResult(pack, input);
	}

	@Override
	public void release(AttributeStorage storage) {
		clean(storage);
		super.release(storage);
	}

	@Override
	public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
		return input != null && input.hasRemaining();
	}

}
