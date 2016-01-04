package onight.osgi.otransio.ck;

import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.beans.FixHeader;
import onight.tfw.otransio.api.beans.FramePacket;

import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ICloseType;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeBuilder;

@Slf4j
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
		hbpack.setFixHead(header);

	}

	boolean isStop = false;

	FramePacket hbpack;

	public void addCheckHealth(final Connection conn) {
		if (conn == null)
			return;
		if(lastCheckHealthMS.isSet(conn)){
			return;
		}
		lastCheckHealthMS.set(conn, System.currentTimeMillis());
		

		exec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					if (!conn.isOpen()) {
						conn.close();
						exec.remove(this);
					} else {
						conn.write(hbpack);
						lastCheckHealthMS.set(conn, System.currentTimeMillis());
						log.trace("CheckHealth TO:" + conn.getPeerAddress()+",From="+conn.getLocalAddress()+",pack="+hbpack.getFixHead());
					}

				} catch (Exception e) {
					log.debug("error In HB thread:", e);
				}
			}
		}, delay, delay, TimeUnit.SECONDS);
	}

	public void addCheckHealth(final CKConnPool pool) {
		exec.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					if (pool.isStop()) {
						exec.remove(this);
					} else {
						for (int i = pool.size(); i < pool.getCore(); i++) {
							Connection conn=pool.createOneConnection();
							addCheckHealth(conn);
						}
					}

				} catch (Exception e) {
					log.debug("error In HB thread:", e);
				}
			}
		}, 1, delay * 2, TimeUnit.SECONDS);
	}

}
