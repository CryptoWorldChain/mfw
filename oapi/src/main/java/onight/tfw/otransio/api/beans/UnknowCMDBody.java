package onight.tfw.otransio.api.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class UnknowCMDBody extends FrameBody {

	private FramePacket org;

	private String message;

	public UnknowCMDBody(String message, FramePacket org) {
		super();
		this.message = message;
		this.org = org;
	}

}
