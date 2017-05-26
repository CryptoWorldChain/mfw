package onight.tfw.orouter.impl;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.orouter.api.ExQProxy;

import org.apache.felix.ipojo.annotations.Validate;

@Slf4j
public class VoidQProxy extends ExQProxy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Validate
	public void init() {
		log.info("start Void Proxy");
	}

	@Override
	public void sendMessage(String ex, Object wmsg) {
		
	}
}
