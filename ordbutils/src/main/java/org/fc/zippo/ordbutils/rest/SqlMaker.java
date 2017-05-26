package org.fc.zippo.ordbutils.rest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.fc.zippo.ordbutils.bean.Col;
import org.fc.zippo.ordbutils.bean.DbCondi;
import org.fc.zippo.ordbutils.bean.FieldsMapperBean;
import org.fc.zippo.ordbutils.bean.FieldsMapperBean.SearchField;
import org.fc.zippo.ordbutils.bean.PageInfo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SqlMaker {

	public final static String TABLE_SUFFIX_KEY = "SUFFIX";

	public final static String TABLE_NAME = "TABLE_NAME";

	public static String getCountSql(DbCondi dc) {
		Map<String, FieldDef> fieldsMap = getFieldsMap(dc.getEntityClass());
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT COUNT(1) AS COUNT FROM ");
		sql.append(dc.getTableName());
		if (dc.getQmb() != null) {

			String whereClause = QueryMapperResolver.genQueyDirectory("", dc.getQmb().getNode(), "and", fieldsMap);
			if (StringUtils.isNoneBlank(whereClause) && whereClause.length() > 0) {
				sql.append(" WHERE ");
				sql.append(whereClause);
			}
		}
		getGroupBy(dc, sql);
		return sql.toString();
	}

	public static void getGroupBy(DbCondi dc, StringBuffer sql) {
		if (StringUtils.isNotBlank(dc.getGroupby())) {
			boolean first = true;
			Map<String, FieldDef> fieldsMap = getFieldsMap(dc.getEntityClass());

			for (String col : dc.getGroupby().split(",")) {
				String COL_sql = fieldsMap.get(col).sqlCol;
				if (COL_sql == null) {
					COL_sql = col;
				}
				if (first) {
					sql.append(" GROUP BY ");
					first = false;
				} else {
					sql.append(",");
				}
				sql.append(COL_sql);
			}
		}

	}

	public static String getOrderBy(DbCondi dc) {
		if (StringUtils.isBlank(dc.getOrderby()))
			return "";
		StringBuffer sb = new StringBuffer();
		getOrderBy(dc, sb);
		return sb.toString();
	}

	public static void getOrderBy(DbCondi dc, StringBuffer sb) {
		if (StringUtils.isBlank(dc.getOrderby()))
			return;
		Map<String, FieldDef> fieldsMap = getFieldsMap(dc.getEntityClass());

		boolean first = true;
		for (String col : dc.getOrderby().split(",")) {
			boolean desc = false;
			if (col.startsWith("-")) {
				col = col.substring(1);
				desc = true;
			}
			String COL_sql = fieldsMap.get(col).sqlCol;
			if (first) {
				sb.append(" ORDER BY ");
				first = false;
			} else {
				sb.append(",");
			}

			if (COL_sql != null) {
				sb.append(COL_sql);
			} else {
				sb.append(col);
			}
			if (desc) {
				sb.append(" DESC");
			}
		}

	}

	public static StringBuffer getSelectFieldNames(Map<String, FieldDef> fieldMap, FieldsMapperBean fmb) {
		StringBuffer fields = new StringBuffer();
		if (fmb != null && fmb.getFields().size() > 0) {
			for (SearchField sf : fmb.getFields()) {
				if (sf.getShow() == 1) {
					if (fieldMap.get(sf.getFieldName()) != null) {
						String dbcolName = fieldMap.get(sf.getFieldName()).sqlCol;
						if (dbcolName == null) {
							log.debug("The query fields[{}] are not among Class [{}]..", sf.getFieldName());
							dbcolName = sf.getFieldName();
						}
						fields.append(dbcolName).append(",");
					}else{
						fields.append(FieldUtils.field2SqlColomn(sf.getFieldName())).append(",");
					}
				}
			}
		} else {
			for (Map.Entry<String, FieldDef> entry : fieldMap.entrySet()) {
				fields.append(entry.getValue().sqlCol).append(",");
			}
		}
		int len = fields.length();
		fields.delete(len - 1, len);

		return fields;
	}

	public static String getSQL(DbCondi dc) {

		Map<String, FieldDef> fieldMap = getFieldsMap(dc.getEntityClass());
		StringBuffer sql = new StringBuffer("SELECT ").append(getSelectFieldNames(fieldMap, dc.getFmb()));

		sql.append(" FROM " + dc.getTableName());

		if (dc.getQmb() != null) {

			String whereClause = QueryMapperResolver.genQueyDirectory("", dc.getQmb().getNode(), "and", fieldMap);
			if (StringUtils.isNotBlank(whereClause)) {
				sql.append(" WHERE ");
				sql.append(whereClause);
			}
		}
		getOrderBy(dc, sql);
		getGroupBy(dc, sql);
		addPageLimit(dc.getPageinfo(), sql);

		return sql.toString();
	}

	static Map<Class, Map<String, FieldDef>> clazzFieldsMap = new HashMap<Class, Map<String, FieldDef>>();

	@AllArgsConstructor
	public static class FieldDef {
		String sqlCol;
		Field feild;
	}

	public static Map<String, FieldDef> getFieldsMap(Class clazz) {

		Map<String, FieldDef> fieldsMap = clazzFieldsMap.get(clazz);
		if (fieldsMap == null) {
			synchronized (clazzFieldsMap) {
				fieldsMap = clazzFieldsMap.get(clazz);
				if (fieldsMap == null) {
					fieldsMap = new HashMap<String, FieldDef>();
					for (Field field : FieldUtils.allDeclaredField(clazz)) {
						// 从注解里获取列名
						Col fieldColAnno = field.getAnnotation(Col.class);
						if (fieldColAnno != null) {
							FieldDef fd = new FieldDef(
									FieldUtils.field2SqlColomn(fieldColAnno.tableAlias() + "." + fieldColAnno.name()),
									field);
							fieldsMap.put(field.getName(), fd);
						} else {
							fieldsMap.put(field.getName(),
									new FieldDef(FieldUtils.field2SqlColomn(field.getName()), field));
						}
					}
					clazzFieldsMap.put(clazz, fieldsMap);
				}
			}
		}

		return fieldsMap;
	}

	public static void addPageLimit(PageInfo para, StringBuffer sql) {
		if (para != null) {
			if (Integer.MAX_VALUE != para.getLimit() || para.getSkip() > 0) {
				// sql=
				String orgsql = sql.toString();
				sql.delete(0, sql.length());
				// for ORACLE
				if (Integer.MAX_VALUE == para.getLimit() || para.getLimit() == -1) {
					sql.append("SELECT * FROM (SELECT A.*, ROWNUM RN FROM (" + orgsql + ") A WHERE ROWNUM <= "
							+ (Integer.MAX_VALUE) + ") WHERE RN > " + para.getSkip());
				} else {
					sql.append("SELECT * FROM (SELECT A.*, ROWNUM RN FROM (" + orgsql + ") A WHERE ROWNUM <= "
							+ (para.getLimit() + para.getSkip()) + ") WHERE RN > " + para.getSkip());
				}
				// sql.append(" limit
				// ").append(para.getSkip()).append(",").append(para.getLimit());
			}
		}
	}

}
