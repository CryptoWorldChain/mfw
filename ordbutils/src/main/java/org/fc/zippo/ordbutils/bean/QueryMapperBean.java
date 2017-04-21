package org.fc.zippo.ordbutils.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.codehaus.jackson.JsonNode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryMapperBean {
	private JsonNode node=null;
	
}
