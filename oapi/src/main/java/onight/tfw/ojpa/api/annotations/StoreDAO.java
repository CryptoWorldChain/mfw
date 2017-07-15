package onight.tfw.ojpa.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.felix.ipojo.annotations.Stereotype;

import onight.tfw.ojpa.api.OJpaDAO;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Stereotype
public @interface StoreDAO {
	/**
	 * 定义结构体的主键
	 * 
	 * @return
	 */
	Class domain() default Object.class;

	String target() default "";
	
	String key() default "";
	
	Class example() default Object.class;

	Class keyclass() default Object.class;

	Class daoClass() default OJpaDAO.class;
}
