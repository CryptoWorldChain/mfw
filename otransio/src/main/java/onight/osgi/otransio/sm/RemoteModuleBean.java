package onight.osgi.otransio.sm;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import onight.tfw.otransio.api.beans.FrameBody;

@Data
@NoArgsConstructor
public class RemoteModuleBean extends FrameBody{

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class ModuleBean {
		String module;
		String nodeID;
	}
	//模块列表
	List<ModuleBean> modules=new ArrayList<>();
}
