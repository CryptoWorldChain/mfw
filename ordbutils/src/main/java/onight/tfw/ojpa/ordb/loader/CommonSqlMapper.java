package onight.tfw.ojpa.ordb.loader;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.SelectProvider;

public interface CommonSqlMapper {

	@SelectProvider(type=CommonSqlProvider.class,method="executeSql")
	public List<HashMap<String,Object>> executeSql(String sql);
	
}
