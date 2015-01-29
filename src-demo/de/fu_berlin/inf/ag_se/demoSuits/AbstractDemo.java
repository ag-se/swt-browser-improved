package de.fu_berlin.inf.ag_se.demoSuits;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public abstract class AbstractDemo {

    private static final Logger LOGGER = Logger.getLogger(AbstractDemo.class);

    /**
     * Contains all controls and the demoAreaContent.
     */
    private Composite composite;
    protected Composite controls;
    protected Composite content;

    public final void createPartControls(Composite composite) {
        this.composite = composite;
        this.composite.setLayout(new GridLayout());

        controls = new Composite(this.composite, SWT.NONE);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = false;
        controls.setLayoutData(gridData);

        RowLayout layout = new RowLayout();
        layout.fill = true;
        controls.setLayout(layout);

        content = new Composite(this.composite, SWT.NONE);
        GridData layoutData = new GridData();
        layoutData.grabExcessVerticalSpace = true;
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = SWT.FILL;
        layoutData.verticalAlignment = SWT.FILL;
        content.setLayoutData(layoutData);
        content.setLayout(new FillLayout());

        createControls(controls);
        createDemo(content);
        this.composite.layout();
    }

    /**
     * Creates the controls for this demo.
     * @param composite
     */
    public abstract void createControls(Composite composite);

    /**
     * Creates a button in the control section. {@link #createControls(org.eclipse.swt.widgets.Composite)} must be overwritten to make the
     * effects of this function visible.
     *
     * @param text
     * @param runnable
     * @return
     */
    public Control createControlButton(String text, final Runnable runnable) {
        Button button = new Button(this.controls, SWT.PUSH);
        button.setText(text);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                runnable.run();
            }
        });
        return button;
    }

    public static void log(final String message) {
        LOGGER.info(message);
    }

    public static void log(Throwable e) {
        log(e.getMessage());
    }

    public void createDemo(Composite parent) {
    }

    protected static void startDemo(AbstractDemo browserDemo) {
        final Display display = Display.getDefault();
        final Shell shell = new Shell(display);
        shell.setMaximized(true);
        browserDemo.createPartControls(shell);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
