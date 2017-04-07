package onight.osgi.otransio.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class NodeInfo {

	String addr="127.0.0.1";
	int port=5100;
	int core=3;
	int max=10;
}
