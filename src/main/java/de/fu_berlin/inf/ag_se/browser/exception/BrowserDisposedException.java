package de.fu_berlin.inf.ag_se.browser.exception;

public class BrowserDisposedException extends RuntimeException {
    public BrowserDisposedException() {
        super("The browser is disposed.");
    }
}
