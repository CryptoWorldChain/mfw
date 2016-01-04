package onight.tfw.ojpa.opm.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.ojpa.api.NoneDomainDao;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.api.annotations.StoreDAO;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;

@iPojoBean
@Slf4j
public class JPAStoreManager {

	private Map<String, StoreServiceProvider> storeServices = new HashMap<String, StoreServiceProvider>();

	// private List<IStoreClient> clientDaos = new
	// ArrayList<IStoreClient>();

	class StoreClientSet {
		IJPAClient client;
		List<OJpaDAO> daos;

		public StoreClientSet(IJPAClient client, List<OJpaDAO> daos) {
			super();
			this.client = client;
			this.daos = daos;
		}

	}

	private Map<Integer, StoreClientSet> daosByClient = new HashMap<>();


	@Validate
	public void start() {
		log.info("JPA管理端启动");
	}

	@Invalidate
	public void destory() {
		log.info("JPA管理端退出");
	}

	@Unbind(aggregate = true, optional = true)
	public void destroyStoreService(StoreServiceProvider dsp) {
		log.info("StoreService.quit " + dsp.getProviderid() + "@" + dsp);

		unwireDaos(dsp);
	}

	@Bind(aggregate = true, optional = true)
	public void bindStoreService(StoreServiceProvider dsp) {
		log.info("StoreService.bind:" + dsp);
		StoreServiceProvider olddsp = storeServices.get(dsp.getProviderid());
		if (olddsp != null) {
			if (olddsp == dsp) {
				return;
			}
			destroyStoreService(olddsp);
			log.warn("绑定已经存在的存储服务,即将被覆盖:" + dsp.getProviderid() + "@old:" + olddsp + "==>new:" + dsp);
		} else {
			log.info("绑定存储服务:" + dsp.getProviderid() + "@" + dsp);
		}
		storeServices.put(dsp.getProviderid(), dsp);
		wireDaos();
	}

	@Unbind(aggregate = true, optional = true)
	public void destroyClientDao(IJPAClient storeClient) {
		log.info("ClientDao.quit:" + storeClient + "@");
		daosByClient.remove(System.identityHashCode(storeClient));
	}

	@Bind(aggregate = true, optional = true)
	public void bindClientDao(IJPAClient storeClient) {
		log.info("ClientDao.bind:" +storeClient+"@"+ System.identityHashCode(storeClient));
		StoreClientSet clientset = daosByClient.get(System.identityHashCode(storeClient));
		if (clientset == null) {
			List<OJpaDAO> daos = new ArrayList<OJpaDAO>();
			daosByClient.put(System.identityHashCode(storeClient), new StoreClientSet(storeClient, daos));
			Class clazz = storeClient.getClass();
			for (Field field : clazz.getDeclaredFields()) {
				StoreDAO anno = field.getAnnotation(StoreDAO.class);
				OJpaDAO dao = null;
				if (anno != null && (anno.domain() != Object.class)) {
					try {
						Method setmethod = clazz.getMethod("set" + StringUtils.capitalize(field.getName()), OJpaDAO.class);
						Method getmethod = clazz.getMethod("get" + StringUtils.capitalize(field.getName()));
						if(getmethod==null){
							log.warn("DAO没有get方法:"+clazz.getName()+",field="+field.getName());
						}
						if(setmethod==null){
							log.warn("DAO没有set方法:"+clazz.getName()+",field="+field.getName());
						}
						if(getmethod==null||setmethod==null){
							continue;
						}

						dao = (OJpaDAO) getmethod.invoke(storeClient);
						if (dao == null) {
							ServiceSpec ss = new ServiceSpec(anno.target());
							dao = new OJpaDAO(ss, anno.domain(),anno.example(),anno.keyclass());
							setmethod.invoke(storeClient, dao);
							dao.setKeyField(anno.key());
							if (!StringUtils.isBlank(anno.key())) {
								List<Method> keyMethods = new ArrayList<Method>();
								for (String keyf : anno.key().split(",")) {
									try {
										keyMethods.add(anno.domain().getMethod("get" + StringUtils.capitalize(keyf.trim())));
									} catch (Exception e) {
										log.warn("key get method not found:"+clazz+",field="+field.getName());
									}
								}
								dao.setKeyMethods(keyMethods);
							}
						}
						daos.add(dao);
					} catch (Exception e) {
					}
				}
			}
			log.debug("Found Daos from " + storeClient + ":@" + this);
		}
		wireDaosForClient(System.identityHashCode(storeClient));
	}

	public void wireDaosForClient(Integer clientid) {
		StoreClientSet clientset = daosByClient.get(clientid);
		if (clientset == null)
			return;
		log.debug("wireDaosForClient::" + clientset.client + ",daosize=" + clientset.daos.size() + "@" + clientid);

		for (OJpaDAO dao : clientset.daos) {
			StoreServiceProvider ssp = storeServices.get(dao.getServiceSpec().getTarget());
			DomainDaoSupport dds;
			if (ssp == null) {
				// dds = new NoneDomainDao();
				dao.setDaosupport(new NoneDomainDao());
			} else {
				dds = ssp.getDaoByBeanName(dao);
				log.debug("RegisterDAO:" + dao.getDomainName() + "]@" + clientid + ":" + dds);
				if (dao.getDaosupport() != dds) {
					dao.setDaosupport(dds);
					try {
						clientset.client.onDaoServiceReady(dao);
					} catch (Exception e) {
						log.error("wire dao err:"+dao,e);
					}
				}
			}
		}
		clientset.client.onDaoServiceAllReady();
	}

	public void wireDaos() {
		log.info("组织DAO：" + daosByClient.size());
		for (Integer storeClient : daosByClient.keySet()) {
			wireDaosForClient(storeClient);
		}
	}

	public void unwireDaos(StoreServiceProvider unssp) {
		log.info("卸载DAOs：" + daosByClient.size());
		for (Integer storeClient : daosByClient.keySet()) {
			StoreClientSet clientset = daosByClient.get(storeClient);
			if (clientset == null)
				continue;
			for (OJpaDAO dao : clientset.daos) {
				if (unssp == storeServices.get(dao.getServiceSpec().getTarget())) {
					dao.setDaosupport(new NoneDomainDao());
					log.debug("注销DAO:" + dao.getDomainName() + "]@" + storeClient + ":" + dao);
				}
			}
		}
	}

}
