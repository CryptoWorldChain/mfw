package org.fc.zippo.ordbutils.rest.filter;

import java.io.UnsupportedEncodingException;

import org.apache.felix.ipojo.annotations.Provides;
import org.fc.zippo.filter.FilterConfig;
import org.fc.zippo.filter.exception.FilterException;

import onight.osgi.annotation.iPojoBean;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.ActWrapper;
import onight.tfw.otransio.api.SimplePacketFilter;
import onight.tfw.otransio.api.beans.FramePacket;

@iPojoBean
@Provides(specifications = { PacketFilter.class }, strategy = "SINGLETON")
public class PacketFilter extends SimplePacketFilter {

	@Override
	public String getSimpleName() {
		return "encodingfilter";
	}

	String defaultCharset = "utf-8";

	public void init(FilterConfig filterConfig) throws FilterException {
		defaultCharset = filterConfig.getStrProp("org.zippo.rest.charset", "utf-8");
	}

	@Override
	public boolean preRoute(ActWrapper arg0, FramePacket pack, CompleteHandler arg2) throws FilterException {
		try {

			if (pack.getHttpServerletRequest() != null) {
				pack.getHttpServerletRequest().setCharacterEncoding(defaultCharset);
			}
			if (pack.getHttpServerletResponse() != null) {
				pack.getHttpServerletResponse().setContentType("application/json; charset=" + defaultCharset);
			}
		} catch (UnsupportedEncodingException e) {
			throw new FilterException(e);
		}

		return true;
	}

}
