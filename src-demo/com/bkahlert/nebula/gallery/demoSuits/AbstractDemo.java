package com.bkahlert.nebula.gallery.demoSuits;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public abstract class AbstractDemo {

    private static final Logger LOGGER = Logger.getLogger(AbstractDemo.class);

	/**
	 * Contains all controls and the demoAreaContent.
	 */
	protected Composite controls;
	protected Composite content;

	/**
	 * Creates the controls for this demo.
	 * <p>
	 * By default they are hidden.
	 *
	 * @param composite
	 */
	public void createControls(Composite composite) {
		this.controls.setVisible(false);
		((GridData) this.controls.getLayoutData()).heightHint = 0;
	}

	/**
	 * Creates a button in the control section.
	 * {@link #createControls(org.eclipse.swt.widgets.Composite)} must be overwritten to make the
	 * effects of this function visible.
	 *
	 * @param caption
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

    /**
     * Adds a message to the console and shows it if hidden.
     *
     * @param message
     */
    public static void log(final String message) {
        LOGGER.info(message);
    }

    public static void log(Throwable e) {
    		log(e.getMessage());
    	}
}
