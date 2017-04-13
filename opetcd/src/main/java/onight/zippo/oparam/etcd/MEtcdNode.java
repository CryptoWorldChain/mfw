package onight.zippo.oparam.etcd;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MEtcdNode {
	String key;
	String value;
	long modifiedIndex;
	long createdIndex;
	long ttl;
	boolean dir;
	String expiration;
	List<MEtcdNode> nodes=new ArrayList<>();

}
