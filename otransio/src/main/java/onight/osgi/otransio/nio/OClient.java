package onight.osgi.otransio.nio;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.sm.OutgoingSessionManager;
import onight.tfw.outils.conf.PropHelper;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OClient {
	Logger log = LoggerFactory.getLogger(OClient.class);

	public TCPNIOTransport transport;
	OSocketImpl oimpl;
	public OClient(OSocketImpl oimpl) {
		// init();
		this.oimpl = oimpl;
	}

	public void init(OutgoingSessionManager sm, PropHelper params) {
		FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
		filterChainBuilder.add(new TransportFilter());
		filterChainBuilder.add(new OTransFilter());
		SessionFilter sf = new SessionFilter(oimpl, params);
		filterChainBuilder.add(sf);

		// Create TCP transport
		transport = TCPNIOTransportBuilder.newInstance().build();
		transport.setProcessor(filterChainBuilder.build());
		ThreadPoolConfig ktpc = ThreadPoolConfig.defaultConfig();
		ktpc.setCorePoolSize(params.get("otransio.ckernel.core", 10)).setMaxPoolSize(params.get("otransio.ckernel.max", 100));
		transport.setKernelThreadPoolConfig(ktpc);

		ThreadPoolConfig wtpc = ThreadPoolConfig.defaultConfig();
		wtpc.setCorePoolSize(params.get("otransio.cworker.core", 10)).setMaxPoolSize(params.get("otransio.cworker.max", 100));
		transport.setWorkerThreadPoolConfig(wtpc);

		transport = TCPNIOTransportBuilder.newInstance().build();
		transport.setProcessor(filterChainBuilder.build());

		try {
			transport.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Connection getConnection(String address, int port) throws InterruptedException, ExecutionException, TimeoutException {
		return transport.connect(address, port).get(10,TimeUnit.SECONDS);
	}

	public void stop() {
		try {
			transport.shutdownNow();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			log.info("socket服务关闭");
		}
	}

}
