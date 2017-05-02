package onight.tfw.outils.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import lombok.Data;

@Data
public class TypeHelper {

	Type type;
	Type name;

	public TypeHelper(Type type, Type name) {
		this.type = type;
		this.name = name;
	}

	public static TypeHelper getClassType(Class<?> clazz) {
		TypeHelper type = new TypeHelper(null, null);
		if (clazz.getGenericSuperclass() instanceof ParameterizedType) {
			Class<?> superClazz = clazz.getSuperclass();
			ParameterizedType paramType = (ParameterizedType) clazz.getGenericSuperclass();
			TypeVariable<?>[] superTypeParameters = superClazz.getTypeParameters();
			if (superTypeParameters!=null) {
				// lookup if type is not found or type needs a lookup in
				// current concrete class
				for (int j = 0; j < superClazz.getTypeParameters().length; ++j) {
					TypeVariable<?> superTypeParam = superTypeParameters[j];
						type.type = paramType.getActualTypeArguments()[j];
						Type[] typeParameters = clazz.getTypeParameters();
						if (typeParameters.length > 0) {
							for (Type typeParam : typeParameters) {
								TypeVariable<?> objectOfComparison = superTypeParam;
								if (type.type instanceof TypeVariable<?>) {
									objectOfComparison = (TypeVariable<?>) type.type;
								}
								if (objectOfComparison.getName().equals(((TypeVariable<?>) typeParam).getName())) {
									type.name = typeParam;
									break;
								}
						}
						break;
					}
				}
			}
		}
		return type;
	}

	public static TypeHelper getType(Class<?> clazz, Field field) {
		TypeHelper type = new TypeHelper(null, null);
		if (field.getGenericType() instanceof TypeVariable<?>) {
			TypeVariable<?> genericTyp = (TypeVariable<?>) field.getGenericType();
			Class<?> superClazz = clazz.getSuperclass();

			if (clazz.getGenericSuperclass() instanceof ParameterizedType) {
				ParameterizedType paramType = (ParameterizedType) clazz.getGenericSuperclass();
				TypeVariable<?>[] superTypeParameters = superClazz.getTypeParameters();
				if (!Object.class.equals(paramType)) {
					if (field.getDeclaringClass().equals(superClazz)) {
						// this is the root class an starting point for this
						// search
						type.name = genericTyp;
						type.type = null;
					} else {
						type = getType(superClazz, field);
					}
				}
				if (type.type == null || type.type instanceof TypeVariable<?>) {
					// lookup if type is not found or type needs a lookup in
					// current concrete class
					for (int j = 0; j < superClazz.getTypeParameters().length; ++j) {
						TypeVariable<?> superTypeParam = superTypeParameters[j];
						if (type.name.equals(superTypeParam)) {
							type.type = paramType.getActualTypeArguments()[j];
							Type[] typeParameters = clazz.getTypeParameters();
							if (typeParameters.length > 0) {
								for (Type typeParam : typeParameters) {
									TypeVariable<?> objectOfComparison = superTypeParam;
									if (type.type instanceof TypeVariable<?>) {
										objectOfComparison = (TypeVariable<?>) type.type;
									}
									if (objectOfComparison.getName().equals(((TypeVariable<?>) typeParam).getName())) {
										type.name = typeParam;
										break;
									}
								}
							}
							break;
						}
					}
				}
			}
		} else {
			type.type = field.getGenericType();
		}

		return type;
	}
}
