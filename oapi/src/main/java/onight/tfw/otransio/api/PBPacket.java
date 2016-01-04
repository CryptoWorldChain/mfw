package onight.tfw.otransio.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import onight.tfw.otransio.api.beans.FramePacket;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.protobuf.Message;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PBPacket extends FramePacket{
	@JsonIgnore
	private Message pbMsg;
}
