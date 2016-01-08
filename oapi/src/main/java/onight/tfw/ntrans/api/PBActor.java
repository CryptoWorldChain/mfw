package onight.tfw.ntrans.api;

import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;

import com.google.protobuf.Message;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.async.CompleteHandler;
import onight.tfw.otransio.api.beans.FramePacket;

/**
 * 处理节点的
 * 
 * @author brew
 *
 */
@Slf4j 
public abstract class PBActor<T extends Message> extends NActor<T>  {

	@Override
	public String getModule() {
		return super.getModule();
	}
	
	@Validate
	public void validate(){
		
	}
	
	@Invalidate
	public void invalidate(){
		
	}
	@Override
	public String[] getCmds() {
		return new String[] { getBeanType().getSimpleName().replace("PB", "").substring(0,3) };
	}

	@Override
	final public void onPacket(FramePacket pack, CompleteHandler handler) {
		T pbo = (T) super.getPBBody(pack);
		onPBPacket(pack, pbo, handler);
	}

	public abstract void onPBPacket(FramePacket pack, T pbo, CompleteHandler handler);

}
