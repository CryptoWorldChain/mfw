package onight.ojpa.redis.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.mservice.ThreadContext;
import onight.tfw.ojpa.api.CASCriteria;
import onight.tfw.ojpa.api.JpaContextConstants;
import onight.tfw.ojpa.api.KVExample;
import onight.tfw.ojpa.api.NoneDomainDao;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.TransactionExecutor;
import onight.tfw.ojpa.api.exception.JPAException;
import onight.tfw.outils.serialize.SerializerUtil;
import onight.tfw.outils.serialize.TransBeanSerializer;
import onight.tfw.outils.serialize.TransBeanSerializer.BeanMap;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;

@Slf4j
public class DaoRedisImpl extends NoneDomainDao {

	RedisConnector redis;
	OJpaDAO jdao;

	public DaoRedisImpl(RedisConnector redis, OJpaDAO jdao) {
		super();
		this.redis = redis;
		this.jdao = jdao;
	}

	protected String localKey(Object key) {
		if (key instanceof Map) {
			Map<String, Object> mb = (HashMap<String, Object>) key;
			StringBuffer sb = new StringBuffer(jdao.getDomainName()).append(":");
			for (String skey : jdao.getKeyField().split(",")) {
				if (sb.length() > 0)
					sb.append("-");
				sb.append(mb.get(skey));
			}
			return sb.toString();
		}
		return String.valueOf(key);
	}

	class KV {
		byte[] _1;
		Map<byte[], byte[]> _2;

		public KV(byte[] _1, Map<byte[], byte[]> _2) {
			super();
			this._1 = _1;
			this._2 = _2;
		}

	}

	static byte[] EXIST_FIELD = "__".getBytes();
	static byte[] EXIST_VALUE = StringType.toTBytes(1);

	
	protected KV kv(Object entity) {
		HashMap<byte[], byte[]> map = new HashMap<byte[], byte[]>();
		map.put(EXIST_FIELD, EXIST_VALUE);
		if (entity instanceof HashMap) {
			BeanMap<String,Object> hmap = (BeanMap<String,Object>) entity;
			for (Map.Entry<String,Object> entry :  hmap.entrySet()) {
				map.put(entry.getKey().getBytes(), StringType.toTBytes(entry.getValue()));
			}
		} else {
			map.put(new byte[] { 0x01 }, StringType.toTBytes(entity));
		}
		return new KV(localKey(entity).getBytes(), map);
	}

	protected KVExample localExmaple(Object entity) {
		try {
			return SerializerUtil.deserialize(entity, KVExample.class);
		} catch (NullPointerException e) {
			throw new JPAException(String.format("domain names is error.. [%s]", KVExample.class), e);
		}
	}

