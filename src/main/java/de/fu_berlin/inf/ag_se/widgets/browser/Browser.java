package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.utils.colors.RGB;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnkerListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener;
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
import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Browser extends Composite implements IBrowser {

    private static Logger LOGGER = Logger.getLogger(Browser.class);

    private static final int STYLES = SWT.INHERIT_FORCE;

    protected InternalBrowserWrapper internalBrowser;

    private boolean initWithSystemBackgroundColor;

    private boolean textSelectionsDisabled = false;

    private EventCatchFunctionality eventCatchFunctionality;

    /**
     * Constructs a new {@link de.fu_berlin.inf.ag_se.widgets.browser.Browser} with the given styles.
     *
     * @param parent
     * @param style  if {@link org.eclipse.swt.SWT#INHERIT_FORCE}) is set the loaded page's background is replaced by the inherited
     *               background color
     */
    public Browser(Composite parent, int style) {
        super(parent, style | SWT.EMBEDDED & ~STYLES);
        setLayout(new FillLayout());
        initWithSystemBackgroundColor = (style & SWT.INHERIT_FORCE) != 0;

        internalBrowser = new InternalBrowserWrapper(this);

        eventCatchFunctionality = new EventCatchFunctionality(internalBrowser);

        executeBeforeCompletion(new Runnable() {
            @Override
            public void run() {
                if (initWithSystemBackgroundColor) {
                    setBackground(SWTUtils.getEffectiveBackground(Browser.this));
                }
                if (textSelectionsDisabled) {
                    try {
                        injectCssImmediately(JavascriptString.createCssToDisableTextSelection());
                    } catch (Exception e) {
                        LOGGER.error(e);
                    }
                }
                eventCatchFunctionality.injectEventCatchScript();
            }
        });
    }

    @Override
    public Future<Boolean> open(String address, Integer timeout) {
        return open(address, timeout, null);
    }

    @Override
    public Future<Boolean> open(URI uri, Integer timeout) {
        return open(uri.toString(), timeout, null);
    }

    @Override
    public Future<Boolean> open(URI uri, Integer timeout,
                                String pageLoadCheckExpression) {
        return open(uri.toString(), timeout, pageLoadCheckExpression);
    }

    @Override
    public Future<Boolean> open(String uri, Integer timeout, String pageLoadCheckScript) {
        return internalBrowser.open(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> openBlank() {
        return open(BrowserUtils.createBlankHTMLFile(), 3000);
    }

    @Override
    public void executeBeforeLoading(Runnable runnable) {
        internalBrowser.executeBeforeLoading(runnable);
    }

    @Override
    public void executeAfterLoading(Runnable runnable) {
        internalBrowser.executeAfterLoading(runnable);
    }

    @Override
    public void executeBeforeCompletion(Runnable runnable) {
        internalBrowser.executeBeforeCompletion(runnable);
    }

    @Override
    public Future<Boolean> inject(URI scriptURI) {
        return internalBrowser.inject(scriptURI);
    }

    @Override
    public Future<Boolean> run(File script) {
        return internalBrowser.run(script);
    }

    @Override
    public Future<Boolean> run(URI scriptURI) {
        return internalBrowser.run(scriptURI);
    }

    @Override
    public Future<Object> run(String script) {
        return internalBrowser.run(script);
    }

    /**
     * Must not be called from the SWT UI thread.
     * May return null.
     *
     * @param script Javascript to be evaluated as string
     * @return the result of the evaluation as Java object
     */
    public Object evaluate(String script) {
        if (ExecUtils.isUIThread())
            throw new IllegalStateException("This method must not be called from the SWT UI thread.");

        Future<Object> res = run(script);
        try {
            return res.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Interrupted while waiting for the result. Returning null.");
            return null;
        } catch (ExecutionException e) {
            LOGGER.error("Could not evaluate script " + script, e);
            return null;
        }
    }

    @Override
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter) {
        return internalBrowser.run(script, converter);
    }

    @Override
    public void runContentsImmediately(File scriptFile) throws Exception {
        internalBrowser.runContentsImmediately(scriptFile);
    }

    @Override
    public void runContentsAsScriptTagImmediately(File scriptFile) throws Exception {
        internalBrowser.runContentsAsScriptTagImmediately(scriptFile);
    }

    @Override
    public <DEST> DEST runImmediately(String script, IConverter<Object, DEST> converter) throws Exception {
        return internalBrowser.runImmediately(script, converter);
    }

    @Override
    public Future<Void> injectJsFile(File file) {
        return internalBrowser.injectJsFile(file);
    }

    @Override
    public void injectJsFileImmediately(File jsExtension) throws Exception {
        internalBrowser.injectJsFileImmediately(jsExtension);
    }

    @Override
    public Future<Void> injectCssFile(URI uri) {
        return internalBrowser.injectCssFile(uri);
    }

    @Override
    public Future<Void> injectCss(String css) {
        return internalBrowser.injectCss(css);
    }

    @Override
    public void injectCssImmediately(String css) throws Exception {
        internalBrowser.injectCssImmediately(css);
    }


    /**
     * Deactivate browser's native context/popup menu. Doing so allows the definition of menus in an inheriting composite via setMenu.
     */
    public void deactivateNativeMenu() {
        internalBrowser.addListener(SWT.MenuDetect, new Listener() {
            @Override
            public void handleEvent(Event event) {
                event.doit = false;
            }
        });
    }

    public void deactivateTextSelections() {
        this.textSelectionsDisabled = true;
    }

    @Override
    public void addAnkerListener(IAnkerListener ankerListener) {
        eventCatchFunctionality.addAnkerListener(ankerListener);
    }

    @Override
    public void removeAnkerListener(IAnkerListener ankerListener) {
        eventCatchFunctionality.removeAnkerListener(ankerListener);
    }

    @Override
    public void addMouseListener(IMouseListener mouseListener) {
        eventCatchFunctionality.addMouseListener(mouseListener);
    }

    @Override
    public void removeMouseListener(IMouseListener mouseListener) {
        eventCatchFunctionality.removeMouseListener(mouseListener);
    }

    @Override
    public void addFocusListener(IFocusListener focusListener) {
        eventCatchFunctionality.addFocusListener(focusListener);
    }

    @Override
    public void removeFocusListener(IFocusListener focusListener) {
        eventCatchFunctionality.removeFocusListener(focusListener);
    }

    @Override
    public void addDNDListener(IDNDListener dndListener) {
        eventCatchFunctionality.addDNDListener(dndListener);
    }

    @Override
    public void removeDNDListener(IDNDListener dndListener) {
        eventCatchFunctionality.removeDNDListener(dndListener);
    }


    @Override
    public void addJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener) {
        internalBrowser.addJavaScriptExceptionListener(javaScriptExceptionListener);
    }

    @Override
    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener) {
        internalBrowser.removeJavaScriptExceptionListener(javaScriptExceptionListener);
    }

    @Override
    public Future<Boolean> containsElementWithID(String id) {
        return run("return document.getElementById('" + id + "') != null", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Boolean> containsElementsWithName(String name) {
        return run("return document.getElementsByName('" + name + "').length > 0", IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Void> setBodyHtml(String html) {
        return run("document.body.innerHTML = ('" + JavascriptString.escape(html) + "');", IConverter.CONVERTER_VOID);
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
    public void setBackground(Color color) {
        super.setBackground(color);
        String hex = color != null ? new RGB(color.getRGB()).toDecString() : "transparent";
        try {
            this.injectCssImmediately("html, body { background-color: " + hex + "; }");
        } catch (Exception e) {
            LOGGER.error("Error setting background color to " + color, e);
        }
    }

    @Override
    public Future<Void> pasteHtmlAtCaret(String html) {
        try {
            File js = File.createTempFile("paste", ".js");
            FileUtils.write(js, JavascriptString.createJavascriptForInsertingHTML(html));
            return injectJsFile(js);
        } catch (Exception e) {
            return new CompletedFuture<Void>(null, e);
        }
    }

    @Override
    public Future<Void> addFocusBorder() {
        return internalBrowser.run("window.__addFocusBorder();", IConverter.CONVERTER_VOID);
    }

    @Override
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
    public String getUrl() {
        return internalBrowser.getUrl();
    }

    @Override
    public void addListener(int eventType, Listener listener) {
        if (EventDelegator.mustDelegate(eventType, this)) {
            internalBrowser.addListener(eventType, listener);
        } else {
            super.addListener(eventType, listener);
        }
    }

    public boolean isLoadingCompleted() {
        return internalBrowser.isLoadingCompleted();
    }

    public IBrowserFunction createBrowserFunction(final String functionName,
                                                  final IBrowserFunction function) {
        return internalBrowser.createBrowserFunction(functionName, function);
    }

    public void waitForCondition(String condition) {
        internalBrowser.waitForCondition(condition);
    }

    @Override
    public void setAllowLocationChange(boolean allowed) {
        internalBrowser.setAllowLocationChange(allowed);
    }

    @Override
    public void scriptAboutToBeSentToBrowser(String script) {

    }

    @Override
    public void scriptReturnValueReceived(Object returnValue) {

    }
}
