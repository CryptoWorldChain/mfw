package onight.tfw.ojpa.api;

import lombok.Data;

@Data
public class ServiceSpec {

	/** 数据存储方案 cassandra,mysql,redis 等... 默认mysql */
	private String target = "mysql";
	/** 存储策略 **/
	private StorePolicy policy = StorePolicy.AUTO;

	public static ServiceSpec MYSQL_STORE = new ServiceSpec();
	public static ServiceSpec REDIS_STORE = new ServiceSpec("redis");
	public static ServiceSpec CASS_STORE = new ServiceSpec("cass");
	public static ServiceSpec CACHE_STORE = new ServiceSpec("cache");
	public static ServiceSpec FILE_STORE = new ServiceSpec("file");
	public static ServiceSpec HDFS_STORE = new ServiceSpec("hdfs");
	public static ServiceSpec SYSLOG_STORE = new ServiceSpec("syslog");
	public static ServiceSpec RMQ_STORE = new ServiceSpec("rabbitmq");

	public ServiceSpec(String target, StorePolicy policy) {
		this.target = target;
		this.policy = policy;
	}

	public ServiceSpec() {
	}

	public ServiceSpec(StorePolicy policy) {
		super();
		this.policy = policy;
	}

	public ServiceSpec(String target) {
		super();
		this.target = target;
	};

}
