package onight.tfw.otransio.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.felix.ipojo.annotations.Stereotype;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Stereotype
public @interface PSender {
	/**
	 * 发送的协议类型
	 * @return
	 */
	String name() default "transio";
	
	/**
	 * 序列号类型
	 * @return
	 */
	String serializer() default "T";
	
	

}
