package onight.tfw.async;

import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.ntrans.api.FilterManager;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.pool.ReusefulLoopPool;

public class CompleteHandlerWrapper implements CompleteHandler {
	CompleteHandler handler;
	FilterManager fm;
	ActWrapper act;

	public static ReusefulLoopPool<CompleteHandlerWrapper> objPool = new ReusefulLoopPool<>();

	public void reset(CompleteHandler handler, FilterManager fm, ActWrapper act) {
		this.handler = handler;
		this.fm = fm;
		this.act = act;
	}

	@Override
	public void onFinished(FramePacket endpack) {
		if (act != null) {
			try {
				handler.onFinished(endpack);
			} finally {
				fm.onCompleteListner(act, endpack);
				if (objPool.size() < ActorRunner.actorPoolSize) {
					this.reset(null, fm, null);
					objPool.retobj(this);
				}
			}
		}

	}

	@Override
	public void onFailed(Exception e) {
		try {
			handler.onFailed(e);
		} finally {
			fm.onErrorListner(act, null);
			if (objPool.size() < ActorRunner.actorPoolSize) {
				objPool.retobj(this);
			}
		}
	}

}
