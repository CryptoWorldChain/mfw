package onight.tfw.orouter.api;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ExQProxy implements IQClient {

	@Setter
	@Getter
	private QService qService = new NoneQService();

	public void sendMessage(final String ex, final Object wmsg) {
		qService.sendMessage(ex, wmsg);
	}

	public void onQServiceReady() {

	}

	public void createMessageListener(String ex, IRecievier reciever) {
		log.debug("创建队列" + ex + "::" + qService);
		qService.createMessageListener(this, ex, reciever, 0, 0);
	}

	
	
}
