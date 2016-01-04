package onight.tfw.orouter.api;

public interface IQClient {

	public void setQService(QService service);

	public QService getQService();

	public void sendMessage(final String ex, final Object wmsg);

	public void onQServiceReady();

	public void createMessageListener(String ex, IRecievier reciever);

}
