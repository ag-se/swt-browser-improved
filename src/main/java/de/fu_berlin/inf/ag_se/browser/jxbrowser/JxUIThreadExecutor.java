package de.fu_berlin.inf.ag_se.browser.jxbrowser;

import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

public class JxUIThreadExecutor implements UIThreadExecutor {

    @Override
    public <V> V syncExec(final NoCheckedExceptionCallable<V> callable) {
        if (isUIThread()) {
            return callable.call();
        }
        final AtomicReference<V> r = new AtomicReference<V>();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    r.set(callable.call());
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }

        return r.get();
    }

    @Override
    public void syncExec(Runnable runnable) {
        if (isUIThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (InvocationTargetException e) {
                throw (RuntimeException) e.getCause();
            }
        }
    }

    @Override
    public boolean isUIThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    @Override
    public void checkNotUIThread() {
        if (isUIThread()) {
            throw new IllegalStateException("This method must not be called from the UI thread.");
        }
    }
}
