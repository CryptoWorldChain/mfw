package onight.osgi.otransio.ck;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeBuilder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;

@Slf4j
@Data
public class CheckHealth {

	ScheduledThreadPoolExecutor exec;

	protected final Attribute<Long> lastCheckHealthMS;
	protected final AttributeBuilder attributeBuilder = Grizzly.DEFAULT_ATTRIBUTE_BUILDER;

	int delay;

	public CheckHealth(int corePool, int delay) {
		exec = new ScheduledThreadPoolExecutor(corePool);
		this.delay = Math.max(5, delay);

		lastCheckHealthMS = attributeBuilder.createAttribute("Decoder.CheckHealth");

		hbpack = new FramePacket();
		FixHeader header = new FixHeader();
		header.setCmd(PackHeader.CMD_HB);
		header.setModule(PackHeader.REMOTE_MODULE);
		header.setBodysize(0);
		header.setExtsize(0);
		header.setEnctype('T');
		header.genBytes();
		hbpack.setFixHead(header);

	}

	boolean isStop = false;

	FramePacket hbpack;

	public void addCheckHealth(final Connection conn) {
		if (conn == null)
			return;
		if (lastCheckHealthMS.isSet(conn.getAttributes())) {
			return;
		}
		lastCheckHealthMS.set(conn.getAttributes(), System.currentTimeMillis());

		exec.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					if (!conn.isOpen()) {
						log.error("connetion is not open:" + conn.getLocalAddress() + ",peer=" + conn.getPeerAddress()
						+",reason="+conn.getCloseReason().getType()+":"+conn.getCloseReason().getCause());
						conn.close();
						lastCheckHealthMS.remove(conn);
						exec.remove(this);
					} else {
						if (!lastCheckHealthMS.isSet(conn.getAttributes())
								|| lastCheckHealthMS.get(conn.getAttributes()) - System.currentTimeMillis() > delay
										* 1000) {
							// conn.write(hbpack);
							lastCheckHealthMS.set(conn.getAttributes(), System.currentTimeMillis());
							// log.trace("!!CheckHealth TO:" +
							// conn.getPeerAddress() + ",From=" +
							// conn.getLocalAddress()
							// + ",pack=" + hbpack.getFixHead());
						}
					}

				} catch (Exception e) {
					log.error("error In HB thread:", e);
				}
			}
		}, 30, delay, TimeUnit.SECONDS);
	}

	class Runner implements Runnable {
		CKConnPool pool;
		ScheduledFuture future;
		NodeConnectionPool nkp;

		Runner(NodeConnectionPool nkp, CKConnPool pool) {
			this.pool = pool;
			this.nkp = nkp;
		}

		@Override
		public void run() {
			try {
				if (pool.isStop()) {
					log.error("stop Pool:" + pool.getIp() + ":" + pool.getPort() + "," + pool.getNameid());
					Iterator<Connection> it = pool.iterator();
					while (it.hasNext()) {
						try {
							Connection conn = it.next();
							if (conn.isOpen()) {
								log.error("stop Pool:" + pool.getIp() + ":" + pool.getPort() + "," + pool.getNameid()+",conn="+conn);
								conn.close();
							}
						} catch (Exception e) {
						}
					}
					if (future != null) {
						future.cancel(true);
					}
					// exec.remove(this);
				} else {
					log.debug("check health:" + pool.ip + ",port=" + pool.port + ",name=" + pool.getNameid());
					for (int i = pool.size(); i < pool.getCore() && !pool.isStop(); i++) {
						Connection conn = pool.createOneConnection(0);
						if (conn != null && conn.isOpen()) {
							pool.retobj(conn);
							addCheckHealth(conn);
						}
						// if (conn != null) {
						// log.debug("add more conn core size." +
						// conn.getPeerAddress());
						//
						// }
					}
					// if (!pool.isStop()) {
					// Enumeration<Connection> it =
					// pool.getAllObjs().elements();
					//// while (it.hasMoreElements()) {
					//// addCheckHealth(it.nextElement());
					//// }
					// }
					// if (pool.size() <= 0) {
					// // pool is stop
					// log.debug("pool is stop. no more connection,size=" +
					// pool.size() + ",core=" + pool.core);
					// pool.setStop(true);
					// nkp.removeByPool(pool);
					// future.cancel(true);
					// }
				}

			} catch (Exception e) {
				log.debug("error In HB thread:", e);
			}
		}

	}

	public void addCheckHealth(NodeConnectionPool nkp, final CKConnPool pool) {
		Runner runner = new Runner(nkp, pool);
		ScheduledFuture<?> future = exec.scheduleWithFixedDelay(runner, delay, delay * 2, TimeUnit.SECONDS);
		runner.future = future;
	}

}
