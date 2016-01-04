package onight.tfw.ojpa.api;

import lombok.Data;

@Data
public class CASCriteria<T> {

	/**
	 * 表名
	 */
	String table;

	/**
	 * 列名
	 */
	String column;
	
	

	/**
	 * 主键
	 */
	String rowkey;
	
	/**
	 * 增量， Interger或者Float,Double类型
	 */
	T increments;

	
	/**
	 * 条件判断，可以为空
	 */
	public interface Cause{
		public boolean isOK(Object obj);
	}
	
	Cause cause;
	String whereCause;
	
}
