package onight.tfw.ojpa.ordb.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubSpringContextLoader extends SpringContextLoader{

	ClassPathXmlApplicationContext parentAppContext;

	public SubSpringContextLoader(Object parentAppContext) {
		this.parentAppContext =(ClassPathXmlApplicationContext) parentAppContext;
	}


	public void init(BundleContext bundleContext, String[] contextConfigs) {

		this.bundleContext = bundleContext;

		HashSet<String> configs = new HashSet<String>();
		HashSet<String> newconfigs = new HashSet<String>();
		newconfigs.addAll(Arrays.asList(contextConfigs));
		
		String bundleid = SpringContextLoader.class.getResource("/SpringContext-ordb-common.xml").getHost();
		loadContext = bundleContext.getBundle(Integer.parseInt(bundleid.split("\\.")[0])).getBundleContext();

		// bundle://42.0:18/SpringContext-ordb-common.xml
		for (String config : newconfigs) {
			if (configs.contains(config)) {
				log.debug("override config:" + config);
			}
			URL url = bundleContext.getBundle().getResource(config);
			if (url == null) {
				throw new RuntimeException(" cannot load config file:" + config);
			}
			configs.add(bundleContext.getBundle().getResource(config).toString());
		}
		// bundle://42.0:18/SpringContext-ordb-common.xml

		appContext = new ClassPathXmlApplicationContext((configs.toArray(new String[] {})),parentAppContext) {
			protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
				super.initBeanDefinitionReader(reader);
				reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
				reader.setBeanClassLoader(getClassLoader());
			}

			@Override
			public Resource getResource(String location) {
				// TODO Auto-generated method stub
				return super.getResource(location);
			}

			@Override
			public Resource[] getResources(String locationPattern) throws IOException {
				Resource res[] = super.getResources(locationPattern);
				if (res == null || res.length == 0) {
					return loadResource(locationPattern);
				}
				return res;
			}

			@Override
			public ClassLoader getClassLoader() {
				super.setClassLoader(new WrapClassLoader(super.getClassLoader()));
				// log.info("getClassLoader:" + super.getClassLoader());
				return super.getClassLoader();
			}
		};

		
		String names[] = appContext.getBeanDefinitionNames();
		log.debug("total beans:" + names.length + ",springcontext=" + appContext + "@" + this);

		txManager = (PlatformTransactionManager) appContext.getBean("transactionManager");
		log.info("txManager=" + txManager);
	}


}
