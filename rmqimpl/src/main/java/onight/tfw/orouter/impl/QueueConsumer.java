package onight.tfw.orouter.impl;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.exception.DropMessageException;
import onight.tfw.ntrans.api.exception.RejectMessageException;
import onight.tfw.orouter.api.IRecievier;
import onight.tfw.outils.serialize.SerializerUtil;

import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

@Slf4j
public class QueueConsumer implements Runnable {

	@Getter
	protected String queueName;

	private ExQueueManager qman;

	@Setter
	boolean stop = false;

	int delay = 100;

	IRecievier callback;

	public QueueConsumer(String queueName, int delay, IRecievier callback,
			ExQueueManager qman) {
		this.queueName = queueName;
		this.delay = delay;
		this.callback = callback;
		this.qman = qman;
	}

	@Override
	public void run() {
		if (stop)
			return;
		int nextdelay = delay;
		try {
			RabbitTemplate template = new RabbitTemplate(qman.getCf());
			nextdelay = template.execute(new ChannelCallback<Integer>() {
				@Override
				public Integer doInRabbit(final Channel channel)
						throws Exception {
					GetResponse response = channel.basicGet(queueName, false);
					if (response != null) {
						byte[] msg = response.getBody();
						log.debug("[RECV]::"+msg);
						boolean rm = false;
						try {
							rm = onMessage(queueName, msg);
						} catch (Exception e) {
							log.error("onMessageError::" + msg, e);
						} finally {
							if (rm) {
								channel.basicAck(response.getEnvelope()
										.getDeliveryTag(), false);
								return 0;
							} else {
								channel.basicNack(response.getEnvelope()
										.getDeliveryTag(), false, true);
							}
						}
					}
					return delay;
				}
			});

		} catch (Throwable t) {

		} finally {
			if (!stop) {
				//log.debug("reQueueTask:delay="+delay+";@"+this.getQueueName());
				qman.reQueueTask(this, nextdelay);
			}
		}
	}

	public boolean onMessage(String qname, byte[] msg) {
		log.trace("onMessage:" + msg);
		try {
			Object ret = callback.onMessage(qname, (Serializable)SerializerUtil.fromBytes(msg));
			if (ret != null && ret instanceof Boolean) {
				return (boolean) ret;
			}
		} catch (RejectMessageException e) {
			log.warn("QueueMsg:Reject:[" + queueName + "]:" + msg);
			return false;
		} catch (DropMessageException e) {
			log.warn("QueueMsg:Drop:[" + queueName + "]:" + msg);
			return true;
		} catch (Exception e) {
			log.error("Unknow Message:Drop:" + msg);
			e.printStackTrace();
		}
		return true;
	}

}
