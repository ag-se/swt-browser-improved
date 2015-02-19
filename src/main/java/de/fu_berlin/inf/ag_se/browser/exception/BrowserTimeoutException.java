package de.fu_berlin.inf.ag_se.browser.exception;

import java.net.URI;

public class BrowserTimeoutException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BrowserTimeoutException() {
		super("Loading timed out");
	}

	public BrowserTimeoutException(String uri) {
		super("Loading " + uri + " timed out");
	}

	public BrowserTimeoutException(URI uri) {
		this(uri.toString());
	}

}
