package onight.ojpa.redis.loader;

import java.util.ArrayList;
import java.util.List;

import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.annotations.Tab;

//import org.springframework.transaction.annotation.Transactional;

@Tab(name = "redisBatch")
public class BatchDao extends DaoRedisImpl {

	public BatchDao(RedisConnector redis, OJpaDAO jdao) {
		super(redis, jdao);
	}

	@Override
	public List<Object> selectByExample(Object example) {
		Object ret = super.doBySQL((String) example);
		List<Object> retarr = new ArrayList<Object>();
		if (ret != null) {
			retarr.add(ret);
		}
		return retarr;
	}
}
