package onight.osgi.otransio.nio;

import java.io.IOException;

import org.apache.felix.ipojo.annotations.Invalidate;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import onight.osgi.otransio.impl.OSocketImpl;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.outils.conf.PropHelper;

public class OServer {

	Logger log = LoggerFactory.getLogger(OServer.class);
	private TCPNIOTransport transport;

	public int PORT = 5100;

	public void startServer(OSocketImpl listener, PropHelper params) {
		// Create a FilterChain using FilterChainBuilder
		FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
		// Add TransportFilter, which is responsible
		// for reading and writing data to the connection
		filterChainBuilder.add(new TransportFilter());

		// StringFilter is responsible for Buffer <-> String conversion
		filterChainBuilder.add(new OTransFilter());

		// 登录协议
		// filterChainBuilder.add(new AuthFilter());

		// EchoFilter is responsible for echoing received messages
		SessionFilter sf = new SessionFilter(listener, params);
		filterChainBuilder.add(sf);

		// Create TCP transport
		transport = TCPNIOTransportBuilder.newInstance().build();
		transport.setProcessor(filterChainBuilder.build());
		ThreadPoolConfig ktpc = ThreadPoolConfig.defaultConfig();
		ktpc.setCorePoolSize(params.get("otrans.kernel.core", 10)).setMaxPoolSize(params.get("otrans.kernel.max", 100));
		transport.setKernelThreadPoolConfig(ktpc);

		ThreadPoolConfig wtpc = ThreadPoolConfig.defaultConfig();
		wtpc.setCorePoolSize(params.get("otrans.worker.core", 10)).setMaxPoolSize(params.get("otrans.worker.max", 100));
		transport.setWorkerThreadPoolConfig(wtpc);
		transport.setIOStrategy(LeaderFollowerNIOStrategy.getInstance());
		transport.setTcpNoDelay(true);
		transport.setKeepAlive(true);
		transport.setOptimizedForMultiplexing(true);
		transport.setClientSocketSoTimeout(60*1000);
		try {
			// binding transport to start listen on certain host and port
			int oport = NodeHelper.getCurrNodeListenInPort();
			log.debug("port=" + oport);
			transport.bind(NodeHelper.getCurrNodeListenInAddr(), oport);
			log.info("socket server started:port = " + oport);
			transport.start();
		} catch (IOException e) {
			log.error("socket server failed to start:", e);

			e.printStackTrace();
		} finally {

		}
	}

	@Invalidate
	public void stop() {
		try {
			transport.shutdownNow();
		} catch (IOException e) {
		} finally {
			log.info("socket server shutdown ");
		}

	}

}
