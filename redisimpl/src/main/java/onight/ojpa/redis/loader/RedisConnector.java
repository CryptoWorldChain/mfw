package onight.ojpa.redis.loader;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
public class RedisConnector  {

	RedisTemplate<String, Object> template;
	LettuceConnectionFactory factory;

	
//	public RedisConnectionFactory jRedisFactory(String addr,int port,int poolsize) {
//		JedisPoolConfig poolConfig=new JedisPoolConfig();
//		poolConfig.setMaxTotal(poolsize);
//		JedisConnectionFactory lfactory=new JedisConnectionFactory(poolConfig);
//		lfactory.setHostName(addr);
//		lfactory.setPort(port);
//		lfactory.setUsePool(true);
////		LettuceConnectionFactory lfactory = new LettuceConnectionFactory(addr, port);
////		lfactory.setShareNativeConnection(true);
//		
//		lfactory.afterPropertiesSet();
////		lfactory.initConnection();
//		return lfactory;
//	}
	
	public LettuceConnectionFactory jRedisFactory(String addr,int port,int poolsize) {
//		LettuceConnectionFactory lfactory=new LettuceConnectionFactory();
//		lfactory.setHostName(addr);
//		lfactory.setPort(port);
//		lfactory.setUsePool(true);
		LettuceConnectionFactory lfactory = new LettuceConnectionFactory(addr, port);
		lfactory.setShareNativeConnection(true);
		lfactory.afterPropertiesSet();
		lfactory.initConnection();
		return lfactory;
	}
	

	public void onStart(String addr,int port,int poolsize) {
		log.info("Redis启动...");
		factory = jRedisFactory(addr,port,poolsize);
	
		template = new FastRedisTemplate<String, Object>();
		template.setConnectionFactory(factory);
		template.afterPropertiesSet();
		long size = exec(new RedisCallback<Long>() {
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				Long size = connection.dbSize();
				return size;
			}
		});
		log.info("Redis启动成功,存储个数：" + size);

	}

	public <T> T exec(RedisCallback<T> callback) {
		return template.execute(callback);
	}

	public void onDestory() {
		log.info("Redis退出...");
		template.discard();
		factory.destroy();
		log.info("Redis退出成功");
	}
	
}
