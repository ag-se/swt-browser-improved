package de.fu_berlin.inf.ag_se.browser.exception;

public class UnexpectedBrowserStateException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnexpectedBrowserStateException(String message) {
		super(message);
	}

}
