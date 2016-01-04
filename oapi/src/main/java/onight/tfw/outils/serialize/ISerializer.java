package onight.tfw.outils.serialize;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 序列化接口 扩展的序列化，需要实现此接口
 * 
 */
public interface ISerializer {

	public static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public <T> Object serialize(T data);

	public <T> Object serializeArray(List<T> list);

	public <T> T deserialize(Object bytes, Class<T> clazz);

	public <T> List<T> deserializeArray(Object bytes, Class<T> clazz);

}
