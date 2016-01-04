package onight.tfw.orouter.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface MsgLsnr {
	String onMessage() default "onMessage";
	String qName() default "";
	int fetchDelayMS() default 20;
	int coreThreadCount() default 2; 
	int sharedThreadCount() default 2;
}
