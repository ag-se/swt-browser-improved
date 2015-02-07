package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.*;
import de.fu_berlin.inf.ag_se.utils.colors.RGB;
import de.fu_berlin.inf.ag_se.widgets.browser.BrowserStatusManager.BrowserStatus;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.UnexpectedBrowserStateException;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnkerListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.CallbackFunctionCallable;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.ScriptExecutingCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class Browser extends Composite implements IBrowser {

    private static Logger LOGGER = Logger.getLogger(Browser.class);

    private static final int STYLES = SWT.INHERIT_FORCE;

    public static final String FOCUS_CONTROL_ID = "com.bkahlert.nebula.browser";

    private org.eclipse.swt.browser.Browser browser;
    private BrowserStatusManager browserStatusManager;

    private boolean initWithSystemBackgroundColor;
    private boolean textSelectionsDisabled = false;
    private boolean settingUri = false;
    private boolean allowLocationChange = false;


    Rectangle cachedContentBounds = null;

    private final List<JavaScriptExceptionListener> javaScriptExceptionListeners = Collections
            .synchronizedList(new ArrayList<JavaScriptExceptionListener>());
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
        this.setLayout(new FillLayout());
        this.initWithSystemBackgroundColor = (style & SWT.INHERIT_FORCE) != 0;

        this.browser = new org.eclipse.swt.browser.Browser(this, SWT.NONE);
        this.browser.setVisible(false);
        browserStatusManager = new BrowserStatusManager();

        final JavaScriptExceptionListener javaScriptExceptionListener = new JavaScriptExceptionListener() {
            @Override
            public void thrown(JavaScriptException javaScriptException) {
                Browser.this
                        .fireJavaScriptExceptionThrown(javaScriptException);
            }
        };

        // throws exception that arise from calls within the browser,
        // meaning code that has not been invoked by Java but by JavaScript
        createBrowserFunction("__error_callback", new IBrowserFunction() {
            @Override
            public Object function(Object[] arguments) {
                JavaScriptException javaScriptException = BrowserUtils
                        .parseJavaScriptException(arguments);
                LOGGER.error(javaScriptException);
                javaScriptExceptionListener.thrown(javaScriptException);
                return false;
            }
        });

        browser.addLocationListener(new LocationAdapter() {
            @Override
            public void changing(LocationEvent event) {
                if (!Browser.this.settingUri) {
                    event.doit = allowLocationChange || getBrowserStatus() == BrowserStatus.LOADING;
                }
            }
        });

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                synchronized (monitor) {
                    if (getBrowserStatus() == BrowserStatus.LOADING) {
                        setBrowserStatus(BrowserStatus.DISPOSED);
                    }
                    monitor.notifyAll();
                }
            }
        });
        // TODO browser not yet created when this escapes...
        eventCatchFunctionality = new EventCatchFunctionality(this);
    }

    /**
     * This method waits for the {@link de.fu_berlin.inf.ag_se.widgets.browser.Browser} to complete loading. <p> It has been observed that
     * the {@link org.eclipse.swt.browser.ProgressListener#completed(org.eclipse.swt.browser.ProgressEvent)} fires to early. This method
     * uses JavaScript to reliably detect the completed state.
     *
     * @param pageLoadCheckExpression
     */
    private void waitAndComplete(String pageLoadCheckExpression) {
        if (browser == null || browser.isDisposed()) {
            return;
        }

        if (getBrowserStatus() != BrowserStatus.LOADING) {
            if (!Arrays.asList(BrowserStatus.TIMEDOUT, BrowserStatus.DISPOSED)
                       .contains(getBrowserStatus())) {
                LOGGER.error("State Error: " + getBrowserStatus());
            }
            return;
        }

        String condition = "document.readyState == 'complete'" + (
                pageLoadCheckExpression != null ?
                " && (" + pageLoadCheckExpression + ")" :
                "");
        BrowserFunction browserFunction = new BrowserFunction(browser,
                BrowserUtils.createRandomFunctionName()) {
            @Override
            public Object function(Object[] arguments) {
                complete();
                this.dispose();
                return null;
            }
        };
        String completedCheckScript = JavascriptString.createWaitForConditionJavascript(
                condition, browserFunction.getName());

        try {
            runImmediately(completedCheckScript, IConverter.CONVERTER_VOID);
        } catch (Exception e) {
            LOGGER.error("An error occurred while checking the page load state", e);
            synchronized (Browser.this.monitor) {
                Browser.this.monitor.notifyAll();
            }
        }
    }

    public void waitForCondition(String condition) {
        if (browser == null || browser.isDisposed()) {
            return;
        }

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        String randomFunctionName = BrowserUtils.createRandomFunctionName();
        createBrowserFunction(randomFunctionName, new IBrowserFunction() {
            public Object function(Object[] arguments) {
                countDownLatch.countDown();
                return null;
            }
        });
        String checkScript = JavascriptString.createWaitForConditionJavascript(condition,
                randomFunctionName);

        try {
            this.runImmediately(checkScript, IConverter.CONVERTER_VOID);
            countDownLatch.await();
            //TODO dispose browser function
        } catch (Exception e) {
            LOGGER.error("An error occurred while checking the condition", e);
        }
    }

    /**
     * This method is called by {@link #waitAndComplete(String)} and post processes the loaded page. <ol> <li>calls {@link
     * #beforeCompletion(String)}</li> <li>injects necessary scripts</li> <li>runs the scheduled user scripts</li> </ol>
     */
    private void complete() {
        final String uri = browser.getUrl();
        if (initWithSystemBackgroundColor) {
            setBackground(SWTUtils.getEffectiveBackground(Browser.this));
        }
        if (textSelectionsDisabled) {
            try {
                injectCssImmediately(
                        "* { -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; }");
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
        final Future<Void> finished = beforeCompletion(uri);
        ExecUtils.nonUISyncExec(Browser.class, "Progress Check for " + uri,
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (finished != null) {
                                finished.get();
                            }
                        } catch (Exception e) {
                            LOGGER.error(e);
                        }

                        eventCatchFunctionality.injectEventCatchScript();
                        SwtUiThreadExecutor.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (!browser.isDisposed()) {
                                    browser.setVisible(true);
                                }
                            }
                        });
                        synchronized (monitor) {
                            if (!Arrays.asList(BrowserStatus.TIMEDOUT,
                                    BrowserStatus.DISPOSED).contains(getBrowserStatus())) {
                                setBrowserStatus(BrowserStatus.LOADED);
                            }
                            monitor.notifyAll();
                        }
                    }
                });
    }

    @Override
    public Future<Boolean> open(final String uri, final Integer timeout,
                                final String pageLoadCheckExpression) {
        if (this.browser.isDisposed()) {
            throw new SWTException(SWT.ERROR_WIDGET_DISPOSED);
        }

        setBrowserStatus(BrowserStatus.LOADING);
        activateExceptionHandling();
        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
               waitAndComplete(pageLoadCheckExpression);
            }
        });

        return ExecUtils.nonUIAsyncExec(Browser.class, "Opening " + uri,
                new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        // stops waiting after timeout
                        Future<Void> timeoutMonitor = null;
                        if (timeout != null && timeout > 0) {
                            timeoutMonitor = ExecUtils.nonUIAsyncExec(Browser.class,
                                    "Timeout Watcher for " + uri, new Runnable() {
                                        @Override
                                        public void run() {
                                            synchronized (monitor) {
                                                if (getBrowserStatus() != BrowserStatus.LOADED) {
                                                    setBrowserStatus(BrowserStatus.TIMEDOUT);
                                                }
                                                monitor.notifyAll();
                                            }
                                        }
                                    }, timeout);
                        } else {
                            LOGGER.warn(
                                    "timeout must be greater or equal 0. Ignoring timeout.");
                        }

                        Browser.this.beforeLoad(uri);

                        SwtUiThreadExecutor.syncExec(new Runnable() {
                            @Override
                            public void run() {
                                Browser.this.settingUri = true;
                                if (!Browser.this.browser.isDisposed()) {
                                    Browser.this.browser.setUrl(uri);
                                }
                                Browser.this.settingUri = false;
                            }
                        });

                        Browser.this.afterLoad(uri);

                        synchronized (monitor) {
                            if (getBrowserStatus() == BrowserStatus.LOADING) {
                                LOGGER.debug(
                                        "Waiting for " + uri + " to be loaded (Thread: "
                                                + Thread.currentThread() + "; status: "
                                                + getBrowserStatus() + ")");
                                monitor.wait();
                                // notified by progresslistener or by
                                // timeout
                            }

                            if (timeoutMonitor != null) {
                                timeoutMonitor.cancel(true);
                            }

                            switch (getBrowserStatus()) {
                                case LOADED:
                                    LOGGER.debug("Successfully loaded " + uri);
                                    break;
                                case TIMEDOUT:
                                    LOGGER.warn(
                                            "Aborted loading " + uri + " due to timeout");
                                    break;
                                case DISPOSED:
                                    if (!Browser.this.browser.isDisposed()) {
                                        LOGGER.info("Aborted loading " + uri
                                                + " due to disposal");
                                    }
                                    break;
                                default:
                                    throw new RuntimeException("Implementation error");
                            }

                            return getBrowserStatus() == BrowserStatus.LOADED;
                        }
                    }
                });
    }

    @Override
    public Future<Boolean> open(String address, Integer timeout) {
        return this.open(address, timeout, null);
    }

    @Override
    public Future<Boolean> open(URI uri, Integer timeout) {
        return this.open(uri.toString(), timeout, null);
    }

    @Override
    public Future<Boolean> open(URI uri, Integer timeout,
                                String pageLoadCheckExpression) {
        return this.open(uri.toString(), timeout, pageLoadCheckExpression);
    }

    @Override
    public Future<Boolean> openBlank() {
        try {
            File empty = File.createTempFile("blank", ".html");
            FileUtils.writeStringToFile(empty,
                    "<html><head></head><body></body></html>", "UTF-8");
            return open(empty.toURI(), 60000);
        } catch (Exception e) {
            return new CompletedFuture<Boolean>(false, e);
        }
    }

    @Override
    public void setAllowLocationChange(boolean allowed) {
        this.allowLocationChange = allowed;
    }

    @Override
    public void beforeLoad(String uri) {
    }

    @Override
    public void afterLoad(String uri) {
    }

    @Override
    public Future<Void> beforeCompletion(String uri) {
        return null;
    }

    @Override
    public void addListener(int eventType, Listener listener) {
        // TODO evtl. erst ausführen, wenn alles wirklich geladen wurde, um
        // evtl. falsche Mauskoordinaten zu verhindern und so ein Fehlverhalten
        // im InformationControl vorzeugen
        if (EventDelegator.mustDelegate(eventType, this)) {
            browser.addListener(eventType, listener);
        } else {
            super.addListener(eventType, listener);
        }
    }

    /**
     * Deactivate browser's native context/popup menu. Doing so allows the definition of menus in an inheriting composite via setMenu.
     */
    public void deactivateNativeMenu() {
        this.browser.addListener(SWT.MenuDetect, new Listener() {
            @Override
            public void handleEvent(Event event) {
                event.doit = false;
            }
        });
    }

    public void deactivateTextSelections() {
        this.textSelectionsDisabled = true;
    }

    /**
     * Return the {@link org.eclipse.swt.browser.Browser} used by this timeline.
     *
     * @return must not return null (but may return an already disposed widget)
     * @internal use of this method potentially dangerous since internal state can transit to an inconsistent one
     */
    public org.eclipse.swt.browser.Browser getBrowser() {
        return this.browser;
    }

    public boolean isLoadingCompleted() {
        return getBrowserStatus() == BrowserStatus.LOADED;
    }

    private final Object monitor = new Object();

    @Override
    public Future<Boolean> inject(URI scriptURI) {
        return run(scriptURI, false);
    }

    @Override
    public Future<Boolean> run(File scriptFile) {
        Assert.isLegal(scriptFile.canRead());
        return run(scriptFile.toURI(), false);
    }

    @Override
    public Future<Boolean> run(final URI scriptURI) {
        return run(scriptURI, true);
    }

    private Future<Boolean> run(final URI scriptURI, final boolean removeAfterExecution) {
        Assert.isLegal(scriptURI != null);
        if ("file".equalsIgnoreCase(scriptURI.getScheme())) {
            File file = new File(scriptURI);
            if (removeAfterExecution) {
                LOGGER.warn("The script "
                        + scriptURI
                        + " is on the local file system. To circument security restrictions its content becomes directly executed and thus cannot be removed.");
            }

            try {
                return run(FileUtils.readFileToString(file), IConverter.CONVERTER_BOOLEAN);
            } catch (IOException e) {
                return new CompletedFuture<Boolean>(null, e);
            }
        } else {
            return ExecUtils.nonUIAsyncExec(Browser.class, "Script Runner for: " + scriptURI,
                    new CallbackFunctionCallable(this, scriptURI, removeAfterExecution));
        }
    }

    @Override
    public Future<Object> run(final String script) {
        return run(script, new IConverter<Object, Object>() {
            @Override
            public Object convert(Object object) {
                return object;
            }
        });
    }

    @Override
    public <DEST> Future<DEST> run(final String script,
                                   final IConverter<Object, DEST> converter) {
        Assert.isLegal(converter != null);
        return browserStatusManager.createFuture(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    @Override
    public <DEST> DEST runImmediately(String script,
                                      IConverter<Object, DEST> converter) throws Exception {
        return SwtUiThreadExecutor.syncExec(new ScriptExecutingCallable<DEST>(this, converter, script));
    }

    @Override
    public void runContentsImmediately(File scriptFile) throws Exception {
        runImmediately(FileUtils.readFileToString(scriptFile), IConverter.CONVERTER_VOID);
    }

    @Override
    public void runContentsAsScriptTagImmediately(File scriptFile)
            throws Exception {
        runImmediately(JavascriptString.embedContentsIntoScriptTag(scriptFile), IConverter.CONVERTER_VOID);
    }

    @Override
    public void scriptAboutToBeSentToBrowser(String script) {
        return;
    }

    @Override
    public void scriptReturnValueReceived(Object returnValue) {
        return;
    }

    @Override
    public Future<Void> injectJsFile(File file) {
        return run(createJsFileInjectionScript(file),
                IConverter.CONVERTER_VOID);
    }

    @Override
    public void injectJsFileImmediately(File file) throws Exception {
        runImmediately(createJsFileInjectionScript(file),
                IConverter.CONVERTER_VOID);
    }

    private static String createJsFileInjectionScript(File file) {
        return
                "var script=document.createElement(\"script\"); script.type=\"text/javascript\"; script.src=\""
                        + file.toURI()
                        + "\"; document.getElementsByTagName(\"head\")[0].appendChild(script);";
    }

    @Override
    public Future<Void> injectCssFile(URI uri) {
        return run(createCssFileInjectionScript(uri),
                IConverter.CONVERTER_VOID);
    }

    public void injectCssFileImmediately(URI uri) throws Exception {
        runImmediately(createCssFileInjectionScript(uri),
                IConverter.CONVERTER_VOID);
    }

    private static String createCssFileInjectionScript(URI uri) {
        return "if(document.createStyleSheet){document.createStyleSheet(\""
                + uri.toString()
                + "\")}else{var link=document.createElement(\"link\"); link.rel=\"stylesheet\"; link.type=\"text/css\"; link.href=\""
                + uri.toString()
                + "\"; document.getElementsByTagName(\"head\")[0].appendChild(link); }";
    }

    @Override
    public Future<Void> injectCss(String css) {
        return run(createCssInjectionScript(css), IConverter.CONVERTER_VOID);
    }

    @Override
    public void injectCssImmediately(String css) throws Exception {
        runImmediately(createCssInjectionScript(css),
                IConverter.CONVERTER_VOID);
    }

    private static String createCssInjectionScript(String css) {
        return
                "(function(){var style=document.createElement(\"style\");style.appendChild(document.createTextNode(\""
                        + css
                        + "\"));(document.getElementsByTagName(\"head\")[0]||document.documentElement).appendChild(style)})()";
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
        this.javaScriptExceptionListeners.add(javaScriptExceptionListener);
    }

    @Override
    public void removeJavaScriptExceptionListener(
            JavaScriptExceptionListener javaScriptExceptionListener) {
        this.javaScriptExceptionListeners.remove(javaScriptExceptionListener);
    }

    @Override
    public String getUrl() {
        return browser.getUrl();
    }

    synchronized protected void fireJavaScriptExceptionThrown(
            JavaScriptException javaScriptException) {
        for (JavaScriptExceptionListener JavaScriptExceptionListener : this.javaScriptExceptionListeners) {
            JavaScriptExceptionListener.thrown(javaScriptException);
        }
    }

    @Override
    public Future<Boolean> containsElementWithID(String id) {
        return this.run("return document.getElementById('" + id + "') != null",
                IConverter.CONVERTER_BOOLEAN);
    }

    @Override
    public Future<Boolean> containsElementsWithName(String name) {
        return this
                .run("return document.getElementsByName('" + name + "').length > 0",
                        IConverter.CONVERTER_BOOLEAN);
    }

    public static String escape(String html) {
        return html.replace("\n", "<br>").replace("&#xD;", "").replace("\r", "")
                   .replace("\"", "\\\"").replace("'", "\\'");
    }

    @Override
    public Future<Void> setBodyHtml(String html) {
        return this
                .run("document.body.innerHTML = ('" + Browser.escape(html) + "');",
                        IConverter.CONVERTER_VOID);
    }

    @Override
    public Future<String> getBodyHtml() {
        return this
                .run("return document.body.innerHTML", IConverter.CONVERTER_STRING);
    }

    @Override
    public Future<String> getHtml() {
        return this.run("return document.documentElement.outerHTML",
                IConverter.CONVERTER_STRING);
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        String hex = color != null ?
                     new RGB(color.getRGB()).toDecString() :
                     "transparent";
        try {
            this.injectCssImmediately(
                    "html, body { background-color: " + hex + "; }");
        } catch (Exception e) {
            LOGGER.error("Error setting background color to " + color, e);
        }
    }

    @Override
    public Future<Void> pasteHtmlAtCaret(String html) {
        String escapedHtml = Browser.escape(html);
        try {
            File js = File.createTempFile("paste", ".js");
            FileUtils.write(js,
                    "if(['input','textarea'].indexOf(document.activeElement.tagName.toLowerCase()) != -1) { document.activeElement.value = '");
            FileOutputStream outStream = new FileOutputStream(js, true);
            IOUtils.copy(IOUtils.toInputStream(escapedHtml), outStream);
            IOUtils.copy(IOUtils.toInputStream(
                            "';} else { var t,n;if(window.getSelection){t=window.getSelection();if(t.getRangeAt&&t.rangeCount){n=t.getRangeAt(0);n.deleteContents();var r=document.createElement(\"div\");r.innerHTML='"),
                    outStream);
            IOUtils.copy(IOUtils.toInputStream(escapedHtml), outStream);
            IOUtils.copy(IOUtils.toInputStream(
                            "';var i=document.createDocumentFragment(),s,o;while(s=r.firstChild){o=i.appendChild(s)}n.insertNode(i);if(o){n=n.cloneRange();n.setStartAfter(o);n.collapse(true);t.removeAllRanges();t.addRange(n)}}}else if(document.selection&&document.selection.type!=\"Control\"){document.selection.createRange().pasteHTML('"),
                    outStream);
            IOUtils
                    .copy(IOUtils.toInputStream(escapedHtml + "')}}"), outStream);
            return this.injectJsFile(js);
        } catch (Exception e) {
            return new CompletedFuture<Void>(null, e);
        }
    }

    @Override
    public Future<Void> addFocusBorder() {
        return run("window.__addFocusBorder();", IConverter.CONVERTER_VOID);
    }

    @Override
    public Future<Void> removeFocusBorder() {
        return run("window.__removeFocusBorder();", IConverter.CONVERTER_VOID);
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Rectangle bounds = this.cachedContentBounds;
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

    public IBrowserFunction createBrowserFunction(final String functionName,
                                                  final IBrowserFunction function) {
        try {
            return SwtUiThreadExecutor.syncExec(new Callable<IBrowserFunction>() {
                @Override
                public IBrowserFunction call() throws Exception {
                    new BrowserFunction(browser, functionName) {
                        @Override
                        public Object function(Object[] arguments) {
                            return function.function(arguments);
                        }
                    };
                    return function;
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }


    public void execute(String javaScript) {
        run(javaScript);
    }

    public Future<Boolean> setUrl(String url) {
        return open(url, 5000);
    }

    public Object evaluate(String javaScript) {
        return browser.evaluate(javaScript);
    }

    /**
     * Notifies all registered Javascript exception listeners in case a JavaScript error occurred.
     */
    public void activateExceptionHandling() {
        try {
            runImmediately(BrowserUtils.getExceptionForwardingScript("__error_callback"), IConverter.CONVERTER_VOID);
        } catch (Exception e) {
            LOGGER.error("Error activating browser's exception handling. JavaScript exceptions are not detected!", e);
        }
    }

    public void setBrowserStatus(BrowserStatus browserStatus) throws UnexpectedBrowserStateException {
        browserStatusManager.setBrowserStatus(browserStatus);
    }

    /**
     * Returns the state of the browser.
     *
     * @return
     */
    public BrowserStatus getBrowserStatus() {
        return browserStatusManager.getBrowserStatus();
    }
}
