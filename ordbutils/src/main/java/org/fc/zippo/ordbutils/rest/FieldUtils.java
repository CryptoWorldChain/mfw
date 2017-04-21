package org.fc.zippo.ordbutils.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
		for (int i = 0; i < field.length(); i++) {
			char ch = field.charAt(i);
			if (ch >= 'A' && ch <= 'Z') {
				buff.append("_").append(ch);
			} else {
				buff.append(ch);
			}
		}
		return buff.toString().toUpperCase();
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
}
