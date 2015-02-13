package de.fu_berlin.inf.ag_se.widgets.browser.functions;

public interface CallbackFunction<V> {

    void run(V input, RuntimeException e);
}
