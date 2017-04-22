package org.fc.zippo.ordbutils.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.fc.zippo.ordbutils.bean.DbCondi;
import org.fc.zippo.ordbutils.bean.ListInfo;
import org.fc.zippo.ordbutils.bean.PageInfo;
import org.fc.zippo.ordbutils.bean.QueryMapperBean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.ordb.StaticTableDaoSupport;
import onight.tfw.ojpa.ordb.loader.CommonSqlMapper;
import onight.tfw.outils.serialize.JsonSerializer;

@AllArgsConstructor
@Slf4j
public abstract class BaseRestCtrl {

	protected StaticTableDaoSupport dao;
	protected CommonSqlMapper mapper;
	@Setter
	@Getter
	protected boolean deleteByExampleEnabled=false;


	public abstract String get(String key, HttpServletRequest req,HttpServletResponse res);

	public abstract String post(byte[] bodies, HttpServletRequest req,HttpServletResponse res);

	public abstract String put(String key, byte[] bodies, HttpServletRequest req,HttpServletResponse res);

	public abstract String delete(String key, byte[] bodies, HttpServletRequest req,HttpServletResponse res);


	public List<Map<String, Object>> reMap(List<Map<String, Object>> list) {
		if (list == null)
			return null;
		ArrayList<Map<String, Object>> retlist = new ArrayList<>();
		for (Map<String, Object> map : list) {
			HashMap remap = new HashMap<>();
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				remap.put(FieldUtils.SqlColomn2Field(entry.getKey()), entry.getValue());
			}
			retlist.add(remap);
		}
		return retlist;

	}

	public String getBySql(Class entityClazz, Class keyClass, String tableName, HttpServletRequest req) {
		int totalCount = -1;
		boolean page = StringHelper.toBool(req.getParameter("page"));
		String query = req.getParameter("query");
		String fields = req.getParameter("fields");
		PageInfo pi = PageInfo.fromReq(req);
		DbCondi dc = new DbCondi();
		dc.setTableName(tableName);
		dc.setEntityClass(entityClazz);
		dc.setKeyClass(keyClass);
		if (StringUtils.isNoneBlank(query)) {
			dc.setQmb(new QueryMapperBean(JsonSerializer.getInstance().deserialize(query, JsonNode.class)));
		}
		dc.setPageinfo(pi);
		dc.setOrderby(req.getParameter("orderby"));
		dc.setGroupby(req.getParameter("groupby"));
		dc.setFmb(FieldsMapperResolver.genQueryMapper(fields));

		if (page) {
			String sql = SqlMaker.getCountSql(dc);
			try {
				totalCount = Integer.parseInt(mapper.executeSql(sql).get(0).get("COUNT") + "");
			} catch (Exception e) {
			}
			log.debug("[SQL].Count:{},sql={}", totalCount, sql);
		}
		List list = null;
		if (totalCount > 0 || totalCount == -1) {
			String sql = SqlMaker.getSQL(dc);
			log.debug("[SQL]:{}", sql);
			list = mapper.executeSql(sql);
			if (list != null && list.size() > 0) {
				list = reMap(list);
			}
		}
		if (page) {
			return JsonSerializer.formatToString(new ListInfo<>(totalCount, list, pi.getSkip(), pi.getLimit()));
		} else {
			return JsonSerializer.formatToString(list);
		}

	}
}
