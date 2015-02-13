package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.demoSuits.AbstractDemo;
import de.fu_berlin.inf.ag_se.utils.StringUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.JQueryBrowser;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnchor;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;
import de.fu_berlin.inf.ag_se.widgets.browser.functions.Function;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import de.fu_berlin.inf.ag_se.widgets.browser.threading.ExecUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

public class JQueryBrowserDemo extends AbstractDemo {

    private JQueryBrowser browser;
    private Integer x = 50;
    private Integer y = 200;

    @Override
    public void createControls(Composite composite) {
        Button scrollTo = new Button(composite, SWT.PUSH);
        scrollTo.setText("scrollTo");
        scrollTo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ExecUtils.nonUISyncExec(new Runnable() {
                    @Override
                    public void run() {
                        log("scrolling to " + JQueryBrowserDemo.this.x + ", "
                                + JQueryBrowserDemo.this.y);
                        try {
                            if (JQueryBrowserDemo.this.browser
                                    .scrollTo(JQueryBrowserDemo.this.x,
                                            JQueryBrowserDemo.this.y).get()) {
                                log("Scrolled");
                            } else {
                                log("Already at desired position");
                            }
                            log("scrolled to " + JQueryBrowserDemo.this.x
                                    + ", " + JQueryBrowserDemo.this.y);
                        } catch (Exception e) {
                            log(e.getMessage());
                        }
                    }
                });
            }
        });

        Text xText = new Text(composite, SWT.BORDER);
        xText.setText(this.x + "");
        xText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                JQueryBrowserDemo.this.x = Integer.valueOf(((Text) e
                        .getSource()).getText());
            }
        });

        Text yText = new Text(composite, SWT.BORDER);
        yText.setText(this.y + "");
        yText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                JQueryBrowserDemo.this.y = Integer.valueOf(((Text) e
                        .getSource()).getText());
            }
        });
    }

    public void createDemo(Composite parent) {
        this.browser = new JQueryBrowser(parent, SWT.BORDER);
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

        try {
            final Future<Boolean> loaded = this.browser.open(
                    new URI("http://amazon.com"), 60000);
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
            this.browser.addFocusListener(new IFocusListener() {
                @Override
                public void focusGained(IElement element) {
                    log("Focus gainedr: " + element);
                }

                @Override
                public void focusLost(IElement element) {
                    log("Focus lost: " + element);
                }
            });
            this.browser.scrollTo(this.x, this.y);
            ExecUtils.nonUIAsyncExec(new Runnable() {
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
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        startDemo(new JQueryBrowserDemo());
    }
}
