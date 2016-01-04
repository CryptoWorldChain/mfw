package onight.tfw.ntrans.api;

import onight.osgi.annotation.iPojoBean;


@iPojoBean
public abstract class ActorModule {

	public abstract String getModule();
	
	public void initModule(){
		
	}
	
}
