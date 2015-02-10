package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.utils.colors.RGB;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.JavaScriptExceptionListener;
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
import java.util.concurrent.Future;

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
    public Future<Boolean> open(String uri, Integer timeout) {
        return open(uri, timeout, null);
    }

    @Override
    public Future<Boolean> open(String uri, Integer timeout, String pageLoadCheckScript) {
        return internalBrowser.open(uri, timeout, pageLoadCheckScript);
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
    public Future<Boolean> openBlank() {
        return open(BrowserUtils.createBlankHTMLFile(), 3000);
    }

    @Override
    public void executeBeforeSettingURI(Runnable runnable) {
        internalBrowser.executeBeforeLoading(runnable);
    }

    @Override
    public void executeAfterSettingURI(Runnable runnable) {
        internalBrowser.executeAfterLoading(runnable);
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
        internalBrowser.waitForCondition(javaScriptExpression);
    }

    @Override
    public void executeBeforeCompletion(Runnable runnable) {
        internalBrowser.executeBeforeCompletion(runnable);
    }

    @Override
    public Future<Void> injectJavascriptFile(File javascriptFile) {
        return internalBrowser.injectJsFile(javascriptFile);
    }

    @Override
    public void injectJavascriptFileImmediately(File javascriptFile) throws Exception {
        internalBrowser.injectJsFileImmediately(javascriptFile);
    }

    @Override
    public Future<Void> injectCssFile(URI uri) {
        return internalBrowser.injectCssFile(uri);
    }

    @Override
    public void injectCssFileImmediately(URI uri) throws Exception {
        internalBrowser.injectCssFileImmediately(uri);
    }

    @Override
    public Future<Void> injectCss(String css) {
        return internalBrowser.injectCss(css);
    }

    @Override
    public void injectCssImmediately(String css) throws Exception {
        internalBrowser.injectCssImmediately(css);
    }

    @Override
    public Future<Boolean> inject(URI scriptURI) {
        return internalBrowser.inject(scriptURI);
    }

    @Override
    public Future<Boolean> run(File scriptFile) {
        return internalBrowser.run(scriptFile);
    }

    @Override
    public Future<Boolean> run(URI scriptURI) {
        return internalBrowser.run(scriptURI);
    }

    @Override
    public Future<Object> run(String script) {
        return internalBrowser.run(script);
    }

    @Override
    public Object syncRun(String script) {
        return internalBrowser.syncRun(script);
    }

    @Override
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter) {
        return internalBrowser.run(script, converter);
    }

    @Override
    public void runContentImmediately(File scriptFile) throws Exception {
        internalBrowser.runContentsImmediately(scriptFile);
    }

    @Override
    public void runContentAsScriptTagImmediately(File scriptFile) throws Exception {
        internalBrowser.runContentsAsScriptTagImmediately(scriptFile);
    }

    @Override
    public <DEST> DEST runImmediately(String script, IConverter<Object, DEST> converter) throws Exception {
        return internalBrowser.runImmediately(script, converter);
    }

    @Override
    public void executeBeforeScript(ParametrizedRunnable<String> runnable) {
        internalBrowser.executeBeforeScript(runnable);
    }

    @Override
    public void executeAfterScript(ParametrizedRunnable<Object> runnable) {
        internalBrowser.executeAfterScript(runnable);
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
    public Future<Void> pasteHtmlAtCaret(String html) {
        try {
            File js = File.createTempFile("paste", ".js");
            FileUtils.write(js, JavascriptString.createJavascriptForInsertingHTML(html));
            return injectJavascriptFile(js);
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
    public void setAllowLocationChange(boolean allowed) {
        internalBrowser.setAllowLocationChange(allowed);
    }

    @Override
    public void deactivateTextSelections() {
        this.textSelectionsDisabled = true;
    }

    @Override
    public IBrowserFunction createBrowserFunction(final String functionName,
                                                  final IBrowserFunction function) {
        return internalBrowser.createBrowserFunction(functionName, function);
    }

    @Override
    public void addAnchorListener(IAnchorListener anchorListener) {
        eventCatchFunctionality.addAnchorListener(anchorListener);
    }

    @Override
    public void removeAnchorListener(IAnchorListener anchorListener) {
        eventCatchFunctionality.removeAnchorListener(anchorListener);
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
            JavaScriptExceptionListener exceptionListener) {
        internalBrowser.addJavaScriptExceptionListener(exceptionListener);
    }

    @Override
    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener exceptionListener) {
        internalBrowser.removeJavaScriptExceptionListener(exceptionListener);
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
            this.injectCssImmediately("html, body { background-color: " + hex + "; }");
        } catch (Exception e) {
            LOGGER.error("Error setting background color to " + color, e);
        }
    }

    @Override
    public void addListener(int eventType, Listener listener) {
        if (EventDelegator.mustDelegate(eventType, this)) {
            internalBrowser.addListener(eventType, listener);
        } else {
            super.addListener(eventType, listener);
        }
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
}
