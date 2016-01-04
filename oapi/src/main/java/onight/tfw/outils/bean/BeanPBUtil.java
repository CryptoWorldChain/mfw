package onight.tfw.outils.bean;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.serialize.TransBeanSerializer;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;

@Slf4j
public class BeanPBUtil {

	private HashMap<Class, HashMap<String, BeanFieldInfo>> beanFields = new HashMap<>();

	public String field2PBName(Field field) {
		StringBuffer sb = new StringBuffer();
		for (char ch : field.getName().toCharArray()) {
			if (ch >= 'A' && ch <= 'Z') {
				sb.append('_').append((char) (ch - 'A' + 'a'));
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	public HashMap<String, BeanFieldInfo> extractMethods(Class<?> clazz) {
		HashMap<String, BeanFieldInfo> props = beanFields.get(clazz);
		if (props == null && clazz.getClassLoader() != null) {
			String uniqueName = clazz.getClassLoader().getClass().getName() + clazz.getName();
			synchronized (uniqueName.intern()) {
				props = beanFields.get(clazz);
				if (props == null) {
					props = new HashMap<>();
					for (Field field : TransBeanSerializer.allDeclaredField(clazz)) {
						if (Modifier.isTransient(field.getModifiers())) {
							continue;
						}
						PropertyDescriptor pd;
						try {
							pd = new PropertyDescriptor(field.getName(), clazz);
							props.put(field2PBName(field), new BeanFieldInfo(field.getName(), pd.getReadMethod(), pd.getWriteMethod(), pd.getPropertyType(),
									TransBeanSerializer.isBaseType(pd.getPropertyType()), field));
						} catch (IntrospectionException e) {
							log.warn("cannot init BeanProp:for class=" + clazz + ",field=" + field.getName());
						}
					}
					beanFields.put(clazz, props);
				}
			}
		}
		return props;
	}

	public <T> T copyFromPB(Message fromMsg, T dst) {
		HashMap<String, BeanFieldInfo> bfis = extractMethods(dst.getClass());
		if (bfis != null) {
			for (Entry<FieldDescriptor, Object> fv : fromMsg.getAllFields().entrySet()) {
				BeanFieldInfo bf = bfis.get(fv.getKey().getName());
				if (bf != null) {
					try {
						bf.setM.invoke(dst, fv.getValue());
					} catch (Exception e) {
						log.debug("cannot invoke SetMethod:for class=" + dst.getClass() + ",field=" + bf.getFieldName());
					}
				}
			}
		}
		return dst;
	}

	public <T> T toPB(com.google.protobuf.GeneratedMessage.Builder msgBuilder, Object src) {
		HashMap<String, BeanFieldInfo> bfis = extractMethods(src.getClass());
		if (bfis != null) {
			for (Entry<String, BeanFieldInfo> bf : bfis.entrySet()) {
				Object v = null;
				try {
					v = bf.getValue().getM.invoke(src, null);
				} catch (Exception e) {
					log.debug("cannot invoke getMethod:for class=" + src.getClass() + ",field=" + bf.getKey());
				}
				if (v != null) {
					FieldDescriptor fd = msgBuilder.getDescriptorForType().findFieldByName(bf.getKey());
					try {
						msgBuilder.setField(fd, v);
					} catch (Exception e) {
						if(fd.getType()==Type.STRING)
						{
							try {
								msgBuilder.setField(fd, v.toString());
							} catch (Exception e1) {
								log.debug("cannot invoke setfield class=" + src.getClass() + ",field=" + bf.getKey()+",fd="+fd+",v="+v);
							}
						}
					}
				}
			}
			return (T) msgBuilder.build();
		}
		return (T) msgBuilder.build();
	}
}
