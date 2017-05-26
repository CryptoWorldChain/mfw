package onight.tfw.ojpa.ordb;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

/**
 * 自定义返回JSON 数据格中日期格式化处理
 *
 */
public class CustomDateDeserializer extends JsonDeserializer<Date> {

	@Override
	public Date deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = jp.getText();
		try {
			return formatter.parse(date);
		} catch (Exception e) {
			try {
				long l = Long.parseLong(date);
				return new Date(l);
			} catch (Exception e1) {
				SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
				try {
					return formatter1.parse(date);
				} catch (Exception e2) {
				}
			}
			throw new RuntimeException(e);
		}
	}

}
