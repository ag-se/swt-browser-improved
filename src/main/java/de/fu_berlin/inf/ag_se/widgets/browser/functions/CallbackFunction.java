package de.fu_berlin.inf.ag_se.widgets.browser.functions;

public interface CallbackFunction<V, T> {

    T apply(V input, Exception e);
}
