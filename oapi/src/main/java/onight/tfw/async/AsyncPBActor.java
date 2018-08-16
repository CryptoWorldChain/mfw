
package onight.tfw.async;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.Message;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.PBActor;
import onight.tfw.otransio.api.PackHeader;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.pool.ReusefulLoopPool;

@Slf4j
public abstract class AsyncPBActor<T extends Message> extends PBActor<T> {

	long def_timeout = new PropHelper(null).get("tfw.async.timeout", 60000);

	@Override
	public void doWeb(final HttpServletRequest req, final HttpServletResponse resp, final FramePacket pack)
			throws IOException {
		resp.setCharacterEncoding("UTF-8");

		pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_REQUEST, req);
		pack.getExtHead().append(PackHeader.EXT_IGNORE_HTTP_RESPONSE, resp);
		AsyncContext asyncContext = req.startAsync();

		String timeout = req.getHeader("tfw_timeout");
		if (StringUtils.isNumeric(timeout)) {
			asyncContext.setTimeout(Integer.parseInt(timeout));
		} else {
			asyncContext.setTimeout(def_timeout);
		}
		ActorRunner act = ActorRunner.actorPool.borrow();
		if (act == null) {
			act = new ActorRunner();
		}
		act.reset(pack, resp, asyncContext, this);
		asyncContext.start(act);
	}
}
