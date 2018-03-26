package onight.osgi.otransio.impl;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.Connection;

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
	// int nodeIdx = NodeHelper.getCurrNodeIdx();
	public static NodeInfo fromName(String nodeuid, Connection<?> conn) {
		InetSocketAddress addr = (InetSocketAddress) conn.getPeerAddress();
		NodeInfo info = new NodeInfo(addr.getHostString(), addr.getPort(), -1, -1, nodeuid);
		return info;
	}

	public static NodeInfo fromURI(String uri, String nodeuid) {
		try {
			URL url = new URL(uri);
			if (StringUtils.isBlank(nodeuid)) {
				nodeuid = url.getHost() + "." + url.getPort();
				if (url.getQuery() != null) {
					String[] querys = url.getQuery().split("&");
					for (String q : querys) {
						String kvs[] = q.split("=");
						if (kvs.length == 2 && StringUtils.equals("name", kvs[0])) {
							nodeuid = kvs[1].trim();
							break;
						}
					}
				}
			}

			NodeInfo info = new NodeInfo(url.getHost(), url.getPort(), 3, 10, nodeuid);
			return info;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

	}

}
