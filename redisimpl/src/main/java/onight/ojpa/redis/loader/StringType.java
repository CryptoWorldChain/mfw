package onight.ojpa.redis.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import onight.tfw.outils.serialize.SerializerUtil;

public class StringType {

	public static interface Factory {
		abstract Object newOne(String str);
	}

	static ConcurrentHashMap<Class, Character> class2Type = new ConcurrentHashMap<Class, Character>();
	static ConcurrentHashMap<Character, Factory> type2Class = new ConcurrentHashMap<Character, Factory>();

	@Data
	@AllArgsConstructor
	public static class TypeC {
		char cc;
		Class clazz1;
		Class clazz2;
		Factory factory;

		public void addToMap() {
			if (clazz1 != null) {
				class2Type.put(clazz1, cc);
			}
			if (clazz2 != null) {
				class2Type.put(clazz2, cc);
			}
			type2Class.put(cc, factory);
		}
	}

	static {
		new TypeC('i', Integer.class, int.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return Integer.parseInt(str);
			}
		}).addToMap();

		new TypeC('s', Short.class, short.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return Short.parseShort(str);
			}
		}).addToMap();

		new TypeC('l', Long.class, long.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return Long.parseLong(str);
			}
		}).addToMap();

		new TypeC('b', Byte.class, byte.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return Byte.parseByte(str);
			}
		}).addToMap();

		new TypeC('d', Double.class, double.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return Double.parseDouble(str);
			}
		}).addToMap();
		new TypeC('f', Float.class, float.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return Float.parseFloat(str);
			}
		}).addToMap();

		new TypeC('a', Boolean.class, boolean.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return Boolean.parseBoolean(str);
			}
		}).addToMap();

		new TypeC('c', Character.class, char.class, new Factory() {
			@Override
			public Object newOne(String str) {
				return str.charAt(0);
			}
		}).addToMap();

		new TypeC('D', java.util.Date.class, null, new Factory() {
			@Override
			public Object newOne(String str) {
				SimpleDateFormat sdf = new SimpleDateFormat();
				try {
					return sdf.parseObject(str);
				} catch (ParseException e) {
					return null;
				}
			}
		}).addToMap();

		new TypeC('S', String.class, null, new Factory() {
			@Override
			public Object newOne(String str) {
				return str;
			}
		}).addToMap();

		// class2Type.put(java.sql.Timestamp.class, "T");
		// class2Type.put(String.class, "S");
		// class2Type.put(BigDecimal.class, "B");

	}

	public static byte[] toTBytes(Object t) {
		Character type = class2Type.get(t.getClass());
		if (type != null) {
			return (type + String.valueOf(t)).getBytes();
		} else {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			bout.write(0);
			try {
				bout.write(SerializerUtil.toBytes(t));
				bout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bout.toByteArray();
		}
	}

	public static Object toTObject(byte[] bb) {
		Character ch=(char)bb[0];
		
		Factory factory = type2Class.get(ch);
		if (factory != null) {
			return factory.newOne(new String(bb,1,bb.length-1));
		} else {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			try {
				bout.write(bb,1,bb.length-1);
				bout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return SerializerUtil.fromBytes(bout.toByteArray());
		}
	}
}
