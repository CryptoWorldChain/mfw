package onight.zippo.oparam.bdb;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CallBack;
import onight.tfw.mservice.ThreadContext;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.oparam.api.OPFace;
import onight.tfw.oparam.api.OTreeValue;

@Slf4j
public class OBDBImpl implements OPFace, DomainDaoSupport {

	@Setter
	@Getter
	String rootPath = "fbs";

	private Environment dbEnv;
	public static final String defaultEnvironmentFolder = "appdb";
	public static final String STRING_DB = "stringDb";

	public static final String BIG_VERSION = "v1.";
	public static final String SUB_VERSION = "0.";
	public static final String MIN_VERSION = "0";

	public static final String FULL_VERSION = BIG_VERSION + SUB_VERSION + MIN_VERSION;

	private final Database stringDb;

	private Environment initDatabaseEnvironment(String folder) {
		File homeDir = new File(folder);
		if (!homeDir.exists()) {
			if (!homeDir.mkdir()) {
				throw new PersistentMapException("");
			}
		}
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setDurability(Durability.COMMIT_SYNC);
		envConfig.setAllowCreate(true);
		return new Environment(homeDir, envConfig);
	}

	private Database openDatabase(String dbName, boolean allowCreate, boolean allowDuplicates) {
		DatabaseConfig objDbConf = new DatabaseConfig();
		objDbConf.setAllowCreate(allowCreate);
		objDbConf.setSortedDuplicates(allowDuplicates);
		objDbConf.setDeferredWrite(true);
		return this.dbEnv.openDatabase(null, dbName, objDbConf);
	}

	public OBDBImpl() {
		this(defaultEnvironmentFolder);
	}

	public OBDBImpl(String folder) {
		this.dbEnv = initDatabaseEnvironment(folder);
		DatabaseConfig dbconf = new DatabaseConfig();

		dbconf.setAllowCreate(true);
		dbconf.setSortedDuplicates(false);
		this.stringDb = openDatabase(STRING_DB, true, false);

		checkVersion();
	}

	public void checkVersion() {
		try {
			Future<OTreeValue> ver = this.get("BC_VERSION");
			if (ver == null || ver.get() == null || ver.get().getValue() == null) {
				this.put("BC_VERSION", FULL_VERSION);
			} else if (!StringUtils.startsWith(ver.get().getValue(), BIG_VERSION)) {
				//
				log.error("DBVersion Check ERROR!Current="+FULL_VERSION+",db version="+ver.get().getValue()+". It will Cause Unknowm Problem!!");
				System.exit(-1);
			} else if (!StringUtils.startsWith(ver.get().getValue(), BIG_VERSION + SUB_VERSION)) {
				//
				log.warn("DBVersion Check Warning!Current="+FULL_VERSION+",db version="+ver.get().getValue()+". It will Cause Unknowm Problem!!");
//				this.put("BC_VERSION", FULL_VERSION);
			} else {
				log.info("DBVersion Check SUCCESS:");
			}
		} catch (Exception e) {
			log.error("DBVersion Check Failed", e);
			System.exit(-1);
		}

	}

	@Override
	public String getHealth() {
		Object obj = ThreadContext.getContext("iscluster");
		return "{\"health\": \"true\"}";
	}

	@Override
	public Future<OTreeValue> put(String key, String value) throws IOException {
		if (value == null || key == null) {
			throw new PersistentMapException("Key or value can not be null for put()");
		}
		try {
			DatabaseEntry keyValue = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry dataValue = new DatabaseEntry(value.getBytes("UTF-8"));
			stringDb.put(null, keyValue, dataValue);
			stringDb.sync();
		} catch (UnsupportedEncodingException e) {
			throw new PersistentMapException("Key or value has unsupported encoding.", e);
		}
		return ConcurrentUtils.constantFuture(new OTreeValue(key, value, null, 0, 0));
	}

	@Override
	public Future<OTreeValue> putDir(String dir) throws IOException {
		return ConcurrentUtils.constantFuture(new OTreeValue(dir, dir, null, 0, 0));
	}

	@Override
	public Future<OTreeValue> post(String key, String value) throws IOException {
		return put(key, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.bdb.OPFace#delete(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> delete(String key) throws IOException {
		try {
			OperationStatus status = stringDb.delete(null, new DatabaseEntry(key.getBytes("UTF-8")));
			if (status == OperationStatus.SUCCESS) {
				return ConcurrentUtils.constantFuture(new OTreeValue(key, "", null, 0, 0));
			} else {
				throw new PersistentMapException("delete Failed:" + status);
			}
		} catch (UnsupportedEncodingException e) {
			throw new PersistentMapException("Key has unsupported encoding.", e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.bdb.OPFace#deleteDir(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> deleteDir(String dir) throws IOException {
		try {
			OperationStatus status = stringDb.delete(null, new DatabaseEntry(dir.getBytes("UTF-8")));
			if (status == OperationStatus.SUCCESS) {
				return ConcurrentUtils.constantFuture(new OTreeValue(dir, "", null, 0, 0));
			} else {
				return ConcurrentUtils.constantFuture(new OTreeValue(dir, "", null, 0, 0));
			}
		} catch (UnsupportedEncodingException e) {
			throw new PersistentMapException("Key has unsupported encoding.", e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see onight.zippo.oparam.bdb.OPFace#get(java.lang.String)
	 */
	@Override
	public Future<OTreeValue> get(String key) throws IOException {
		try {
			DatabaseEntry searchEntry = new DatabaseEntry();
			stringDb.get(null, new DatabaseEntry(key.getBytes("UTF-8")), searchEntry, LockMode.DEFAULT);
			if (searchEntry.getData() == null) {
				return null;
			} else {
				String value = new String(searchEntry.getData(), "UTF-8");
				return ConcurrentUtils.constantFuture(new OTreeValue(key, value, null, 0, 0));
			}

		} catch (UnsupportedEncodingException e) {
			throw new PersistentMapException("Key has unsupported encoding.", e);
		}

	}

	@Override
	public Future<OTreeValue> getDir(String dir) throws IOException {
		throw new PersistentMapException("Unsupported Operation");
	}

	@Override
	public Future<OTreeValue> getAll() throws IOException {
		throw new PersistentMapException("Unsupported Operation");
	}

	@Override
	public void watchOnce(final String key, final CallBack<OTreeValue> cb) {
		watch(key, cb, false);
	}

	boolean shutdown = false;

	@Override
	public void watch(final String key, final CallBack<OTreeValue> cb, final boolean always) {
		throw new PersistentMapException("Unsupported Operation");
	}

	@Override
	public DomainDaoSupport getDaosupport() {
		return this;
	}

	@Override
	public Class<?> getDomainClazz() {
		return Object.class;
	}

	@Override
	public String getDomainName() {
		return "etcd";
	}

	@Override
	public ServiceSpec getServiceSpec() {
		return new ServiceSpec("obdb");
	}

	@Override
	public void setDaosupport(DomainDaoSupport dao) {
		log.trace("setDaosupport::dao=" + dao);
	}

	@Override
	public Future<OTreeValue> compareAndDelete(String key, String value) throws IOException {
		throw new PersistentMapException("Unsupported Operation");
	}

	@Override
	public Future<OTreeValue> compareAndSwap(String key, String newvalue, String comparevalue) throws IOException {
		throw new PersistentMapException("Unsupported Operation");
	}

	public void close() {
		if (this.stringDb != null) {
			this.stringDb.close();
		}
		if (this.dbEnv != null) {
			this.dbEnv.close();
		}
	}
}
