package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.browser.functions.Function;
import de.fu_berlin.inf.ag_se.browser.html.IAnchor;
import de.fu_berlin.inf.ag_se.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.browser.swt.SWTBootstrapBrowser;
import de.fu_berlin.inf.ag_se.browser.utils.StringUtils;
import de.fu_berlin.inf.ag_se.demoSuits.AbstractDemo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.util.concurrent.Future;

public class BootstrapBrowserDemo extends AbstractDemo {

    private SWTBootstrapBrowser browser;
    private String html = "<p>Hello <a href=\"#\">World</a>!</p>";

    @Override
    public void createControls(Composite composite) {
        Button setBodyHtml = new Button(composite, SWT.PUSH);
        setBodyHtml.setText("setBodyHtml");
        setBodyHtml.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        log("setting body html to "
                                + BootstrapBrowserDemo.this.html);
                        try {
                            BootstrapBrowserDemo.this.browser
                                    .setBodyHtml(BootstrapBrowserDemo.this.html)
                                    .get();
                            log("body html set");
                        } catch (Exception e) {
                            log(e.getMessage());
                        }
                    }
                });
            }
        });

        Text html = new Text(composite, SWT.BORDER);
        html.setText(this.html + "");
        html.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                BootstrapBrowserDemo.this.html = ((Text) e.getSource())
                        .getText();
            }
        });

        Button scrollButton = new Button(composite, SWT.PUSH);
        scrollButton.setText("scroll down");
        scrollButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        log("scrolling down " + BootstrapBrowserDemo.this.html);
                        try {
                            BootstrapBrowserDemo.this.browser
                                    .scrollTo(0, 9999).get();
                            log("scrolled down");
                        } catch (Exception e) {
                            log(e.getMessage());
                        }
                    }
                });
            }
        });
    }

    public void createDemo(Composite parent) {
        this.browser = SWTBootstrapBrowser.createSWTBrowser(parent, SWT.BORDER);
        browser.executeBeforeScript(new Function<String>() {
            @Override
            public void run(String input) {
                log("SENT: " + StringUtils.shorten(input));
            }
        });
        browser.executeAfterScript(new Function<Object>() {
            @Override
            public void run(Object input) {
                log("RETN: " + input);
            }
        });
        final Future<Boolean> loaded = this.browser.openBlank();
        this.browser.addAnchorListener(new IAnchorListener() {
            @Override
            public void anchorHovered(IAnchor anchor, boolean entered) {
                if (entered) {
                    log("Anchor hovered over: " + anchor);
                } else {
                    log("Anchor hovered out: " + anchor);
                }
            }
        });
//        this.browser.addFocusListener(new IFocusListener() {
//            @Override
//            public void focusGained(IElement element) {
//                log("Focus gainedr: " + element);
//            }
//
//            @Override
//            public void focusLost(IElement element) {
//                log("Focus lost: " + element);
//            }
//        });
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (loaded.get()) {
                        log("loaded successfully");
                    } else {
                        log("loading failed");
                    }
                } catch (Exception e) {
                    log("loading error: " + e);
                }
            }
        });
        this.browser
                .setBodyHtml("<div class=\"container\">"
                        + "<form class=\"form-horizontal\" role=\"form\">"
                        + "<div class=\"form-group\">"
                        + "<label for=\"inputEmail1\" class=\"col-lg-2 control-label\">Email</label>"
                        + "<div class=\"col-lg-10\">"
                        + "<p class=\"form-control-static\"><a href=\"mailto:email@example.com\">email@example.com</a></p>"
                        + "</div>" + "</div>" + "</form>" + "</div>");
    }

    public static void main(String[] args) throws InterruptedException {
        startDemo(new BootstrapBrowserDemo());
    }
}
