package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.functions.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.functions.JavascriptFunction;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import org.eclipse.swt.browser.Browser;

/**
 * This is the interface to be implemented when a new browser is introduced.
 * It abstract the concrete implementation of those browsers so that {@link InternalBrowserWrapper}
 * does not need to change when wrapping a new browser.
 *
 * Currently, this interface is strongly influenced by the one of the {@link org.eclipse.swt.browser.Browser}
 * because this is only fully supported browser.
 * The motivation for this interface was that the SWT browser probably has to be replaced to support Java 7 and 8
 * on Mac OS X, for example.
 *
 * The interface should work for basic JxBrowser, JavaFx Webview, and Native Swing implementations as well, but may have
 * to be adapted for complete support of those browsers.
 */
public interface IWrappedBrowser {

    /**
     * Registers the given runnable to be executed
     * when the browser informs about the completion.
     * This method may be called multiple times to
     * register multiple runnables.
     *
     * Please note that the loaded detection of the
     * wrapped browser might fire too early. This
     * has been observed for the {@link Browser}
     *
     * @param runnable the runnable to be executed
     */
    void addLoadedListener(Runnable runnable);

    /**
     * Sets the URL in the browser
     * @param url the URL to be set as string
     */
    void setUrl(String url);

    /**
     * Gets the URL currently set in browser.
     * @return the URL as string
     */
    String getUrl();

    /**
     * Evaluates the given Javascript expression in the browser
     * in the context of the currently loaded document
     * and returns the result.
     *
     *  If the script returns a value with a supported type then a java
      * representation of the value is returned.  The supported
      * javascript -> java mappings are:
     *
      * javascript null or undefined -> null
      * javascript number -> java.lang.Double
      * javascript string -> java.lang.String
      * javascript boolean -> java.lang.Boolean
      * javascript array whose elements are all of supported types -> java.lang.Object[]
     *
     * @param javascript the Javascript expression as string
     * @return the result of the evaluation
     */
    Object evaluate(String javascript);

    /**
     * Tests whether the browser is disposed
     * @return true if the browser is disposed, false otherwise
     */
    boolean isDisposed();

    /**
     * Creates a Javascript function that can call Java code.
     * Depending on the browser implementation this function may have to be disposed by the caller.
     *
     * @param function {@link JavascriptFunction} containing name in Javscript and the Java code
     * @return an instance of {@link IBrowserFunction} which can be used to dispose the function.
     */
    IBrowserFunction createBrowserFunction(JavascriptFunction function);

    /**
     * The wrapped browser may be part of different GUI toolkits like SWT, Swing, or JavaFx.
     * Each of them are single-threaded for GUI operations and use a different event dispatch thread.
     *
     * This method gets an abstraction for the EDT mechanism needed by the wrapped browser.
     *
     * @return an instance of {@link UIThreadExecutor}
     */
    UIThreadExecutor getUIThreadExecutor();

    /**
     * Sets the browser visible or hides it.
     *
     * @param visible true to make it visible, false to hide it
     */
    void setVisible(boolean visible);

    /**
     * Sets the size of the browser
     * @param width the width in pixels
     * @param height the height in pixels
     */
    void setSize(int width, int height);

    /**
     * Sets the content of the currently loaded document
     * to the given HTML string.
     * Note that this only works for static content.
     * @param html the HTML code to be set
     */
    void setText(String html);

    /**
     * Give the focus to the browser so that all
     * keyboard events will be delivered to it.
     *
     * @return true if successful, false otherwise
     */
    boolean setFocus();
}
