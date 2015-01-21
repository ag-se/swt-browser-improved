package de.fu_berlin.inf.ag_se.widgets;

import org.eclipse.swt.events.DisposeListener;

public interface IWidget {
	/**
	 * @see org.eclipse.swt.widgets.Composite#layout()
	 */
	public void layout();

	/**
	 * @see org.eclipse.swt.widgets.Widget#getData()
	 */
	public Object getData();

	/**
	 * @see org.eclipse.swt.widgets.Widget#getData(String)
	 */
	public Object getData(String key);

	/**
	 * @see org.eclipse.swt.widgets.Widget#setData(Object)
	 */
	public void setData(Object data);

	/**
	 * @see org.eclipse.swt.widgets.Widget#setData(String, Object)
	 */
	public void setData(String key, Object value);

	/**
	 * @see org.eclipse.swt.widgets.Widget#addDisposeListener(org.eclipse.swt.events.DisposeListener)
	 */
	public void addDisposeListener(DisposeListener disposeListener);

	/**
	 * @see org.eclipse.swt.widgets.Widget#removeDisposeListener(org.eclipse.swt.events.DisposeListener)
	 */
	public void removeDisposeListener(DisposeListener disposeListener);

	/**
	 * @see org.eclipse.swt.widgets.Widget#isDisposed()
	 */
	public boolean isDisposed();

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose();
}
