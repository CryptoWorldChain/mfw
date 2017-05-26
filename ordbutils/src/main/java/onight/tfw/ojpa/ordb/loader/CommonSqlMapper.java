package onight.tfw.ojpa.ordb.loader;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

public interface CommonSqlMapper {

	@SelectProvider(type = CommonSqlProvider.class, method = "executeSql")
	public List<Map<String, Object>> executeSql(String sql);

	@UpdateProvider(type = CommonSqlProvider.class, method = "executeSql")
	public int executeUpdate(String sql);

}
