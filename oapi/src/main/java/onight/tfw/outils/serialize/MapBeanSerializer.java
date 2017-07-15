package onight.tfw.outils.serialize;

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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MapBeanSerializer implements ISerializer {

	private final static MapBeanSerializer instance = new MapBeanSerializer();

	private MapBeanSerializer() {
	}

	public static MapBeanSerializer getInstance() {
		return instance;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public <T> Object serialize(T data) {
		if (data == null)
			return null;
		Class clazz = data.getClass();
		if (clazz.getClassLoader() == null)
			return data;
		Map<String, Object> mb = new HashMap<String, Object>();
		Map<String, Pair<Method, Method>> getsetMethods = extractMethods(clazz);
		try {
			for (Entry<String, Pair<Method, Method>> entry : getsetMethods.entrySet()) {
				Object value = entry.getValue().getLeft().invoke(data);
				if (value != null) {
					if (value instanceof List) {// list属性的处理
						List list = (List) value;
						if (list.size() > 0) {
							if (isBaseType(list.get(0)) || list.get(0) instanceof Map) {
								mb.put(entry.getKey(), value);
							} else {// List里是特定对象，转成List<Map>
								List<Object> list2 = new ArrayList<>();
								for (Object o : list) {
									list2.add(bean2Map(o));
								}
								mb.put(entry.getKey(), list2);
							}
						}
					} else if (value instanceof Map) {// list属性的处理
						Map list = (Map) value;
						if (list.size() > 0) {
							Map<Object, Object> list2 = new HashMap<Object, Object>();
							for (Object o : list.keySet()) {
								list2.put(o, bean2Map(list.get(o)));
							}
							mb.put(entry.getKey(), list2);
						}
					} else if (value instanceof Set) {
						Set set = (Set) value;
						mb.put(entry.getKey(), set);
					} else if (isBaseType(value)) {
						mb.put(entry.getKey(), value);
					} else {// 属性是特定对象,转成Map
						mb.put(entry.getKey(), bean2Map(value));
					}
				}
			}
			return mb;
		} catch (Exception e) {
			log.warn("将数据 [" + data + "]序列化失败 ", e);
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public <T> T deserialize(Object object, Class<T> clazz) {
		try {
			if (object == null)
				return null;
			if (isBaseType(clazz))
				return (T) object;
			T t = clazz.newInstance();
			if (clazz.getClassLoader() == null)
				return (T) object;

			Map<String, Object> mb = (HashMap<String, Object>) object;
			for (Field field : allDeclaredField(clazz)) {
				Object o = mb.get(field.getName());
				if (o != null) {
					Method set = setMethod(clazz, field);
					if (isBaseType(field.getType()) || isBaseType(o)) {// 基本类型
						set.invoke(t, o);
					} else if (field.getType() == List.class) {
						List orginallist = (List) o;
						if (isBaseType(orginallist.get(0))) {
							set.invoke(t, o);
						} else {
							Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

							if (type == Map.class) {// 如果属性是List<Map>
								set.invoke(t, o);
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
								set.invoke(t, list2);
							}
						}
					} else if (field.getType() == Set.class) {
						Set s = (Set) o;
						set.invoke(t, s);
					} else {// 如果属性是特定对象
						if (!(o instanceof Map)) {
							set.invoke(t, o);
							log.warn(field.getName() + "需要的Map类型,给的类型" + o.getClass() + "不匹配");
							continue;
						} else {
							Object obj = deserialize((Map) o, field.getType());
							set.invoke(t, obj);
						}
					}
				}
			}
			return t;
		} catch (Exception e) {
			log.warn("将数据[" + object + "]反序列化成 " + clazz + "失败", e);
		}
		return null;
	}

	@Override
	public <T> Object serializeArray(List<T> list) {
		if (list != null && list.size() > 0) {
			List<Object> maps = new ArrayList<>();
			for (T t : list) {
				maps.add(bean2Map(t));
			}
			return maps;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> deserializeArray(Object obj, Class<T> clazz) {
		if (obj != null) {
			List<HashMap<String, Object>> maps = (List<HashMap<String, Object>>) obj;
			List<T> list = new ArrayList<>();
			for (HashMap<String, Object> mb : maps) {
				list.add((T) deserialize(mb, clazz));
			}
			return list;
		}
		return null;
	}

	static Map<Class<?>, Map<String, Pair<Method, Method>>> class2Map = new HashMap<>();

	public static Map<String, Pair<Method, Method>> extractMethods(Class<?> clazz) {
		Map<String, Pair<Method, Method>> map = class2Map.get(clazz);
		if (map == null && clazz.getClassLoader() != null) {
			String uniqueName = clazz.getClassLoader().getClass().getName() + clazz.getName();
			synchronized (uniqueName.intern()) {
				map = class2Map.get(clazz);
				if (map == null) {
					map = new HashMap<>();
					for (Field field : allDeclaredField(clazz)) {
						if (Modifier.isTransient(field.getModifiers())) {
							continue;
						}
						Method get = getMethod(clazz, field);
						Method set = setMethod(clazz, field);
						if (get != null && set != null) {
							map.put(field.getName(), Pair.of(get, set));
						}
					}
					class2Map.put(clazz, map);

				}
			}
		}
		return map;
	}

	private static Method getMethod(Class<?> clazz, Field field) {
		Method method = null;
		try {
			if (field.getType() == boolean.class) {
				method = clazz.getMethod("is" + StringUtils.capitalize(field.getName()));
				if (method == null) {
					method = clazz.getMethod(field.getName());
				}
			}
			if (method == null) {
				method = clazz.getMethod("get" + StringUtils.capitalize(field.getName()));
			}
		} catch (Exception e) {
			log.warn("取" + clazz + "属性" + field.getName() + "的get方法异常", e);
		}
		return method;
	}

	private static Method setMethod(Class<?> clazz, Field field) {
		Method method = null;
		try {
			if (field.getType() == Boolean.class && field.getName().startsWith("is")) {
				method = clazz.getMethod(field.getName().substring(2), Boolean.class);
				return method;
			} else {
				method = clazz.getMethod("set" + StringUtils.capitalize(field.getName()), field.getType());
			}
		} catch (Exception e) {
			log.warn("取" + clazz + "属性" + field.getName() + "的set方法异常", e);
		}
		return method;
	}

	public static Object bean2Map(Object obj) {
		if (obj == null) {
			return null;
		}
		if (isBaseType(obj))
			return obj;
		Map<String, Object> map = new HashMap<>();
		try {
			Map<String, Pair<Method, Method>> getsetMethods = extractMethods(obj.getClass());
			for (Entry<String, Pair<Method, Method>> entry : getsetMethods.entrySet()) {
				Object prrpValue = entry.getValue().getLeft().invoke(obj);
				if (prrpValue != null) {
					if (isBaseType(prrpValue)) {
						map.put(entry.getKey(), prrpValue);
					} else if (prrpValue instanceof List) {// list属性的处理
						List list2 = new ArrayList<>();
						for (Object inobj : (List) prrpValue) {
							list2.add(bean2Map(inobj));
						}
						map.put(entry.getKey(), list2);
					} else {
						map.put(entry.getKey(), bean2Map(prrpValue));
					}
				}
			}
			return map;
		} catch (Exception e) {
			log.warn("transMap2Bean Error ", e);
		}
		return null;
	}

	public static <T> Object map2Bean(Map<String, Object> map, Class<T> clz) {
		try {
			T t = clz.newInstance();
			Map<String, Pair<Method, Method>> getsetMethods = extractMethods(clz);
			for (Entry<String, Pair<Method, Method>> entry : getsetMethods.entrySet()) {
				String key = entry.getKey();
				if (map.containsKey(key)) {
					Object value = map.get(key);
					if (value != null) {
						Method setter = entry.getValue().getRight();
						if (value instanceof List) {
							ArrayList list = new ArrayList();
							for (Object ol : (List) value) {
								list.add(ol);
							}
							setter.invoke(t, list);
						} else {

							setter.invoke(t, value);
						}
					}
				}
			}
			return t;
		} catch (Exception e) {

			log.warn("transMap2Bean Error ", e);
		}
		return null;
	}

	static Map<Class<?>, List<Field>> classFields = new HashMap<>();

	private static List<Field> allDeclaredField(Class<?> clazz) {
		List<Field> fieldList = classFields.get(clazz);
		if (fieldList == null) {
			String uniqueName = clazz.getClassLoader().getClass().getName() + clazz.getName();
			synchronized (uniqueName.intern()) {
				if (fieldList == null) {
					fieldList = new ArrayList<>();
					Class<?> targetClass = clazz;
					do {
						Field[] fields = targetClass.getDeclaredFields();
						for (Field f : fields) {
							if (Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers())) {
							} else {
								fieldList.add(f);
							}
						}
						targetClass = targetClass.getSuperclass();
					} while (targetClass != null && targetClass != Object.class);
					classFields.put(clazz, fieldList);
				}
			}
		}
		return fieldList;
	}

	private static boolean isBaseType(Object value) {
		if (value instanceof String || value instanceof Integer || value instanceof Double || value instanceof Short || value instanceof Byte
				|| value instanceof Float || value instanceof Boolean || value instanceof Character || value instanceof Long || value instanceof BigDecimal
				|| value instanceof java.util.Date || value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
			return true;
		}
		return false;
	}

	private static boolean isBaseType(Class<?> clazz) {
		if (clazz == String.class || clazz == Integer.class || clazz == Double.class || clazz == Short.class || clazz == Byte.class || clazz == Float.class
				|| clazz == Boolean.class || clazz == Character.class || clazz == Long.class || clazz.isPrimitive() || clazz == BigDecimal.class
				|| clazz == java.util.Date.class || clazz == java.sql.Date.class || clazz == java.sql.Timestamp.class) {
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		Object s1 = SerializerUtil.serialize(123);
		System.out.println((int) s1);
		Object ds1 = SerializerUtil.deserialize(s1, Integer.class);
		System.out.println(ds1);
	}
}