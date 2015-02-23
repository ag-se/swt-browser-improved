package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.browser.html.IAnchor;
import de.fu_berlin.inf.ag_se.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.browser.swt.SWTBrowser;
import de.fu_berlin.inf.ag_se.browser.swt.SWTEventCatchBrowser;
import de.fu_berlin.inf.ag_se.demoSuits.AbstractDemo;
import org.apache.commons.io.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

public class MultipleBrowsersDemo extends AbstractDemo {

    private SWTEventCatchBrowser[] browsers;
    private String alertString = "Hello World!";
    private static String timeoutString = "10000";
    private static String[] URLS = new String[]{"http://www.google.de",
            "http://www.bkahlert.com", "http://google.com"};

    @Override
    public void createControls(Composite composite) {
        Button alert = new Button(composite, SWT.PUSH);
        alert.setText("alert");
        alert.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        log("alerting");
                        for (SWTBrowser browser : browsers) {
                            try {
                                browser.run("alert(\"" + alertString + "\");").get();
                            } catch (Exception e) {
                                log(e.toString());
                            }
                        }
                        log("alerted");
                    }
                }).start();
            }
        });

        Button fileAlert = new Button(composite, SWT.PUSH);
        fileAlert.setText("alert using external file");
        fileAlert.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        log("alerting using external file");
                        try {
                            File jsFile = File.createTempFile(MultipleBrowsersDemo.class.getSimpleName(), ".js");
                            FileUtils.write(jsFile, "alert(\"" + alertString + "\");");
                            for (SWTBrowser browser : browsers) {
                                browser.run(jsFile);
                            }
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
        timeout.setText(MultipleBrowsersDemo.timeoutString);
        timeout.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                timeoutString = ((Text) e.getSource()).getText();
            }
        });
    }

    public void createDemo(Composite parent) {
        browsers = new SWTEventCatchBrowser[URLS.length];
        for (int i = 0; i < browsers.length; i++) {
            final int num = i;
            browsers[i] = SWTEventCatchBrowser.createSWTBrowser(parent, SWT.BORDER);
            browsers[i].addAnchorListener(new IAnchorListener() {
                @Override
                public void anchorHovered(IAnchor anchor, boolean entered) {
                    log("(" + num + ") hovered " + (entered ? "over" : "out") + " " + anchor);
                }
            });
            try {
                final Future<Boolean> success = this.browsers[i]
                        .open(new URI(URLS[i]), Integer.parseInt(timeoutString));
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
                            log("An error occured while loading: "
                                    + e.getMessage());
                        }
                    }
                });
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            log(this.browsers[i].getUrl());
        }

        parent.layout();
    }

    public static void main(String[] args) throws InterruptedException {
        startDemo(new MultipleBrowsersDemo());
    }
}
