package onight.tfw.outils.serialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.util.TokenBuffer;

public class JsonSerializer implements ISerializer {

	private final static JsonSerializer instance = new JsonSerializer();

	private JsonSerializer() {
	}

	public static JsonSerializer getInstance() {
		return instance;
	}

	@Override
	public <T> T deserialize(Object dataArray, Class<T> clazz) {
		try {
			if (dataArray != null) {
				if (dataArray instanceof String) {
					return mapper.readValue((String)dataArray, clazz);
				}
				return mapper.readValue(new String((byte[]) dataArray, DEFAULT_CHARSET), clazz);
			}
			return null;
		} catch (

		Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> Object serialize(T data) {
		try {
			if (data == null) {
				return null;
			}
			TokenBuffer buffer = new TokenBuffer(mapper);
			mapper.writeValue(buffer, data);
			return mapper.readTree(buffer.asParser()).toString().getBytes(DEFAULT_CHARSET);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String formatToString(Object data) {
		try {
			if (data == null) {
				return null;
			}
			TokenBuffer buffer = new TokenBuffer(mapper);
			mapper.writeValue(buffer, data);
			return mapper.readTree(buffer.asParser()).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> Object serializeArray(List<T> list) {
		return this.serialize(list);
	}

	@Override
	public <T> List<T> deserializeArray(Object bytes, Class<T> clazz) {
		try {
			List<T> list = new ArrayList<>();
			String jsontxt = new String((byte[]) bytes, DEFAULT_CHARSET);
			ArrayNode nodes = mapper.readValue(jsontxt, ArrayNode.class);
			for (JsonNode node : nodes) {
				list.add(mapper.readValue(node, clazz));
			}
			return list;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES, false);

	}

}
