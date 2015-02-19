package de.fu_berlin.inf.ag_se.browser.html;


/**
 * Abstractions of an anchor tag like &lt;a
 * href=&quot;http://bkahlert.com&quot;&gt;bkahlert.com&lt;/a&gt;.
 * 
 * @author bkahlert
 * 
 */
public interface IAnchor extends IElement {

	/**
	 * Returns the {@link IElement}'s href attribute.
	 * 
	 * @return
	 */
	public String getHref();

}
