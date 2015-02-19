package de.fu_berlin.inf.ag_se.widgets.browser;

import com.sun.istack.internal.Nullable;
import de.fu_berlin.inf.ag_se.utils.Assert;
import de.fu_berlin.inf.ag_se.utils.IConverter;
import de.fu_berlin.inf.ag_se.utils.SWTUtils;
import de.fu_berlin.inf.ag_se.utils.colors.RGB;
import de.fu_berlin.inf.ag_se.widgets.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.widgets.browser.functions.Function;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.JavaScriptExceptionListener;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

public class SwtBrowser<T extends Browser> extends AbstractSwtBrowser<T> implements IBrowser {

    private static Logger LOGGER = Logger.getLogger(SwtBrowser.class);

    private static final int STYLES = SWT.INHERIT_FORCE;

    /**
     * Constructs a new browser composite with the given styles.
     *
     * @param parent a widget which will be the parent of the new instance (cannot be null)
     * @param style  if {@link org.eclipse.swt.SWT#INHERIT_FORCE} is set the loaded page's
     *               background is replaced by the inherited background color
     */
    public SwtBrowser(final Composite parent, int style) {
        super(parent, style);
        setLayout(new FillLayout());
    }

    public static SwtBrowser createSWTBrowser(final Composite parent, final int style) {
        final SwtBrowser swtBrowser = new SwtBrowser(parent, style);
        SwtInternalBrowserWrapper internalBrowser = new SwtInternalBrowserWrapper(swtBrowser);
        swtBrowser.setInternalBrowser(internalBrowser);
        swtBrowser.setBrowser(new Browser(internalBrowser));
        swtBrowser.executeAfterCompletion(new Runnable() {
            @Override
            public void run() {
                boolean initWithSystemBackgroundColor = (style & SWT.INHERIT_FORCE) != 0;
                if (initWithSystemBackgroundColor) {
                    swtBrowser.setBackground(SWTUtils.getEffectiveBackground(parent));
                }
            }
        });
        return swtBrowser;
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
        return browser.run("window.__addFocusBorder();", IConverter.CONVERTER_VOID);
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
        return browser.run("window.__removeFocusBorder();", IConverter.CONVERTER_VOID);
    }

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

    public void setBackground(Color color) {
        super.setBackground(color);
        String hex = color != null ? new RGB(color.getRGB()).toDecString() : "transparent";
        try {
            injectCss("html, body { background-color: " + hex + "; }");
        } catch (RuntimeException e) {
            LOGGER.error("Error setting background color to " + color, e);
        }
    }

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

    @Override
    public Future<Boolean> open(String uri, int timeout) {
        return browser.open(uri, timeout);
    }

