package onight.osgi.otransio.impl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.NonePackSender;
import onight.tfw.otransio.api.PSender;
import onight.tfw.otransio.api.PSenderService;

@Slf4j
public class SenderPolicy {

	public static void bindPSender(PSenderService senderClient, IPacketSender sender) {
		Class clazz = senderClient.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			PSender anno = field.getAnnotation(PSender.class);
			if (anno != null && (anno.name().equals("transio"))) {
				try {
					PropertyDescriptor pd;
					try {
						pd = new PropertyDescriptor(field.getName(), clazz);
						Method wm = pd.getWriteMethod();
						wm.invoke(senderClient, sender);
					} catch (IntrospectionException e) {
						log.warn("cannot init bindPSender class=" + clazz + ",field=" + field.getName(),e);
					}
				} catch (Exception e) {
				}
			}
		}
	}
	
	public static void unBindPSender(PSenderService senderClient) {
		Class clazz = senderClient.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			PSender anno = field.getAnnotation(PSender.class);
			if (anno != null && (anno.name().equals("transio"))) {
				try {
					PropertyDescriptor pd;
					try {
						pd = new PropertyDescriptor(field.getName(), clazz);
						Method wm = pd.getWriteMethod();
						wm.invoke(senderClient, new NonePackSender());
					} catch (IntrospectionException e) {
						log.warn("cannot init bindPSender class=" + clazz + ",field=" + field.getName(),e);
					}
				} catch (Exception e) {
				}
			}
		}
	}
	
}
