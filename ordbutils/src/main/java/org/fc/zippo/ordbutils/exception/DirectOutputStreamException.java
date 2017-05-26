package org.fc.zippo.ordbutils.exception;

import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.fc.zippo.filter.exception.FilterException;

import lombok.Data;

@Data
public abstract class DirectOutputStreamException extends FilterException {

	public DirectOutputStreamException() {
		super();
	}

	public abstract void doReponse(HttpServletResponse res);
}
