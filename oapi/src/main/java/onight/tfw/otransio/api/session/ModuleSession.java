package onight.tfw.otransio.api.session;

import java.util.HashMap;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

@Slf4j
@Data
public class ModuleSession extends PSession {
	protected String module;

	protected HashMap<String, CMDService> serviceByCMD = new HashMap<>();

	public void onPacket(FramePacket pack, final CompleteHandler handler) {
		CMDService service = serviceByCMD.get(pack.getCMD());
		if (service != null) {
			service.onPacket(pack, new CompleteHandler() {
				@Override
				public void onFinished(FramePacket packet) {
					handler.onFinished(packet);
				}
			});
		}
	}

	public void registerService(String cmd, CMDService service) {
		if (serviceByCMD.containsKey(cmd)) {
			log.warn("Overrided Command Service:cmd=" + cmd + ",oldservice=" + service);
		}
		serviceByCMD.put(cmd, service);
	}

	public void destroyService(String cmd, CMDService service) {
		if (!serviceByCMD.containsKey(cmd)) {
			log.warn(" Command Service Not Found:cmd=" + cmd + ",oldservice=" + service);
			serviceByCMD.remove(cmd);
		}
	}

	public ModuleSession(String module) {
		super();
		this.module = module;
	}

}
