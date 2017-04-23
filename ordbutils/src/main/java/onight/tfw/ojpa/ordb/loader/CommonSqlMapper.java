package onight.tfw.ojpa.ordb.loader;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.SelectProvider;

public interface CommonSqlMapper {

	@SelectProvider(type=CommonSqlProvider.class,method="executeSql")
	public List<Map<String,Object>> executeSql(String sql);
	
}
