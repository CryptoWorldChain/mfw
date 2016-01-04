package onight.tfw.otransio.api.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CookieBean {

	Object value;
	int expiry=Integer.MAX_VALUE;
	String path;
	String domain;

	public CookieBean(Object value, int expiry) {
		super();
		this.value = value;
		this.expiry = expiry;
	}

}
