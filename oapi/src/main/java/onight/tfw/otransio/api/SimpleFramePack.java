package onight.tfw.otransio.api;

import java.util.Map;

import lombok.Data;

@Data
public class SimpleFramePack<T> {

	// fix header
	String fh;

	Map<String, String> eh;

	T body;

}
