package onight.tfw.outils.serialize;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 序列化工厂 定义序列化类型，注册序列化对象，查找序列化对象 开发者可以使用其他序列化方式，使用时只要把自定义序列化对象注册到该工厂中
 * 
 */
public class SerializerFactory {

	// AVRO序列化
	public static final char SERIALIZER_AVRO = 'A';
	// JSON序列化
	public static final char SERIALIZER_JSON = 'J';
	// protobuf序列化
	public static final char SERIALIZER_PROTOBUF = 'P';
	// mapbean序列化
	public static final char SERIALIZER_MAPBEAN = 'M';
	// transbean序列化
	public static final char SERIALIZER_TRANSBEAN = 'T';

	private static Map<Character, ISerializer> serializerHandlerMap = new HashMap<Character, ISerializer>();

	static {
		SerializerFactory.registerSerializer(SERIALIZER_AVRO,
				AvroSerializer.getInstance());
		SerializerFactory.registerSerializer(SERIALIZER_JSON,
				JsonSerializer.getInstance());
		SerializerFactory.registerSerializer(SERIALIZER_PROTOBUF,
				ProtobufSerializer.getInstance());
		SerializerFactory.registerSerializer(SERIALIZER_MAPBEAN,
				MapBeanSerializer.getInstance());
		SerializerFactory.registerSerializer(SERIALIZER_TRANSBEAN,
				TransBeanSerializer.getInstance());
	}

	public static void registerSerializer(char type, ISerializer serializer) {
		serializerHandlerMap.put(type, serializer);
	}

	public static ISerializer getSerializer(char type) {
		return serializerHandlerMap.get(type);
	}

}
