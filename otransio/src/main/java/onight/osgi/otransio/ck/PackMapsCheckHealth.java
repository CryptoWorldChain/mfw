package onight.osgi.otransio.ck;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.TimeoutException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.nio.PacketTuple;
import onight.osgi.otransio.sm.MSessionSets;
import onight.tfw.async.CompleteHandler;

@Slf4j
@Data
public class PackMapsCheckHealth implements Runnable {

	MSessionSets mss;

	public PackMapsCheckHealth(MSessionSets mss) {
		this.mss = mss;
	}

	@Override
	public void run() {
		try {
			log.debug("PackMapsCheckHealth:--START");
			Enumeration<String> en = mss.getPackMaps().keys();
			ArrayList<String> rmkeys = new ArrayList<>();
			while (en.hasMoreElements()) {
				String key = en.nextElement();
				try {
					String times[] = key.split("_");
					if (times.length > 2) {
						long startTime = Long.parseLong(times[times.length - 2]);
						if (System.currentTimeMillis() - startTime > mss.getResendTimeOutMS()) {
							rmkeys.add(key);
							PacketTuple pt = mss.getPackMaps().get(key);
							if (pt != null && pt.getPackQ() != null) {
								log.warn("remove timeout sync pack:" + key + ",past["
										+ (System.currentTimeMillis() - startTime) + "]" + ",pt,name="
										+ pt.getPackQ().getName() + ",pt.uri=" + pt.getPackQ().getCkpool().getIp() + ":"
										+ pt.getPackQ().getCkpool().getPort());
							} else {
								log.warn("remove timeout sync pack:" + key + ",past["
										+ (System.currentTimeMillis() - startTime) + "]" + ",pt is null=");
							}
						}
					}
				} catch (Exception e) {
					log.error("get unknow error when check uri for pack.key=" + key, e);
				}
			}
			for (String key : rmkeys) {
				PacketTuple pt = mss.getPackMaps().remove(key);
				if (pt != null) {
					if (pt.getHandler() != null) {
						try {
							pt.getHandler().onFailed(new TimeoutException("pack send timeout"));
						} catch (Exception e) {
						}
					}
					mss.getPackPool().retobj(pt);

				}
			}
		} catch (Exception e) {
			log.warn("error In PacketMapCheck thread:", e);
		} finally {
			log.debug("PackMapsCheckHealth: -- END");
		}
	}


}
