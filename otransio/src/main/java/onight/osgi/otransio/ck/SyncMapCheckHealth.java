package onight.osgi.otransio.ck;

import java.util.ArrayList;
import java.util.Enumeration;
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
	public void removeDuplicateMap() {

		try {
			ArrayList<String> rmkeys = new ArrayList<>();
			Enumeration<String> en = mss.getDuplicateCheckMap().keys();
			while (en.hasMoreElements()) {
				String key = en.nextElement();
				try {
					Long tt = mss.getDuplicateCheckMap().get(key);
					if (tt != null && System.currentTimeMillis() - tt >= mss.getResendTimeOutMS()) {
						rmkeys.add(key);
					}
				} catch (Exception e) {
					log.error("error in checking map:",e);
				}
			}
			for(String key:rmkeys)
			{
				mss.getDuplicateCheckMap().remove(key);
			}
		} catch (Exception e) {
			log.error("error in checking map:",e);
		}
	}

	@Override
	public void run() {
		try {
			log.debug("Sync MapsCheckHealth:--START");
			Iterator<PacketTuple> it = mss.getResendMap().values().iterator();
			long checkTime = System.currentTimeMillis();
			List<String> removed = new ArrayList<>();
			while (it.hasNext()) {
				PacketTuple pt = it.next();
				String resendid = pt.getPack().getExtStrProp(PacketQueue.PACK_RESEND_ID);
				try {
					if (pt.getRewriteTimes() >= mss.getResendTryTimes()) {
						removed.add(resendid);
					} else if (resendid != null && !pt.isResponsed() && pt.getWriteTime() > 0
							&& checkTime - pt.getWriteTime() >= mss.getResendTimeMS()) {
						// String destTO =
						// pt.getPack().getExtStrProp(OSocketImpl.PACK_TO);
						// String from =
						// pt.getPack().getExtStrProp(OSocketImpl.PACK_FROM);

						// log.debug("add to rewrite GCMD=" +
						// pt.getPack().getFixHead().getCmd()+pt.getPack().getFixHead().getModule()
						// +",rewritetimes="+pt.getRewriteTimes()
						// + ",packid=" + resendid
						// + ",to=" + destTO
						// + ",from=" + from
						// +",qname="+pt.getPackQ().getName()+",to:"+pt.getPackQ().getCkpool().ip+":"+pt.getPackQ().getCkpool().port);
						mss.getResendTimes().incrementAndGet();
						removed.add(resendid);
						pt.setWriteTime(-1);
						if(pt.getRewriteTimes()<=0){
							mss.getResendPacketTimes().incrementAndGet();
						}
						pt.setRewriteTimes(pt.getRewriteTimes() + 1);
						pt.getPackQ().offer(pt);
					}
				} catch (Exception e) {

				}
			}
			for (String key : removed) {
				mss.getResendMap().remove(key);
			}

		} catch (Exception e) {
			log.error("error In PacketMapCheck thread:", e);
		} finally {
			
		}
		removeDuplicateMap();
		
		log.debug("SyncMapsCheckHealth: -- END");
	}

}
