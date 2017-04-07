package onight.osgi.otransio.impl;

import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.osgi.otransio.sm.MSessionSets;
import onight.tfw.ntrans.api.NActor;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;
import onight.tfw.oparam.api.OParam;
import onight.tfw.oparam.api.OTreeValue;

@NActorProvider
@Slf4j
public class ModuleDiscovery extends NActor{


	public ModuleDiscovery() {
	}
	
	
	@StoreDAO(target = "etcd", daoClass = OParam.class)
	@Getter
	OParam oparam;

	public void setOparam(DomainDaoSupport daoparam) {
		if (daoparam != null && daoparam instanceof OParam) {
			oparam = (OParam) daoparam;
		} else {
			log.warn("cannot set OParam from:" + daoparam);
		}
	}
	@Override
	public void onDaoServiceAllReady() {
		// TODO Auto-generated method stub
		try {
			log.debug("onDaoServiceAllReady:oparam=:" + oparam);
			//String v1 = oparam.get("aabbcc").get();
			log.info("param.health==" + oparam.getHealth());
		} catch (Exception e) {
			//e.printStackTrace();
			log.warn("Read param Value failed:");
		}
	}
	
	public void updateModuleToGlobal(MSessionSets mss){
		log.debug("nodeid="+mss.getCurrentNodeID()+"::");
		for(String module:mss.getLocalModules()){
			if(StringUtils.isNotBlank(module)){
				log.info("module:=="+module+",@nodeid="+mss.getCurrentNodeID());
				try {
					Future<OTreeValue> f=oparam.put("/zippo/nds/"+mss.getCurrentNodeID()+"/"+module, mss.getCurrentNodeID());
					log.info("putresult=="+f.get());
				} catch (Exception e) {
					log.warn("cannot set param:"+module+",mss="+mss.getCurrentNodeID(),e);
				}
			}
		}
		try {
			log.debug("dir=="+oparam.getDir("/zippo").get());
		} catch (Exception e) {
			log.warn("dir.error",e);
		}
	}

}
