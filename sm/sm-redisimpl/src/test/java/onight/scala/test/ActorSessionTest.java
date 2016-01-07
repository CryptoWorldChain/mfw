package onight.scala.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.codehaus.jackson.annotate.JsonIgnore;

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
	Collection<Object> kvs_values;
	int status;
	
	List<Integer> command;
	
	@JsonIgnore
	ConcurrentHashMap<String, Object> kvs = new ConcurrentHashMap<String, Object>();

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
