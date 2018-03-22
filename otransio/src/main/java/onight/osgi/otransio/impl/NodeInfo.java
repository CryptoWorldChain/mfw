package onight.osgi.otransio.impl;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;

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

	public static NodeInfo fromURI(String uri) {
		try {
			URL url = new URL(uri);
			String name = url.getHost()+"."+url.getPort();
			if(url.getQuery()!=null){
				String []querys=url.getQuery().split("&");
				for(String q:querys){
					String kvs[]=q.split("=");
					if(kvs.length==2&&StringUtils.equals("name",kvs[0])){
						name = kvs[1].trim();
						break;
					}
				}
			}
			
			NodeInfo info = new NodeInfo(url.getHost(), url.getPort(), 3, 10, name);
			return info;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}

	}

}
