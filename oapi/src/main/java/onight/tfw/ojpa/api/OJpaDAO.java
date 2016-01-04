package onight.tfw.ojpa.api;

import java.lang.reflect.Method;
import java.util.List;

import lombok.Data;
import onight.tfw.ojpa.api.exception.JPAException;
import onight.tfw.outils.serialize.SerializerUtil;

@Data
public class OJpaDAO<T> implements DomainDaoSupport<T> {
	private ServiceSpec serviceSpec;
	private String domainName;
	private Class<T> domainClazz;

	private Class exampleClazz;
	private Class keyClazz;

	private DomainDaoSupport daosupport;
	private String keyField;
	private List<Method> keyMethods;

	@Override
	public int countByExample(Object example) {
		return daosupport.countByExample(SerializerUtil.serialize(example));
	}

	@Override
	public int deleteByExample(Object example) {
		return daosupport.deleteByExample(SerializerUtil.serialize(example));
	}

	@Override
	public int deleteByPrimaryKey(Object key) {
		return daosupport.deleteByPrimaryKey(SerializerUtil.serialize(key));
	}

	@Override
	public int insert(Object record) {
		return daosupport.insert(SerializerUtil.serialize(record));
	}

	@Override
	public int insertSelective(Object record) {
		return daosupport.insertSelective(SerializerUtil.serialize(record));
	}

	@Override
	public int batchInsert(List<Object> records) {
		return daosupport.batchInsert((List<Object>) SerializerUtil.serializeArray(records));
	}

	@Override
	public int batchUpdate(List<Object> records) {
		return daosupport.batchUpdate((List<Object>) SerializerUtil.serializeArray(records));
	}

	@Override
	public int batchDelete(List<Object> records) {
		return daosupport.batchDelete((List<Object>) SerializerUtil.serializeArray(records));
	}

	@Override
	public List<Object> selectByExample(Object example) {
		return (List<Object>) SerializerUtil.deserializeArray(daosupport.selectByExample(SerializerUtil.serialize(example)), domainClazz);
	}

	@Override
	public T selectByPrimaryKey(Object key) {
		return SerializerUtil.deserialize(daosupport.selectByPrimaryKey(SerializerUtil.serialize(key)), domainClazz);
	}

	@Override
	public List<Object> findAll(List<Object> records) {
		return (List<Object>) SerializerUtil.deserializeArray(daosupport.findAll((List<Object>) SerializerUtil.serializeArray(records)), domainClazz);
	}

	@Override
	public int updateByExampleSelective(Object record, Object example) {
		return daosupport.updateByExampleSelective(SerializerUtil.serialize(record), SerializerUtil.serialize(example));
	}

	@Override
	public int updateByExample(Object record, Object example) {
		return daosupport.updateByExample(SerializerUtil.serialize(record), SerializerUtil.serialize(example));
	}

	@Override
	public int updateByPrimaryKeySelective(Object record) {
		return daosupport.updateByPrimaryKeySelective(SerializerUtil.serialize(record));
	}

	@Override
	public int updateByPrimaryKey(Object record) {
		return daosupport.updateByPrimaryKey(SerializerUtil.serialize(record));
	}

	@Override
	public int sumByExample(Object example) {
		return daosupport.sumByExample(SerializerUtil.serialize(example));
	}

	@Override
	public void deleteAll() {
		daosupport.deleteAll();

	}

	@Override
	public Object getExample(Object record) {
		return SerializerUtil.deserialize(daosupport.getExample(SerializerUtil.serialize(record)), exampleClazz);
	}

	// @Override
	// public Object doBySQL(String sql) throws JPAException {
	// return daosupport.doBySQL(sql);
	// }

	@Override
	public Object insertIfNoExist(Object entity) throws JPAException {
		return daosupport.insertIfNoExist(SerializerUtil.serialize(entity));
	}

	@Override
	public Object increAnGetInt(CASCriteria<?> counterCri) throws JPAException {
		return daosupport.increAnGetInt(counterCri);
	}

	@Override
	public Object checkAndIncr(CASCriteria<?> counterCri) throws JPAException {
		return daosupport.checkAndIncr(counterCri);
	}

	public OJpaDAO(ServiceSpec serviceSpec, Class domainClazz, Class exampleClazz, Class keyClazz) {
		super();
		this.serviceSpec = serviceSpec;
		this.domainName = domainClazz.getSimpleName().toLowerCase();
		this.domainClazz = domainClazz;
		this.exampleClazz = exampleClazz;
		this.keyClazz = keyClazz;
	}

	public OJpaDAO() {
		super();
		this.serviceSpec = ServiceSpec.MYSQL_STORE;
	}

	@Override
	public Object doInTransaction(TransactionExecutor exec) throws JPAException {
		return daosupport.doInTransaction(exec);
	}

	@Override
	public Object selectOneByExample(Object key) {
		return daosupport.selectOneByExample(key);
	}

	@Override
	public Object checkAndSet(CASCriteria<?> counterCri) throws JPAException {
		return daosupport.checkAndSet(counterCri);
	}

	@Override
	public T getAndSet(Object record) {
		Object oldv = daosupport.getAndSet(SerializerUtil.serialize(record));
		if (oldv != null) {
			return SerializerUtil.deserialize(oldv, domainClazz);
		}
		return null;
	}
}
