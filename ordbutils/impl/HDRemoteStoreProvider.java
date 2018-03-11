package org.fc.hzq.orcl.impl;

import java.util.HashMap;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.fc.zippo.ordbutils.rest.RestfulDBStoreProvider;
import org.osgi.framework.BundleContext;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.ordb.StaticTableDaoSupport;
import onight.tfw.otransio.api.IPacketSender;
import onight.tfw.otransio.api.PSender;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.PacketFilter;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.proxy.IActor;

@iPojoBean
@Slf4j
@Provides(specifications = { IActor.class, StoreServiceProvider.class, ActorService.class ,PSenderService.class}, strategy = "SINGLETON")
@Instantiate
public class HDRemoteStoreProvider extends RestfulDBStoreProvider implements IActor, PSenderService,ActorService {

	BundleContext bundleContext;
	public static String PACK_TO = PackHeader.EXT_HIDDEN + "_" + "to";

	HashMap<String, RemoteDataService> rdsByDaoName = new HashMap<>();

	public HDRemoteStoreProvider(BundleContext bundleContext) {
		super(bundleContext);
		this.bundleContext = bundleContext;
	}

	// @Override
	// public String getProviderid() {
	// return ServiceSpec.MYSQL_STORE.getTarget();
	// }

	@AllArgsConstructor
	public class SqlMapperInfo {
		Object sqlmapper;
		String sf;
	}

	@PSender(name="transio")
	IPacketSender sender;

	DeferSender dsender = new DeferSender(sender);

	public IPacketSender getSender() {
		return sender;
	}

	public void setSender(IPacketSender sender) {
		log.debug("setSender:" + sender + ",this.sender=" + this.sender);
		this.sender = sender;
		dsender.setSender(sender);
	}

	@Validate
	public synchronized void startup() {
		log.info("启动完成...");
		log.debug("startup HDRemoteStoreProvider");
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(10000);
						FramePacket fp = PacketHelper.genSyncPack("HHB", "LOG", "");
						fp.putHeader(PACK_TO, "vws");
						FramePacket ret = sender.send(fp, 60000);
						log.debug("check::"+ret);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

	}

	@Invalidate
	public void shutdown() {
		log.info("退出中...");
		log.info("退出完成...");
	}

	@Override
	public DomainDaoSupport getDaoByBeanName(DomainDaoSupport dao) {
		RemoteDataService rd = rdsByDaoName.get(dao.getDomainName());
		if (rd == null) {
			rd = new RemoteDataService(dsender, dao.getDomainName());
			rdsByDaoName.put(dao.getDomainName(), rd);
		}
		return rd;
	}

	public StaticTableDaoSupport getStaticDao(String beanname) {
		return null;
	}

	public Object getApplicationCtx() {
		return null;
	}

	@Override
	public String[] getContextConfigs() {
		return new String[] {};
	}

	@Override
	public String getProviderid() {
		return "hdremote";
	}

	@Override
	public String[] getWebPaths() {
		return new String[] { "/hdremote" };
	}

	@Override
	public String[] getCtrlPaths() {
		return new String[] { "org.fc.hd.ordbgens" };
	}

	@Override
	public PacketFilter[] getFilters() {
		return new PacketFilter[] {};
	}

	@Override
	public String getModule() {
		return "rdb";
	}

}