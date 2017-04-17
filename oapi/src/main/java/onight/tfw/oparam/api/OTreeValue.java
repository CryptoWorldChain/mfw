package onight.tfw.oparam.api;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OTreeValue {
	String key;
	String value;
	List<OTreeValue> nodes;
	long modifiedIndex;
	long createdIndex;

	public String toString() {
		return "OTreeValue@"+this.hashCode()+"[key="+key+",value="+value+",nodes="+nodes+",modifiedIndex="+modifiedIndex+",createdIndex="+createdIndex+"]";
	}
}
