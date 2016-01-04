package onight.tfw.otransio.api.beans;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.outils.serialize.ISerializer;
import onight.tfw.outils.serialize.SerializerFactory;
import onight.tfw.outils.serialize.SerializerUtil;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FramePacket{
	FixHeader fixHead;
	ExtHeader extHead;

	
	@JsonIgnore
	protected byte[] body;

	@JsonIgnore
	transient ISerializer sio;

	transient Object fbody;

	@JsonIgnore
	transient String globalCMD;

	
	@JsonIgnore
	transient HashMap<String,Object> payloads=new HashMap<String, Object>();

	public String getExtProp(String key) {
		return extHead.get(key);
	}

	public String putHeader(String key, String value) {
		if (extHead == null) {
			extHead = new ExtHeader();
		}
		return extHead.append(key, value);
	}

	public Map<String, String> getExts() {
		if (extHead != null) {
			return extHead.kvs;
		} else {
			return null;
		}
	}

	public boolean isSync() {
		return fixHead.isSync();
	}

	public String getCMD() {
		return fixHead.getCmd();
	}

	public String getModule() {
		return fixHead.getModule();
	}

	public String getModuleAndCMD() {
		if (globalCMD == null) {
			globalCMD = fixHead.getCmd() + fixHead.getModule();
		}
		return globalCMD;
	}

	public byte[] genBodyBytes() {
		if (fbody != null) {
			if(sio==null){
				sio = SerializerFactory.getSerializer(fixHead.getEnctype());
			}
			if(fbody instanceof Message && fixHead.getEnctype() == SerializerFactory.SERIALIZER_JSON){//
				//pb 2 json
				body=JsonFormat.printToString((Message)fbody).getBytes();
			}else{
				body = SerializerUtil.toBytes(sio.serialize(fbody));
			}
		} else if (body == null) {
			body = PackHeader.EMPTY_BYTES;
		}
		return body;
	}

	public byte[] genExtBytes() {
		if(extHead==null){
			return PackHeader.EMPTY_BYTES;
		}
		return extHead.genBytes();
	}

	public <T> T parseBO(Class<T> clazz) {
		if(fbody!=null){
			return (T)fbody;
		}
		if (clazz != null) {
			fbody =  sio.deserialize(SerializerUtil.fromBytes(body), clazz);
			return (T)fbody;
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
