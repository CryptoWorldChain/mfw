package onight.tfw.ojpa.ordb.loader;

import onight.tfw.outils.serialize.JsonSerializer;
import onight.zippo.oparam.etcd.EtcdMembersResponse;

public class TestJSON {

	public static void main(String[] args) {
		String v = "{\"members\":[{\"id\":\"ce2a822cea30bfca\",\"name\":\"default\",\"peerURLs\":[\"http://localhost:2380\",\"http://localhost:7001\"],\"clientURLs\":[\"http://localhost:2379\",\"http://localhost:4001\"]}]}";
		EtcdMembersResponse res = JsonSerializer.getInstance().deserialize(v, EtcdMembersResponse.class);
		System.out.println(res);
	}
}
