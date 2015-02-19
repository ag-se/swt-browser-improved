package de.fu_berlin.inf.ag_se.browser.threading;

import java.util.concurrent.Callable;

public interface NoCheckedExceptionCallable<V> extends Callable<V> {
    @Override
    V call();
}
