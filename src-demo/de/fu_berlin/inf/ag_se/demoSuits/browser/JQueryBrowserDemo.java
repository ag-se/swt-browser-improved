package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.annotations.Demo;
import de.fu_berlin.inf.ag_se.demoSuits.AbstractDemo;
import de.fu_berlin.inf.ag_se.utils.ExecUtils;
import de.fu_berlin.inf.ag_se.utils.StringUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.JQueryBrowser;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IAnker;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IAnkerListener;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IFocusListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

@Demo
public class JQueryBrowserDemo extends AbstractDemo {

    private JQueryBrowser jQueryBrowserComposite;
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
                            if (JQueryBrowserDemo.this.jQueryBrowserComposite
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
        this.jQueryBrowserComposite = new JQueryBrowser(parent, SWT.BORDER) {
            @Override
            public void scriptAboutToBeSentToBrowser(String script) {
                log("SENT: " + StringUtils.shorten(script));
            }

            @Override
            public void scriptReturnValueReceived(Object returnValue) {
                log("RETN: " + returnValue);
            }
        };
        try {
            final Future<Boolean> loaded = this.jQueryBrowserComposite.open(
                    new URI("http://amazon.com"), 60000);
            this.jQueryBrowserComposite.addAnkerListener(new IAnkerListener() {
                @Override
                public void ankerHovered(IAnker anker, boolean entered) {
                    if (entered) {
                        log("Anker hovered over: " + anker);
                    } else {
                        log("Anker hovered out: " + anker);
                    }
                }

				@Override
				public void ankerClicked(IAnker anker) {
					log("Anker clicked: " + anker);
				}
            });
            this.jQueryBrowserComposite.addFocusListener(new IFocusListener() {
                @Override
                public void focusGained(IElement element) {
                    log("Focus gainedr: " + element);
                }

                @Override
                public void focusLost(IElement element) {
                    log("Focus lost: " + element);
                }
            });
            this.jQueryBrowserComposite.scrollTo(this.x, this.y);
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
