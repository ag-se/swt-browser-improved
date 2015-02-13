package de.fu_berlin.inf.ag_se.widgets.browser.threading;

import java.util.concurrent.Callable;

public interface NoCheckedExceptionCallable<V> extends Callable<V> {
    @Override
    V call();
}
