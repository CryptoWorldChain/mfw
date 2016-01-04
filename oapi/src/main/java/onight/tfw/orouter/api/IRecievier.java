package onight.tfw.orouter.api;

import java.io.Serializable;

public interface IRecievier {
	public boolean onMessage(final String ex, final Serializable wmsg);
}
