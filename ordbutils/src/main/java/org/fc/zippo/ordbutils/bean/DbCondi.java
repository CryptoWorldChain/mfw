package org.fc.zippo.ordbutils.bean;

import java.util.Map;

import lombok.Data;

public @Data class DbCondi {

	private Class<?> keyClass;
	private Class<?> entityClass;
	// private Class<?> mapperClass;
	private FieldsMapperBean fmb;
	private PageInfo pageinfo;
	private QueryMapperBean qmb;
	private Map<String, Object> other;
	private String tableName;
	String orderby = "";
	String groupby = "";
}
