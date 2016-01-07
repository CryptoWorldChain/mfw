package onight.tfw.otransio.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActorSession {
	String smid;
	String loginId;
	String resId;// 资源id
	
	long lastUpdateMS;
	long loginMS;
	String status;
//	int maxInactiveInterval;
	Map<String, Object> kvs = new ConcurrentHashMap<String, Object>();
	
	boolean validate = true;

	public Object getAttribute(String name) {
		return kvs.get(name);
	}

	public Object setAttribute(String key, Object value) {
		return kvs.put(key, value);
	}

	public Object removeAttribute(String name) {
		return kvs.get(name);
	}

	public void invalidate() {
		validate = false;
	}
}
