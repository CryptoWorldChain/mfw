package onight.tfw.ojpa.ordb;

import java.util.HashMap;
import java.util.Map;

import onight.tfw.ojpa.api.StorePolicy;
import lombok.Getter;


public enum ORDBBean {
	test(StorePolicy.class);

	@Getter
	private Class<?> clazz;
	
	private ORDBBean(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	private static Map<String,ORDBBean> dbInfo = new HashMap<>();
	
	static{
		for(ORDBBean info :ORDBBean.values()){
			dbInfo.put(info.name(), info);
		}
	}
	
	public static Class<?> getClass2Name(String name){
		return dbInfo.get(name.toLowerCase()).getClazz();
	}
	
}
