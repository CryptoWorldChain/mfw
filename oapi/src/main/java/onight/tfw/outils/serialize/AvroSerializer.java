package onight.tfw.outils.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;

class AvroSerializer implements ISerializer {

	private final static AvroSerializer instance = new AvroSerializer();
	static String MAP_SCHEMA = "{\"type\":\"map\", \"values\" :[\"string\",\"bytes\", \"int\", \"long\", \"null\",\"boolean\",\"float\",\"double\", {\"type\":\"record\",\"name\":\"Date\",\"namespace\":\"java.util\",\"fields\":[]}]}";

	private AvroSerializer() {
	}

	public static AvroSerializer getInstance() {
		return instance;
	}


	public Schema getSchema(Class<?> clazz) {
		ReflectData reflectData = ReflectData.get();
		return reflectData.getSchema(clazz);
	}

	/**
	 * Avro序列化对象
	 * 
	 * @param data
	 */
	@SuppressWarnings({ "unchecked", "resource" })
	public <T> Object serialize(T data) {
		try {
			Class<T> clazz = (Class<T>) data.getClass();
			Schema schema = getSchema(clazz);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			GenericDatumWriter<T> datumWriter = new ReflectDatumWriter<T>(clazz);
			DataFileWriter<T> writer = new DataFileWriter<T>(datumWriter)
					.create(schema, outputStream);
			writer.append(data);
			writer.close();
			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Avro反序列化对象
	 * 
	 * @param bytes
	 * @param clazz
	 */
	public <T> T deserialize(Object bytes, Class<T> clazz) {
		try {
			Schema schema = getSchema(clazz);
			ByteArrayInputStream inputStream = new ByteArrayInputStream((byte[])bytes);
			GenericDatumReader<T> datumReader = new ReflectDatumReader<T>(clazz);
			DataFileStream<T> reader = new DataFileStream<T>(inputStream,
					datumReader);
			datumReader.setExpected(schema);
			T info = reader.next();
			reader.close();
			return info;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	public <T> byte[] serializeArray(List<T> list) {
		try {
			Class<T> clazz = (Class<T>) list.get(0).getClass();
			Schema schema = getSchema(clazz);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			GenericDatumWriter<T> datumWriter = new ReflectDatumWriter<T>(clazz);
			DataFileWriter<T> writer = new DataFileWriter<T>(datumWriter)
					.create(schema, outputStream);
			for (T info : list) {
				writer.append(info);
			}
			writer.close();
			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> List<T> deserializeArray(Object bytes, Class<T> clazz) {
		try {
			Schema schema = getSchema(clazz);
			ByteArrayInputStream inputStream = new ByteArrayInputStream((byte[])bytes);
			GenericDatumReader<T> datumReader = new ReflectDatumReader<T>(clazz);
			DataFileStream<T> reader = new DataFileStream<T>(inputStream,
					datumReader);
			datumReader.setExpected(schema);
			List<T> list = new ArrayList<>();
			while (reader.hasNext()) {
				T info = reader.next();
				list.add(info);
			}
			reader.close();
			return list;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
