package onight.tfw.outils.bean;

import static com.googlecode.protobuf.format.util.TextUtils.unsignedToString;

import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FieldOptions.JSType;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.MapEntry;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

public class JsonPBFormat extends JsonFormat {

	@Override
	public void printField(FieldDescriptor field, Object value,
			JsonGenerator generator) throws IOException {
		printSingleField(field, value, generator);
	}

	@Override
	protected void print(Message message, JsonGenerator generator)
			throws IOException {
		Map<Descriptors.FieldDescriptor, Object> mapf = message.getAllFields();
		boolean hasprev = false;
		for (Iterator<FieldDescriptor> it = message.getDescriptorForType()
				.getFields().iterator(); it.hasNext();) {
			FieldDescriptor fd = it.next();
			if (mapf.get(fd) == null) {
				if (!isNumberType(fd.getType()))
					continue;
			}
			if (hasprev) {
				generator.print(",");
			}
			printField(fd, message.getField(fd), generator);
			hasprev = true;
		}
		if (message.getUnknownFields().asMap().size() > 0)
			generator.print(", ");
		printUnknownFields(message.getUnknownFields(), generator);
	}

	private boolean isNumberType(Type type) {
		switch (type) {
		case INT32:
		case INT64:
		case SINT32:
		case SINT64:
		case SFIXED32:
		case SFIXED64:
		case FLOAT:
		case DOUBLE:
		case BOOL:
		case UINT32:
		case FIXED32:
		case UINT64:
		case FIXED64:
			return true;
		default:
			return false;
		}

	}

	private void printSingleField(FieldDescriptor field, Object value,
			JsonGenerator generator) throws IOException {
		if (field.isExtension()) {
			generator.print("\"");
			// We special-case MessageSet elements for compatibility with
			// proto1.
			if (field.getContainingType().getOptions()
					.getMessageSetWireFormat()
					&& (field.getType() == FieldDescriptor.Type.MESSAGE)
					&& (field.isOptional())
					// object equality
					&& (field.getExtensionScope() == field.getMessageType())) {
				generator.print(field.getMessageType().getFullName());
			} else {
				generator.print(field.getFullName());
			}
			generator.print("\"");
		} else {
			generator.print("\"");
			if (field.getType() == FieldDescriptor.Type.GROUP) {
				// Groups must be serialized with their original capitalization.
				generator.print(field.getMessageType().getName());
			} else {
				generator.print(field.getName());
			}
			generator.print("\"");
		}

		// Done with the name, on to the value

		if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
			generator.print(": ");
			generator.indent();
		} else {
			generator.print(": ");
		}

