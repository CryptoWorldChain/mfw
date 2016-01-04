package onight.tfw.ojpa.api;

import java.util.List;

import onight.tfw.ojpa.api.exception.JPAException;

/**
 * 参数说明：<br>
 * domains: 设置查询类型，表名等信息<br>
 * entity: 序列化后的数据<br>
 * 
 */
public interface SpecDomainDaoSupport<T> {

	/**
	 * @param dc
	 * @param entity
	 *            pojoJson
	 * @throws JPAException
	 */
	Object insert(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * @param dc
	 * @param entities
	 *            serialize list
	 * @throws JPAException
	 */
	Object batchInsert(ServiceSpec dc, List<T> entities) throws JPAException;

	/**
	 * @param dc
	 * @param entity
	 *            pojoJson
	 * @throws JPAException
	 */
	Object update(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * @param dc
	 * @param entities
	 *            serialize list
	 * @throws JPAException
	 */
	Object batchUpdate(ServiceSpec dc, List<T> entities) throws JPAException;

	/**
	 * 
	 * @param dc
	 * @param entity
	 *            ID
	 * @return pojoJson
	 * @throws JPAException
	 */
	Object findOne(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * @param dc
	 * @param entity
	 * @return
	 * @throws JPAException
	 */
	Object findByExample(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * @param dc
	 * @param entities
	 *            serialize list
	 * @return ListJson
	 * @throws JPAException
	 */
	Object findAll(ServiceSpec dc, List<T> entities) throws JPAException;

	/**
	 * 
	 * @param dc
	 * @param entity
	 *            pojoJson
	 * @return boolean
	 * @throws JPAException
	 */
	Object exists(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * 
	 * @param dc
	 * @throws JPAException
	 */
	Object count(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * @param dc
	 * @param entities
	 *            serialize list
	 * @throws JPAException
	 */
	Object delete(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * @param dc
	 * @param entities
	 *            serialize list
	 * @throws JPAException
	 */
	Object batchDelete(ServiceSpec dc, List<T> entities) throws JPAException;

	/**
	 * @param dc
	 * @throws JPAException
	 */
	Object deleteAll(ServiceSpec dc) throws JPAException;

	/**
	 * @param domain
	 * @param sql
	 * @return
	 * @throws JPAException
	 */
	Object doBySQL(ServiceSpec dc, String sql) throws JPAException;

	/**
	 * 不存在是插入进去
	 * 
	 * @param dc
	 * @param entity
	 * @return
	 * @throws JPAException
	 */
	Object insertIfNoExist(ServiceSpec dc, T entity) throws JPAException;

	/**
	 * 给某个计数器添加
	 * 
	 * @param dc
	 * @param counter
	 * @return
	 * @throws JPAException
	 */
	Object increAnGetInt(ServiceSpec dc, CASCriteria<?> counterCri)
			throws JPAException;

	/**
	 * 先检查然后修改
	 * @param dc
	 * @param counterCri
	 * @return
	 * @throws JPAException
	 */
	Object checkAndIncr(ServiceSpec dc, CASCriteria<?> counterCri)
			throws JPAException;

}
