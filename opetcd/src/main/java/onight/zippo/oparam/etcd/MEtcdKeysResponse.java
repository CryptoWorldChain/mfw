package onight.zippo.oparam.etcd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MEtcdKeysResponse {
	String action;
	MEtcdNode node;
	MEtcdNode prevNode;
}
