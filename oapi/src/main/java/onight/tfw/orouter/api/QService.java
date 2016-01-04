package onight.tfw.orouter.api;

import onight.tfw.ntrans.api.exception.MessageException;

public interface QService {
	public void sendMessage(final String ex, final Object wmsg) throws MessageException;

	public void createMessageListener(IQClient proxy, String qName, IRecievier reciever, int delay, int sharedThreadCount) throws MessageException;

	public int getQueueSize(String ex);

	public void removeQueue(String ex);

	public Object syncSendMessage(String ex,String tmpQName,Object message) throws MessageException;

}
