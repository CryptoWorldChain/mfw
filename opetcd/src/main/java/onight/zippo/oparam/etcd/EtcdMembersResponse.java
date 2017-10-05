package onight.zippo.oparam.etcd;

import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Etcd Members response
 */
@NoArgsConstructor
@Data
public class EtcdMembersResponse {

	// The json

	private List<MemberInfo> members;

	public EtcdMembersResponse(List<MemberInfo> members) {
		this.members = Collections.unmodifiableList(members);
	}

	public List<MemberInfo> getMembers() {
		return members;
	}

	@NoArgsConstructor
	@Data
	public static class MemberInfo {

		private String id;
		private String name;
		private List<String> peerURLs;
		private List<String> clientURLs;

		MemberInfo(final String id, final String name, final List<String> peerURLs, final List<String> clientURLs) {

			this.id = id;
			this.name = name;
			this.peerURLs = Collections.unmodifiableList(peerURLs);
			this.clientURLs = Collections.unmodifiableList(clientURLs);
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<String> getPeerURLs() {
			return peerURLs;
		}

		public List<String> getClientURLs() {
			return clientURLs;
		}
	}
}