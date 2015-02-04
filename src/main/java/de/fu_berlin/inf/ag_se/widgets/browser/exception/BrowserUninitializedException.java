package de.fu_berlin.inf.ag_se.widgets.browser.exception;


import de.fu_berlin.inf.ag_se.widgets.browser.Browser;

public class BrowserUninitializedException extends Exception {

	private static final long serialVersionUID = 1L;

	public BrowserUninitializedException(Browser browser) {
		super("The " + browser.getClass().getSimpleName()
				+ " has not been initialized, yet");
	}

}
