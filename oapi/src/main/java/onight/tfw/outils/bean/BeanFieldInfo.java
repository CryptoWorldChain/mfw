package onight.tfw.outils.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class BeanFieldInfo {
	String fieldName;
	Method getM;
	Method setM;
	Class<?> fieldType;
	boolean isBasicType;
	Field field;
}
