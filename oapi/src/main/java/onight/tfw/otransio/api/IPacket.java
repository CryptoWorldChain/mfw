package onight.tfw.otransio.api;

public interface IPacket<T> {

	public abstract T getBO();

	public abstract Class<T> getBOClazz();

	
}