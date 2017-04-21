package org.fc.zippo.ordbutils.bean;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Col {
    public String name() default "";
    public String tableAlias() default "";
    public boolean autoField()  default true;
}
