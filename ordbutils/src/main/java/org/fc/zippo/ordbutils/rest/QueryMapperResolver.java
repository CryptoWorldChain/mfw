package org.fc.zippo.ordbutils.rest;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;


public class QueryMapperResolver {

	
	public static String genQueyStr(String key, JsonNode value, String type) {
		if (type == "like") {
			return String.format(" %s like '%%%s%%' ", key, value.asText());
		} else if (type == "in") {
			return String.format(" %s in (%s%) ", key, value.asText());
		} else {
			return String.format(" %s = '%s' ", key, value.asText());
		}
	}
	public static String getValueByType(JsonNode value){
		if(value.isNumber()) return value.asText();
		return "'"+value.asText()+"'";
	}
	public static String mapKey(String key,Map<String,String> fieldsMap){
		if(fieldsMap.containsKey(key)){
			return fieldsMap.get(key);
		}
		return key;
	}
	public static String genQueyDirectory(String pkey, JsonNode node,
			String type,Map<String,String> fieldsMap) {
		StringBuffer sb = new StringBuffer();
		Iterator<Map.Entry<String, JsonNode>> iter = node.getFields();
		if (node.isArray()) {
			int i = 0;
			ArrayNode arrnode = (ArrayNode) node;
			if (arrnode.size() > 1) {
				sb.append("(");
			}
			Iterator<JsonNode> itn = arrnode.iterator();
			while (itn.hasNext()) {
				JsonNode inode = itn.next();

				if (type.equalsIgnoreCase("or")) {
					if (i > 0)
						sb.append(") or (");
				} else if (type.equalsIgnoreCase("and")) {
					if (i > 0)
						sb.append(") and (");
				}
				String str1 = genQueyDirectory("", inode, type,fieldsMap);
				sb.append(str1);
				i++;
			}
			if(i>1)
			{
				sb.append(")");
			}
			return sb.toString();
		}

		if (iter.hasNext()) {
			Entry<String, JsonNode> entry = iter.next();
			String key = entry.getKey();
			JsonNode value = entry.getValue();
			if (key.equalsIgnoreCase("$or")) {
				type = "or";
			} else { // if (key.equalsIgnoreCase("$and")) {
				type = "and";
			}
			if (node.size() == 2 && node.has("$regex")) {
				return genQueyStr(pkey, value, "like");
			} else if (node.size() == 1 && !value.isArray()) {
				if (isConditionType(key, value)) {
					String cond=getConditionType(key,value);
					String condvalue=getConditionValue(value);	
					return String.format(" %s %s %s ", mapKey(key,fieldsMap), cond,condvalue);
				} else 
				if(isLikeAndType(key, value)){
					return String.format(" %s like '%%%s%%' ", mapKey(key,fieldsMap), value.get("$regex").asText());
				}
				else{
					return genQueyStr(mapKey(key,fieldsMap), value, "and");
				}
			}
			int i = 0;
			if (node.size() > 1) {
				sb.append("(");
			}
			do {
				key = entry.getKey();
				value = entry.getValue();
				if (type.equalsIgnoreCase("or")) {
					if (i > 0)
						sb.append(") or (");
				} else if (type.equalsIgnoreCase("and")) {
					if (i > 0)
						sb.append(") and (");
				}
				String str1 = genQueyDirectory(mapKey(key,fieldsMap), value, type,fieldsMap);
				sb.append(str1);
				if (iter.hasNext()) {
					entry = iter.next();
				} else {
					entry = null;
				}
				i++;
			} while (entry != null);
			if (i > 1)
				sb.append(")");
		}

		return sb.toString();

	}

	public static void main(String[] args) {
//		String vv = "{\"$and\":[{\"userName\":{\"$regex\":\"user\",\"$options\":\"i\"}},{\"userName\":{\"$regex\":\"mick\",\"$options\":\"i\"}}]}";
//		String vv = "{\"$or\":[{\"userName\":{\"$regex\":\"user\",\"$options\":\"i\"}},{\"userName\":{\"$regex\":\"mick\",\"$options\":\"i\"}}]}";
//		String vv = "{\"$or\":[{\"age\":{\"$gte\":\"10\"}},{\"userName\":{\"$regex\":\"mick\",\"$options\":\"i\"}}]}";
//		String vv = "{\"$or\":[{\"age\":{\"$lte\":\"10\"}},{\"userName\":{\"$regex\":\"mick\",\"$options\":\"i\"}}]}";
//		String vv = "{\"$or\":[{\"age\":{\"$lt\":\"10\"}},{\"userName\":{\"$regex\":\"mick\",\"$options\":\"i\"}}]}";

		String vv = "{\"$or\":[{\"age\":{\"$in\":[1,2,3]}},{\"userName\":{\"$regex\":\"mick\",\"$options\":\"i\"}}]}";

//		ObjectNode node = JsonUtil.toObjectNode(vv);

//		String where = genQueyDirectory("", node, "and",new HashMap<String,String>());
//		System.out.println("where:" + where);
	}


	private static boolean isConditionType(String key, JsonNode value) {
		// condition {"keyy":{"<":"10",">":"1"}}
		return (StringUtils.containsAny(value.toString(), '>', '=', '<')
				|| StringUtils.containsIgnoreCase(value.toString(), "{\"$in\":")
				|| StringUtils.containsIgnoreCase(value.toString(), "{\"$nin\":")
				|| StringUtils.containsIgnoreCase(value.toString(), "{\"$lt\":")
				|| StringUtils.containsIgnoreCase(value.toString(), "{\"$lte\":")
				|| StringUtils.containsIgnoreCase(value.toString(), "{\"$gt\":")
				|| StringUtils.containsIgnoreCase(value.toString(), "{\"$gt\":")
				|| StringUtils.containsIgnoreCase(value.toString(), "{\"$gte\":") || StringUtils
					.containsIgnoreCase(value.toString(), "{\"$ne\":"));
	}

	private static String getConditionType(String key, JsonNode value) {
		// condition {"keyy":{"<":"10",">":"1"}}
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$lt\":"))
			return "<";
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$lte\":"))
			return "<=";
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$gt\":"))
			return ">";
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$gte\":"))
			return ">=";
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$ne\":"))
			return "!=";
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$in\":"))
			return "in";
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$nin\":"))
			return "not in";
		return "=";
	}
	private static String getConditionValue( JsonNode value) {
		// condition {"keyy":{"<":"10",">":"1"}}
		
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$lt\":"))
			return getValueByType(value.get("$lt"));
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$lte\":"))
			return getValueByType(value.get("$lte"));
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$gt\":"))
			return getValueByType(value.get("$gt"));
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$gte\":"))
			return getValueByType(value.get("$gte"));
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$ne\":"))
			return getValueByType(value.get("$ne"));
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$in\":"))
			return value.get("$in").toString().replaceAll("\\[", "(").replaceAll("]", ")");
		if (StringUtils.containsIgnoreCase(value.toString(), "{\"$nin\":"))
			return value.get("$nin").toString().replaceAll("\\[", "(").replaceAll("]", ")");;
		return value.asText();
	}

	private static boolean isLikeAndType(String key, JsonNode value) {
		// like {"keyy":{"$regex":"testcode","$options":"i"},"value":"a"}
		return StringUtils.containsIgnoreCase(value.toString(), "$regex");
	}

}
