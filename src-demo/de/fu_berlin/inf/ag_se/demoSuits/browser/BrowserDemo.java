package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.demoSuits.AbstractDemo;
import de.fu_berlin.inf.ag_se.utils.ExecUtils;
import de.fu_berlin.inf.ag_se.utils.SwtUiThreadExecutor;
import de.fu_berlin.inf.ag_se.utils.colors.ColorUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.Browser;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnker;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnkerListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IMouseListener;
import de.fu_berlin.inf.ag_se.widgets.browser.runner.JavaScriptExceptionListener;
import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.*;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BrowserDemo extends AbstractDemo {

    private Browser browser;
    private String alertString = "Hello World!";
    private static String timeoutString = "15000";

    @Override
    public void createControls(Composite composite) {
        this.createControlButton(
                "alert",
                new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                log("alerting");
                                try {
                                    browser.run("alert('" + alertString + "');").get();
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                } catch (ExecutionException e2) {
                                    e2.printStackTrace();
                                }
                                log("alerted");
                            }
                        }).start();
                    }
                });

        this.createControlButton(
                "alert using external file",
                new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                log("alerting using external file");
                                try {
                                    File jsFile = File.createTempFile(BrowserDemo.class.getSimpleName(), ".js");
                                    FileUtils.write(jsFile, "alert(\"" + alertString + "\");");
                                    browser.run(jsFile);
                                } catch (Exception e) {
                                    log(e.toString());
                                }
                                log("alerted using external file");
                            }
                        }).start();
                    }
                });

        Text text = new Text(composite, SWT.BORDER);
        text.setText(alertString);
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                alertString = ((Text) e.getSource()).getText();
            }
        });

        Text timeout = new Text(composite, SWT.BORDER);
        timeout.setText(timeoutString);
        timeout.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                timeoutString = ((Text) e.getSource()).getText();
            }
        });

        this.createControlButton(
                "change background color using CSS injection",
                new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                log("changing background");
                                try {
                                    browser.injectCss("html, body { background-color: "
                                            + ColorUtils.getRandomRGB().toDecString() + "; }");
                                } catch (Exception e) {
                                    log(e.toString());
                                }
                                log("changed background");
                            }
                        }).start();
                    }
                });

        this.createControlButton("add focus border",
                new Runnable() {
                    @Override
                    public void run() {
                        browser.addFocusBorder();
                    }
                });

        this.createControlButton("remove focus border",
                new Runnable() {
                    @Override
                    public void run() {
                        browser.removeFocusBorder();
                    }
                });

        new Label(composite, SWT.NONE).setText("Exception Handling:");

        this.createControlButton("raise runtime exception", new Runnable() {
            @Override
            public void run() {
                try {
                    browser.run("alert(x);").get();
                } catch (Exception e) {
                    log(e);
                }
            }
        });

        this.createControlButton("raise syntax exception", new Runnable() {
            @Override
            public void run() {
                try {
                    browser.run("alert('x);").get();
                } catch (Exception e) {
                    log(e);
                }
            }
        });

        this.createControlButton(
                "raise asynchronous runtime exception",
                new Runnable() {
                    @Override
                    public void run() {
                        final JavaScriptExceptionListener javaScriptExceptionListener = new JavaScriptExceptionListener() {
                            @Override
                            public void thrown(JavaScriptException javaScriptException) {
                                log(javaScriptException);
                            }
                        };
                        browser.addJavaScriptExceptionListener(javaScriptExceptionListener);
                        try {
                            browser.run("window.setTimeout(function() { alert(x); }, 50);").get();
                            ExecUtils.nonUIAsyncExec(
                                    new Callable<Void>() {
                                        @Override
                                        public Void call() throws Exception {
                                            browser.removeJavaScriptExceptionListener(
                                                    javaScriptExceptionListener);
                                            return null;
                                        }
                                    }, 100);
                        } catch (Exception e) {
                            log("IMPLEMENTATION ERROR - This exception should have be thrown asynchronously!");
                            log(e);
                        }
                    }
                });

    }

    @Override
    public void createDemo(Composite parent) {
        browser = new Browser(parent, SWT.BORDER);
        browser.setAllowLocationChange(true);
        browser.addAnkerListener(new IAnkerListener() {
            @Override
            public void ankerHovered(IAnker anker, boolean entered) {
                log("hovered " + (entered ? "over" : "out") + " " + anker);
            }
        });
        browser.addFocusListener(new IFocusListener() {
            @Override
            public void focusLost(IElement element) {
                log("focus lost " + element);
            }

            @Override
            public void focusGained(IElement element) {
                log("focus gained " + element);
            }
        });
//        this.browser.addMouseMoveListener(new MouseMoveListener() {
//            @Override
//            public void mouseMove(MouseEvent e) {
//                log("relative mouse pos " + e.x + "," + e.y);
//            }
//        });
        browser.addMouseListener(new IMouseListener() {
            @Override
            public void mouseMove(double x, double y) {
                log("absolute mouse pos " + x + "," + y);
            }

            @Override
            public void mouseDown(double x, double y, IElement element) {
                log("mouse down " + x + "," + y + " - " + element);
            }

            @Override
            public void mouseUp(double x, double y, IElement element) {
                log("mouse up " + x + "," + y + " - " + element);
            }

            @Override
            public void clicked(double x, double y, IElement element) {
                log("clicked " + x + "," + y + " - " + element);
            }
        });

        final Future<Boolean> success = browser.open("http://inf.fu-berlin.de", Integer.parseInt(timeoutString));
        ExecUtils.nonUIAsyncExec(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    if (success.get()) {
                        log("Page loaded successfully");
                    } else {
                        log("Page load timed out");
                    }
                } catch (Exception e) {
                    log(e.getMessage());
                }
                log(SwtUiThreadExecutor.syncExec(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return browser.getUrl();
                    }
                }));
                return null;
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        startDemo(new BrowserDemo());
    }

}
