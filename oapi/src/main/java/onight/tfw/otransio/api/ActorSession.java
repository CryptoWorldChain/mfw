package onight.tfw.otransio.api;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActorSession {

	long loginMS;
	String smid;
	long lastUpdateMS;
	int maxInactiveInterval;
	boolean validate = true;

	ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public Enumeration<String> getAttributeNames() {
		return attributes.keys();
	}

	public Object setAttribute(String key, Object value) {
		return attributes.put(key, value);
	}

	public Object removeAttribute(String name) {
		return attributes.get(name);
	}

	public void invalidate() {
		validate = false;
	}
}
