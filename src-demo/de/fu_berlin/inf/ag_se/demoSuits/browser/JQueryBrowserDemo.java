package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.browser.functions.CallbackFunction;
import de.fu_berlin.inf.ag_se.browser.functions.Function;
import de.fu_berlin.inf.ag_se.browser.html.IAnchor;
import de.fu_berlin.inf.ag_se.browser.listener.IAnchorListener;
import de.fu_berlin.inf.ag_se.browser.swt.SWTJQueryBrowser;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

public class JQueryBrowserDemo extends AbstractDemo {

    private SWTJQueryBrowser browser;
    private Integer x = 50;
    private Integer y = 200;

    @Override
    public void createControls(Composite composite) {
        Button scrollTo = new Button(composite, SWT.PUSH);
        scrollTo.setText("scrollTo");
        scrollTo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        log("scrolling to " + x + ", " + y);
                        try {
                            Future<Boolean> future = browser.scrollTo(x, y);
                            if (future.get()) {
                                log("Scrolled");
                            } else {
                                log("Already at desired position");
                            }
                            log("scrolled to " + x + ", " + y);
                        } catch (Exception e) {
                            log(e.getMessage());
                        }
                    }
                });
            }
        });

        Text xText = new Text(composite, SWT.BORDER);
        xText.setText(x + "");
        xText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                x = Integer.valueOf(((Text) e.getSource()).getText());
            }
        });

        Text yText = new Text(composite, SWT.BORDER);
        yText.setText(this.y + "");
        yText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                y = Integer.valueOf(((Text) e.getSource()).getText());
            }
        });
    }

    public void createDemo(Composite parent) {
        browser = SWTJQueryBrowser.createSWTBrowser(parent, SWT.BORDER);
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
            browser.open(new URI("http://amazon.com"), 6000, new CallbackFunction<Boolean, Boolean>() {
                @Override
                public Boolean apply(Boolean input, Exception e) {
                    if (input) {
                        log("loaded successfully");
                    } else {
                        log("loading failed");
                    }
                    if (e != null) {
                        log("loading error:");
                        log(e);
                    }
                    return input;
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        browser.addAnchorListener(new IAnchorListener() {
            @Override
            public void anchorHovered(IAnchor anchor, boolean entered) {
                if (entered) {
                    log("Anchor hovered over: " + anchor);
                } else {
                    log("Anchor hovered out: " + anchor);
                }
            }
        });
//        browser.addFocusListener(new IFocusListener() {
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
        browser.scrollTo(this.x, this.y);
    }

    public static void main(String[] args) throws InterruptedException {
        startDemo(new JQueryBrowserDemo());
    }
}
