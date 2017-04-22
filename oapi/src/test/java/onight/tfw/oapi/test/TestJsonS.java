package onight.tfw.oapi.test;

import org.codehaus.jackson.JsonNode;

import onight.tfw.outils.serialize.JsonSerializer;

public class TestJsonS {

	
	public static void main(String[] args) {
		String json="{\"menuId\":{\"$regex\":\"%00\",\"$options\":\"i\"}}\";";
		JsonNode jn = JsonSerializer.getInstance().deserialize(json, JsonNode.class);
		System.out.println(jn);
	}
}
