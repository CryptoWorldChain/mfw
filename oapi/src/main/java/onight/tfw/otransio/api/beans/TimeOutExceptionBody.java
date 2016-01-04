package onight.tfw.otransio.api.beans;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class TimeOutExceptionBody extends FrameBody {

	private FramePacket org;

	private String message;

	public TimeOutExceptionBody(String message, FramePacket org) {
		super();
		this.message = message;
		this.org = org;
	}

}
