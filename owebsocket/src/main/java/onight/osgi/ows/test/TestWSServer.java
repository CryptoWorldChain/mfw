package onight.osgi.ows.test;

import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Hex;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.DefaultWebSocket;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestWSServer extends WebSocketApplication implements Runnable {

	public static void main(String[] args) {
		System.out.println("Hello");
		final HttpServer server = HttpServer.createSimpleServer("./www", 8081);
		WebSocketAddOn addon = new WebSocketAddOn();
		for (NetworkListener listener : server.getListeners()) {
			listener.registerAddOn(addon);
		}
		final TestWSServer app = new TestWSServer();
		WebSocketEngine.getEngine().register("/ows", "/test/*", app);
		try {
			server.start();
			new Thread(app).start();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					System.out.println("end");
					server.shutdownNow();
				}
			}, "Server-Showdown-Thread"));

			Thread.sleep(100000000L);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		System.out.println("onClose::" + socket + ",frame=" + frame);
		super.onClose(socket, frame);
	}

	@Override
	public void onConnect(WebSocket socket) {
		System.out.println("onConnect::" + socket);
		super.onConnect(socket);
	}

	@Override
	public boolean remove(WebSocket socket) {
		System.out.println("remove::" + socket);
		return super.remove(socket);
	}

	@Override
	protected boolean onError(WebSocket webSocket, Throwable t) {
		System.out.println("onError::" + webSocket + ":" + t);
		return super.onError(webSocket, t);
	}

	@Override
	public void onMessage(WebSocket socket, String text) {
		System.out.println("onMessage:" + socket + ",text=" + text);
		super.onMessage(socket, text);
	}

	@Override
	public void onMessage(WebSocket socket, byte[] bytes) {
		System.out.println("onMessage:" + socket + ",byte=" + Hex.encodeHexString(bytes));
		super.onMessage(socket, bytes);
	}

	@Override
	public void onPing(WebSocket socket, byte[] bytes) {
		System.out.println("onPing:" + socket + ",byte=" + Hex.encodeHexString(bytes));
		super.onPing(socket, bytes);
	}

	@Override
	public void onPong(WebSocket socket, byte[] bytes) {
		System.out.println("onPong:" + socket + ",byte=" + Hex.encodeHexString(bytes));
		super.onPong(socket, bytes);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(1000);
				for (WebSocket ws : this.getWebSockets()) {
					ws.send("time tick:" + System.currentTimeMillis());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public WebSocket createSocket(ProtocolHandler handler, HttpRequestPacket requestPacket,
			WebSocketListener... listeners) {
		WebSocket ws = super.createSocket(handler, requestPacket, listeners);
		return ws;
	}

	@Override
	protected boolean add(WebSocket socket) {
		DefaultWebSocket dws = (DefaultWebSocket) socket;
		HttpSession session = dws.getUpgradeRequest().getSession(true);
		System.out.println("session==" + session.getId());
		System.out.println("add.WebSocket" + socket);

		return super.add(socket);
	}

}
