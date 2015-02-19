package de.fu_berlin.inf.ag_se.browser.swt;

import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The methods in this class all execute stuff on the SWT UI thread
 */
public class SwtUiThreadExecutor implements UIThreadExecutor {

    private static final Logger LOGGER = Logger.getLogger(SwtUiThreadExecutor.class);

    @Override
    public <V> V syncExec(final NoCheckedExceptionCallable<V> callable) {
        if (isUIThread()) {
            return callable.call();
        }
        final AtomicReference<V> r = new AtomicReference<V>();
        final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    r.set(callable.call());
                } catch (RuntimeException e) {
                    exception.set(e);
                }
            }
        });
        if (exception.get() != null) {
            throw exception.get();
        }
        return r.get();
    }

    @Override
    public void syncExec(final Runnable runnable) {
        if (isUIThread()) {
            runnable.run();
        } else {
            final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
            //TODO maybe catch SWTExceptions?
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (RuntimeException e) {
                        exception.set(e);
                    }
                }
            });
            if (exception.get() != null) {
                throw exception.get();
            }
        }
    }

    @Override
    public boolean isUIThread() {
        try {
            return Display.getCurrent() != null;
        } catch (SWTException e) {
            return false;
        }
    }

    @Override
    public void checkNotUIThread() {
        if (isUIThread()) {
            throw new IllegalStateException("This method must not be called from the UI thread.");
        }
    }
}
