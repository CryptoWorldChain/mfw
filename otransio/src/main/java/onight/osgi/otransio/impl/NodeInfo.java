package onight.osgi.otransio.impl;

import java.net.MalformedURLException;
import java.net.URL;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import onight.tfw.mservice.NodeHelper;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class NodeInfo {

	String addr = NodeHelper.getCurrNodeListenOutAddr();
	int port = NodeHelper.getCurrNodeListenOutPort();
	int core = 3;
	int max = 10;
	String nodeName = NodeHelper.getCurrNodeName();
	int nodeIdx = NodeHelper.getCurrNodeIdx();

	public static NodeInfo fromURI(String uri) {
		try {
			URL url = new URL(uri);
			NodeInfo info = new NodeInfo(url.getHost(), url.getPort(), 3, 10, url.getHost(), url.getHost().hashCode());
			return info;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

	}

}
