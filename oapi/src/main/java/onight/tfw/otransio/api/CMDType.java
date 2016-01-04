package onight.tfw.otransio.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CMDType<T> {

	String[] cmds;

	Class<T> mclazz;

	public CMDType(String cmd) {
		this.cmds = new String[] { cmd };
	}

	public CMDType(String cmd, Class<T> clazz) {
		this.cmds = new String[] { cmd };
		this.mclazz = clazz;
	}

}
