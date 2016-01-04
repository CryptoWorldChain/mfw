package onight.tfw.orouter.impl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.exception.MessageException;
import onight.tfw.ntrans.api.exception.TimeOutMessageException;
import onight.tfw.orouter.api.IQClient;
import onight.tfw.orouter.api.IRecievier;
import onight.tfw.orouter.api.NoneQService;
import onight.tfw.orouter.api.QService;
import onight.tfw.otransio.api.PSender;
import onight.tfw.otransio.api.PSenderService;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.serialize.SerializerUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Channel;

/**
 * 管理队列的
 * 
 * @author brew
 *
 */
@Component(immediate = true)
@Instantiate(name = "exqueuemanager")
@Slf4j
@Provides
public class ExQueueManager implements QService {

	BundleContext context;

	public ExQueueManager(BundleContext context) {
		this.context = context;
	}

	HashSet<String> queueCreated = new HashSet<String>();

	List<IQClient> wishlist = new ArrayList<IQClient>();

	@Getter
	CachingConnectionFactory cf;

	private PropHelper params;

	private ThreadPoolExecutor readExecPool;
	ScheduledThreadPoolExecutor schPool;

	@Validate
	public void startup() {
		if (schPool != null)
			return;
		log.info("启动中.....");

		params = new PropHelper(context);
		params.init();

		initConnections();
		// int sc = params.get("rabbit.send.threads", 2);
		// sendExecPool = new ThreadPoolExecutor(sc + 1, (sc + 1) * 2, 60,
		// TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

		int tc = params.get("rabbit.fetch.threads", 2);

		readExecPool = new ThreadPoolExecutor(tc + 1, (tc + 1) * 2, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

		readExecPool.execute(new Runnable() {

			@Override
			public void run() {
				ensureQueue("_I_" + params.get("otrans.node.qid", "__"));
				synchronized (wishlist) {
					for (IQClient qproxy : wishlist) {
						qproxy.setQService(ExQueueManager.this);
						qproxy.onQServiceReady();
						log.info("bindQProxy from wishlist OK:" + qproxy);
					}
					wishlist.clear();
					schPool = new ScheduledThreadPoolExecutor(10);
				}
			}
		});

		log.info("启动成功");
	}

	@Invalidate
	public void contextDestroyed() {
		log.warn("关闭中……........");
		// if (sendExecPool != null) {
		// sendExecPool.shutdown();
		// }
		if (readExecPool != null) {
			readExecPool.shutdown();
		}
		if (schPool != null) {
			schPool.shutdown();
		}
		for (IQClient proxy : clientByProxyID.values()) {
			destroyMessageListener(proxy);
		}

		consumerByProxyHashID.clear();
		clientByProxyID.clear();
		log.warn("关闭与MQ链接中。。。:" + consumerByProxyHashID.keySet().size());

		cf.destroy();
		wishlist.clear();
		log.warn("ExQueueManager关闭成功");
	}

	HashMap<Integer, List<QueueConsumer>> consumerByProxyHashID = new HashMap<>();
	HashMap<Integer, IQClient> clientByProxyID = new HashMap<>();

	public ConnectionFactory initConnections() {
		if (cf == null) {
			cf = new CachingConnectionFactory();
			cf.setConnectionCacheSize(params.get("ofw.rmq.conn.size", 20));
			cf.setAddresses(params.get("ofw.rmq.addresses", "172.30.12.44:5672"));
			cf.setUsername(params.get("ofw.rmq.user", "guest"));
			cf.setConnectionTimeout(params.get("ofw.rmq.conn.timeoutms", 60 * 1000));
			cf.setPassword(params.get("ofw.rmq.pwd", "guest"));
			cf.setVirtualHost(params.get("ofw.rmq.vhost", "/"));
		}
		return cf;
	}

	protected int getQueueCount(RabbitAdmin admin, final String name) {
		DeclareOk declareOk = admin.getRabbitTemplate().execute(new ChannelCallback<DeclareOk>() {
			public DeclareOk doInRabbit(Channel channel) throws Exception {
				return channel.queueDeclarePassive(name);
			}
		});
		log.info("consumerCount:", declareOk.getConsumerCount());
		return declareOk.getMessageCount();
	}

	public boolean ensureQueue(String queuename) {
		if (StringUtils.isBlank(queuename))
			return false;
		if (queueCreated.contains(queuename))
			return true;
		synchronized (queueCreated) {
			if (queueCreated.contains(queuename))
				return true;
			RabbitAdmin admin = new RabbitAdmin(cf);
			Queue queue = new Queue(queuename);
			admin.declareQueue(queue);
			TopicExchange exchange = new TopicExchange(queuename);
			admin.declareExchange(exchange);
			admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("*"));
			queueCreated.add(queuename);
		}
		return true;
	}

	@Override
	public void removeQueue(String queuename) {
		RabbitAdmin admin = new RabbitAdmin(cf);
		admin.deleteQueue(queuename);
		admin.deleteExchange(queuename);
		queueCreated.remove(queuename);
	}

