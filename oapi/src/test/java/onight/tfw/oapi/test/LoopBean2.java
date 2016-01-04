package onight.tfw.oapi.test;

import lombok.Data;

@Data
public class LoopBean2 {

	boolean bol;
	
	LoopDepBean2 depbean;

	public String toString(){
		return "LoopBean2(bol="+bol+",LoopDepBean2="+(depbean)+")@"+System.identityHashCode(this);
	}

}
