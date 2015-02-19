package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.browser.swt.SwtBrowser;
import de.fu_berlin.inf.ag_se.browser.utils.ClasspathFileUtils;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class CssInjectionTest {

    @BeforeClass
    public static void beforeClass() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Display display = Display.getDefault();

                // Set up the event loop.
                while (!display.isDisposed()) {
                    if (!display.readAndDispatch()) {
                        // If no more entries in event queue
                        display.sleep();
                    }
                }
            }
        }).start();
    }

    private final AtomicReference<Shell> shellAtomicReference = new AtomicReference<Shell>();
    private final AtomicReference<SwtBrowser> browserAtomicReference = new AtomicReference<SwtBrowser>();

    @Before
    public void before() {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                final Shell shell = new Shell(Display.getCurrent());
                shellAtomicReference.set(shell);
                shell.setLayout(new FillLayout());
                browserAtomicReference.set(SwtBrowser.createSWTBrowser(shell, SWT.NONE));
                shell.open();
            }
        });
    }

    @After
    public void after() {
        Display.getDefault().syncExec(
                new Runnable() {
                    @Override
                    public void run() {
                        shellAtomicReference.get().close();
                    }
                }
        );
    }

    @Test
    public void testInjectJavascriptFile() throws InterruptedException {
        final CountDownLatch finishedCatch = new CountDownLatch(1);
        final AtomicReference<Integer> returnValue = new AtomicReference<Integer>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                SwtBrowser browser = browserAtomicReference.get();
                browser.open(ClasspathFileUtils.getFileUri("/empty_html5.html"), 5000);
                browser.injectJavascript(ClasspathFileUtils.getFile("/returning42.js"));
                browser.run("return newFunction();", IConverter.CONVERTER_INTEGER, new CallbackFunction<Integer, Void>() {
                    @Override
                    public Void apply(Integer input, Exception e) {
                        returnValue.set(input);
                        finishedCatch.countDown();
                        return null;
                    }
                });
            }
        });
        finishedCatch.await();
        Assert.assertEquals((Integer) 42, returnValue.get());
    }

    @Test
    public void testInjectJavascriptURI() throws InterruptedException {
        final CountDownLatch finishedCatch = new CountDownLatch(1);
        final AtomicReference<Integer> returnValue = new AtomicReference<Integer>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                SwtBrowser browser = browserAtomicReference.get();
                browser.open(ClasspathFileUtils.getFileUri("/empty_html5.html"), 5000);
                browser.injectJavascript(ClasspathFileUtils.getFileUri("/returning42.js"));
                browser.run("return newFunction();", IConverter.CONVERTER_INTEGER, new CallbackFunction<Integer, Void>() {
                    @Override
                    public Void apply(Integer input, Exception e) {
                        returnValue.set(input);
                        finishedCatch.countDown();
                        return null;
                    }
                });
            }
        });
        finishedCatch.await();
        Assert.assertEquals((Integer) 42, returnValue.get());
    }

    @Test
    public void testInjectCssFile() throws InterruptedException, IOException {
        final CountDownLatch finishedCatch = new CountDownLatch(1);

        final AtomicReference<String> returnValue = new AtomicReference<String>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                SwtBrowser browser = browserAtomicReference.get();
                browser.open(ClasspathFileUtils.getFileUri("/empty_html5.html"), 5000);
                browser.injectCssURI(ClasspathFileUtils.getFileUri("/redBackground.css"));
                browser.run("return getComputedStyle(document.body, null).backgroundColor;", IConverter.CONVERTER_STRING,
                        new CallbackFunction<String, Void>() {
                            @Override
                            public Void apply(String input, Exception e) {
                                returnValue.set(input);
                                finishedCatch.countDown();
                                return null;
                            }
                        });
            }
        });
        finishedCatch.await();
        Assert.assertEquals("rgb(255, 0, 0)", returnValue.get());
    }

    @Test
    public void testInjectCss() throws InterruptedException, IOException {
        final CountDownLatch finishedCatch = new CountDownLatch(1);
        final AtomicReference<String> returnValue = new AtomicReference<String>();
        final String css = "body { background-color: red; }";
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                SwtBrowser browser = browserAtomicReference.get();
                browser.open(ClasspathFileUtils.getFileUri("/empty_html5.html"), 5000);

                browser.injectCss(css);
                browser.getHtml(new CallbackFunction<String, Void>() {
                    @Override
                    public Void apply(String input, Exception e) {
                        returnValue.set(input);
                        finishedCatch.countDown();
                        return null;
                    }
                });
            }
        });
        finishedCatch.await();
        Assert.assertTrue(returnValue.get().contains(css));
    }

    @Test
    public void testRunContentAsScriptTag() throws InterruptedException {
        final CountDownLatch finishedCatch = new CountDownLatch(1);
        final AtomicReference<String> returnValue = new AtomicReference<String>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                SwtBrowser browser = browserAtomicReference.get();
                browser.open(ClasspathFileUtils.getFileUri("/empty_html5.html"), 5000);

            }
        });
        Thread.sleep(2000);
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                SwtBrowser browser = browserAtomicReference.get();
                try {
                    browser.runContentAsScriptTag(ClasspathFileUtils.getFile("/empty.js"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                browser.run("return getComputedStyle(document.body, null).backgroundColor;", IConverter.CONVERTER_STRING,
                        new CallbackFunction<String, Void>() {
                            @Override
                            public Void apply(String input, Exception e) {
                                returnValue.set(input);
                                finishedCatch.countDown();
                                return null;
                            }
                        });
            }
        });
        finishedCatch.await();
        Assert.assertEquals("rgb(255, 0, 0)", returnValue.get());
    }

    @Test
    public void testRunContent() throws InterruptedException {
        final CountDownLatch finishedCatch = new CountDownLatch(1);
        final AtomicReference<String> returnValue = new AtomicReference<String>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                SwtBrowser browser = browserAtomicReference.get();
                browser.open(ClasspathFileUtils.getFileUri("/empty_html5.html"), 5000);

                browser.run(ClasspathFileUtils.getFile("/empty.js"));
                browser.run("return getComputedStyle(document.body, null).backgroundColor;", IConverter.CONVERTER_STRING,
                        new CallbackFunction<String, Void>() {
                            @Override
                            public Void apply(String input, Exception e) {
                                returnValue.set(input);
                                finishedCatch.countDown();
                                return null;
                            }
                        });
            }
        });
        finishedCatch.await();
        Assert.assertEquals("rgb(255, 0, 0)", returnValue.get());
    }
}
