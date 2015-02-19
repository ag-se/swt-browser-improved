package de.fu_berlin.inf.ag_se.browser.javafx;

import de.fu_berlin.inf.ag_se.browser.threading.NoCheckedExceptionCallable;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class JavaFxUiThreadExecutor implements UIThreadExecutor {

    @Override
    public <V> V syncExec(final NoCheckedExceptionCallable<V> callable) {
        if (isUIThread()) {
            return callable.call();
        }
        final AtomicReference<V> r = new AtomicReference<V>();
        final AtomicReference<RuntimeException> exception = new AtomicReference<RuntimeException>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    r.set(callable.call());
                } catch (RuntimeException e) {
                    exception.set(e);
                } finally {
                    countDownLatch.countDown();
                }
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (RuntimeException e) {
                        exception.set(e);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (exception.get() != null) {
                throw exception.get();
            }
        }
    }

    @Override
    public boolean isUIThread() {
        return Platform.isFxApplicationThread();
    }

    @Override
    public void checkNotUIThread() {
        if (isUIThread()) {
            throw new IllegalStateException("This method must not be called from the UI thread.");
        }
    }
}
