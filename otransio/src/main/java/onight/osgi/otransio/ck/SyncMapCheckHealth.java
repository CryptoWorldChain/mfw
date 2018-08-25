package onight.osgi.otransio.ck;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
						log.debug("add to rewrite GCMD=" + pt.getPack().getGlobalCMD() + ",packid=" + resendid);
						removed.add(resendid);
						pt.setWriteTime(-1);
						pt.getPackQ().offer(pt.getPack(), pt.getHandler());
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