    @Override
    public Future<Boolean> open(String uri, int timeout, @Nullable String pageLoadCheckScript) {
        return browser.open(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout) {
        return browser.open(uri, timeout);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout) {
        return browser.syncOpen(uri, timeout);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, CallbackFunction<Boolean, Boolean> callback) {
        return browser.open(uri, timeout, callback);
    }

    @Override
    public Future<Boolean> open(URI uri, int timeout, @Nullable String pageLoadCheckScript) {
        return browser.open(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public boolean syncOpen(URI uri, int timeout, @Nullable String pageLoadCheckScript) {
        return browser.syncOpen(uri, timeout, pageLoadCheckScript);
    }

    @Override
    public Future<Boolean> openWithCallback(URI uri, int timeout, @Nullable String pageLoadCheckScript,
                                            CallbackFunction<Boolean, Boolean> callback) {
        return browser.openWithCallback(uri, timeout, pageLoadCheckScript, callback);
    }

    @Override
    public Future<Boolean> openBlank() {
        return browser.openBlank();
    }

    @Override
    public boolean isLoadingCompleted() {
        return browser.isLoadingCompleted();
    }

    @Override
    public String getUrl() {
        return browser.getUrl();
    }

    @Override
    public void waitForCondition(String javaScriptExpression) {
        browser.waitForCondition(javaScriptExpression);
    }

    @Override
    public Future<Void> checkCondition(String javaScriptExpression) {
        return browser.checkCondition(javaScriptExpression);
    }

    @Override
    public <DEST> Future<DEST> executeWhenConditionIsMet(String javaScriptExpression, CallbackFunction<Void, DEST> callback) {
        return browser.executeWhenConditionIsMet(javaScriptExpression, callback);
    }

    @Override
    public void executeAfterCompletion(Runnable runnable) {
        browser.executeAfterCompletion(runnable);
    }

    @Override
    public Future<Boolean> injectJavascript(File javascriptFile) {
        return browser.injectJavascript(javascriptFile);
    }

    @Override
    public Future<Boolean> injectJavascript(URI scriptURI) {
        return browser.injectJavascript(scriptURI);
    }

    @Override
    public boolean syncInjectJavascript(URI scriptURI) {
        return browser.syncInjectJavascript(scriptURI);
    }

    @Override
    public Future<Boolean> injectCssURI(URI cssURI) {
        return browser.injectCssURI(cssURI);
    }

    @Override
    public boolean syncInjectCssURI(URI cssURI) {
        return browser.syncInjectCssURI(cssURI);
    }

    @Override
    public Future<Boolean> injectCss(String css) {
        return browser.injectCss(css);
    }

    @Override
    public boolean syncInjectCss(String css) {
        return browser.syncInjectCss(css);
    }

    @Override
    public <DEST> Future<DEST> injectCss(String css, CallbackFunction<Boolean, DEST> callback) {
        return browser.injectCss(css, callback);
    }

    @Override
    public Future<Boolean> run(File scriptFile) {
        return browser.run(scriptFile);
    }

    @Override
    public Future<Boolean> runContentAsScriptTag(File scriptFile) throws IOException {
        return browser.runContentAsScriptTag(scriptFile);
    }

    @Override
    public Future<Boolean> run(URI scriptURI) {
        return browser.run(scriptURI);
    }

    @Override
    public Future<Object> run(String script) {
        return browser.run(script);
    }

    @Override
    public Object syncRun(String script) {
        return browser.syncRun(script);
    }

    @Override
    public <DEST> Future<DEST> run(String script, CallbackFunction<Object, DEST> callback) {
        return browser.run(script, callback);
    }

    @Override
    public <DEST> Future<DEST> run(String script, IConverter<Object, DEST> converter) {
        return browser.run(script, converter);
    }

    @Override
    public <DEST> DEST syncRun(String script, IConverter<Object, DEST> converter) {
        return browser.syncRun(script, converter);
    }

    @Override
    public <T, DEST> Future<T> run(String script, IConverter<Object, DEST> converter, CallbackFunction<DEST, T> callback) {
        return browser.run(script, converter, callback);
    }

    @Override
    public void executeBeforeScript(Function<String> function) {
        browser.executeBeforeScript(function);
    }

    @Override
    public void executeAfterScript(Function<Object> function) {
        browser.executeAfterScript(function);
    }

    @Override
    public Future<Boolean> containsElementWithID(String id) {
        return browser.containsElementWithID(id);
    }

    @Override
    public Future<Boolean> containsElementsWithName(String name) {
        return browser.containsElementsWithName(name);
    }

    @Override
    public Future<Boolean> setBodyHtml(String html) {
        return browser.setBodyHtml(html);
    }

    @Override
    public Future<String> getBodyHtml() {
        return browser.getBodyHtml();
    }

    @Override
    public Future<String> getHtml() {
        return browser.getHtml();
    }

    @Override
    public <T> Future<T> getHtml(CallbackFunction<String, T> callback) {
        return browser.getHtml(callback);
    }

    @Override
    public Future<Boolean> pasteHtmlAtCaret(String html) {
        return browser.pasteHtmlAtCaret(html);
    }

    @Override
    public void setAllowLocationChange(boolean allowed) {
        browser.setAllowLocationChange(allowed);
    }

    @Override
    public void deactivateTextSelections() {
        browser.deactivateTextSelections();
    }

    @Override
    public IBrowserFunction createBrowserFunction(IBrowserFunction function) {
        return browser.createBrowserFunction(function);
    }

    @Override
    public void addJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener) {
        browser.addJavaScriptExceptionListener(exceptionListener);
    }

    @Override
    public void removeJavaScriptExceptionListener(JavaScriptExceptionListener exceptionListener) {
        browser.removeJavaScriptExceptionListener(exceptionListener);
    }

    public void setCachedContentBounds(Rectangle rectangle) {
        browser.setCachedContentBounds(rectangle);
    }

    public Rectangle getCachedContentBounds() {
        return browser.getCachedContentBounds();
    }

    public void layoutRoot() {
        internalBrowser.layoutRoot();
    }

    /**
     * This class helps by delegating the registration of event listeners.
     *
     * This can be useful for {@link Composite}s who's child controls catch the
     * events making the {@link Composite} not notice any events.
     */
    private static class EventDelegator {
        /**
         * Returns true if the given eventType can't be processed by the given
         * {@link org.eclipse.swt.widgets.Control}.
         */
        public static boolean mustDelegate(int eventType, Control control) {
            Assert.isLegal(control != null);
            return control instanceof Composite &&
                    (eventType == SWT.MouseMove
                            || eventType == SWT.MouseEnter
                            || eventType == SWT.MouseHover
                            || eventType == SWT.MouseExit
                            || eventType == SWT.FocusIn
                            || eventType == SWT.FocusOut);
        }
    }
}
