package de.fu_berlin.inf.ag_se.demoSuits.browser;

import de.fu_berlin.inf.ag_se.annotations.Demo;
import de.fu_berlin.inf.ag_se.demoSuits.AbstractDemo;
import de.fu_berlin.inf.ag_se.utils.ExecUtils;
import de.fu_berlin.inf.ag_se.utils.StringUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.Browser;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;
import de.fu_berlin.inf.ag_se.widgets.browser.listener.IDNDListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.concurrent.Future;

@Demo
public class DragnDropBrowserDemo extends AbstractDemo {

    private Browser browser;
    private Label dropArea;


    public void createControls(Composite composite) {
        this.controls.setVisible(false);
        ((GridData) this.controls.getLayoutData()).heightHint = 0;
    }

    public void createDemo(Composite parent) {
        parent.setLayout(new FillLayout());
        this.browser = new Browser(parent, SWT.BORDER) {
            @Override
            public void scriptAboutToBeSentToBrowser(String script) {
                log("SENT: " + StringUtils.shorten(script));
            }

            @Override
            public void scriptReturnValueReceived(Object returnValue) {
                log("RETN: " + returnValue);
            }
        };

        final Future<Boolean> loaded = this.browser.openBlank();
        this.browser.injectCss("[draggable] { border: 1px solid #ff0000 }");
        this.browser.injectCss("[droppable] { border: 1px solid #ff00ff }");

        this.browser
                .setBodyHtml("<p>Hello World!</p>"
                        + "<div draggable=\"true\" data-dnd-mime=\"text/html\" data-dnd-data=\"<b>Hello</b> <em>World!</em>\">Drag me! <p style=\"border: 1px solid #999; margin: 1em;\">Child Element</p></div>"
                        + "<br>"
                        + "<div droppable=\"true\">Drop here! <p style=\"border: 1px solid #999; margin: 1em;\">Child Element</p></div>");
        this.browser.addDNDListener(new IDNDListener() {
            @Override
            public void dragStart(long offsetX, long offsetY, IElement element,
                                  String mimeType, String data) {
                log("Dragging started " + offsetX + ", " + offsetY + ", "
                        + element + ", (" + mimeType + "): " + data);
            }

            @Override
            public void drop(long offsetX, long offsetY, IElement element,
                             String mimeType, String data) {
                log("Dropped at " + offsetX + ", " + offsetY + ", " + element
                        + " (" + mimeType + "): " + data);
            }
        });

        this.dropArea = new Label(parent, SWT.NONE);
        this.dropArea.setText("Drop here");
        DropTarget dropTarget = new DropTarget(this.dropArea, DND.DROP_LINK);
        dropTarget.setTransfer(new Transfer[]{HTMLTransfer.getInstance()});
        dropTarget.addDropListener(new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetEvent event) {
                event.detail = DND.DROP_LINK;
            }

            @Override
            public void dragLeave(DropTargetEvent event) {
                event.detail = DND.DROP_NONE;
            }

            @Override
            public void drop(DropTargetEvent event) {
                String content = (String) HTMLTransfer.getInstance()
                                                      .nativeToJava(event.currentDataType);
                DragnDropBrowserDemo.this.dropArea.setText(content);
            }
        });

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
    }

    public static void main(String[] args) throws InterruptedException {
        startDemo(new DragnDropBrowserDemo());
    }
}
