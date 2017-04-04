package onight.tfw.ojpa.api;

import java.util.List;

import onight.tfw.ojpa.api.exception.JPAException;
import onight.tfw.ojpa.api.exception.NotSuportException;

/**
 * 参数说明：<br>
 * domains: 设置查询类型，表名等信息<br>
 * entity: 序列化后的数据<br>
 * 
 */
public class NoneDomainDao implements ORMappingDao<Object> {

	@Override
	public int countByExample(Object example) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int deleteByExample(Object example) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int deleteByPrimaryKey(Object key) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int insert(Object record) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int insertSelective(Object record) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int batchInsert(List<Object> records) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int batchUpdate(List<Object> records) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int batchDelete(List<Object> records) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public List<Object> selectByExample(Object example) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public Object selectByPrimaryKey(Object key) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public List<Object> findAll(List<Object> records) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int updateByExampleSelective(Object record, Object example) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int updateByExample(Object record, Object example) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int updateByPrimaryKeySelective(Object record) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int updateByPrimaryKey(Object record) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public int sumByExample(Object example) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public void deleteAll() {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public Object getExample(Object record) {
		throw new NotSuportException("DaoNotFound");
	}

	// @Override
	// public Object doBySQL(String sql) throws JPAException {
	// throw new NotSuportException("DaoNotFound");
	// }

	@Override
	public Object insertIfNoExist(Object entity) throws JPAException {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public Object increAnGetInt(CASCriteria<?> counterCri) throws JPAException {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public Object checkAndIncr(CASCriteria<?> counterCri) throws JPAException {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public Object doInTransaction(TransactionExecutor exec) throws JPAException {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public Object selectOneByExample(Object key) {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public Object checkAndSet(CASCriteria<?> counterCri) throws JPAException {
		throw new NotSuportException("DaoNotFound");
	}
	
	@Override
	public Object getAndSet(Object object) throws JPAException {
		throw new NotSuportException("DaoNotFound");
	}

	@Override
	public void setDaosupport(DomainDaoSupport support) {
		
	}

	@Override
	public DomainDaoSupport getDaosupport() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceSpec getServiceSpec() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDomainName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getDomainClazz() {
		// TODO Auto-generated method stub
		return null;
	}

}
