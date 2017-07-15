package onight.tfw.outils.bean;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonPBUtil {

	static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(SerializationConfig.Feature.WRITE_NULL_PROPERTIES,
				false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object getValue(FieldDescriptor fd, JsonNode node,
			Message.Builder builder) {
		if (fd.isMapField() && fd.getJavaType().equals(JavaType.MESSAGE)
				&& !node.isArray()) {
			json2PBMap(fd, node, builder);
			return null;
		}
		if (fd.isRepeated() && (node.isArray() || node.isObject())) {
			Iterator<JsonNode> it = node.getElements();
			Message.Builder subbuilder = null;
			if (fd.getJavaType().equals(JavaType.STRING)&&node.isObject()) {
				if (!node.isTextual()) {
					return node.toString();
				} else
					return node.asText();
			}

			while (it.hasNext()) {

				JsonNode itnode = it.next();

				Object v = null;

				if (fd.getJavaType().equals(JavaType.MESSAGE)) {
					subbuilder = builder.newBuilderForField(fd);
					json2PB(itnode, subbuilder);
					v = subbuilder.build();
				}
				else
				{
					subbuilder = builder;
					v = getValue(fd, itnode, subbuilder);
				}
				if (v != null) {
					builder.addRepeatedField(fd, v);
				} else if (builder != null) {
					builder.addRepeatedField(fd, subbuilder.build());
				}
			}
			return null;
		}
		if (fd.getJavaType().equals(JavaType.STRING)) {
			if (!node.isTextual()) {
				return node.toString();
			} else
				return node.asText();
		}
		if (fd.getJavaType().equals(JavaType.INT)) {
			return node.asInt();
		}
		if (fd.getJavaType().equals(JavaType.DOUBLE)) {
			return node.asDouble();
		}
		if (fd.getJavaType().equals(JavaType.FLOAT)) {
			return node.asDouble();
		}
		if (fd.getJavaType().equals(JavaType.LONG)) {
			return node.asLong();
		}
		if (fd.getJavaType().equals(JavaType.BOOLEAN)) {
			return node.asBoolean();
		}
		if (fd.getJavaType().equals(JavaType.ENUM)) {
			EnumValueDescriptor evd = fd.getEnumType().findValueByNumber(
					node.asInt());
			return evd;
		}
		if (fd.getJavaType().equals(JavaType.MESSAGE)) {
			// System.out.println("message.file=" + fd);
			if (fd.isMapField()) {
				json2PBMap(fd, node, builder);
				return null;
			} else {
				Message.Builder subbuilder = builder.newBuilderForField(fd);
				json2PB(node, subbuilder);
				return subbuilder.build();
			}
		}
		return null;
	}

	public static void json2PB(String jsonTxt, Message.Builder msgBuilder) {

		try {
			JsonNode tree = mapper.readTree(jsonTxt);
			json2PB(tree, msgBuilder);
		} catch (Exception e) {
			log.warn("error in json2PB:jsonTxt=" + jsonTxt + ",builder="
					+ msgBuilder, e);
		}
	}

	public static void json2PB(byte[] jsonbytes, Message.Builder msgBuilder) {

		try {
			JsonNode tree = mapper.readTree(jsonbytes);
			json2PB(tree, msgBuilder);
		} catch (Exception e) {
			log.warn("error in json2PB:jsonTxt=" + new String(jsonbytes)
					+ ",builder=" + msgBuilder, e);
		}
	}
	public static void json2PBArrayB(byte[] jsonbytes, BuilderFactory factory) {
		try {
			JsonNode tree = mapper.readTree(jsonbytes);
			json2PBArray(tree, factory);
		} catch (Exception e) {
			log.warn("error in json2PB:jsonTxt=" + new String(jsonbytes)
					+ ",builders=" + factory, e);
		}
	}
	public static interface BuilderFactory{
		public Message.Builder getBuilder();
	}
	public static void json2PBArrayS(String jsontext, BuilderFactory factory) {
		try {
			JsonNode tree = mapper.readTree(jsontext);
			json2PBArray(tree, factory);
		} catch (Exception e) {
			log.warn("error in json2PB:jsonTxt=" + jsontext
					+ ",builders=" + factory, e);
		}
	}
	public static void json2PBMap(FieldDescriptor fd, JsonNode node,
			Message.Builder msgBuilder) {
		Iterator<Map.Entry<String, JsonNode>> it = (Iterator<Map.Entry<String, JsonNode>>) node
				.getFields();
		while (it.hasNext()) {
			Map.Entry<String, JsonNode> item = it.next();
			MapEntry.Builder mb = (MapEntry.Builder) msgBuilder
					.newBuilderForField(fd);
			FieldDescriptor fd2 = mb.getDescriptorForType().getFields().get(1);
			mb.setKey(item.getKey().trim());
			mb.setValue(getValue(fd2, item.getValue(), mb));
			msgBuilder.addRepeatedField(fd, mb.build());
		}
	}
	public static void json2PBArray(JsonNode tree, BuilderFactory factory) {
		if(tree.isArray()){
			Iterator<JsonNode> it = tree.getElements();
			while (it.hasNext()) {
				JsonNode itnode = it.next();
				json2PB(itnode,factory.getBuilder());
			}
		}
	}
	public static void json2PB(JsonNode tree, Message.Builder msgBuilder) {
		try {
			List<FieldDescriptor> fds = msgBuilder.getDescriptorForType()
					.getFields();

			for (FieldDescriptor fd : fds) {
				JsonNode node = tree.get(fd.getName());
				if (node == null)
					continue;
				Object v = getValue(fd, node, msgBuilder);
				if (v != null) {
					msgBuilder.setField(fd, v);
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}
}
