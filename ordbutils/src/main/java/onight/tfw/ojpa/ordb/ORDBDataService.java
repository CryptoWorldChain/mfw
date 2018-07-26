package onight.tfw.ojpa.ordb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;

import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.mservice.ThreadContext;
import onight.tfw.ojpa.api.CASCriteria;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.TransactionExecutor;
import onight.tfw.ojpa.api.exception.JPADuplicateIDException;
import onight.tfw.ojpa.api.exception.JPAException;

@Slf4j
@Data
public class ORDBDataService extends SerializedDomainDao {

	StaticTableDaoSupport dao;

	PlatformTransactionManager txManager;

	public ORDBDataService(ExtendDaoSupper daoSupport, PlatformTransactionManager txManager) {
		super(daoSupport);
		this.txManager = txManager;
		this.dao = daoSupport;
	}

	@Override
	public int countByExample(Object example) {
		return dao.countByExample(localExample(example));
	}

	@Override
	public int deleteByExample(Object example) {
		try {
			return dao.deleteByExample(localExample(example));
		} catch (Exception e) {
			throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int deleteByPrimaryKey(Object key) {
		try {
			return dao.deleteByPrimaryKey(localKey(key));
		} catch (Exception e) {
			throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int insert(Object record) {
		try {
			return dao.insert(localBean(record));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (DuplicateKeyException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage() != null && (e.getMessage().contains("MySQLIntegrityConstraintViolationException")
					|| e.getMessage().contains("SQLIntegrityConstraintViolationException") || e.getMessage().contains("ORA-00001: unique constraint"))) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public Object getAndSet(Object record) {
		try {
			Object oldv = dao.selectByPrimaryKey(localKey(record));
			dao.insert(localBean(record));
			if (oldv != null) {
				return serial(oldv);
			} else {
				return null;
			}
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage().contains("MySQLIntegrityConstraintViolationException")) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int insertSelective(Object record) {
		try {
			return dao.insertSelective(localBean(record));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (DuplicateKeyException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage() != null && (e.getMessage().contains("MySQLIntegrityConstraintViolationException")
					|| e.getMessage().contains("SQLIntegrityConstraintViolationException") || e.getMessage().contains("ORA-00001: unique constraint"))) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int batchInsert(List<Object> records) {
		try {
			return dao.batchInsert(localBean2List(records));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (DuplicateKeyException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage() != null && (e.getMessage().contains("MySQLIntegrityConstraintViolationException")
					|| e.getMessage().contains("SQLIntegrityConstraintViolationException") || e.getMessage().contains("ORA-00001: unique constraint"))) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int batchUpdate(List<Object> records) {	
		try {
			return dao.batchUpdate(localBean2List(records));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage() != null && (e.getMessage().contains("MySQLIntegrityConstraintViolationException")
					|| e.getMessage().contains("SQLIntegrityConstraintViolationException") || e.getMessage().contains("ORA-00001: unique constraint"))) {

				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int batchDelete(List<Object> records) {
		try {
			return dao.batchDelete(localBean2List(records));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage().contains("MySQLIntegrityConstraintViolationException")) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public List<Object> selectByExample(Object example) {
		return serial(dao.selectByExample(localExample(example)));
	}

	@Override
	public Object selectByPrimaryKey(Object key) {
		return serial(dao.selectByPrimaryKey(localKey(key)));
	}

	@Override
	public List<Object> findAll(List<Object> records) {
		return serial(dao.findAll(localBean2List(records)));
	}

	@Override
	public int updateByExampleSelective(Object record, Object example) {
		try {
			return dao.updateByExampleSelective(localBean(record), localExample(example));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage().contains("MySQLIntegrityConstraintViolationException")) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int updateByExample(Object record, Object example) {
		try {
			return dao.updateByExample(localBean(record), localExample(example));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage().contains("MySQLIntegrityConstraintViolationException")) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int updateByPrimaryKeySelective(Object record) {
		try {
			return dao.updateByPrimaryKeySelective(localBean(record));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage().contains("MySQLIntegrityConstraintViolationException")) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int updateByPrimaryKey(Object record) {
		try {
			return dao.updateByPrimaryKey(localBean(record));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage().contains("MySQLIntegrityConstraintViolationException")) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public int sumByExample(Object example) {
		return dao.countByExample(localExample(example));
	}

	@Override
	public void deleteAll() {
		try {
			dao.deleteAll();
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			throw new JPAException(e.getMessage());
		}
	}

	@Override
	public Object getExample(Object record) {
		return serial(dao.getExample(localBean(record)));
	}

	private boolean needTransaction(String sql) {
		if (StringUtils.containsIgnoreCase(sql, "INSERT") || StringUtils.containsIgnoreCase(sql, "UPDATE") || StringUtils.containsIgnoreCase(sql, "DELETE")) {
			return true;
		}
		return false;
	}

	public Object doBySQL(String sql) throws JPAException {
		List<Map> newMap = new ArrayList<>();
		List<HashMap> maps = null;
		if (needTransaction(sql)) {
			return doSqlByTransaction(sql);
		} else {
			maps = doSqlByNoTransaction(sql);
		}
		return maps;
	}

	private static HashMap<String, Object> getResultMap(ResultSet rs) throws SQLException {
		HashMap<String, Object> hm = new HashMap<String, Object>();
		ResultSetMetaData rsmd = rs.getMetaData();
		int count = rsmd.getColumnCount();
		for (int i = 1; i <= count; i++) {
			String key = rsmd.getColumnLabel(i);
			Object value = rs.getObject(i);
			hm.put(key, value);
		}
		return hm;
	}

	@SuppressWarnings("rawtypes")
	public int doSqlByTransaction(String sql) throws JPAException {

		TransactionStatus status = null;
		Connection txconn = (Connection) ThreadContext.getContext("__connection");
		Object ret = null;
		Connection conn = txconn;
		Statement st = null;
		JdbcTransactionObjectSupport txObject = null;
		try {

			if (txconn == null||conn==null||conn.isClosed()) {
				DefaultTransactionDefinition def = new DefaultTransactionDefinition();
				def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
				status = txManager.getTransaction(def);
				txObject = (JdbcTransactionObjectSupport) ((DefaultTransactionStatus) status).getTransaction();
				conn = txObject.getConnectionHolder().getConnection();
				conn.setAutoCommit(false);
				ThreadContext.setContext("__connection", conn);
			}

			st = conn.createStatement();
			int rs = st.executeUpdate(sql);
			log.debug("doSqlByTransaction=" + sql);
			if (txconn == null) {
				txManager.commit(status);
			}
			return rs;
		} catch (Exception e) {
			log.error("exception in execSql:" + sql, e);
			if (txconn == null) {
				try {
					txManager.rollback(status);
				} catch (Exception e1) {
					log.error("rollback error:", e);
				}
			}
			throw new JPAException(e);
		} finally {

			if (st != null) {
				try {
					st.close();
				} catch (SQLException e) {
				}
			}
			if (txconn == null) {
				ThreadContext.ensureMap().remove("zp."+"__connection");
			}
			if (txObject != null) {
				try {
					log.debug("release connection:");
					txObject.getConnectionHolder().released();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

	}

	public List<HashMap> result2List(ResultSet rs) throws SQLException {
		val ret = new ArrayList<HashMap>();
		while (rs.next()) {
			ret.add(getResultMap(rs));
		}
		return ret;
	}

	public List<HashMap> doSqlByNoTransaction(String sql) {
		SqlSession session = dao.getSqlSessionFactory().openSession();
		Connection conn = session.getConnection();
		Statement st = null;
		ResultSet rs = null;
		try {
			st = conn.createStatement();
			rs = st.executeQuery(sql);
			return result2List(rs);
		} catch (SQLException e) {
			log.error("exception in execSql:" + sql);
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			if (st != null) {
				try {
					st.close();
				} catch (SQLException e) {
				}
			}
			if (session != null)
				session.close();
		}
		return new ArrayList<HashMap>();
	}

	@Override
	public Object insertIfNoExist(Object entity) throws JPAException {
		try {
			return dao.insert(localBean(entity));
		} catch (MySQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (SQLIntegrityConstraintViolationException e) {
			throw new JPADuplicateIDException(e);
		} catch (DuplicateKeyException e) {
			throw new JPADuplicateIDException(e);
		} catch (Exception e) {
			if (e.getMessage() != null && (e.getMessage().contains("MySQLIntegrityConstraintViolationException")
					|| e.getMessage().contains("SQLIntegrityConstraintViolationException") || e.getMessage().contains("ORA-00001: unique constraint"))) {
				throw new JPADuplicateIDException(e);
			} else
				throw new JPAException(e.getMessage());
		}
	}

	@Override
	public Object increAnGetInt(CASCriteria<?> counterCri) throws JPAException {
		String tablename = counterCri.getTable();
		String colname = counterCri.getColumn();
		String sql = "UPDATE " + tablename + " SET " + colname + "=" + colname + "+" + counterCri.getIncrements() + " WHERE " + counterCri.getWhereCause();
		return doBySQL(sql);
	}

	public String transNames(String tb) {
		StringBuffer sb = new StringBuffer();
		for (char ch : tb.toCharArray()) {
			if (ch >= 'A' && ch <= 'Z' && sb.length() > 0) {
				sb.append("_");
			}
			sb.append(ch);
		}
		return sb.toString().toUpperCase();
	}

	@Override
	public Object checkAndIncr(CASCriteria<?> counterCri) throws JPAException {
		String tablename = counterCri.getTable();
		String colname = counterCri.getColumn();
		String sql = "UPDATE " + tablename + " SET " + colname + "=" + colname + "+" + counterCri.getIncrements() + " WHERE " + counterCri.getWhereCause();
		return doBySQL(sql);
	}

	@Override
	public Object doInTransaction(TransactionExecutor exec) throws JPAException {
		TransactionStatus status = null;
		Connection txconn = (Connection) ThreadContext.getContext("__connection");
		Object ret = null;
		Connection conn = txconn;
		JdbcTransactionObjectSupport txObject = null;
		try {

			if (txconn == null||conn==null||conn.isClosed()) {
				log.debug("recreate tx");
				DefaultTransactionDefinition def = new DefaultTransactionDefinition();
				def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
				status = txManager.getTransaction(def);
				txObject = (JdbcTransactionObjectSupport) ((DefaultTransactionStatus) status).getTransaction();
				conn = txObject.getConnectionHolder().getConnection();
				conn.setAutoCommit(false);
				ThreadContext.setContext("__connection", conn);
			}

			ret = exec.doInTransaction();
			if (ret != null && ret instanceof ResultSet) {
				ret = result2List((ResultSet) ret);
			}
			if (txconn == null) {
				txManager.commit(status);
			}
		} catch (JPAException je) {
			log.error("JPAException in execSql:" + exec, je);
			if (txconn == null) {

				try {
					txManager.rollback(status);
				} catch (Exception e1) {
					log.error("rollback error:", je);
				}
			}
			throw je;
		} catch (Exception e) {
			log.error("exception in execSql:" + exec, e);
			if (txconn == null) {

				try {
					txManager.rollback(status);
				} catch (Exception e1) {
					log.error("rollback error:", e);
				}
			}
			throw new JPAException(e);
		} finally {
			if (txconn == null) {
				ThreadContext.ensureMap().remove("zp."+"__connection");
			}
			if (txObject != null) {
				try {
					log.debug("release connection:");
					txObject.getConnectionHolder().released();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}

	@Override
	public Object selectOneByExample(Object example) {
		return serial(dao.selectOneByExample(localExample(example)));
		// return dao.selectOneByExample(example);
	}

	@Override
	public Object checkAndSet(CASCriteria<?> counterCri) throws JPAException {
		String tablename = counterCri.getTable();
		String colname = counterCri.getColumn();
		String sql = "SELECT " + colname + " FROM " + tablename + " WHERE " + counterCri.getWhereCause() + " FOR UPDATE;";
		sql += "UPDATE " + tablename + " SET " + colname + "=" + counterCri.getIncrements() + " WHERE " + counterCri.getWhereCause();
		return doBySQL(sql);
	}

	@Override
	public DomainDaoSupport getDaosupport() {
		return null;
	}

	@Override
	public Class<?> getDomainClazz() {
		return null;
	}

	@Override
	public String getDomainName() {
		return null;
	}

	@Override
	public ServiceSpec getServiceSpec() {
		return null;
	}

	@Override
	public void setDaosupport(DomainDaoSupport arg0) {

	}

}
