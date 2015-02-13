package de.fu_berlin.inf.ag_se.widgets.browser.exception;

public class BrowserDisposedException extends RuntimeException {
    public BrowserDisposedException() {
        super("The browser is disposed.");
    }
}
