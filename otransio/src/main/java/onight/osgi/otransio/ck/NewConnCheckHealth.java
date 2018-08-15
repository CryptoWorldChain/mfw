package onight.osgi.otransio.ck;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ICloseType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@SuppressWarnings({ "rawtypes" })
public class NewConnCheckHealth {

	public HashMap<String, List<Connection<?>>> connByIP = new HashMap<>();

	public static String CONN_AUTH_INFO = "org.zippo.conn.auth.info";

	int maxConnPreIP;
	int maxConnTimeoutSec;

	boolean isStop = false;
	ScheduledThreadPoolExecutor exec;

	public NewConnCheckHealth(int maxConnPreIP, int maxConnTimeoutSec, ScheduledThreadPoolExecutor exec) {
		this.maxConnPreIP = maxConnPreIP;
		this.maxConnTimeoutSec = maxConnTimeoutSec;
		this.exec = exec;
	}

	public synchronized void addCheckHealth(final Connection conn) {
		InetSocketAddress addr = (InetSocketAddress) conn.getPeerAddress();
		final String ip = addr.getAddress().toString();
		List<Connection<?>> conns = connByIP.get(ip);

		if (conns == null) {
			conns = new ArrayList<Connection<?>>();
			connByIP.put(ip, conns);
		}
		if (conns.size() > maxConnPreIP) {
			conn.close();
		} else {
			conns.add(conn);
		}
		final List<Connection<?>> fconns = conns;
		conn.addCloseListener(new CloseListener<Closeable, ICloseType>() {
			@Override
			public void onClosed(Closeable closeable, ICloseType type) throws IOException {
				fconns.remove(conn);
				removeConnection(conn, ip);
			}

		});
		exec.schedule(new Runner(conn), maxConnTimeoutSec, TimeUnit.SECONDS);
	}

	public synchronized void removeConnection(Connection<?> conn, String ip) {
		List<Connection<?>> conns = connByIP.get(ip);
		if (conns != null && conns.size() == 0) {
			connByIP.remove(ip);
		}
	}

	class Runner implements Runnable {
		long connCreateTime;
		Connection conn;

		Runner(Connection conn) {
			this.conn = conn;
			this.connCreateTime = System.currentTimeMillis();
		}

		@Override
		public void run() {
			try {
				if (conn != null && conn.isOpen()) {
					// exec.remove(this);
					if (conn.getAttributes().getAttribute(CONN_AUTH_INFO) == null) {
						log.debug("drop connection because no auth");
						conn.close();
					}
				}

			} catch (Exception e) {
				log.debug("error In HB thread:", e);
			}
		}

	}

}
