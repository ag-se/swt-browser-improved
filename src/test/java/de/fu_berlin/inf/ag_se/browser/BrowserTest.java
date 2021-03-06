package de.fu_berlin.inf.ag_se.browser;

import de.fu_berlin.inf.ag_se.browser.functions.Function;
import de.fu_berlin.inf.ag_se.browser.swt.SWTBrowser;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.fail;

public class BrowserTest {

    @Test
    public void testJavaScriptRunOrder() throws InterruptedException,
            ExecutionException {

        final int numRuns = 20;
        final int numThreads = 5;

        Display display = Display.getDefault();
        final Shell shell = new Shell(display);

        shell.setLayout(new FillLayout());

        final List<String> scriptSubmitOrder = Collections.synchronizedList(new ArrayList<String>());
        final List<String> scriptExecutionOrder = Collections.synchronizedList(new ArrayList<String>());
        final Map<String, String> scriptResults = Collections.synchronizedMap(new HashMap<String, String>());
        final List<String> resultFinishedOrder = Collections.synchronizedList(new ArrayList<String>());
        final SWTBrowser browser = SWTBrowser.createSWTBrowser(shell, SWT.NONE);

        browser.executeBeforeScript(new Function<String>() {
            @Override
            public void run(String script) {
                if (!script.contains("successfullyInjectedAnchorHoverCallback") && !script.contains("complete") && script
                        .contains("document.write(")) {
                    scriptExecutionOrder.add(script);
                }
            }
        });
        browser.executeAfterScript(new Function<Object>() {
            @Override
            public void run(Object returnValue) {
                if (!returnValue.toString().equals("true")
                        && !returnValue.toString().equals("false")) {
                    resultFinishedOrder.add(returnValue.toString());
                }
            }
        });

        shell.setSize(800, 600);
        shell.open();

        browser.setAllowLocationChange(true);
        browser.open("http://bkahlert.com", 5000);

        final Thread[] threads = new Thread[numThreads];
        for (int thread = 0; thread < threads.length; thread++) {
            threads[thread] = new Thread(new Runnable() {
                @Override
                public void run() {
                    List<Future<String>> finish = new ArrayList<Future<String>>();
                    for (int run = 0; run < numRuns; run++) {
                        final String random = new BigInteger(130,
                                new SecureRandom()).toString(32);
                        String script = "document.write(\"" + random
                                + "<br>\");return \"" + random + "\";";

                        if (run > numRuns / 2) {
                            // run the other half when browser is loaded
                            while (!browser.isLoadingCompleted()) {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        synchronized (BrowserTest.class) {
//                            scriptResults.put(random, script);
                            scriptSubmitOrder.add(script);
                            finish.add(browser.run(script, IConverter.CONVERTER_STRING));
                        }
                    }
                    for (Future<String> f : finish) {
                        try {
                            f.get();
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail();
                        }
                    }
                }

            });
            threads[thread].start();
        }

        final FutureTask<Void> assertionJoin = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() {
                for (Thread thread : threads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                for (int i = 0, m = numRuns * numThreads; i < m; i++) {
                    String submitted = scriptSubmitOrder.get(i);
                    String executed = scriptExecutionOrder.get(i);

                    Assert.assertEquals(submitted, executed);

                    String returnValue = resultFinishedOrder.get(i);
//                    String finished = scriptResults.get(returnValue);
//
//                    Assert.assertEquals(executed, finished);
                }

                System.err.println("exit");
                return null;
            }
        });

        new Thread(assertionJoin).start();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    assertionJoin.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        shell.close();
                    }
                });
            }
        });
        thread.start();

        // Set up the event loop.
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                // If no more entries in event queue
                display.sleep();
            }
        }
    }
}
