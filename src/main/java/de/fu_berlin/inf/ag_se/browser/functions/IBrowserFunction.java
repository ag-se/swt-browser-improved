package de.fu_berlin.inf.ag_se.browser.functions;

/**
 * Should be implemented by framework specific wrapper classes
 * for browser functions.
 */
public interface IBrowserFunction {

    /**
     * Disposes the browser function.
     * This method must only be called if a browser function
     * should be disposed before the enclosing browser.
     */
    void dispose();
}
