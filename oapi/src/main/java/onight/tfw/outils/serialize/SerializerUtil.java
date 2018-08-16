package onight.tfw.outils.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * 序列化接口 扩展的序列化，需要实现此接口
 * 
 */
public class SerializerUtil {

	/*** 默认序列化类型 */
	private static final char DEFAULT_TYPE = SerializerFactory.SERIALIZER_TRANSBEAN;

	public static <T> Object serialize(T data) {
		return SerializerFactory.getSerializer(DEFAULT_TYPE).serialize(data);
	}

	public static <T> T deserialize(Object dataArray, Class<T> clazz) {
		return SerializerFactory.getSerializer(DEFAULT_TYPE).deserialize(dataArray, clazz);
	}

	public static <T> Object serializeArray(List<T> list) {
		return SerializerFactory.getSerializer(DEFAULT_TYPE).serializeArray(list);
	}

	public static <T> List<T> deserializeArray(Object bytes, Class<T> clazz) {
		return SerializerFactory.getSerializer(DEFAULT_TYPE).deserializeArray(bytes, clazz);
	}

	public static byte[] toBytes(Object object) {
		if (object instanceof byte[]) {
			return (byte[]) object;
		} else {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream();ObjectOutput out = new ObjectOutputStream(bos);){
				out.writeObject(object);
				return bos.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Object fromBytes(byte [] bytes) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) bytes);
			ObjectInput in = new ObjectInputStream(bis);){
			return in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
