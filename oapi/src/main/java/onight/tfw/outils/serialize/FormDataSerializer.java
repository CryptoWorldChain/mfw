package onight.tfw.outils.serialize;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import onight.tfw.outils.serialize.TransBeanSerializer.BeanMap;

@Slf4j
public class FormDataSerializer implements ISerializer {

	private static int GLOBAL_MAX_DEEP = 5;// 只能有10层的递归

	private final static FormDataSerializer instance = new FormDataSerializer();

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

	private FormDataSerializer() {
	}

	public static FormDataSerializer getInstance() {
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
			log.warn("bean对象层次过深，存在循环依赖，序列化抛弃属性：depth=" + (depth - 1) + "@class=" + clazz);
			return null;
		}
		StringBuffer sb = new StringBuffer();
		List<BeanProp> getsetMethods = extractMethods(clazz);
		for (BeanProp bp : getsetMethods) {
			try {
				Object value = bp.getM.invoke(data);
				if (value != null) {
					if (sb.length() > 0) {
						sb.append("&");
					}
					if (bp.isBasicType) {
						// mb.put(bp.fieldName, value);
						sb.append(bp.fieldName).append("=").append(URLEncoder.encode(value + "", "UTF-8"));
					} else if (value instanceof Map) {// 是一个map
						StringBuffer subsb = new StringBuffer();
						sb.append(bp.fieldName).append("=");
						for (Entry<Object, Object> kv : ((Map<Object, Object>) value).entrySet()) {
							if (subsb.length() > 0) {
								subsb.append(",");
							}
							subsb.append(kv.getKey()).append("=").append(URLEncoder.encode(_serialize(kv.getValue(), depth) + "", "UTF-8"));
						}
						sb.append(URLEncoder.encode(subsb.toString(), "UTF-8"));
					} else if (value instanceof List) {// list属性的处理
						StringBuffer subsb = new StringBuffer();
						sb.append(bp.fieldName).append("=");
						for (Object obj : ((List) value)) {
							if (subsb.length() > 0) {
								subsb.append(",");
							}
							subsb.append(URLEncoder.encode("" + _serialize(obj, depth), "UTF-8"));
						}
						sb.append(URLEncoder.encode(subsb.toString(), "UTF-8"));
						// mb.put(bp.fieldName, list);
					} else {// 不知道是什么属性
						sb.append(bp.fieldName).append("=").append(URLEncoder.encode("" + _serialize(value, depth), "UTF-8"));
						// mb.put(bp.fieldName, _serialize(value, depth));
					}
				}

			} catch (Exception e) {
				log.warn("将数据 [" + data + "]序列化失败 ", e);
			}
		}

		return sb.toString();
	}

	public <T> T deserialize(Object object, Class<T> clazz) {
		return _deserialize(object, clazz, 0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> T _deserialize(Object object, Class<T> clazz, int depth) {

		if (object == null)
			return null;
		if (depth++ >= maxdeep) {
			log.warn("bean对象层次过深，存在循环依赖，反序列化抛弃属性：depth=" + (depth - 1) + "@class=" + clazz);
			return null;
		}
		if (isBaseType(clazz))
			return (T) object;
		if (clazz.getClassLoader() == null)
			return (T) object;

		T t = null;
		try {
			t = clazz.newInstance();
			List<BeanProp> getsetMethods = extractMethods(clazz);
			String pairs[];
			HashMap<String, String> kvs = new HashMap<>();
			if (object instanceof byte[]) {
				pairs = new String((byte[]) object, "UTF-8").split("&");
			} else {
				pairs = ("" + object).split("&");
			}
			for (String kv : pairs) {
				String p[] = kv.split("=");
				if (p.length == 2) {
					kvs.put(p[0].trim(), URLDecoder.decode(p[1].trim(), "UTF-8"));
				}
			}
			for (BeanProp bp : getsetMethods) {
				if (!kvs.containsKey(bp.fieldName)) {
					continue;// no override
				}
				String v = kvs.get(bp.fieldName);
				if (bp.isBasicType || v == null) {
					bp.setM.invoke(t, v);
				} else {
					List<String> orginallist = new ArrayList<>();
					for (String env : v.split(",")) {
						String srcv = URLDecoder.decode(env, "UTF-8");
						orginallist.add(srcv);
					}
					if (orginallist.size() > 0) {
						Type type = ((ParameterizedType) bp.field.getGenericType()).getActualTypeArguments()[0];
						if (type == Map.class) {// 如果属性是List<Map>
							// bp.setM.invoke(t, v);
							HashMap<String, Object> map = new HashMap<>();
							for (String env : orginallist) {
								String kv[] = env.split("=");
								map.put(kv[0], kv[1]);
							}
							bp.setM.invoke(t, map);
						} else {// 如果属性是List<特定对象>
							List list2 = new ArrayList<>();
							Class transclazz = clazz;
							if (type instanceof Class) {
								transclazz = (Class) type;
							} else {
								Type tType = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
								transclazz = (Class) tType;
							}

							for (Object obj : orginallist) {
								Map<String, Object> map = (Map<String, Object>) obj;
								list2.add(deserialize(map, (Class) transclazz));
							}
							bp.setM.invoke(t, list2);
						}
					}
				}
			}
		} catch (

		Exception e) {
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
			String uniqueName = clazz.getClassLoader().getClass().getName() + clazz.getName();
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
							props.add(new BeanProp(field.getName(), pd.getReadMethod(), pd.getWriteMethod(), pd.getPropertyType(), isBaseType(pd.getPropertyType()), field));
						} catch (IntrospectionException e) {
							log.warn("cannot init BeanProp:for class=" + clazz + ",field=" + field.getName());
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
		if (clazz == String.class || clazz == Integer.class || clazz == Double.class || clazz == Short.class || clazz == Byte.class || clazz == Float.class
				|| clazz == Boolean.class || clazz == Character.class || clazz == Long.class || clazz.isPrimitive() || clazz == BigDecimal.class || clazz == java.util.Date.class
				|| clazz == java.sql.Date.class || clazz == java.sql.Timestamp.class) {
			return true;
		}
		return false;
	}
	
	public static void main(String[] args) {
		
	}

}