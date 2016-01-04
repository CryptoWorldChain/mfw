package onight.tfw.oapi.test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BasicA<T> {

	public T obj;

	public int i;

	public Class getSubType() {
		Class ret = null;
		try {
			Field field = BasicA.class.getField("obj");
			ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
			ret = (Class)parameterizedType.getActualTypeArguments()[0];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;

	}
}
