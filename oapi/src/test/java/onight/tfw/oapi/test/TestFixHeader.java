package onight.tfw.oapi.test;

import onight.tfw.otransio.api.beans.FixHeader;

public class TestFixHeader {

	public static void main(String[] args) {
		
		FixHeader header=new FixHeader();
		header.setBodysize(260);
		header.setCmd("cmd");
		header.setModule("abc");
		header.setVer('v');
		header.setEnctype('T');
		header.setExtsize(260);
		byte bb[]=header.toBytes(true);
		String hstr=new String(bb);
		System.out.println(hstr.length()+"::"+hstr);
		FixHeader h2=FixHeader.parseFrom(bb);
		System.out.println(h2);
	}
}
