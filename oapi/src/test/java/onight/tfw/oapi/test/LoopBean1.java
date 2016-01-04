package onight.tfw.oapi.test;

import lombok.Data;

@Data
public class LoopBean1 {

	boolean bol;
	
	LoopDepBean1 depbean;
	
	
	
	public String toString(){
		return "LoopBean1(bol="+bol+",LoopDepBean1="+(depbean)+")@"+System.identityHashCode(this);
	}

}