		if (field.isRepeated()) {
			// Repeated field. Print each element.
			if (field.isMapField()) {
				generator.print("{");
				for (Iterator<?> iter = ((List<?>) value).iterator(); iter
						.hasNext();) {
					MapEntry map = (MapEntry) (iter.next());
					generator.print("\"" + map.getKey() + "\":");
					printFieldValue(
							map.getDescriptorForType().getFields().get(1),
							map.getValue(), generator);
					if (iter.hasNext()) {
						generator.print(",");
					}
				}
				generator.print("}");
			} else {
				generator.print("[");
				for (Iterator<?> iter = ((List<?>) value).iterator(); iter
						.hasNext();) {
					printFieldValue(field, iter.next(), generator);
					if (iter.hasNext()) {
						generator.print(",");
					}
				}
				generator.print("]");
			}
		} else {
			printFieldValue(field, value, generator);
			if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
				generator.outdent();
			}
		}
	}

	private void printFieldValue(FieldDescriptor field, Object value,
			JsonGenerator generator) throws IOException {
		switch (field.getType()) {
		case INT32:
		case INT64:
		case SINT32:
		case SINT64:
		case SFIXED32:
		case SFIXED64:
		case FLOAT:
		case DOUBLE:
		case BOOL:
			// Good old toString() does what we want for these types.
			generator.print(value.toString());
			break;

		case UINT32:
		case FIXED32:
			generator.print(unsignedToString((Integer) value));
			break;

		case UINT64:
		case FIXED64:
			generator.print(unsignedToString((Long) value));
			break;

		case STRING:
			if (field.getOptions().hasJstype()
					&& field.getOptions().getJstype() == JSType.JS_NORMAL) {
				generator.print((String) value);
			} else {
				generator.print("\"");
				generator.print(escapeText((String) value));
				generator.print("\"");
			}
			break;

		case BYTES: {
			generator.print("\"");
			generator.print(escapeBytes((ByteString) value));
			generator.print("\"");
			break;
		}

		case ENUM: {
			generator.print("\"");
			generator.print(((EnumValueDescriptor) value).getName());
			generator.print("\"");
			break;
		}

		case MESSAGE:
		case GROUP:
			generator.print("{");
			print((Message) value, generator);
			generator.print("}");
			break;
		}
	}

	static String escapeText(String input) {
		StringBuilder builder = new StringBuilder(input.length());
		CharacterIterator iter = new StringCharacterIterator(input);
		for (char c = iter.first(); c != CharacterIterator.DONE; c = iter
				.next()) {
			switch (c) {
			case '\b':
				builder.append("\\b");
				break;
			case '\f':
				builder.append("\\f");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\t':
				builder.append("\\t");
				break;
			case '\\':
				builder.append("\\\\");
				break;
			case '"':
				builder.append("\\\"");
				break;
			default:
				// Check for other control characters
				if (c >= 0x0000 && c <= 0x001F) {
					appendEscapedUnicode(builder, c);
				} else if (Character.isHighSurrogate(c)) {
					// Encode the surrogate pair using 2 six-character sequence
					// (\\uXXXX\\uXXXX)
					appendEscapedUnicode(builder, c);
					c = iter.next();
					if (c == CharacterIterator.DONE)
						throw new IllegalArgumentException(
								"invalid unicode string: unexpected high surrogate pair value without corresponding low value.");
					appendEscapedUnicode(builder, c);
				} else {
					// Anything else can be printed as-is
					builder.append(c);
				}
				break;
			}
		}
		return builder.toString();
	}

	static void appendEscapedUnicode(StringBuilder builder, char ch) {
		String prefix = "\\u";
		if (ch < 0x10) {
			prefix = "\\u000";
		} else if (ch < 0x100) {
			prefix = "\\u00";
		} else if (ch < 0x1000) {
			prefix = "\\u0";
		}
		builder.append(prefix).append(Integer.toHexString(ch));
	}

	static String escapeBytes(ByteString input) {
		StringBuilder builder = new StringBuilder(input.size());
		for (int i = 0; i < input.size(); i++) {
			byte b = input.byteAt(i);
			switch (b) {
			// Java does not recognize \a or \v, apparently.
			case 0x07:
				builder.append("\\a");
				break;
			case '\b':
				builder.append("\\b");
				break;
			case '\f':
				builder.append("\\f");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\t':
				builder.append("\\t");
				break;
			case 0x0b:
				builder.append("\\v");
				break;
			case '\\':
				builder.append("\\\\");
				break;
			case '\'':
				builder.append("\\\'");
				break;
			case '"':
				builder.append("\\\"");
				break;
			default:
				if (b >= 0x20) {
					builder.append((char) b);
				} else {
					final String unicodeString = unicodeEscaped((char) b);
					builder.append(unicodeString);
				}
				break;
			}
		}
		return builder.toString();
	}

	static String unicodeEscaped(char ch) {
		if (ch < 0x10) {
			return "\\u000" + Integer.toHexString(ch);
		} else if (ch < 0x100) {
			return "\\u00" + Integer.toHexString(ch);
		} else if (ch < 0x1000) {
			return "\\u0" + Integer.toHexString(ch);
		}
		return "\\u" + Integer.toHexString(ch);
	}
}
