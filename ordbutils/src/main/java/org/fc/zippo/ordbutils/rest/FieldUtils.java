package org.fc.zippo.ordbutils.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class FieldUtils {

	public static List<Field> allDeclaredField(Class<?> clazz) {
		List<Field> fieldList = new ArrayList<>();
		Class<?> targetClass = clazz;
		do {
			Field[] fields = targetClass.getDeclaredFields();
			for (Field field : fields) {
				fieldList.add(field);
			}
			targetClass = targetClass.getSuperclass();
		} while (targetClass != null && targetClass != Object.class);
		return fieldList;
	}

	public static String field2SqlColomn(String field) {
		StringBuffer buff = new StringBuffer();
		boolean quota = false;
		for (int i = 0; i < field.length(); i++) {
			char ch = field.charAt(i);
			if (ch >= 'A' && ch <= 'Z' && !quota) {
				buff.append("_").append(ch);
			} else {
				if (ch >= 'a' && ch <= 'z' && !quota) {

					buff.append((char) (ch - 'a' + 'A'));
				} else {
					buff.append(ch);
				}
			}
			if (ch == '\'') {
				quota = !quota;
			}
		}
		return buff.toString();// .toUpperCase();
	}

	public static String SqlColomn2Field(String sqlCol) {
		StringBuffer buff = new StringBuffer();
		for (String cc : sqlCol.toLowerCase().split("_")) {
			if (buff.length() == 0) {
				buff.append(cc);
			} else {
				buff.append(StringUtils.capitalize(cc));
			}
		}
		return buff.toString();
	}

	public static List<Map<String, Object>> reMap(List<Map<String, Object>> list) {
		if (list == null)
			return null;
		ArrayList<Map<String, Object>> retlist = new ArrayList<>();
		for (Map<String, Object> map : list) {
			HashMap remap = new HashMap<>();
			if (map != null) {
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					remap.put(FieldUtils.SqlColomn2Field(entry.getKey()), entry.getValue());
				}
			}
			retlist.add(remap);
		}
		return retlist;

	}

	public static void main(String[] args) {
		System.out.println(":convertto:" + FieldUtils.field2SqlColomn("a.roleId=b.roleId and a.userId='test1'"));
	}

}
