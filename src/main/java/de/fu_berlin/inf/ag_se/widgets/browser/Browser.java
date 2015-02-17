package de.fu_berlin.inf.ag_se.widgets.browser;

import com.sun.istack.internal.Nullable;
import de.fu_berlin.inf.ag_se.utils.EventDelegator;
import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.utils.SWTUtils;
import de.fu_berlin.inf.ag_se.utils.colors.RGB;
import de.fu_berlin.inf.ag_se.widgets.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.widgets.browser.functions.Function;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.*;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.futures.CompletedFuture;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.fu_berlin.inf.ag_se.widgets.browser.threading.SwtUiThreadExecutor.checkNotUIThread;

/**
 * This class is a {@link org.eclipse.swt.widgets.Composite} that provides extended functionality to
 * the {@link org.eclipse.swt.browser.Browser}.
 *
 * Its features include reporting Javascript errors back to Java,
 * checking the loading state dependent on custom Javascript conditions,
 * delaying the execution of Javascript code based on the loading state,
 * and providing listener for different kinds of Javascript events.
 */
public class Browser extends Composite implements IBrowser {

    private static final int STYLES = SWT.INHERIT_FORCE;
    private static Logger LOGGER = Logger.getLogger(Browser.class);
    protected InternalBrowserWrapper internalBrowser;

    private boolean initWithSystemBackgroundColor;

    private boolean textSelectionsDisabled = false;

    private EventCatchFunctionality eventCatchFunctionality;

    /**
     * Constructs a new browser composite with the given styles.
     *
     * @param parent a widget which will be the parent of the new instance (cannot be null)
     * @param style  if {@link org.eclipse.swt.SWT#INHERIT_FORCE} is set the loaded page's
     *               background is replaced by the inherited background color
     */
    public Browser(Composite parent, int style) {
        super(parent, style | SWT.EMBEDDED & ~STYLES);
        setLayout(new FillLayout());
        initWithSystemBackgroundColor = (style & SWT.INHERIT_FORCE) != 0;

        internalBrowser = new InternalBrowserWrapper(this);

        eventCatchFunctionality = new EventCatchFunctionality(internalBrowser);

        executeAfterCompletion(new Runnable() {
            @Override
            public void run() {
                if (initWithSystemBackgroundColor) {
                    setBackground(SWTUtils.getEffectiveBackground(Browser.this));
                }
                if (textSelectionsDisabled) {
                    try {
                        injectCss(JavascriptString.createCssToDisableTextSelection());
                    } catch (RuntimeException e) {
                        LOGGER.error(e);
                    }
                }
                eventCatchFunctionality.injectEventCatchScript();
            }
        });
    }

    @Override
    public Future<Boolean> open(String uri, int timeout) {
        checkNotNull(uri);
        return open(uri, timeout, null);
    }

