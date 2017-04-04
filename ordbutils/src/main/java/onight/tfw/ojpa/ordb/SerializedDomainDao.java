package onight.tfw.ojpa.ordb;

import java.lang.reflect.Type;
import java.util.List;

import onight.tfw.ojpa.api.ORMappingDao;
import onight.tfw.ojpa.api.annotations.Tab;
import onight.tfw.ojpa.api.exception.JPAException;
import onight.tfw.outils.serialize.SerializerUtil;

public abstract class SerializedDomainDao implements ORMappingDao<Object> {

	protected Class beanClazz;
	protected Class beanExampleClazz;
	protected Class beanKeyClazz;

	public void findClazz(ExtendDaoSupper dao) {
		Type types[] = dao.getParameterizedType();
		if (types != null && types.length >= 3) {
			beanClazz = (Class)types[0];
			beanExampleClazz = (Class)types[1];
			beanKeyClazz =(Class)types[2];
		}
		Tab tab = dao.getClass().getAnnotation(Tab.class);
		String packagename = dao.getClass().getPackage().getName();
		packagename = packagename.substring(0, packagename.length() - 3) + "entity";
		String beanname = dao.getClass().getSimpleName().substring(0, dao.getClass().getSimpleName().length() - 3);
		try {
			if (beanClazz == null) {
				if (tab != null && !void.class.equals(tab.beanClass())) {
					beanClazz = tab.beanClass();
				} else {
					beanClazz = Class.forName(packagename + "." + beanname);
				}
			}
			if (beanKeyClazz == null) {
				if (tab != null && !void.class.equals(tab.keyClass())) {
					beanKeyClazz = tab.keyClass();
				} else {
					beanKeyClazz = Class.forName(packagename + "." + beanname + "Key");
				}
			}
			if (beanExampleClazz == null) {
				if (tab != null && !void.class.equals(tab.exampleClass())) {
					beanExampleClazz = tab.exampleClass();
				} else {
					beanExampleClazz = Class.forName(packagename + "." + beanname + "Example");

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SerializedDomainDao(ExtendDaoSupper dao) {
		super();
		findClazz(dao);
	}

	protected Object localBean(Object entity) {
		try {
			return SerializerUtil.deserialize(entity, beanClazz);
		} catch (NullPointerException e) {
			throw new JPAException(String.format("domain names is error.. [%s]", beanClazz), e);
		}
	}

	protected Object serial(Object entity) {
		return SerializerUtil.serialize(entity);
	}

	protected List<Object> serial(List<Object> entity) {
		return (List<Object>) SerializerUtil.serializeArray(entity);
	}

	protected Object localExample(Object entity) {
		try {
			return SerializerUtil.deserialize(entity, beanExampleClazz);
		} catch (NullPointerException e) {
			throw new JPAException(String.format("domain names is error.. [%s]", beanClazz), e);
		}
	}

	protected Object localKey(Object entity) {
		try {
			return SerializerUtil.deserialize(entity, beanKeyClazz);
		} catch (NullPointerException e) {
			throw new JPAException(String.format("domain names is error.. [%s]", beanClazz), e);
		}
	}

	protected List<?> localBean2List(Object entity) {
		try {
			return SerializerUtil.deserializeArray(entity, beanClazz);
		} catch (NullPointerException e) {
			throw new JPAException(String.format("domain names is error.. [%s]", beanClazz), e);
		}
	}
}
