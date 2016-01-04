package onight.tfw.outils.bean;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ClassUtils {

	public static Type[] getParameterizedType(Class clazz) {
		try {
			ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericSuperclass();
			return parameterizedType.getActualTypeArguments();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Type getParameterizedType(Class clazz, int index) {
		Type[] types = getParameterizedType(clazz);
		if (types != null && types.length > 0 && types.length < index) {
			return types[index];
		}
		return null;
	}

	public static Class getParameterizedClass(Class clazz, int index) {
		Type[] types = getParameterizedType(clazz);
		if (types != null && types.length > 0 && index < types.length) {
			try {
				return (Class) types[index];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Class getFirstParameterizedClass(Class clazz) {
		return getParameterizedClass(clazz, 0);
	}

	public static Type getFirstParameterizedType(Class clazz) {
		return getParameterizedType(clazz, 0);
	}
}
