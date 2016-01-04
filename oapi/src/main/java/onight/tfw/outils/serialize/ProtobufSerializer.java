package onight.tfw.outils.serialize;

import java.lang.reflect.Method;
import java.util.List;

import com.google.protobuf.MessageLite;

class ProtobufSerializer implements ISerializer {

	private final static ProtobufSerializer instance = new ProtobufSerializer();

	private ProtobufSerializer() {
	}

	public static ProtobufSerializer getInstance() {
		return instance;
	}

	@Override
	public <T> byte[] serialize(T data) {
		if (data != null) {
			return ((MessageLite) data).toByteArray();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(Object dataArray, Class<T> clazz) {
		if (dataArray != null) {
			try {
				Method method = clazz.getMethod(METHOD, dataArray.getClass());
				if (method != null) {
					return (T) method.invoke(clazz,(byte[]) dataArray);
				} else {
					throw new RuntimeException("protocol pojo hasn't method: "
							+ METHOD);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	private static String METHOD = "parseFrom";

	@Override
	public <T> byte[] serializeArray(List<T> list) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<T> deserializeArray(Object bytes, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}
}
