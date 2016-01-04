package onight.tfw.mservice;

import java.util.HashMap;

public class ThreadContext {

	static ThreadLocal<HashMap<String, Object>> context = new ThreadLocal<>();

	public static void cleanContext() {
		ensureMap().clear();
	}

	public static void setContext(String key, Object object) {
		ensureMap().put(key, object);
	}

	public static HashMap<String, Object> ensureMap() {
		HashMap<String, Object> map = context.get();
		if (map == null) {
			map = new HashMap<String, Object>();
			context.set(map);
		}
		return map;
	}

	public static Object getContext(String key) {
		return ensureMap().get(key);
	}

	public static int getContextInt(String key, int defaultv) {
		try {
			Object v = ensureMap().get(key);
			if(v==null){
				return defaultv;
			}
			if(v instanceof Integer){
				return (Integer)v;
			}
			if(v instanceof Long){
				return ((Long)v).intValue();
			}
			if(v instanceof String){
				return Integer.parseInt((String)v);
			}
		} catch (Exception e) {
		}
		return defaultv;
	}

	public static String getContextStr(String key, String defaultv) {
		try {
			Object v =  ensureMap().get(key);
			if (v != null && v instanceof String) {
				return (String)v;
			}
			return defaultv;
		} catch (Exception e) {
		}
		return defaultv;
	}
}
