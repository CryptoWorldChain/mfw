package onight.tfw.ojpa.api;

import java.util.List;

import onight.tfw.ojpa.api.exception.JPAException;

/**
 * 参数说明：<br>
 * domains: 设置查询类型，表名等信息<br>
 * entity: 序列化后的数据<br>
 * 
 */
public  interface ORMappingDao<T> extends DomainDaoSupport {

	abstract int countByExample(Object example);

	abstract int deleteByExample(Object example);

	abstract int deleteByPrimaryKey(Object key);

	abstract int insert(Object record);

	abstract int insertSelective(Object record);

	abstract int batchInsert(List<Object> records);

	abstract int batchUpdate(List<Object> records);

	abstract int batchDelete(List<Object> records);

	abstract List<Object> selectByExample(Object example);

	abstract Object selectByPrimaryKey(Object key);

	abstract Object selectOneByExample(Object key);

	abstract List<Object> findAll(List<Object> records);

	int updateByExampleSelective(Object record, Object example);

	int updateByExample(Object record, Object example);

	int updateByPrimaryKeySelective(Object record);

	int updateByPrimaryKey(Object record);

	int sumByExample(Object example);

	void deleteAll();

	Object getExample(Object record);

	/**
	 * @param domain
	 * @param sql
	 * @return
	 * @throws JPAException
	 * @
	 */
//	Object doBySQL(String sql) throws JPAException;

	/**
	 * 
	 * @param sch
	 * @return
	 * @throws JPAException
	 */
	Object doInTransaction(TransactionExecutor exec) throws JPAException;

	/**
	 * 不存在是插入进去
	 * 
	 * @param dc
	 * @param entity
	 * @return
	 * @throws JPAException
	 */
	Object insertIfNoExist(Object entity) throws JPAException;

	/**
	 * 给某个计数器添加
	 * 
	 * @param dc
	 * @param counter
	 * @return
	 * @throws JPAException
	 */
	Object increAnGetInt(CASCriteria<?> counterCri) throws JPAException;

	/**
	 * 先检查然后修改
	 * 
	 * @param dc
	 * @param counterCri
	 * @return
	 * @throws JPAException
	 */
	Object checkAndIncr(CASCriteria<?> counterCri) throws JPAException;

	
	/**
	 * 先检查然后set
	 * 
	 * @param dc
	 * @param counterCri
	 * @return
	 * @throws JPAException
	 */
	Object checkAndSet(CASCriteria<?> counterCri) throws JPAException;

	/**
	 * 插入新的值，返回旧值
	 * 
	 * @param dc
	 * @param counterCri
	 * @return
	 * @throws JPAException
	 */
	Object getAndSet(Object record) throws JPAException;

}
