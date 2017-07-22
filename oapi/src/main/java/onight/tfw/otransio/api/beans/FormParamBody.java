package onight.tfw.otransio.api.beans;

import java.util.HashMap;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class FormParamBody extends FrameBody {

	HashMap<String, String> items = new HashMap<>();

	public FormParamBody() {
		super();
	}

	public void addItem(String key, String value) {
		items.put(key, value);
	}

}
