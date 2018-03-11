package org.fc.hzq.orcl.impl;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.CASCriteria;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.TransactionExecutor;
import onight.tfw.ojpa.api.exception.JPAException;
import onight.tfw.ojpa.ordb.ExtendDaoSupper;
import onight.tfw.ojpa.ordb.SerializedDomainDao;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@Slf4j
@Data
public class RemoteDataService extends SerializedDomainDao {

	DeferSender sender;

	String daoname;

	public RemoteDataService(DeferSender sender, String daoname) {
		super(null);
		this.sender = sender;
		this.daoname = daoname;
	}

	@Override
	public void findClazz(ExtendDaoSupper dao) {

	}

	public FramePacket toRemotePack(Object obj, String method) {
		return toRemotePack(obj, "NEN", method);
	}

	public FramePacket toRemotePack(Object obj1, Object obj2, String method) {
		ArrayList list = new ArrayList();
		list.add(obj1);
		list.add(obj2);
		FramePacket fp = PacketHelper.genSyncPack("RDB", "HDB", list);
		fp.putHeader("__rdb_method", method);
		fp.putHeader("__rdb_dao", daoname);
		return fp;
	}

	public <T> T deserial(FramePacket fp, Class<T> clazz) {
		if (StringUtils.isNotBlank(fp.getExtStrProp("__exception"))) {
			throw fp.getSio().deserialize(fp.getBody(), JPAException.class);
		}
		return fp.getSio().deserialize(fp.getBody(), clazz);
	}

	@Override
	public int countByExample(Object example) {
		try {
			FramePacket ret = sender.send(toRemotePack(example, "countByExample"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int deleteByExample(Object example) {
		try {
			FramePacket ret = sender.send(toRemotePack(example, "deleteByExample"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int deleteByPrimaryKey(Object key) {
		try {
			FramePacket ret = sender.send(toRemotePack(key, "deleteByPrimaryKey"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int insert(Object record) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, "insert"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public Object getAndSet(Object record) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, "getAndSet"), 60 * 1000);
			return deserial(ret, Object.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int insertSelective(Object record) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, "insertSelective"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int batchInsert(List<Object> records) {
		try {
			FramePacket ret = sender.send(toRemotePack(records, "batchInsert"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int batchUpdate(List<Object> records) {
		try {
			FramePacket ret = sender.send(toRemotePack(records, "batchUpdate"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int batchDelete(List<Object> records) {
		try {
			FramePacket ret = sender.send(toRemotePack(records, "batchDelete"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public List<Object> selectByExample(Object example) {
		try {
			FramePacket ret = sender.send(toRemotePack(example, "selectByExample"), 60 * 1000);
			return deserial(ret, List.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public Object selectByPrimaryKey(Object key) {
		try {
			FramePacket ret = sender.send(toRemotePack(key, "selectByPrimaryKey"), 60 * 1000);
			return deserial(ret, Object.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public List<Object> findAll(List<Object> records) {
		try {
			FramePacket ret = sender.send(toRemotePack(records, "findAll"), 60 * 1000);
			return deserial(ret, List.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int updateByExampleSelective(Object record, Object example) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, "updateByExampleSelective"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int updateByExample(Object record, Object example) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, example, "updateByExample"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int updateByPrimaryKeySelective(Object record) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, "updateByPrimaryKeySelective"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int updateByPrimaryKey(Object record) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, "updateByPrimaryKey"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public int sumByExample(Object example) {
		try {
			FramePacket ret = sender.send(toRemotePack(example, "sumByExample"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public void deleteAll() {
		throw new JPAException("UNSUPPORTED");
	}

	@Override
	public Object getExample(Object record) {
		try {
			FramePacket ret = sender.send(toRemotePack(record, "getExample"), 60 * 1000);
			return deserial(ret, Object.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	private boolean needTransaction(String sql) {
		if (StringUtils.containsIgnoreCase(sql, "INSERT") || StringUtils.containsIgnoreCase(sql, "UPDATE") || StringUtils.containsIgnoreCase(sql, "DELETE")) {
			return true;
		}
		return false;
	}

	public Object doBySQL(String sql) throws JPAException {
		try {
			FramePacket ret = sender.send(toRemotePack(sql, "doBySQL"), 60 * 1000);
			return deserial(ret, Object.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
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

		try {
			FramePacket ret = sender.send(toRemotePack(sql, "doSqlByTransaction"), 60 * 1000);
			return deserial(ret, Integer.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
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
		try {
			FramePacket ret = sender.send(toRemotePack(sql, "doSqlByNoTransaction"), 60 * 1000);
			return deserial(ret, List.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public Object insertIfNoExist(Object entity) throws JPAException {
		try {
			FramePacket ret = sender.send(toRemotePack(entity, "insertIfNoExist"), 60 * 1000);
			return deserial(ret, Object.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public Object increAnGetInt(CASCriteria<?> counterCri) throws JPAException {
		String tablename = counterCri.getTable();
		String colname = transNames(counterCri.getColumn());
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
		String tablename = "T_" + transNames(counterCri.getTable());
		String colname = transNames(counterCri.getColumn());
		String sql = "UPDATE " + tablename + " SET " + colname + "=" + colname + "+" + counterCri.getIncrements() + " WHERE " + counterCri.getWhereCause();
		return doBySQL(sql);
	}

	@Override
	public Object doInTransaction(TransactionExecutor exec) throws JPAException {

		throw new JPAException("UN_SUPPORTED");
	}

	@Override
	public Object selectOneByExample(Object example) {
		try {
			FramePacket ret = sender.send(toRemotePack(example, "selectOneByExample"), 60 * 1000);
			return deserial(ret, Object.class);
		} catch (Exception e) {
			throw new JPAException("remote call error:");
		}
	}

	@Override
	public Object checkAndSet(CASCriteria<?> counterCri) throws JPAException {
		String tablename = "T_" + transNames(counterCri.getTable());
		String colname = transNames(counterCri.getColumn());
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
