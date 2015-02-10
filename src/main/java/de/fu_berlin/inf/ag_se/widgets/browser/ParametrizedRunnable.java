package de.fu_berlin.inf.ag_se.widgets.browser;

public interface ParametrizedRunnable<V>  {

    /**
     * Runs the runnable with given input parameter.
     * To be overridden by concrete classes.
     *
     * @param input the input parameter
     */
    public abstract void run(V input);
}
