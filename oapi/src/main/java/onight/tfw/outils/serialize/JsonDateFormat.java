package onight.tfw.outils.serialize;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JsonDateFormat extends SimpleDateFormat {

	public JsonDateFormat() {
		super();
		// TODO Auto-generated constructor stub
	}

	public JsonDateFormat(String pattern, DateFormatSymbols formatSymbols) {
		super(pattern, formatSymbols);
		// TODO Auto-generated constructor stub
	}

	public JsonDateFormat(String pattern, Locale locale) {
		super(pattern, locale);
		// TODO Auto-generated constructor stub
	}

	public JsonDateFormat(String pattern) {
		super(pattern);
		// TODO Auto-generated constructor stub
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(date, toAppendTo, fieldPosition);
	}

	@Override
	public Date parse(String source, ParsePosition pos) {
		Date dd =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(source, pos);
		if (dd != null)
			return dd;
		dd = new SimpleDateFormat("yyyy-MM-dd").parse(source, pos);
		if (dd != null)
			return dd;
		dd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(source, pos);

		return dd;

	}

}
