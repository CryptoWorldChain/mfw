package onight.tfw.ojpa.opm.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.ojpa.api.NoneDomainDao;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.ojpa.api.StoreServiceProvider;
import onight.tfw.ojpa.api.annotations.StoreDAO;
import onight.tfw.outils.conf.PropHelper;

@iPojoBean
@Slf4j
public class JPAStoreManager {

	private Map<String, StoreServiceProvider> storeServices = new HashMap<String, StoreServiceProvider>();

	// private List<IStoreClient> clientDaos = new
	// ArrayList<IStoreClient>();
	BundleContext btx;
	PropHelper prop;

	public JPAStoreManager(BundleContext btx) {
		this.btx = btx;
		prop = new PropHelper(btx);
	}

	class StoreClientSet {
		IJPAClient client;
		List<DomainDaoSupport> daos;
		Bundle bundle;

		public StoreClientSet(IJPAClient client, List<DomainDaoSupport> daos, Bundle bundle) {
			super();
			this.client = client;
			this.daos = daos;
			this.bundle = bundle;
		}

	}

	private Map<Integer, StoreClientSet> daosByClient = new HashMap<>();

	@Validate
	public void start() {
		log.debug("JPA Store Manager Started");
	}

	@Invalidate
	public void destory() {
		log.debug("JPA Store Manager ... End");
	}

	@Unbind(aggregate = true, optional = true)
	public void destroyStoreService(StoreServiceProvider dsp, ServiceReference ref) {
		log.debug("StoreService.quit " + dsp.getProviderid() + "@" + dsp);
		String prodid = getBundleProviderId(dsp, ref);
		storeServices.remove(prodid);
		unwireDaos(dsp, ref);
	}

	public String getBundleProviderId(StoreServiceProvider provider, ServiceReference ref) {
		String ret = provider.getProviderid();
		if (ret == null) {
			ret = "__bundle_" + ref.getBundle().getBundleId();
		}
		return ret;
	}

	@Bind(aggregate = true, optional = true)
	public void bindStoreService(StoreServiceProvider dsp, ServiceReference ref) {
		log.debug("StoreService.bind:" + dsp);
		String prodid = getBundleProviderId(dsp, ref);
		StoreServiceProvider olddsp = storeServices.get(prodid);
		if (olddsp != null) {
			if (olddsp == dsp) {
				return;
			}
			destroyStoreService(olddsp, null);
			log.debug("Overrided StoreService:" + prodid + "@old:" + olddsp + "==>new:" + dsp);
		} else {
			log.debug("Bind New StoreService:" + prodid + "@" + dsp);
		}
		storeServices.put(prodid, dsp);

		wireDaos();
	}

	@Unbind(aggregate = true, optional = true)
	public void destroyClientDao(IJPAClient storeClient) {
		log.debug("ClientDao.quit:" + storeClient + "@");
		daosByClient.remove(System.identityHashCode(storeClient));
	}

	public String getOverrideTarget(Class<?> clazz, Field field) {
		String override_target = null;
		StringBuffer sb = new StringBuffer("org.zippo.store");
		for (String v : (clazz.getName() + "." + field.getName()).split("\\.")) {
			sb.append(".").append(v);
			override_target = prop.get(sb.toString(), override_target);
		}

		return override_target;
	}

