package onight.tfw.otransio.api.session;

import java.util.HashMap;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

@Slf4j
@Data
@EqualsAndHashCode(callSuper=true)
public class LocalModuleSession extends PSession {
	protected String module;
	protected HashMap<String, CMDService> serviceByCMD = new HashMap<>();

	public void onPacket(FramePacket pack, final CompleteHandler handler) {
		CMDService service = serviceByCMD.get(pack.getCMD());
		if (service != null) {
			service.doPacketWithFilter(pack,handler);
		}
	}

	public String getJsonStr() {
		StringBuffer sb = new StringBuffer();
		sb.append("{\"module\":\"" + module + "\"");
		sb.append(",\"cmds\":").append("[");
		int v = 0;
		for (CMDService service : serviceByCMD.values()) {
			if (v > 0)
				sb.append(",");
			v++;
			int i = 0;
			for (String cmd : service.getCmds()) {
				if (i > 0)
					sb.append(",");
				i++;
				sb.append("\"").append(cmd).append("\"");
			}

		}
		sb.append("]}");

		return sb.toString();
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

	public LocalModuleSession(String module) {
		super();
		this.module = module;
	}

}