	public void sendMessage(final String ex, final Object wmsg) {
		RabbitTemplate template = new RabbitTemplate(cf);
		ensureQueue(ex);
		try {
			template.convertAndSend(ex, "*", SerializerUtil.toBytes(wmsg));
			log.debug("[SEND.OK]:", wmsg);
		} catch (AmqpException e) {
			log.debug("[SEND.error]:", wmsg);
			e.printStackTrace();
		}
	}

	public void reQueueTask(Runnable run, int delay) {
		if (delay == 0) {
			readExecPool.execute(run);
		} else {
			schPool.schedule(run, delay, TimeUnit.MICROSECONDS);
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void destroyMessageListener(IQClient invokerObj) {
		log.info("清除Client:for " + invokerObj);
		int id = System.identityHashCode(invokerObj);
		List<QueueConsumer> qcs = consumerByProxyHashID.get(id);
		if (qcs != null && qcs.size() > 0) {
			val names = new HashSet<String>();
			for (QueueConsumer qc : qcs) {
				names.add(qc.getQueueName());
				qc.setStop(true);
			}
			log.info("清除队列监控:[" + names + "]");

		}
		consumerByProxyHashID.remove(id);
		clientByProxyID.remove(id);
		if (invokerObj != null) {
			invokerObj.setQService(new NoneQService());
		}
	}

	@Bind(aggregate = true, optional = true)
	public void bindProxy(IQClient qproxy) {
		synchronized (wishlist) {
			if (schPool == null) {
				wishlist.add(qproxy);
				log.info("bindQProxy to wish list:" + qproxy);

			} else {
				log.info("bindQProxy:" + qproxy);
				qproxy.setQService(this);
				qproxy.onQServiceReady();
			}
		}

	}

	QSender qPackSender = new QSender();

	@Bind(aggregate = true, optional = true)
	public void bindPSender(PSenderService pl) {
		log.info("Register PSender::" + pl + ",sender=");
		qPackSender.setQService(this);
		Class clazz = pl.getClass();
		for (Field field : clazz.getDeclaredFields()) {
			PSender anno = field.getAnnotation(PSender.class);
			if (anno != null && (anno.name().equals("qsender"))) {
				try {
					PropertyDescriptor pd;
					try {
						pd = new PropertyDescriptor(field.getName(), clazz);
						Method wm = pd.getWriteMethod();
						wm.invoke(pl, qPackSender);
					} catch (IntrospectionException e) {
						log.warn("cannot init bindPSender class=" + clazz + ",field=" + field.getName(), e);
					}
				} catch (Exception e) {
				}
			}
		}
	}

	@Unbind(aggregate = true, optional = true)
	public void unbindPSender(PSenderService pl) {
		log.info("Remove PSender::" + pl);
	}

	public synchronized void createMessageListener(IQClient proxy, String qName, IRecievier reciever, int delay, int sharedThreadCount) {
		log.info("CreateMessageCallback on " + proxy + ",@" + qName + ",delay=" + delay + ",tc=" + sharedThreadCount);
		if (delay == 0) {
			delay = params.get("rabbit.fetch.delay", 5000);
		}

		if (sharedThreadCount == 0) {
			sharedThreadCount = params.get("rabbit.fetch.threads", 2);
		}

		int id = System.identityHashCode(proxy);

		if (ensureQueue(qName)) {
			List<QueueConsumer> qcs = consumerByProxyHashID.get(id);
			if (qcs == null) {
				qcs = new ArrayList<QueueConsumer>();
				consumerByProxyHashID.put(id, qcs);
				clientByProxyID.put(id, proxy);
			}
			for (int i = 0; i < sharedThreadCount; i++) {
				QueueConsumer qc = new QueueConsumer(qName, delay, reciever, this);
				qcs.add(qc);
				schPool.schedule(qc, delay, TimeUnit.MICROSECONDS);
			}
		}
	}

	@Override
	public int getQueueSize(String ex) {
		RabbitAdmin admin = new RabbitAdmin(cf);
		return getQueueCount(admin, ex);
	}

	@Override
	public Object syncSendMessage(String ex, String qName, Object message) throws MessageException {

		log.debug("syncSendMessage on queuename=" + qName);
		int delay = params.get("rabbit.fetch.delay", 5000);
		RabbitAdmin admin = new RabbitAdmin(cf);
		Queue queue = new Queue(qName, false, false, true);
		admin.declareQueue(queue);
		TopicExchange exchange = new TopicExchange(qName);
		admin.declareExchange(exchange);
		admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("*"));
		final CountDownLatch cdl = new CountDownLatch(1);
		final LinkedList<Object> result = new LinkedList<Object>();
		final QueueConsumer qc = new QueueConsumer(qName, delay, new IRecievier() {
			@Override
			public boolean onMessage(String ex, Serializable wmsg) {
				cdl.countDown();
				result.offer(wmsg);
				return true;
			}
		}, this);
		sendMessage(ex, message);
		schPool.execute(qc);
		try {
			if (cdl.await(60, TimeUnit.SECONDS)) {

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		removeQueue(qName);

		if (cdl.getCount() == 0 || result.size() <= 0) {
			throw new TimeOutMessageException("60");
		} else {
			return result.poll();
		}
	}

}
