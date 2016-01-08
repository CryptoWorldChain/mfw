package onight.tfw.outils.bean;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.DeserializationConfig.Feature;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.serialize.TransBeanSerializer;

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.MapEntry;
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
							BeanFieldInfo bfi = new BeanFieldInfo(field.getName(), pd.getReadMethod(), pd.getWriteMethod(), pd.getPropertyType(),
									TransBeanSerializer.isBaseType(pd.getPropertyType()), field);
							props.put(field2PBName(field), bfi);
							props.put(field.getName(), bfi);
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

	public Object pbValue2Java(Object obj, Class dstClass) {
		if (obj instanceof Message) {
			try {
				return copyFromPB((Message) obj, dstClass.newInstance());
			} catch (Exception e) {
			}
		} else if (obj instanceof EnumValueDescriptor) {
			EnumValueDescriptor evd = (EnumValueDescriptor) obj;
			return evd.getNumber();
		}
//		System.out.println("::obj=" + obj + ",dstclass=" + dstClass + ",objclas=" + obj.getClass());
		return obj;
	}

	public <T> T copyFromPB(Message fromMsg, T dst) {
		HashMap<String, BeanFieldInfo> bfis = extractMethods(dst.getClass());
		if (bfis != null) {
			for (Entry<FieldDescriptor, Object> fv : fromMsg.getAllFields().entrySet()) {
				BeanFieldInfo bf = bfis.get(fv.getKey().getName());
				if (bf != null) {
					try {
						if (fv.getValue() instanceof List) {
							List list = (List) fv.getValue();
							if (list.size() > 0) {
								if (list.get(0) instanceof MapEntry) {
									Map<Object, Object> map = (Map<Object, Object>) bf.getFieldType().newInstance();
									ParameterizedType parameterizedType = (ParameterizedType) ((bf.getField().getGenericType()));
									for (MapEntry entry : (List<MapEntry>) fv.getValue()) {
										map.put(pbValue2Java(entry.getKey(), (Class) parameterizedType.getActualTypeArguments()[0]),
												pbValue2Java(entry.getValue(), (Class) parameterizedType.getActualTypeArguments()[1]));
									}
									bf.getSetM().invoke(dst, map);
								} else {
									ArrayList olist = new ArrayList();
									ParameterizedType parameterizedType = (ParameterizedType) ((bf.getField().getGenericType()));
									// System.out.println("atype:"+);
									for (Object obj : list) {
										olist.add(pbValue2Java(obj, (Class) parameterizedType.getActualTypeArguments()[0]));
									}
									bf.getSetM().invoke(dst, olist);
								}
							}
						} else {
							bf.getSetM().invoke(dst, pbValue2Java(fv.getValue(), bf.getField().getType()));
						}
					} catch (Exception e) {
						log.debug("cannot invoke SetMethod:for class=" + dst.getClass() + ",field=" + bf.getFieldName() + "," + fv.getValue().getClass(), e);
					}
				}
			}
		}
		return dst;
	}

	public <T> T toPB(Message.Builder msgBuilder, Object src) {
		HashMap<String, BeanFieldInfo> bfis = extractMethods(src.getClass());
		if (bfis != null) {
			for (Entry<String, BeanFieldInfo> bf : bfis.entrySet()) {
				Object v = null;
				try {
					v = bf.getValue().getGetM().invoke(src, null);
				} catch (Exception e) {
					log.debug("cannot invoke getMethod:for class=" + src.getClass() + ",field=" + bf.getKey());
				}
				if (v != null) {
					FieldDescriptor fd = msgBuilder.getDescriptorForType().findFieldByName(bf.getKey());
					if (fd == null)
						continue;
					try {
						if (fd.isRepeated()) {
							if (v instanceof List) {
								Message.Builder subbuilder = null;
								for (Object lv : (List) v) {
									Object pv = null;
									if (fd.getJavaType().equals(JavaType.MESSAGE)) {
										subbuilder = msgBuilder.newBuilderForField(fd);
										toPB(subbuilder, lv);
										pv = subbuilder.build();
									} else {
										pv = lv;
									}
									if (v != null) {
										msgBuilder.addRepeatedField(fd, pv);
									} else if (subbuilder != null) {
										msgBuilder.addRepeatedField(fd, subbuilder.build());
									}
								}
							} else if (v instanceof Map) {
								for (Map.Entry item : (Set<Map.Entry>) ((Map) v).entrySet()) {
									MapEntry.Builder mb = (MapEntry.Builder) msgBuilder.newBuilderForField(fd);
									FieldDescriptor fd2 = mb.getDescriptorForType().getFields().get(1);
									mb.setKey(item.getKey());
									if (fd2.getJavaType() == JavaType.MESSAGE) {
										mb.setValue(toPB(mb.newBuilderForField(fd2), item.getValue()));
									} else {
										mb.setValue(item.getValue());
									}
									msgBuilder.addRepeatedField(fd, mb.build());
									System.out.println(msgBuilder.build());
								}
							}
						} else if (fd.getJavaType() == JavaType.MESSAGE) {
							Message.Builder subbuilder = msgBuilder.newBuilderForField(fd);
							toPB(subbuilder, v);
							Object pv = subbuilder.build();
							msgBuilder.setField(fd, pv);
						} else if (fd.getJavaType() == JavaType.ENUM) {
							EnumValueDescriptor evd = fd.getEnumType().findValueByNumber((int)v);
							msgBuilder.setField(fd, evd);

							
						} else {
							msgBuilder.setField(fd, v);
						}
					} catch (Exception e) {
						if (fd.getType() == Type.STRING) {
							try {
								msgBuilder.setField(fd, v.toString());
							} catch (Exception e1) {
								log.debug("cannot invoke setfield class=" + src.getClass() + ",field=" + bf.getKey() + ",fd=" + fd + ",v=" + v);
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
