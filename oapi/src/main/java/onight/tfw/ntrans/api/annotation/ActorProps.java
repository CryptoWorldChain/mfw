package onight.tfw.ntrans.api.annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.felix.ipojo.annotations.Stereotype;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Stereotype
public @interface ActorProps {

	String name() default "";
	
	String value() default "";//参数信息
	
}
