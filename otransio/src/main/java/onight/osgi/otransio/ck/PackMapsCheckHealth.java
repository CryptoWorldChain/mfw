package onight.osgi.otransio.ck;

import java.util.ArrayList;
import java.util.Enumeration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.otransio.sm.MSessionSets;

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
			log.debug("PackMapsCheckHealth: [START]");
			Enumeration<String> en = mss.getPackMaps().keys();
			ArrayList<String> rmkeys = new ArrayList<>();
			while (en.hasMoreElements()) {
				String key = en.nextElement();
				try {
					String times[] = key.split("_");
					if (times.length > 2) {
						long startTime = Long.parseLong(times[times.length - 2]);
						if (System.currentTimeMillis() - startTime > 60 * 1000) {
							log.debug("remove timeout sync pack:" + key + ",past["
									+ (System.currentTimeMillis() - startTime) + "]");
							rmkeys.add(key);
						}
					}
				} catch (Exception e) {

				}
			}
			for (String key : rmkeys) {
				mss.getPackMaps().remove(key);
			}
		} catch (Exception e) {
			log.debug("error In PacketMapCheck thread:", e);
		}finally{
			log.debug("PackMapsCheckHealth: [STOP]");
		}
	}

}