	@Bind(aggregate = true, optional = true)
	public void bindClientDao(IJPAClient storeClient, ServiceReference ref) {
		log.debug("ClientDao.bind:" + storeClient + "@" + System.identityHashCode(storeClient));
		StoreClientSet clientset = daosByClient.get(System.identityHashCode(storeClient));
		if (clientset == null) {
			List<DomainDaoSupport> daos = new ArrayList<DomainDaoSupport>();
			daosByClient.put(System.identityHashCode(storeClient),
					new StoreClientSet(storeClient, daos, ref.getBundle()));

			Class clazz = storeClient.getClass();

			for (Field field : clazz.getDeclaredFields()) {
				StoreDAO anno = field.getAnnotation(StoreDAO.class);
				DomainDaoSupport dao = null;
				if (anno != null) {
					try {
						Method setmethod = null;
						try {
							setmethod = clazz.getMethod("set" + StringUtils.capitalize(field.getName()),
									DomainDaoSupport.class);
						} catch (Exception e1) {
							setmethod = clazz.getMethod("set" + StringUtils.capitalize(field.getName()), OJpaDAO.class);
						}

						Method getmethod = clazz.getMethod("get" + StringUtils.capitalize(field.getName()));
						if (getmethod == null) {
							log.warn("DAO has not get Method:" + clazz.getName() + ",field=" + field.getName());
						}
						if (setmethod == null) {
							log.warn("DAO has not set Method" + clazz.getName() + ",field=" + field.getName());
						}
						if (getmethod == null || setmethod == null) {
							continue;
						}

						dao = (DomainDaoSupport) getmethod.invoke(storeClient);
						// if (dao == null) {

						Class domainClazz = anno.domain();
						if ((domainClazz == null || domainClazz == Object.class)
								&& field.getGenericType() instanceof ParameterizedType) {
							for (Type type : ((ParameterizedType) field.getGenericType()).getActualTypeArguments()) {
								if (type instanceof Class) {
									domainClazz = (Class) type;
									log.debug("get JPADAOType==" + domainClazz + ",type=" + type + ",typeclass="
											+ type.getClass());
								}
							}
						}

						ServiceSpec ss;
						String target = getOverrideTarget(clazz, field);
						if (StringUtils.isBlank(target)) {
							target = anno.target();
						}
						if (StringUtils.isNotBlank(target) && target.indexOf('.') > 0) {
							// sub class
							String name = target.substring(1);
							Method getNameMethod = clazz.getMethod("get" + StringUtils.capitalize(name));
							if (getNameMethod != null) {
								target = (String) getNameMethod.invoke(storeClient);
							}
						}
						ss = new ServiceSpec(target);
						Constructor ct = null;

						try {
							ct = anno.daoClass().getConstructor(ServiceSpec.class, Class.class, Class.class,
									Class.class);
							dao = (DomainDaoSupport) ct.newInstance(ss, domainClazz, anno.example(), anno.keyclass());

						} catch (NoSuchMethodException nsme) {
							ct = anno.daoClass().getConstructor(ServiceSpec.class);
							dao = (DomainDaoSupport) ct.newInstance(ss);
						}
						setmethod.invoke(storeClient, dao);
						if (dao instanceof OJpaDAO) {
							OJpaDAO ojdao = (OJpaDAO) dao;

							ojdao.setKeyField(anno.key());
							if (!StringUtils.isBlank(anno.key())) {
								List<Method> keyMethods = new ArrayList<Method>();
								for (String keyf : anno.key().split(",")) {
									try {
										keyMethods.add(
												domainClazz.getMethod("get" + StringUtils.capitalize(keyf.trim())));
									} catch (Exception e) {
										log.warn("key get method not found:" + clazz + ",field=" + field.getName());
									}
								}
								ojdao.setKeyMethods(keyMethods);
							}
						}
						// }
						daos.add(dao);
					} catch (Exception e) {
						log.debug("wair dao error:", e);
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
		log.debug("wireDaosForClient::" + clientset.client + ",daosize=" + clientset.daos.size() + ",id=@" + clientid);
		boolean AlldaoReady = true;
		for (DomainDaoSupport dao : clientset.daos) {
			String target = dao.getServiceSpec().getTarget();
			StoreServiceProvider ssp = null;
			DomainDaoSupport dds = null;

			if (StringUtils.isBlank(target)) {
				// try bundle id
				ssp = storeServices.get("__bundle_" + clientset.bundle.getBundleId());
				if (ssp != null) {
					dds = ssp.getDaoByBeanName(dao);
				}
			} else {//
				ssp = storeServices.get(target);
				if (ssp != null) {
					dds = ssp.getDaoByBeanName(dao);
				}
			}
			if (ssp == null || dds == null) {// 动态配置取默认值
				String default_target = prop.get("org.zippo.store.default_target", "orcl");
				ssp = storeServices.get(default_target);
				if (ssp != null) {
					dds = ssp.getDaoByBeanName(dao);
				}
			}

			if (ssp == null) {
				// dds = new NoneDomainDao();
				AlldaoReady = false;
				log.debug("nonDomainDao.DAO NOT READY " + clientid + ":" + dds + ",AlldaoReady=" + AlldaoReady);
				dao.setDaosupport(new NoneDomainDao());
			} else {
				log.debug("RegisterDAO:" + dao.getDomainName() + "]@" + clientid + ":" + dds);
				if (dao.getDaosupport() != dds) {
					dao.setDaosupport(dds);
					try {
						clientset.client.onDaoServiceReady(dao);
					} catch (Exception e) {
						AlldaoReady = false;
						log.error("wire dao err:" + dao, e);
					}
				}
			}
		}
		if (AlldaoReady) {
			clientset.client.onDaoServiceAllReady();
		}
	}

	public void wireDaos() {
		log.debug("wired DAO：" + daosByClient.size());
		for (Integer storeClient : daosByClient.keySet()) {
			wireDaosForClient(storeClient);
		}
	}

	public void unwireDaos(StoreServiceProvider unssp, ServiceReference<?> sf) {
		log.debug("unwried DAOs：" + daosByClient.size());
		for (Integer storeClient : daosByClient.keySet()) {
			StoreClientSet clientset = daosByClient.get(storeClient);
			if (clientset == null)
				continue;
			for (DomainDaoSupport dao : clientset.daos) {
				if (unssp == storeServices.get(dao.getServiceSpec().getTarget())) {
					dao.setDaosupport(new NoneDomainDao());
					log.debug("注销DAO:" + dao.getDomainName() + "]@" + storeClient + ":" + dao);
				}
			}
		}
		if (sf != null) {
			for (Integer storeClient : daosByClient.keySet()) {
				StoreClientSet clientset = daosByClient.get(storeClient);
				if (clientset == null)
					continue;
				if (clientset.bundle == sf.getBundle()) {
					for (DomainDaoSupport dao : clientset.daos) {
						dao.setDaosupport(new NoneDomainDao());
						log.debug("注销DAO:" + dao.getDomainName() + "]@" + storeClient + ":" + dao);
					}
				}
			}
		}
	}

}