	@Override
	public int countByExample(final Object example) {
		return redis.exec(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection) throws DataAccessException {
				KVExample kvexm = localExmaple(example);
				int size = 0;
				for (Object cri : kvexm.getCriterias()) {
					size += connection.keys(((String) cri).getBytes()).size();
				}
				return size;
			}
		});

	}

	@Override
	public int deleteByExample(final Object example) {
		return redis.exec(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection) throws DataAccessException {
				KVExample kvexm = localExmaple(example);
				int size = 0;
				if (kvexm.getCriterias().size() == 0)
					return 0;
				for (Object cri : kvexm.getCriterias()) {
					size += connection.del(((String) cri).getBytes());
				}
				return size;
			}
		});
	}

	@Override
	public int deleteByPrimaryKey(final Object key) {
		return redis.exec(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection) throws DataAccessException {
				int size = 0;
				size += connection.del(localKey(key).getBytes());
				return size;
			}
		});
	}

	@Override
	public int insert(final Object record) {
		return redis.exec(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection) throws DataAccessException {
				KV kv = kv(record);
				connection.hMSet(kv._1, kv._2);
				int ll = ThreadContext.getContextInt(JpaContextConstants.Cache_Timeout_Second, -1);
				if (ll > 0) {
					connection.expire(kv._1, ll);
				}
				return 1;
			}
		});
	}

	public BeanMap<String, Object> byteFieldMaps(Map<byte[], byte[]> bmap) {
		if(bmap==null||bmap.size()==0)return null;
		BeanMap<String, Object> retmapper = new BeanMap<String, Object>();
		for (Entry<byte[], byte[]> entry : bmap.entrySet()) {
			retmapper.put(new String(entry.getKey()), StringType.toTObject(entry.getValue()));
		}
		return retmapper;
	}

	@Override
	public Object getAndSet(final Object record) {
		return redis.exec(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				KV kv = kv(record);
				int ll = ThreadContext.getContextInt(JpaContextConstants.Cache_Timeout_Second, -1);
				for (int i = 0; i < ThreadContext.getContextInt("_redis_wait_timems", 30 * 1000) / 10; i++) {// 30秒钟
					if (i > 0) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
					}
					connection.watch(kv._1);
					connection.multi();
					connection.hGetAll(kv._1);
					connection.hMSet(kv._1, kv._2);
					if(ll>0)
					{
						connection.expire(kv._1, ll);
					}
					List<Object> ret = connection.exec();
					if (ret != null && ret.size() > 0) {
						return byteFieldMaps((Map<byte[], byte[]>) ret.get(0));
					}
				}
				throw new JPAException("checkAndSet Failed for Timeout");
			}
		});
	}

	@Override
	public int insertSelective(Object record) {
		return insert(record);
	}

	@Override
	public int batchInsert(final List<Object> records) {
		return redis.exec(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection) throws DataAccessException {
				int ll = ThreadContext.getContextInt(JpaContextConstants.Cache_Timeout_Second, -1);
				for (Object record : records) {
					KV kv = kv(record);
					connection.hMSet(kv._1, kv._2);
					if (ll > 0) {
						connection.expire(kv._1, ll);
					}
				}
				return records.size();
			}
		});
	}

	@Override
	public int batchUpdate(List<Object> records) {
		return batchInsert(records);
	}

	@Override
	public int batchDelete(final List<Object> records) {
		return redis.exec(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection) throws DataAccessException {
				int size = 0;
				for (Object record : records) {
					size += connection.del(localKey(record).getBytes());
				}
				return size;
			}
		});
	}

	@Override
	public List<Object> selectByExample(final Object example) {
		return redis.exec(new RedisCallback<List<Object>>() {
			public List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
				KVExample kvexm = localExmaple(example);
				ArrayList<Object> ret = new ArrayList<Object>();
				for (Object cri : kvexm.getCriterias()) {
					ret.add(byteFieldMaps(connection.hGetAll(((String) cri).getBytes())));
				}
				return ret;
			}
		});
	}

	@Override
	public Object selectByPrimaryKey(final Object key) {
		return redis.exec(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				return byteFieldMaps(connection.hGetAll(localKey(key).getBytes()));
			}
		});
	}

	@Override
	public List<Object> findAll(final List<Object> records) {
		return redis.exec(new RedisCallback<List<Object>>() {
			public List<Object> doInRedis(RedisConnection connection) throws DataAccessException {
				ArrayList<Object> ret = new ArrayList<Object>();
				for (Object record : records) {
					byte[] objb = connection.get(localKey(record).getBytes());
					Object recordr = SerializerUtil.fromBytes(objb);
					ret.add(recordr);
				}
				return ret;
			}
		});
	}

	@Override
	public int updateByPrimaryKeySelective(Object record) {
		return insert(record);
	}

	@Override
	public int updateByPrimaryKey(Object record) {
		return insert(record);
	}

	@Override
	public int sumByExample(final Object example) {
		return redis.exec(new RedisCallback<Integer>() {
			public Integer doInRedis(RedisConnection connection) throws DataAccessException {
				KVExample kvexm = localExmaple(example);
				int size = 0;
				for (Object cri : kvexm.getCriterias()) {
					size += connection.keys(((String) cri).getBytes()).size();
				}
				return size;
			}
		});
	}

	// @Override
	public Object doBySQL(final String sql) throws JPAException {
		return redis.exec(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				return connection.execute(sql);
			}
		});
	}

	@Override
	public Object insertIfNoExist(final Object record) throws JPAException {
		return redis.exec(new RedisCallback<Boolean>() {
			public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
				KV kv = kv(record);
				int ll = ThreadContext.getContextInt(JpaContextConstants.Cache_Timeout_Second, -1);
				if (connection.hSetNX(kv._1, EXIST_FIELD, EXIST_VALUE)) {
					if (ll > 0) {
						connection.hMSet(kv._1, kv._2);
						connection.expire(kv._1, ll);
					}
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public Object increAnGetInt(final CASCriteria<?> counterCri) throws JPAException {
		return redis.exec(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				if (counterCri.getIncrements() instanceof Integer) {
					return connection.hIncrBy(counterCri.getRowkey().getBytes(), counterCri.getColumn().getBytes(),
							Integer.parseInt(counterCri.getIncrements().toString()));
				} else if (counterCri.getIncrements() instanceof Float) {
					return connection.hIncrBy(counterCri.getRowkey().getBytes(), counterCri.getColumn().getBytes(), Float.parseFloat(counterCri.getIncrements().toString()));
				} else if (counterCri.getIncrements() instanceof Double) {
					return connection.hIncrBy(counterCri.getRowkey().getBytes(), counterCri.getColumn().getBytes(), Double.parseDouble(counterCri.getIncrements().toString()));
				}
				throw new JPAException("unknow incremen type:" + counterCri.getIncrements());
			}
		});
	}

	@Override
	public Object checkAndIncr(final CASCriteria<?> counterCri) throws JPAException {

		return redis.exec(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.watch(counterCri.getRowkey().getBytes());
				connection.multi();
				byte[] obj = connection.get(counterCri.getRowkey().getBytes());
				if (counterCri.getCause() != null && !counterCri.getCause().isOK(obj)) {
					connection.discard();
					return null;
				}
				if (counterCri.getIncrements() instanceof Integer) {
					connection.hIncrBy(counterCri.getRowkey().getBytes(), counterCri.getColumn().getBytes(), Integer.parseInt(counterCri.getIncrements().toString()));
				} else if (counterCri.getIncrements() instanceof Float) {
					connection.hIncrBy(counterCri.getRowkey().getBytes(), counterCri.getColumn().getBytes(), Float.parseFloat(counterCri.getIncrements().toString()));
				} else if (counterCri.getIncrements() instanceof Double) {
					connection.hIncrBy(counterCri.getRowkey().getBytes(), counterCri.getColumn().getBytes(), Double.parseDouble(counterCri.getIncrements().toString()));
				} else {
					connection.discard();
					return null;
				}
				return connection.exec();
			}
		});
		//
		// WATCH mykey
		// val = GET mykey
		// val = val + 1
		// MULTI
		// SET mykey $val
		// EXEC
	}

	@Override
	public Object doInTransaction(final TransactionExecutor exec) throws JPAException {
		return redis.exec(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				connection.multi();
				Object ret = null;
				try {
					ret = exec.doInTransaction();
					connection.exec();
				} catch (Exception e) {
					e.printStackTrace();
					log.error("doInTransaction:" + exec, e);
					connection.discard();
					throw e;
				}
				return ret;
			}
		});
	}

	@Override
	public Object checkAndSet(final CASCriteria<?> counterCri) throws JPAException {
		return redis.exec(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				Object ret = null;
				KV kv = kv(counterCri.getIncrements());
				int ll = ThreadContext.getContextInt(JpaContextConstants.Cache_Timeout_Second, -1);
				for (int i = 0; i < ThreadContext.getContextInt("_redis_wait_timems", 30 * 1000) / 10; i++) {// 30秒钟
					if (i > 0) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
					}
					connection.watch(kv._1);
					connection.multi();
					byte[] objb = connection.get(kv._1);
					Object obj = SerializerUtil.fromBytes(objb);
					if (counterCri.getCause() != null && !counterCri.getCause().isOK(obj)) {
						connection.discard();
						return null;
					}
					connection.hMSet(kv._1, kv._2);
					if (ll > 0) {
						connection.expire(kv._1, ll);
					}
					ret = connection.exec();
					if (ret != null)
						return ret;
				}
				throw new JPAException("checkAndSet Failed for Timeout");
			}
		});
		//
		// WATCH mykey
		// val = GET mykey
		// val = val + 1
		// MULTI
		// SET mykey $val
		// EXEC
	}

}
