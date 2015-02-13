package de.fu_berlin.inf.ag_se.widgets.browser;

public abstract class ParametrizedRunnable<V> implements Runnable {

    private final V input;
    private final RuntimeException exception;

    public ParametrizedRunnable(V input, RuntimeException exception) {
        this.input = input;
        this.exception = exception;
    }

    /**
     * Runs the runnable with given input parameter.
     * To be overridden by concrete classes.
     *
     * @param input the input parameter
     */
    public abstract void run(V input, RuntimeException exception);

    @Override
    public void run() {
        run(input, exception);
    }
}
