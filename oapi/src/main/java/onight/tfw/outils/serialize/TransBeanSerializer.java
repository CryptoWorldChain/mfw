package onight.tfw.outils.serialize;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransBeanSerializer implements ISerializer {

	private static int GLOBAL_MAX_DEEP = 5;// 只能有10层的递归

	private final static TransBeanSerializer instance = new TransBeanSerializer();

	@Getter
	@Setter
	private int maxdeep = GLOBAL_MAX_DEEP;

	@AllArgsConstructor
	@Data
	public static class BeanProp {
		String fieldName;
		Method getM;
		Method setM;
		Class<?> fieldType;
		boolean isBasicType;
		Field field;
	}

	public static class BeanMap<K, V> extends HashMap<K, V> implements
			Externalizable {
		private static final long serialVersionUID = -2226652610945990798L;

		@Override
		public String toString() {
			return "BeanMap@" + System.identityHashCode(this);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeInt(this.size());
			for (Entry<K, V> entry : this.entrySet()) {
				out.writeObject(entry.getKey());
				out.writeObject(entry.getValue());
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void readExternal(ObjectInput in) throws IOException,
				ClassNotFoundException {
			int cc = in.readInt();
			for (int i = 0; i < cc; i++) {
				K key = (K) in.readObject();
				V value = (V) in.readObject();
				put(key, value);
			}
		}
	}

	private TransBeanSerializer() {
	}

	public static TransBeanSerializer getInstance() {
		return instance;
	}

	public <T> Object serialize(T data) {
		return _serialize(data, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> Object _serialize(T data, int depth) {
		if (data == null)
			return null;
		Class clazz = data.getClass();
		if (depth++ >= maxdeep) {
			log.warn("bean对象层次过深，存在循环依赖，序列化抛弃属性：depth=" + (depth - 1)
					+ "@class=" + clazz);
			return null;
		}
		if (clazz.getClassLoader() == null || data instanceof BeanMap)
			return data;
		Map<String, Object> mb = new BeanMap<String, Object>();
		List<BeanProp> getsetMethods = extractMethods(clazz);
		for (BeanProp bp : getsetMethods) {
			try {
				Object value = bp.getM.invoke(data);
				if (value != null) {
					if (bp.isBasicType || value instanceof BeanMap) {
						mb.put(bp.fieldName, value);
					} else if (value instanceof Map) {// 是一个map
						Map<Object, Object> map = new HashMap<Object, Object>();
						for (Entry<Object, Object> kv : ((Map<Object, Object>) value)
								.entrySet()) {
							map.put(kv.getKey(),
									_serialize(kv.getValue(), depth));
						}
						mb.put(bp.fieldName, map);
					} else if (value instanceof List) {// list属性的处理
						List<Object> list = new ArrayList<Object>();
						for (Object obj : ((List) value)) {
							list.add(_serialize(obj, depth));
						}
						mb.put(bp.fieldName, list);
					} else {// 不知道是什么属性
						mb.put(bp.fieldName, _serialize(value, depth));
					}
				}
			} catch (Exception e) {
				log.warn("将数据 [" + data + "]序列化失败 ", e);
			}
		}

		return mb;
	}

	public <T> T deserialize(Object object, Class<T> clazz) {
		return _deserialize(object, clazz, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> T _deserialize(Object object, Class<T> clazz, int depth) {

		if (object == null)
			return null;
		if (depth++ >= maxdeep) {
			log.warn("bean对象层次过深，存在循环依赖，反序列化抛弃属性：depth=" + (depth - 1)
					+ "@class=" + clazz);
			return null;
		}
		if (isBaseType(clazz))
			return (T) object;
		if (clazz.getClassLoader() == null)
			return (T) object;
		if (!(object instanceof BeanMap)) {
			return (T) object;
		}

		T t = null;
		try {
			t = clazz.newInstance();
			BeanMap<String, Object> mb = (BeanMap<String, Object>) object;
			List<BeanProp> getsetMethods = extractMethods(clazz);
			for (BeanProp bp : getsetMethods) {
				if (!mb.containsKey(bp.fieldName)) {
					continue;// no override
				}
				Object v = mb.get(bp.fieldName);
				if (bp.isBasicType || v == null) {
					bp.setM.invoke(t, v);
				} else if (v instanceof List) {
					List orginallist = (List) v;
					if(orginallist.size()==0){
						bp.setM.invoke(t, v);
					}
					else if (isBaseType(orginallist.get(0).getClass())) {
						bp.setM.invoke(t, v);
					} else {

						Type type = ((ParameterizedType) bp.field
								.getGenericType()).getActualTypeArguments()[0];
						// bp.setM.invoke(t, deserializeArray(v, (Class) type));
						// Type type = ((ParameterizedType)
						// field.getGenericType()).getActualTypeArguments()[0];
						if (type == Map.class) {// 如果属性是List<Map>
							bp.setM.invoke(t, v);
						} else {// 如果属性是List<特定对象>
							List list2 = new ArrayList<>();
							Class transclazz = clazz;
							if (type instanceof Class) {
								transclazz = (Class) type;
							} else {
								Type tType = ((ParameterizedType) clazz
										.getGenericSuperclass())
										.getActualTypeArguments()[0];
								transclazz = (Class) tType;
							}

							for (Object obj : orginallist) {
								Map<String, Object> map = (Map<String, Object>) obj;
								list2.add(deserialize(map, (Class) transclazz));
							}
							bp.setM.invoke(t, list2);
						}
					}

				} else if (v instanceof BeanMap) {
					bp.setM.invoke(t,
							_deserialize(v, (Class) bp.fieldType, depth));
				} else if (v instanceof HashMap) {
					Type types[] = ((ParameterizedType) bp.field
							.getGenericType()).getActualTypeArguments();
					HashMap<Object, Object> newhash = new HashMap<Object, Object>();
					for (Entry<Object, Object> entry : ((HashMap<Object, Object>) v)
							.entrySet()) {
						newhash.put(
								deserialize(entry.getKey(), (Class) types[0]),
								_deserialize(entry.getValue(),
										(Class) types[1], depth));
					}
					bp.setM.invoke(t, newhash);
				} else {
					bp.setM.invoke(t, _serialize(v, depth));
				}
			}
		} catch (Exception e) {
			log.warn("将数据[" + object + "]反序列化成 " + clazz + "失败", e);
		}
		return t;

	}

	@Override
	public <T> Object serializeArray(List<T> list) {
		if (list != null) {
			List<Object> maps = new ArrayList<>();
			for (T t : list) {
				maps.add(serialize(t));
			}
			return maps;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> deserializeArray(Object obj, Class<T> clazz) {
		if (obj != null) {
			List<BeanMap<String, Object>> maps = (List<BeanMap<String, Object>>) obj;
			List<T> list = new ArrayList<>();
			for (BeanMap<String, Object> mb : maps) {
				list.add((T) deserialize(mb, clazz));
			}
			return list;
		}
		return null;
	}

	static Map<Class<?>, List<BeanProp>> class2BPs = new HashMap<>();

	public static List<BeanProp> extractMethods(Class<?> clazz) {
		List<BeanProp> props = class2BPs.get(clazz);
		if (props == null && clazz.getClassLoader() != null) {
			String uniqueName = clazz.getClassLoader().getClass().getName()
					+ clazz.getName();
			synchronized (uniqueName.intern()) {
				props = class2BPs.get(clazz);
				if (props == null) {
					props = new ArrayList<BeanProp>();
					for (Field field : allDeclaredField(clazz)) {
						if (Modifier.isTransient(field.getModifiers())) {
							continue;
						}
						PropertyDescriptor pd;
						try {
							pd = new PropertyDescriptor(field.getName(), clazz);
							props.add(new BeanProp(field.getName(), pd
									.getReadMethod(), pd.getWriteMethod(), pd
									.getPropertyType(), isBaseType(pd
									.getPropertyType()), field));
						} catch (IntrospectionException e) {
							log.warn("cannot init BeanProp:for class=" + clazz
									+ ",field=" + field.getName());
						}
					}
					class2BPs.put(clazz, props);
				}
			}
		}
		return props;
	}

	public static List<Field> allDeclaredField(Class<?> clazz) {
		List<Field> fieldList = new ArrayList<>();
		Class<?> targetClass = clazz;
		do {
			Field[] fields = targetClass.getDeclaredFields();
			for (Field f : fields) {
				if (Modifier.isFinal(f.getModifiers())) {// 去掉static的set
				} else {
					fieldList.add(f);
				}
			}
			targetClass = targetClass.getSuperclass();
		} while (targetClass != null && targetClass != Object.class);
		return fieldList;
	}

	public static boolean isBaseType(Class<?> clazz) {
		if (clazz == String.class || clazz == Integer.class
				|| clazz == Double.class || clazz == Short.class
				|| clazz == Byte.class || clazz == Float.class
				|| clazz == Boolean.class || clazz == Character.class
				|| clazz == Long.class || clazz.isPrimitive()
				|| clazz == BigDecimal.class || clazz == java.util.Date.class
				|| clazz == java.sql.Date.class
				|| clazz == java.sql.Timestamp.class) {
			return true;
		}
		return false;
	}

}