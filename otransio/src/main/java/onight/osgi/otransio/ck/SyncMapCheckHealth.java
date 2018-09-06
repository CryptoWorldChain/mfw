package onight.osgi.otransio.ck;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.impl.OSocketImpl;
import onight.osgi.otransio.nio.PacketQueue;
import onight.osgi.otransio.nio.PacketTuple;
import onight.osgi.otransio.sm.MSessionSets;

@Slf4j
@Data
public class SyncMapCheckHealth implements Runnable {

	MSessionSets mss;

	public SyncMapCheckHealth(MSessionSets mss) {
		this.mss = mss;
	}

	@Override
	public void run() {
		try {
			log.debug("PackMapsCheckHealth:--START");
			Iterator<PacketTuple> it = mss.getResendMap().values().iterator();
			long checkTime = System.currentTimeMillis();
			List<String> removed = new ArrayList<>();
			while (it.hasNext()) {
				PacketTuple pt = it.next();
				String resendid = pt.getPack().getExtStrProp(PacketQueue.PACK_RESEND_ID);
				try {
					if (resendid != null && !pt.isResponsed() && pt.getWriteTime() > 0
							&& checkTime - pt.getWriteTime() > 3000) {
						String destTO = pt.getPack().getExtStrProp(OSocketImpl.PACK_TO);
						String from = pt.getPack().getExtStrProp(OSocketImpl.PACK_FROM);

						log.debug("add to rewrite GCMD=" + pt.getPack().getFixHead().getCmd()+pt.getPack().getFixHead().getModule()
								+",rewritetimes="+pt.getRewriteTimes()
								+ ",packid=" + resendid
								+ ",to=" + destTO
								+ ",from=" + from
								+",qname="+pt.getPackQ().getName()+",to:"+pt.getPackQ().getCkpool().ip+":"+pt.getPackQ().getCkpool().port);
						removed.add(resendid);
						pt.setWriteTime(-1);
						pt.getPackQ().offer(pt);
					}
				} catch (Exception e) {

				}
			}
		} catch (Exception e) {
			log.debug("error In PacketMapCheck thread:", e);
		} finally {
			log.debug("PackMapsCheckHealth: -- END");
		}
	}

}
