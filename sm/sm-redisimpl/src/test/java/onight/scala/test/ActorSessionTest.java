package onight.scala.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ActorSessionTest {

	long loginMS;
	String smid;
	long lastUpdateMS;
	int maxInactiveInterval;
	boolean validate = true;
	List<String> users;

	Token token;

	List<Token> tokens=new ArrayList<Token>();

	Set<String> kvs_keys;
	Collection<String> kvs_values;
	int status;
	
	List<Integer> command;
	
	ConcurrentHashMap<String, String> kvs = new ConcurrentHashMap<String, String>();

	public void mapkvs() {
		kvs_keys = kvs.keySet();
		kvs_values = kvs.values();
	}

	public Object getAttribute(String name) {
		return kvs.get(name);
	}

	public Enumeration<String> getAttributeNames() {
		return kvs.keys();
	}

	public Object setAttribute(String key, String value) {
		return kvs.put(key, value);
	}

	public Object removeAttribute(String name) {
		return kvs.get(name);
	}

	public void invalidate() {
		validate = false;
	}
}
