package onight.ojpa.redis.loader;

import java.lang.reflect.Proxy;
import java.util.HashMap;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ClassUtils;

/**
 * spring 的redistemplate反射性能问题
 * 
 * @author brew
 *
 * @param <K>
 * @param <V>
 */
public class FastRedisTemplate<K, V> extends RedisTemplate<K, V> {

	HashMap<Class, Class<?>[]> cachedFaced = new HashMap<Class, Class<?>[]>();

	public FastRedisTemplate() {
		super();
		// TODO Auto-generated constructor stub
	}

	protected RedisConnection createRedisConnectionProxy(RedisConnection pm) {
		Class<?>[] ifcs = cachedFaced.get(pm.getClass());
		if (ifcs == null) {
			synchronized (cachedFaced) {
				ifcs = cachedFaced.get(pm.getClass());
				if (ifcs == null) {
					ifcs = ClassUtils.getAllInterfacesForClass(pm.getClass(), getClass().getClassLoader());
					cachedFaced.put(pm.getClass(), ifcs);
				}
			}
		}
		return (RedisConnection) Proxy.newProxyInstance(pm.getClass().getClassLoader(), ifcs, new CloseSuppressingInvocationHandler(pm));

		// return super.createRedisConnectionProxy(pm);
		// Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(pm.getClass(),
		// getClass().getClassLoader());
		// return (RedisConnection)
		// Proxy.newProxyInstance(pm.getClass().getClassLoader(), ifcs,
		// new CloseSuppressingInvocationHandler(pm));
	}

}
