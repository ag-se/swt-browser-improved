package de.fu_berlin.inf.ag_se.browser.listener;

import de.fu_berlin.inf.ag_se.browser.exception.JavaScriptException;

/**
 * Instances of this class can handle asynchronous {@link JavaScriptException}s, that are exceptions raised by the {@link
 * org.eclipse.swt.browser.Browser} itself and not provoked by Java invocations.
 */
public interface JavaScriptExceptionListener {
    public void thrown(JavaScriptException javaScriptException);
}
