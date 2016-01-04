package onight.tfw.ojpa.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tab {
	public String name() default "";

	public String tableAlias() default "";

	public Class beanClass() default void.class;

	public Class keyClass() default void.class;

	public Class exampleClass() default void.class;
}
