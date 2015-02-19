package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.browser.JavaFxBrowser;
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class JavaFxBrowserDemo extends Application {

    private static final Logger LOGGER = Logger.getLogger(JavaFxBrowserDemo.class);

    private JavaFxBrowser browser;
    private String alertString = "Hello World!";
    private static String timeoutString = "15000";

    Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            new Thread(command).start();
        }
    };

//    @Override
//    public void createControls(Stage stage) {
//        this.createControlButton(
//                "alert",
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        log("alerting");
//                        try {
//                            browser.syncRun("alert('" + alertString + "');");
//                        } catch (RuntimeException e) {
//                            log(e);
//                        }
//                        log("alerted");
//                    }
//                });
//
//        this.createControlButton(
//                "alert using external file",
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        executor.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                log("alerting using external file");
//                                try {
//                                    File jsFile = File.createTempFile(JavaFxBrowserDemo.class.getSimpleName(), ".js");
//                                    FileUtils.write(jsFile, "alert(\"" + alertString + "\");");
//                                    browser.run(jsFile).get();
//                                } catch (Exception e) {
//                                    log(e);
//                                }
//                                log("alerted using external file");
//                            }
//                        });
//                    }
//                });
//
//        Text text = new Text(composite, SWT.BORDER);
//        text.setText(alertString);
//        text.addModifyListener(new ModifyListener() {
//            @Override
//            public void modifyText(ModifyEvent e) {
//                alertString = ((Text) e.getSource()).getText();
//            }
//        });
//
//        Text timeout = new Text(composite, SWT.BORDER);
//        timeout.setText(timeoutString);
//        timeout.addModifyListener(new ModifyListener() {
//            @Override
//            public void modifyText(ModifyEvent e) {
//                timeoutString = ((Text) e.getSource()).getText();
//            }
//        });
//
//        this.createControlButton(
//                "change background color using CSS injection",
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        String newColor = ColorUtils.getRandomRGB().toDecString();
//                        log("changing background to " + newColor);
//                        final Future<Boolean> voidFuture = browser
//                                .injectCss("html, body { background-color: " + newColor + "; }");
//                        executor.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    voidFuture.get();
//                                    log("changed background");
//                                } catch (InterruptedException e) {
//                                    Thread.currentThread().interrupt();
//                                } catch (ExecutionException e) {
//                                    log("could not change background: ");
//                                    log(e);
//                                }
//                            }
//                        });
//                    }
//                });
//
//        new Label(composite, SWT.NONE).setText("Exception Handling:");
//
//        this.createControlButton("raise runtime exception", new Runnable() {
//            @Override
//            public void run() {
//                browser.run("alert(x);", new CallbackFunction<Object, Void>() {
//                    @Override
//                    public Void apply(Object input, Exception e) {
//                        log(e);
//                        return null;
//                    }
//                });
//            }
//        });
//
//        this.createControlButton("raise syntax exception", new Runnable() {
//            @Override
//            public void run() {
//                final Future<Object> future = browser.run("alert('x);");
//                executor.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            future.get();
//                        } catch (Exception e) {
//                            log(e);
//                        }
//                    }
//                });
//            }
//        });
//
//        this.createControlButton(
//                "raise asynchronous runtime exception",
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        final JavaScriptExceptionListener javaScriptExceptionListener = new JavaScriptExceptionListener() {
//                            @Override
//                            public void thrown(JavaScriptException javaScriptException) {
//                                log(javaScriptException);
//                            }
//                        };
//                        browser.addJavaScriptExceptionListener(javaScriptExceptionListener);
//                        try {
//                            browser.run("window.setTimeout(function() { alert(x); }, 50);").get();
//                            executor.execute(
//                                    new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            try {
//                                                Thread.sleep(1000);
//                                            } catch (InterruptedException e) {
//                                                Thread.currentThread().interrupt();
//                                            }
//                                            browser.removeJavaScriptExceptionListener(javaScriptExceptionListener);
//                                        }
//                                    });
//                        } catch (Exception e) {
//                            log("IMPLEMENTATION ERROR - This exception should have be thrown asynchronously!");
//                            log(e);
//                        }
//                    }
//                });
//
//    }

    public void createDemo(Stage stage) {
        browser = new JavaFxBrowser(stage);;
        browser.setAllowLocationChange(true);
//        browser.addAnchorListener(new IAnchorListener() {
//            @Override
//            public void anchorHovered(IAnchor anchor, boolean entered) {
//                log("hovered " + (entered ? "over" : "out") + " " + anchor);
//            }
//        });
//        browser.addFocusListener(new IFocusListener() {
//            @Override
//            public void focusLost(IElement element) {
//                log("focus lost " + element);
//            }
//
//            @Override
//            public void focusGained(IElement element) {
//                log("focus gained " + element);
//            }
//        });
//        this.browser.addMouseMoveListener(new MouseMoveListener() {
//            @Override
//            public void mouseMove(MouseEvent e) {
//                log("relative mouse pos " + e.x + "," + e.y);
//            }
//        });
//        browser.addMouseListener(new IMouseListener() {
//            @Override
//            public void mouseMove(double x, double y) {
//                log("absolute mouse pos " + x + "," + y);
//            }
//
//            @Override
//            public void mouseDown(double x, double y, IElement element) {
//                log("mouse down " + x + "," + y + " - " + element);
//            }
//
//            @Override
//            public void mouseUp(double x, double y, IElement element) {
//                log("mouse up " + x + "," + y + " - " + element);
//            }
//
//            @Override
//            public void clicked(double x, double y, IElement element) {
//                log("clicked " + x + "," + y + " - " + element);
//            }
//        });

//        final Future<Boolean> success = browser.openBlank();
        final Future<Boolean> success = browser.open("https://google.de", Integer.parseInt(timeoutString));
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (success.get()) {
                        log("Page loaded successfully");
                    } else {
                        log("Page load timed out");
                    }
                } catch (Exception e) {
                    log(e.getMessage());
                }
                log(browser.getUrl());
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // create the scene
        stage.setTitle("Web View");
        stage.show();
        URL resource = JavaFxBrowserDemo.class.getResource("/test.html");
        createDemo(stage);
    }

    public static void log(final String message) {
            LOGGER.info(message);
        }

        public static void log(Throwable e) {
            log(e.getMessage());
        }
}
