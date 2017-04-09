package onight.tfw.otransio.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.beans.ExtHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FrameBody;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.serialize.HttpHelper;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.type.TypeReference;

@Slf4j
public class PacketHelper {
	public static FramePacket toPBReturn(FramePacket fp, Object body) {
		FramePacket ret = new FramePacket();
		ret.setFbody(body);
		ret.setExtHead(fp.getExtHead());
		ret.setFixHead(fp.getFixHead());
		ret.getFixHead().reset();
		// ret.getFixHead().setEnctype(SerializerFactory.SERIALIZER_PROTOBUF);
		return ret;
	}
	
	public  static FramePacket clonePacket(FramePacket fp) {
		FramePacket ret = new FramePacket();
		ret.setExtHead(fp.getExtHead());
		ret.setFixHead(fp.getFixHead());
		ret.setBody(fp.getBody());
		ret.setFbody(ret.getFbody());
		ret.getFixHead().reset();
		ret.getExtHead().reset();
		// ret.getFixHead().setEnctype(SerializerFactory.SERIALIZER_PROTOBUF);
		return ret;
	}

	public static FramePacket genASyncPack(String cmd, String module, Object body) {
		return genPack(cmd, module, body, false, (byte) 0);
	}

	public static FramePacket genASyncPBPack(String cmd, String module, Object body) {
		FramePacket pack = genPack(cmd, module, body, false, (byte) 0);
		pack.getFixHead().setEnctype(SerializerFactory.SERIALIZER_PROTOBUF);
		return pack;
	}

	public static FramePacket genSyncPack(String cmd, String module, Object body) {
		return genPack(cmd, module, body, true, (byte) 0);
	}

	public static FramePacket genPack(String cmd, String module, Object body, boolean isSync, byte pio) {
		FramePacket ret = new FramePacket();
		ret.setFbody(body);
		FixHeader fh = new FixHeader();
		fh.setCmd(cmd);
		fh.setModule(module);
		fh.setPrio(pio);
		fh.setSync(isSync);
		fh.setEnctype(SerializerFactory.SERIALIZER_TRANSBEAN);
		ret.setFixHead(fh);
		ret.setExtHead(new ExtHeader());
		return ret;
	}

	public static FramePacket buildHeaderFromHttpGet(HttpServletRequest req) {
		if (StringUtils.isBlank(req.getParameter(PackHeader.HTTP_PARAM_FIX_HEAD)))
			return null;
		return buildHeaderFromHttp(req, null);
	}

	public static FramePacket buildHeaderFromHttpPost(HttpServletRequest req) throws IOException {
		if (StringUtils.isBlank(req.getParameter(PackHeader.HTTP_PARAM_FIX_HEAD)))
			return null;
		FramePacket ret = (FramePacket) req.getAttribute("__framepack");
		if (ret != null) {
			return ret;
		}
		return buildHeaderFromHttp(req, HttpHelper.getRequestContentBytes(req));
	}

	static FramePacket buildHeaderFromHttp(HttpServletRequest req, byte[] postData) {
		FramePacket ret = (FramePacket) req.getAttribute("__framepack");
		if (ret != null) {
			return ret;
		}
		ret = new FramePacket();
		req.setAttribute("__framepack", ret);
		ret.setFixHead(FixHeader.buildFrom(req));
		ret.setExtHead(ExtHeader.buildFrom(req));
		if (postData == null) {
			try {
				ret.setBody(req.getParameter(PackHeader.HTTP_PARAM_BODY_DATA).getBytes("UTF-8"));
			} catch (Exception e) {
			}
		} else {
			ret.setBody(postData);
		}

		return ret;
	}

	static ObjectMapper mapper = new ObjectMapper();
	static ISerializer transSIO = SerializerFactory.getSerializer(SerializerFactory.SERIALIZER_TRANSBEAN);
	static {
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);

	}

	@SuppressWarnings("rawtypes")
	public static SimpleFramePack getSimplePack(FramePacket fp) {
		fp.getFixHead().genBytes();
		SimpleFramePack jsonpack = new SimpleFramePack();
		jsonpack.setFh(new String(fp.getFixHead().genBytes()));
		jsonpack.setEh(fp.getExtHead().getVkvs());
		jsonpack.setBody(fp.getFbody());
		return jsonpack;
	}

	public static byte[] toJsonBytes(FramePacket fp) {
		fp.getFixHead().setEnctype(SerializerFactory.SERIALIZER_JSON);
		SimpleFramePack<?> jsonpack = getSimplePack(fp);
		try {
			return (byte[]) mapper.writeValueAsBytes(jsonpack);
		} catch (Exception e) {
			log.debug("toJsonBytesError:" + fp, e);
		}
		return null;
	}

	public static byte[] toTransBytes(FramePacket fp) {
		byte[] bodyb = fp.genBodyBytes();
		byte[] extb = fp.genExtBytes();
		fp.getFixHead().setExtsize(extb.length);
		fp.getFixHead().setBodysize(bodyb.length);
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream(fp.getFixHead().getTotalSize());) {
			bout.write(fp.getExtHead().genBytes());
			if (extb.length > 0) {
				bout.write(extb);
			}
			if (bodyb.length > 0) {
				bout.write(bodyb);
			}
			return bout.toByteArray();
		} catch (Exception e) {
			log.debug("toTransBytesError:" + fp, e);
		}
		return null;
	}

	public static String toJsonString(FramePacket fp) {
		try {
			return new String(toJsonBytes(fp), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.debug("UnsupportedEncodingException:" + fp, e);
		}
		return null;
	}

	public static FramePacket buildPacketFromJson(byte[] jsonbytes, Class<? extends FrameBody> bodyClazz) {
		try {
			JsonNode dataNode = mapper.readTree(jsonbytes);
			JsonNode bodyNode = dataNode.get("body");
			FrameBody fbody = mapper.readValue(bodyNode, bodyClazz);
			FixHeader fh = FixHeader.parseFrom(dataNode.get("fh").asText().getBytes());
			ExtHeader eh = null;
			if (dataNode.get("eh") != null) {
				eh = new ExtHeader();
				Map<String, Object> map = (mapper.<HashMap<String, Object>> readValue(dataNode.get("eh"), new TypeReference<HashMap<String, Object>>() {
				}));
				for (Entry<String, Object> entry : map.entrySet()) {
					eh.append(entry.getKey(), entry.getValue());
				}

			}

			FramePacket ret = new FramePacket();
			ret.setFixHead(fh);
			ret.setExtHead(eh);
			ret.setFbody(fbody);
			return ret;
		} catch (Exception e) {
			log.debug("json read value error:" + new String(jsonbytes), e);
		}

		return null;
	}

	public static FramePacket buildPacketFromTransBytes(byte[] transbytes) {
		try {
			FixHeader header = FixHeader.parseFrom(transbytes);
			ExtHeader ext = null;
			if (header.getExtsize() > 0) {
				ext = ExtHeader.buildFrom(transbytes, FixHeader.LENGTH, header.getExtsize());
			}
			byte bb[] = null;
			if (header.getBodysize() > 0) {
				bb = new byte[header.getBodysize()];
				System.arraycopy(transbytes, FixHeader.LENGTH + header.getExtsize(), bb, 0, header.getBodysize());
			}
			FramePacket pack = new FramePacket(header, ext, bb, header.getCmd() + header.getModule());
			return pack;
		} catch (Exception e) {
			log.debug("transbyes read value error:" + new String(transbytes), e);
		}

		return null;
	}

}
