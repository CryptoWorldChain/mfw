package onight.ojpa.redis.loader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.data.redis.connection.RedisConnection;

public class CloseSuppressingInvocationHandler implements InvocationHandler {

	private static final String CLOSE = "close";
	private static final String HASH_CODE = "hashCode";
	private static final String EQUALS = "equals";

	private final RedisConnection target;

	public CloseSuppressingInvocationHandler(RedisConnection target) {
		this.target = target;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		if (method.getName().equals(EQUALS)) {
			// Only consider equal when proxies are identical.
			return (proxy == args[0]);
		} else if (method.getName().equals(HASH_CODE)) {
			// Use hashCode of PersistenceManager proxy.
			return System.identityHashCode(proxy);
		} else if (method.getName().equals(CLOSE)) {
			// Handle close method: suppress, not valid.
			return null;
		}

		// Invoke method on target RedisConnection.
		try {
			Object retVal = method.invoke(this.target, args);
			return retVal;
		} catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
	}
}
