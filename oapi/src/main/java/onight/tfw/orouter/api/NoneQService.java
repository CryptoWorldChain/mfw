package onight.tfw.orouter.api;

import onight.tfw.ntrans.api.exception.MessageException;
import onight.tfw.ntrans.api.exception.ServiceNotReadyException;

public class NoneQService implements QService {

	@Override
	public void sendMessage(String ex, Object wmsg) {
		throw new ServiceNotReadyException();
	}

	@Override
	public void createMessageListener(IQClient proxy, String qName,
			IRecievier reciever, int delay, int sharedThreadCount) {
		throw new ServiceNotReadyException();
	}

	@Override
	public int getQueueSize(String ex) {
		return -1;
	}

	@Override
	public void removeQueue(String ex) {
		throw new ServiceNotReadyException();		
	}

	@Override
	public Object syncSendMessage(String ex,String tmpQName,Object reciever) throws MessageException {
		throw new ServiceNotReadyException();		
	}

}
