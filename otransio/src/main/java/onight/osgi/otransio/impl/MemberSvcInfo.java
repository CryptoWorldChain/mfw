package onight.osgi.otransio.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberSvcInfo {
	String nodeid;

	String outaddr;
	int outport;
	String token;

	String inaddr;
	int inport;
	boolean isUp;
	String healthy;

	String role;
	String auditstatus;

	int coreconn = 1;
	int maxconn = 1;

	String org;

}
