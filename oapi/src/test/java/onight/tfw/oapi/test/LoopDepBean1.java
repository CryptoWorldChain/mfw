package onight.tfw.oapi.test;

import lombok.Data;

@Data
public class LoopDepBean1 {

	
	int in1;

	LoopBean1 loopbean;
	
	public String toString(){
		return "LoopBean1(bol="+in1+",loopbean="+System.identityHashCode(loopbean)+")@"+System.identityHashCode(this);
	}

}
