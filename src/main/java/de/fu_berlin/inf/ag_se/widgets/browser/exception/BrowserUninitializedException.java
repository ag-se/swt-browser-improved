package de.fu_berlin.inf.ag_se.widgets.browser.exception;

public class BrowserUninitializedException extends Exception {

	private static final long serialVersionUID = 1L;

	public BrowserUninitializedException() {
		super("The browser has not been initialized, yet");
	}

}
