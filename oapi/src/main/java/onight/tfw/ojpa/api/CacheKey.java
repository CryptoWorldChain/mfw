package onight.tfw.ojpa.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheKey {
	String table;
	String key;
	String col;
	Class clazz;
	Object defautlV;
	public CacheKey(String table, String key, Class clazz) {
		super();
		this.table = table;
		this.key = key;
		this.clazz = clazz;
	}
	
	
	public CacheKey(String table, String key, Class clazz, Object defautlV) {
		super();
		this.table = table;
		this.key = key;
		this.clazz = clazz;
		this.defautlV = defautlV;
	}
	public CacheKey(String table, String key) {
		super();
		this.table = table;
		this.key = key;
	}


	public CacheKey(String table, String key, String col) {
		super();
		this.table = table;
		this.key = key;
		this.col = col;
	}


	public CacheKey(String table, String key, String col, Object defautlV) {
		super();
		this.table = table;
		this.key = key;
		this.col = col;
		this.defautlV = defautlV;
	}
	
}
