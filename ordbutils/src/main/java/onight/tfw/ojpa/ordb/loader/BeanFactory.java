package onight.tfw.ojpa.ordb.loader;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * bean容器
 */
@Slf4j
public class BeanFactory implements ApplicationContextAware {
	private static ApplicationContext ctx;

	public static Object getBean(String beanName) {
		Object bean=ctx.getBean(beanName);
		log.debug("getBean:"+beanName+"==>"+bean);
		return bean;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		log.info("setApplicationContext:"+applicationContext);
		ctx = applicationContext;
	}
	
}
