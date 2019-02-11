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
	String nodeName = NodeHelper.getCurrNodeName();

	String uname = nodeName + "://" + addr + ":" + port;

	public String getURI(){
		return addr+":"+port;
	}
	// int nodeIdx = NodeHelper.getCurrNodeIdx();
	public static NodeInfo fromName(String nodeuid, Connection<?> conn) {
		InetSocketAddress addr = (InetSocketAddress) conn.getPeerAddress();
		NodeInfo info = new NodeInfo(addr.getHostString(), addr.getPort(), nodeuid,
				nodeuid + "://" + addr.getHostString() + ":" + addr.getPort());
		return info;
	}

	public static NodeInfo fromURI(String uri, String nodeuid) {
		try {
			URL url = new URL(uri.split(",")[0]);
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

			NodeInfo info = new NodeInfo(url.getHost(), url.getPort(), nodeuid,
					nodeuid + "://" + url.getHost() + ":" + url.getPort());
			return info;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

	}

}
