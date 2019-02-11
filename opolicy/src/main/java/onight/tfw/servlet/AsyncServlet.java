package onight.tfw.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import onight.tfw.proxy.IActor;

@WebServlet(asyncSupported = true)
public class AsyncServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1228658706010025990L;
	IActor factor;

	public AsyncServlet(IActor factor) {
		super();
		this.factor = factor;
	}

	protected void doGet(HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		// FramePacket pack = PacketHelper.buildHeaderFromHttpGet(req);
		// CompleteHandler handler = new CompleteHandler() {
		// @Override
		// public void onFinished(FramePacket packet) {
		// try {
		// postRouteListner(packet, null);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// };
		// if (preRouteListner(pack, handler)) {
		// return;
		// }
		factor.doGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		// FramePacket pack = PacketHelper.buildHeaderFromHttpPost(req);
		// CompleteHandler handler = new CompleteHandler() {
		// @Override
		// public void onFinished(FramePacket packet) {
		// try {
		// if (packet != null) {
		// resp.getOutputStream().write(PacketHelper.toTransBytes(packet));
		// }
		// postRouteListner(packet, null);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// };
		// if (preRouteListner(pack, handler)) {
		// return;
		// }
		factor.doPost(req, resp);

	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		factor.doPut(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		factor.doDelete(req, resp);
	}
}
