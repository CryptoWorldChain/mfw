package onight.tfw.sm.osgi;

import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;

@iPojoBean
@Slf4j
public class SessionWatcher {
	BundleContext context;

	public SessionWatcher(BundleContext context) {
		super();
		this.context = context;
	}

	@Validate
	public void start() {
		log.info("SessionWatcher:Start!");
	}

	@Invalidate
	public void stop() {
	}

	
	
}
