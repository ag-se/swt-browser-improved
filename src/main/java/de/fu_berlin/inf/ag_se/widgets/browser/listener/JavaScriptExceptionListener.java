package de.fu_berlin.inf.ag_se.widgets.browser.listener;

import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;

/**
 * Instances of this class can handle asynchronous {@link de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException}s, that are exceptions raised by the {@link
 * org.eclipse.swt.browser.Browser} itself and not provoked by Java invocations.
 */
public interface JavaScriptExceptionListener {
    public void thrown(JavaScriptException javaScriptException);
}
