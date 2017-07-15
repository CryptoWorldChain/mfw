package onight.tfw.otransio.api;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.protobuf.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import onight.tfw.otransio.api.beans.FramePacket;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class PBPacket extends FramePacket{
	@JsonIgnore
	private Message pbMsg;
}
