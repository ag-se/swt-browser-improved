package de.fu_berlin.inf.ag_se.widgets.browser;

import java.io.File;
import java.net.URI;
import java.util.concurrent.Future;

import de.fu_berlin.inf.ag_se.widgets.IWidget;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnkerListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.IBrowserScriptRunner;

/**
 * Instances of this interface denote a {@link org.eclipse.swt.widgets.Composite} that is based on a native {@link
 * org.eclipse.swt.browser.Browser}.
 *
 * @author bkahlert
 */
public interface IBrowser extends IBrowserScriptRunner, IWidget {

    /**
     * Opens the given address.
     *
     * @param address the URI to open as string
     * @param timeout the time after which the browser stops loading
     * @return true if page could be successfully loaded; false if the timeout was reached
     */
    public Future<Boolean> open(String address, Integer timeout);

    /**
     * Opens the given address.
     *
     * @param address the URI to open as string
     * @param timeout the time after which the browser stops loading
     * @param pageLoadCheckScript that must return true if the page correctly loaded. This is especially useful if some inner page setup takes
     * @return true if page could be successfully loaded; false if the timeout was reached
     * place.
     */
    public Future<Boolean> open(String address, Integer timeout,
                                String pageLoadCheckScript);

    /**
     * Opens the given address.
     *
     * @param uri     the URI to load
     * @param timeout the time after which the browser stops loading
     * @return true if page could be successfully loaded; false if the timeout was reached
     */
    public Future<Boolean> open(URI uri, Integer timeout);

    /**
     * Opens the given address.
     *
     * @param uri     the URI to load
     * @param timeout the time after which the browser stops loading
     * @param pageLoadCheckScript that must return true if the page correctly loaded. This is especially useful if some inner page setup takes
     * @return true if page could be successfully loaded; false if the timeout was reached
     * place.
     */
    public Future<Boolean> open(URI uri, Integer timeout,
                                String pageLoadCheckScript);

    /**
     * Opens a blank page.
     *
     * @return true if page could be successfully loaded; false if an error occurred
     */
    public Future<Boolean> openBlank();

    /**
     * Sets if the browser may change its location by actions now invoked by {@link
     * #open(String, Integer)}.
     *
     * @param allowed true or false
     */
    public void setAllowLocationChange(boolean allowed);

    /**
     * This method is called from a non-UI thread before the {@link org.eclipse.swt.browser.Browser#setUrl(String)} method is called.
     *
     * @param uri
     */
    public void beforeLoad(String uri);

    /**
     * This method is called from a non-UI thread after the {@link org.eclipse.swt.browser.Browser#setUrl(String)} method has been called.
     *
     * @param uri
     */
    public void afterLoad(String uri);

    /**
     * This method is called from the-UI thread after the browser completed loading the
     * page.
     *
     * @param uri
     * @return
     */
    public Future<Void> beforeCompletion(String uri);

    Future<Void> injectJsFile(File file);

    void injectJsFileImmediately(File jsExtension) throws Exception;

    /**
     * Includes the given path as a cascading style sheet.
     *
     * @param uri
     * @return
     */
    public Future<Void> injectCssFile(URI uri);

    /**
     * Adds the given CSS code to the head.
     *
     * @return
     */
    public Future<Void> injectCss(String css);

    /**
     * Adds immediately the given CSS code to the head. <p> In contrast to other injection functions this does not wait for the {@link
     * de.fu_berlin.inf.ag_se.widgets.browser.IBrowser} to finish loading and thus running its script queue.
     *
     * @return
     * @throws Exception
     */
    public void injectCssImmediately(String css) throws Exception;

    /**
     * Returns a {@link java.util.concurrent.Future} that tells you if an element with the given id exists.
     *
     * @param id
     * @return
     */
    public Future<Boolean> containsElementWithID(String id);

    /**
     * Returns a {@link java.util.concurrent.Future} that tells you if at least one element with the given name exists.
     *
     * @param name
     * @return
     */
    public Future<Boolean> containsElementsWithName(String name);

    public void addAnkerListener(IAnkerListener ankerListener);

    public void removeAnkerListener(IAnkerListener ankerListener);

    public void addMouseListener(IMouseListener mouseListener);

    public void removeMouseListener(IMouseListener mouseListener);

    public void addFocusListener(IFocusListener focusListener);

    public void removeFocusListener(IFocusListener focusListener);

    public void addDNDListener(IDNDListener dNDListener);

    public void removeDNDListener(IDNDListener dNDListener);

    /**
     * Sets the body's inner HTML.
     *
     * @param html
     * @return
     */
    public Future<Void> setBodyHtml(String html);

    /**
     * Returns the body's inner HTML.
     *
     * @return
     */
    public Future<String> getBodyHtml();

    /**
     * Returns the document's HTML
     */
    public Future<String> getHtml();

    /**
     * Inserts the given html at the current caret / cursor position.
     *
     * @return
     */
    public Future<Void> pasteHtmlAtCaret(String html);

    /**
     * Adds a border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     *
     * @return
     */
    public Future<Void> addFocusBorder();

    /**
     * Removes the border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     *
     * @return
     */
    public Future<Void> removeFocusBorder();

    /**
     * Adds a {@link de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener} that is notified if a
     * exception is thrown in the {@link org.eclipse.swt.browser.Browser} by code that was not invoked from the Java but the JavaScript
     * world (e.g. a click on a button invoking erroneous code).
     *
     * @param javaScriptExceptionListener
     */
    public void addJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener);

    /**
     * Removed the given {@link de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener} from the list
     * of notified {@link de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener}s.
     *
     * @param javaScriptExceptionListener
     */
    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener);

    /**
     * @return the current url
     */
    String getUrl();
}