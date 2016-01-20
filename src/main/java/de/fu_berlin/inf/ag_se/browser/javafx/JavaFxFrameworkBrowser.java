package de.fu_berlin.inf.ag_se.browser.javafx;

import de.fu_berlin.inf.ag_se.browser.functions.IBrowserFunction;
import de.fu_berlin.inf.ag_se.browser.IWrappedBrowser;
import de.fu_berlin.inf.ag_se.browser.functions.InternalJavascriptFunction;
import de.fu_berlin.inf.ag_se.browser.threading.UIThreadExecutor;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Group;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaFxFrameworkBrowser implements IWrappedBrowser {

    private final WebView browser;
    private final WebEngine engine;
    private final UIThreadExecutor uiThreadExecutor = new JavaFxUiThreadExecutor();

    private AtomicInteger index = new AtomicInteger(1);

    private HashMap<String, InternalJavascriptFunction> functionHashMap = new HashMap<>();

    public JavaFxFrameworkBrowser(Group root) {
        browser = new WebView();
        engine = browser.getEngine();
        root.getChildren().add(browser);
        JSObject win =
                (JSObject) engine.executeScript("window");
        Object javaApp = new JavaCaller();
        win.setMember("app", javaApp);
    }

    @Override
    public void addLoadedListener(final Runnable runnable) {
        engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State state2) {
                if (state2 == Worker.State.SUCCEEDED) {
                    runnable.run();
                }
            }
        });
    }

    @Override
    public String getUrl() {
        return engine.getLocation();
    }

    @Override
    public Object evaluate(String script) {
        return engine.executeScript(script);
    }

    @Override
    public void setVisible(boolean visible) {

    }

    @Override
    public void setSize(int width, int height) {

    }

    @Override
    public void setText(String html) {

    }

    @Override
    public boolean setFocus() {
        return false;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public IBrowserFunction createBrowserFunction(InternalJavascriptFunction function) {
        String index = getNextIndex();
        functionHashMap.put(index, function);

        StringBuffer functionBuffer = new StringBuffer(function.getName());
        functionBuffer.append(" = function ");
        functionBuffer.append(function.getName());
        functionBuffer.append("() {var result = app.callJava('");
        functionBuffer.append(index);
        functionBuffer.append("', Array.prototype.slice.call(arguments));");
        functionBuffer.append("};");
        engine.executeScript(functionBuffer.toString());
        return new JavaFxBrowserFunction(function);
    }

    private String getNextIndex() {
        return "" + index.getAndIncrement();
    }

    @Override
    public void setUrl(String url) {
        uiThreadExecutor.syncExec(new Runnable() {
            @Override
            public void run() {
                engine.load(url);

            }
        });
    }

    @Override
    public UIThreadExecutor getUIThreadExecutor() {
        return uiThreadExecutor;
    }

    public class JavaCaller {
        public void callJava(int index, Object[] args) {
            InternalJavascriptFunction function = functionHashMap.get(Integer.toString(index));
            if (function != null) {
                function.function(args);
            }
        }
    }
}
