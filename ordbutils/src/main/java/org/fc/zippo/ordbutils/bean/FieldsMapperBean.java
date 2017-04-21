package org.fc.zippo.ordbutils.bean;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

public @Data class FieldsMapperBean {

	List<SearchField> fields = new ArrayList<>();
	
	public @Data static class SearchField{
		private String fieldName;
		private int show;//1:true 0:false
	}
}
