package onight.tfw.ojpa.api;

import java.util.List;

import onight.tfw.ojpa.api.exception.JPAException;

/**
 * 参数说明：<br>
 * domains: 设置查询类型，表名等信息<br>
 * entity: 序列化后的数据<br>
 * 
 */
public interface DomainDaoSupport<T> {

	int countByExample(Object example);

	int deleteByExample(Object example);

	int deleteByPrimaryKey(Object key);

	int insert(Object record);

	int insertSelective(Object record);

	int batchInsert(List<Object> records);

	int batchUpdate(List<Object> records);

	int batchDelete(List<Object> records);

	List<Object> selectByExample(Object example);

	Object selectByPrimaryKey(Object key);

	Object selectOneByExample(Object key);

	List<Object> findAll(List<Object> records);

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
	 * @param exec
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
