package de.fu_berlin.inf.ag_se.widgets.browser;

public abstract class IBrowserFunction {

    private final String name;

    public IBrowserFunction(String name) {
        this.name = name;
    }

    public abstract Object function(Object[] arguments);

    public void dispose() {
        // TODO
    }

    public String getName() {
        return name;
    }
}
