package org.fc.zippo.ordbutils.rest;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.fc.zippo.ordbutils.bean.FieldsMapperBean;
import org.fc.zippo.ordbutils.bean.FieldsMapperBean.SearchField;

import onight.tfw.outils.serialize.JsonSerializer;


public class FieldsMapperResolver {
	
	public static FieldsMapperBean genQueryMapper(String json) {
		
		FieldsMapperBean fmb = new FieldsMapperBean();
		if(StringUtils.isBlank(json))return fmb;
		ObjectNode node = JsonSerializer.getInstance().deserialize(json,ObjectNode.class);
		Iterator<Map.Entry<String, JsonNode>> iter = node.getFields();
		while (iter.hasNext()) {
			Entry<String, JsonNode> entry = iter.next();
			String key = entry.getKey();
			JsonNode value = entry.getValue();
			SearchField sf = new SearchField();
			sf.setFieldName(key);
			sf.setShow(value.asInt());
			fmb.getFields().add(sf);
		}
		return fmb;
	}
}
