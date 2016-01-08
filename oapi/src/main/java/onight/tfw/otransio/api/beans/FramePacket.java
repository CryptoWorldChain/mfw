package onight.tfw.otransio.api.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import onight.tfw.async.OFuture;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.outils.bean.JsonPBFormat;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.protobuf.Message;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FramePacket {
	FixHeader fixHead;
	ExtHeader extHead;

	@JsonIgnore
	protected byte[] body;

	@JsonIgnore
	transient ISerializer sio;

	transient Object fbody;

	@JsonIgnore
	transient String globalCMD;

	public Object getExtProp(String key) {
		return extHead.get(key);
	}

	public OFuture<FramePacket> getExtPropFuture(String key) {
		Object ret = getExtProp(key);
		if (ret instanceof OFuture) {
			return (OFuture<FramePacket>) ret;
		}
		return new OFuture(ret);
	}

	public OFuture<FramePacket> getSession() {
		return getExtPropFuture(ExtHeader.PACK_SESSION);
	}

	public void putSession(OFuture<FramePacket> sessionfuture) {
		getExtHead().append(ExtHeader.PACK_SESSION, sessionfuture);
	}

	public String getExtStrProp(String key) {
		Object obj = extHead.get(key);
		if (obj != null) {
			return String.valueOf(obj);
		}
		return null;
	}

	public Object putHeader(String key, String value) {
		if (extHead == null) {
			extHead = new ExtHeader();
		}
		return extHead.append(key, value);
	}

	// public Map<String, Object> getExts() {
	// if (extHead != null) {
	// return extHead.kvs;
	// } else {
	// return null;
	// }
	// }

	public boolean isSync() {
		return fixHead.isSync();
	}

	public String getCMD() {
		return fixHead.getCmd();
	}

	public String getModule() {
		return fixHead.getModule();
	}

	// public ActorSession getSession() {
	// return extHead.getSession();
	// }

	public String getModuleAndCMD() {
		if (globalCMD == null) {
			globalCMD = fixHead.getCmd() + fixHead.getModule();
		}
		return globalCMD;
	}

	public byte[] genBodyBytes() {
		if (fbody != null) {
			if (sio == null) {
				sio = SerializerFactory.getSerializer(fixHead.getEnctype());
			}
			if (fbody instanceof Message && fixHead.getEnctype() == SerializerFactory.SERIALIZER_JSON) {//
				// pb 2 json
				body = new JsonPBFormat().printToString((Message) fbody).getBytes(); 
			} else {
				body = SerializerUtil.toBytes(sio.serialize(fbody));
			}
		} else if (body == null) {
			body = PackHeader.EMPTY_BYTES;
		}
		return body;
	}

	public byte[] genExtBytes() {
		if (extHead == null) {
			return PackHeader.EMPTY_BYTES;
		}
		return extHead.genBytes();
	}

	public <T> T parseBO(Class<T> clazz) {
		if (fbody != null) {
			return (T) fbody;
		}
		if (clazz != null) {
			fbody = sio.deserialize(SerializerUtil.fromBytes(body), clazz);
			return (T) fbody;
		} else {
			return null;
		}
	}

	public FramePacket(FixHeader fixHead, ExtHeader extHead, byte[] body, String globalCMD) {
		super();
		this.fixHead = fixHead;
		this.extHead = extHead;
		this.body = body;
		this.globalCMD = globalCMD;
		this.sio = SerializerFactory.getSerializer(fixHead.enctype);
	}

}