    @Override
    public Future<Boolean> open(String uri, int timeout, @Nullable String pageLoadCheckScript) {
        checkNotNull(uri);
        return internalBrowser.open(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout) {
        checkNotNull(uri);
        return open(uri.toString(), timeout, null);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout) {
        checkNotNull(uri);
        checkNotUIThread();
        return syncOpen(uri, timeout, null);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, CallbackFunction<Boolean, Boolean> callback) {
        checkNotNull(uri);
        checkNotNull(callback);
        return openWithCallback(uri, timeout, null, callback);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, @Nullable String pageLoadCheckExpression) {
        checkNotNull(uri);
        return open(uri.toString(), timeout, pageLoadCheckExpression);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout, @Nullable String pageLoadCheckScript) {
        checkNotNull(uri);
        checkNotUIThread();
        return internalBrowser.syncOpen(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> openWithCallback(URI uri, int timeout, @Nullable String pageLoadCheckScript,
                                            CallbackFunction<Boolean, Boolean> callback) {
        checkNotNull(uri);
        checkNotNull(callback);
        return internalBrowser.openWithCallback(uri, timeout, pageLoadCheckScript, callback);
    }

    @Override
    public Future<Boolean> openBlank() {
        return open(BrowserUtils.createBlankHTMLFile(), 3000);
    }

    @Override
    public boolean isLoadingCompleted() {
        return internalBrowser.isLoadingCompleted();
    }

    @Override
    public String getUrl() {
        return internalBrowser.getUrl();
    }

    @Override
    public void waitForCondition(String javaScriptExpression) {
        checkNotNull(javaScriptExpression);
        checkNotUIThread();
        internalBrowser.waitForCondition(javaScriptExpression);
    }

    @Override
    public Future<Void> checkCondition(String javaScriptExpression) {
        checkNotNull(javaScriptExpression);
        return internalBrowser.checkCondition(javaScriptExpression);
    }

    @Override
    public <DEST> Future<DEST> executeWhenConditionIsMet(String javaScriptExpression, CallbackFunction<Void, DEST> callback) {
        checkNotNull(javaScriptExpression);
        checkNotNull(callback);
        return internalBrowser.executeWhenConditionIsMet(javaScriptExpression, callback);
    }

    @Override
    public void executeAfterCompletion(Runnable runnable) {
        checkNotNull(runnable);
        internalBrowser.executeBeforeCompletion(runnable);
    }

    @Override
    public Future<Boolean> injectJavascript(File javascriptFile) {
        checkNotNull(javascriptFile);
        return internalBrowser.injectJavascript(javascriptFile);
    }

    @Override
    public Future<Boolean> injectJavascript(URI scriptURI) {
        checkNotNull(scriptURI);
        return internalBrowser.injectJavascript(scriptURI);
    }

    @Override
    public boolean syncInjectJavascript(URI scriptURI) {
        checkNotNull(scriptURI);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncInjectJavascript(scriptURI);
    }

    @Override
    public Future<Boolean> injectCssURI(URI cssURI) {
        checkNotNull(cssURI);
        return internalBrowser.injectCss(cssURI);
    }

    @Override
    public boolean syncInjectCssURI(URI cssURI) {
        checkNotNull(cssURI);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncInjectCssURI(cssURI);
    }

    @Override
    public Future<Boolean> injectCss(String css) {
        checkNotNull(css);
        return internalBrowser.injectCss(css);
    }

    @Override
    public boolean syncInjectCss(String css) {
        checkNotNull(css);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncInjectCss(css);
    }

    @Override
    public <DEST> Future<DEST> injectCss(String css, CallbackFunction<Boolean, DEST> callback) {
        checkNotNull(css);
        checkNotNull(callback);
        return internalBrowser.injectCss(css, callback);
    }

    @Override
    public Future<Boolean> run(File scriptFile) {
        checkNotNull(scriptFile);
        return internalBrowser.run(scriptFile);
    }

    @Override
    public Future<Boolean> runContentAsScriptTag(File scriptFile) throws IOException{
        checkNotNull(scriptFile);
        return internalBrowser.runContentAsScriptTag(scriptFile);
    }

    @Override
    public Future<Boolean> run(URI scriptURI) {
        checkNotNull(scriptURI);
        return internalBrowser.run(scriptURI);
    }

    @Override
    public Future<Object> run(String script) {
        checkNotNull(script);
        return internalBrowser.run(script);
    }

    @Override
    public Object syncRun(String script) {
        checkNotNull(script);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncRun(script);
    }

    @Override
    public <DEST> Future<DEST> run(String script, CallbackFunction<Object, DEST> callback) {
        checkNotNull(script);
        checkNotNull(callback);
        return internalBrowser.run(script, callback);
    }

    @Override
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter) {
        checkNotNull(script);
        checkNotNull(converter);
        return internalBrowser.run(script, converter);
    }

    @Override
    public <DEST> DEST syncRun(String script, IConverter<Object, DEST> converter) {
        checkNotNull(script);
        checkNotNull(converter);
        if (!isLoadingCompleted()) {
            checkNotUIThread();
        }
        return internalBrowser.syncRun(script, converter);
    }

    @Override
    public <T, DEST> Future<T> run(String script, IConverter<Object, DEST> converter, CallbackFunction<DEST, T> callback) {
        checkNotNull(script);
        checkNotNull(converter);
        checkNotNull(callback);
        return internalBrowser.runWithCallback(run(script, converter), callback);
    }

    @Override
    public void executeBeforeScript(Function<String> function) {
        checkNotNull(function);
        internalBrowser.executeBeforeScript(function);
    }

    @Override
    public void executeAfterScript(Function<Object> function) {
        internalBrowser.executeAfterScript(function);
    }

    @Override
    public Future<Boolean> containsElementWithID(String id) {
        checkNotNull(id);
        return run("return document.getElementById('" + id + "') != null", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Boolean> containsElementsWithName(String name) {
        checkNotNull(name);
        return run("return document.getElementsByName('" + name + "').length > 0", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Boolean> setBodyHtml(String html) {
        checkNotNull(html);
        return run("document.body.innerHTML = ('" + JavascriptString.escape(html) + "');", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<String> getBodyHtml() {
        return run("return document.body.innerHTML", IConverter.CONVERTER_STRING);
    }

    @Override
    public Future<String> getHtml() {
        return run("return document.documentElement.outerHTML", IConverter.CONVERTER_STRING);
    }

    @Override
    public <T> Future<T> getHtml(CallbackFunction<String, T> callback) {
        checkNotNull(callback);
        return internalBrowser.runWithCallback(getHtml(), callback);
    }

    @Override
    public Future<Boolean> pasteHtmlAtCaret(String html) {
        checkNotNull(html);
        try {
            File js = File.createTempFile("paste", ".js");
            FileUtils.write(js, JavascriptString.createJavascriptForInsertingHTML(html));
            return injectJavascript(js);
        } catch (IOException e) {
            return new CompletedFuture<Boolean>(null, e);
        }
    }

    @Override
    public void setAllowLocationChange(boolean allowed) {
        internalBrowser.setAllowLocationChange(allowed);
    }

    @Override
    public void deactivateTextSelections() {
        textSelectionsDisabled = true;
    }

    @Override
    public IBrowserFunction createBrowserFunction(final String functionName,
                                                  final IBrowserFunction function) {
        checkNotNull(functionName);
        checkNotNull(function);
        return internalBrowser.createBrowserFunction(functionName, function);
    }

    @Override
    public void addAnchorListener(IAnchorListener anchorListener) {
        checkNotNull(anchorListener);
        eventCatchFunctionality.addAnchorListener(anchorListener);
    }

    @Override
    public void removeAnchorListener(IAnchorListener anchorListener) {
        checkNotNull(anchorListener);
        eventCatchFunctionality.removeAnchorListener(anchorListener);
    }

    @Override
    public void addMouseListener(IMouseListener mouseListener) {
        checkNotNull(mouseListener);
        eventCatchFunctionality.addMouseListener(mouseListener);
    }

    @Override
    public void removeMouseListener(IMouseListener mouseListener) {
        checkNotNull(mouseListener);
        eventCatchFunctionality.removeMouseListener(mouseListener);
    }

    @Override
    public void addFocusListener(IFocusListener focusListener) {
        checkNotNull(focusListener);
        eventCatchFunctionality.addFocusListener(focusListener);
    }

    @Override
    public void removeFocusListener(IFocusListener focusListener) {
        checkNotNull(focusListener);
        eventCatchFunctionality.removeFocusListener(focusListener);
    }

    @Override
    public void addDNDListener(IDNDListener dndListener) {
        checkNotNull(dndListener);
        eventCatchFunctionality.addDNDListener(dndListener);
    }

    @Override
    public void removeDNDListener(IDNDListener dndListener) {
        checkNotNull(dndListener);
        eventCatchFunctionality.removeDNDListener(dndListener);
    }

    @Override
    public void addJavaScriptExceptionListener(
            JavaScriptExceptionListener exceptionListener) {
        checkNotNull(exceptionListener);
        internalBrowser.addJavaScriptExceptionListener(exceptionListener);
    }

    @Override
    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener exceptionListener) {
        checkNotNull(exceptionListener);
        internalBrowser.removeJavaScriptExceptionListener(exceptionListener);
    }

    /**
     * Adds a border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     * The execution of this method may be delayed.
     * The returned {@link Future} can be used to check
     * if it has happened.
     *
     * @return a future to check whether the delayed execution has happened
     */
    public Future<Void> addFocusBorder() {
        return internalBrowser.run("window.__addFocusBorder();", IConverter.CONVERTER_VOID);
    }

    /**
     * Removes the border that signifies the {@link org.eclipse.swt.widgets.Control}'s focus.
     * The execution of this method may be delayed.
     * The returned {@link Future} can be used to check
     * if it has happened.
     *
     * @return a future to check whether the delayed execution has happened
     */
    public Future<Void> removeFocusBorder() {
        return internalBrowser.run("window.__removeFocusBorder();", IConverter.CONVERTER_VOID);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Rectangle bounds = internalBrowser.getCachedContentBounds();
        if (bounds == null) {
            return super.computeSize(wHint, hHint, changed);
        }
        Point size = new Point(bounds.x + bounds.width,
                bounds.y + bounds.height);
        LOGGER.debug(
                Browser.class.getSimpleName() + ".computeSize(" + wHint + ", "
                        + hHint + ", " + changed + ") -> " + size);
        return size;
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        String hex = color != null ? new RGB(color.getRGB()).toDecString() : "transparent";
        try {
            injectCss("html, body { background-color: " + hex + "; }");
        } catch (RuntimeException e) {
            LOGGER.error("Error setting background color to " + color, e);
        }
    }

    @Override
    public void addListener(int eventType, Listener listener) {
        checkNotNull(listener);
        if (EventDelegator.mustDelegate(eventType, this)) {
            internalBrowser.addListener(eventType, listener);
        } else {
            super.addListener(eventType, listener);
        }
    }

    /**
     * Deactivate browser's native context/popup menu. Doing so allows the definition of menus in an inheriting composite via setMenu.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void deactivateNativeMenu() {
        internalBrowser.addListener(SWT.MenuDetect, new Listener() {
            @Override
            public void handleEvent(Event event) {
                event.doit = false;
            }
        });
    }
}
