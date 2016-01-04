package onight.tfw.proxy.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
public @interface LogWare {
	String module() default "";
}
