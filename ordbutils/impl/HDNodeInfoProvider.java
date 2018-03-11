package org.fc.hzq.orcl.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.JsonSerializer;

@Component(immediate = true)
@Instantiate()
@Provides(specifications = StoreServiceProvider.class, strategy = "SINGLETON")
@Slf4j
public class HDNodeInfoProvider implements StoreServiceProvider {

	@Override
	public String getProviderid() {
		return ServiceSpec.ETCD_STORE.getTarget();
	}

	BundleContext bundleContext;
	PropHelper params;

	WatchableHashParam localHashParam = new WatchableHashParam();

	public HDNodeInfoProvider(BundleContext bundleContext) {
		super();
		params = new PropHelper(bundleContext);
		this.bundleContext = bundleContext;

	}

	@Validate
	public synchronized void startup() {
		log.info("启动中...@" + bundleContext);
		try {
			log.debug("create:NodeInfoImpl:");
			final String remoteip = params.get("hd.remote.ip", "127.0.0.1");
			final int port = params.get("hd.remote.port", 5100);
			final String enabled = params.get("hd.remote.enabled", "true");
			if (StringUtils.equalsIgnoreCase(enabled, "true") || StringUtils.equalsIgnoreCase(enabled, "1") || StringUtils.equalsIgnoreCase(enabled, "on")) {

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							Thread.sleep(10 * 1000);
							MemberSvcInfo svc = new MemberSvcInfo();
							svc.setAuditstatus("audit_ok");
							svc.setCoreconn(params.get("hd.remote.coreconn", 10));
							svc.setMaxconn(params.get("hd.remote.maxconn", 100));
							svc.setHealthy("true");
							svc.setNodeid("remote_db");
							svc.setOutport(port);
							svc.setOutaddr(remoteip);
							svc.setInaddr(remoteip);
							svc.setInport(port);
							svc.setUp(true);
							svc.setRole("member");
							svc.setOrg("vws");
							svc.setToken(params.get("hd.remote.token", "vws123456"));
							localHashParam.put("/zippo/members/vws", JsonSerializer.getInstance().formatToString(svc));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}).start();

			}

		} catch (Exception e) {
			log.warn("consensus start error", e);
		}
		log.info("启动完成...");
	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");

		log.info("退出完成...");
	}

	@Override
	public synchronized DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		return localHashParam;
	}

	@Override
	public String[] getContextConfigs() {
		return null;
	}

}
