package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.IWrappedBrowser;
import de.fu_berlin.inf.ag_se.browser.functions.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.functions.JavascriptFunction;
import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import de.fu_berlin.inf.ag_se.browser.utils.SWTUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

public class SWTFrameworkBrowser implements IWrappedBrowser {

    private final Browser browser;
    private final SWTThreadExecutor uiThreadExecutor = new SWTThreadExecutor();

    public SWTFrameworkBrowser(Composite parent) {
        browser = new Browser(parent, SWT.NONE);
    }

    @Override
    public void setVisible(final boolean visible) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.setVisible(visible);
            }
        });
    }

    /**
     * May be called from whatever thread.
     */
    @Override
    public IBrowserFunction createBrowserFunction(final JavascriptFunction function) {
        return uiThreadExecutor.syncExec(new NoCheckedExceptionCallable<IBrowserFunction>() {
            @Override
            public IBrowserFunction call() {
                BrowserFunction swtFunction = new BrowserFunction(browser, function.getName()) {
                    @Override
                    public Object function(Object[] arguments) {
                        return function.function(arguments);
                    }
                };
                return new SWTBrowserFunction(swtFunction);
            }
        });
    }

    @Override
    public String getUrl() {
        return uiThreadExecutor.syncExec(new NoCheckedExceptionCallable<String>() {
            @Override
            public String call() {
                return browser.getUrl();
            }
        });
    }

    @Override
    public void setUrl(final String url) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.setUrl(url);
            }
        });
    }

    @Override
    public UIThreadExecutor getUIThreadExecutor() {
        return uiThreadExecutor;
    }

    @Override
    public void setSize(final int width, final int height) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.setSize(width, height);
            }
        });
    }

    @Override
    public void setText(final String html) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.setText(html);
            }
        });
    }

    @Override
    public boolean setFocus() {
        return uiThreadExecutor.syncExec(new NoCheckedExceptionCallable<Boolean>() {
            @Override
            public Boolean call() {
                return browser.setFocus();
            }
        });
    }

    @Override
    public Object evaluate(final String javascript) {
        return uiThreadExecutor.syncExec(new NoCheckedExceptionCallable<Object>() {
            @Override
            public Object call() {
                return browser.evaluate(javascript);
            }
        });
    }

    @Override
    public void addLoadedListener(final Runnable runnable) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.addProgressListener(new ProgressAdapter() {
                    @Override
                    public void completed(ProgressEvent event) {
                        runnable.run();
                    }
                });
            }
        });
    }

    @Override
    public boolean isDisposed() {
        return browser.isDisposed();
    }

    public void addLocationListener(final LocationListener listener) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.addLocationListener(listener);
            }
        });
    }

    public void addDisposeListener(final DisposeListener disposeListener) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.addDisposeListener(disposeListener);
            }
        });

    }

    public void addListener(final int eventType, final Listener listener) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                browser.addListener(eventType, listener);
            }
        });
    }

    public void layoutRoot() {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                Composite root = SWTUtils.getRoot(browser);
                root.layout(true, true);
            }
        });
    }
}
