package de.fu_berlin.inf.ag_se.browser.javafx;

import de.fu_berlin.inf.ag_se.browser.functions.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.functions.InternalJavascriptFunction;

public class JavaFxBrowserFunction implements IBrowserFunction {

  private final InternalJavascriptFunction function;

  public JavaFxBrowserFunction(InternalJavascriptFunction function) {
    this.function = function;
  }

  @Override
  public void dispose() {

  }
}
