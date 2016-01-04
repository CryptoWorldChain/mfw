package onight.tfw.oapi.test;

import lombok.Data;

@Data
public class LoopDepBean2 {

	
	int in1;

	LoopBean2 loopbean;
	public String toString(){
		return "LoopBean2(bol="+in1+",loopbean="+System.identityHashCode(loopbean)+")@"+System.identityHashCode(this);
	}

}
