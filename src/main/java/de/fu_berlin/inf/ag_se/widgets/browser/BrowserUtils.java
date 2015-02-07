package de.fu_berlin.inf.ag_se.widgets.browser;

import de.fu_berlin.inf.ag_se.utils.ImageUtils;
import de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException;
import de.fu_berlin.inf.ag_se.widgets.browser.extended.html.IElement;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("restriction")
public class BrowserUtils {

    public static final String ERROR_RETURN_MARKER = BrowserUtils.class
            .getCanonicalName() + ".error_return";

    private static final String TRACK_ATTR_NAME = "data-nebula-track";
    private static final String TRACK_ATTR_VALUE = "true";

    private static Pattern TAG_NAME_PATTERN = Pattern.compile(
            "^[^<]*<(\\w+)[^<>]*\\/?>.*", Pattern.DOTALL);

    /**
     * Returns the first tag name that could be found in the given HTML code.
     *
     * @param html
     * @return
     */
    public static String getFirstTagName(String html) {
        if (html == null) {
            return null;
        }
        Matcher matcher = TAG_NAME_PATTERN.matcher(html);
        if (!matcher.matches()) {
            return null;
        }
        if (matcher.groupCount() != 1) {
            return null;
        }
        return matcher.group(1);
    }

    public static boolean fuzzyEquals(String uri1, String uri2) {
        if (uri1 == null && uri2 == null) {
            return true;
        } else if (uri1 == null || uri2 == null) {
            return false;
        } else if (uri1.endsWith(uri2) || uri2.endsWith(uri1)) {
            return true;
        }
        return false;
    }

    public static IElement extractElement(String html) {
        if (html == null) {
            return null;
        }
        String tagName = getFirstTagName(html);
        if (tagName == null) {
            return null;
        }

        // add attribute to make the element easily locatable
        String trackAttr = " " + TRACK_ATTR_NAME + "=\"" + TRACK_ATTR_VALUE
                + "\"";
        if (html.endsWith("/>")) {
            html = html.substring(0, html.length() - 2) + trackAttr + "/>";
        } else {
            html = html.replaceFirst(">", trackAttr + ">");
        }

        // add missing tags, otherwise JSoup will simply delete those
        // "mis-placed" tags
        if (tagName.equals("td")) {
            html = "<table><tbody><tr>" + html + "</tr></tbody></table>";
        } else if (tagName.equals("tr")) {
            html = "<table><tbody>" + html + "</tbody></table>";
        } else if (tagName.equals("tbody")) {
            html = "<table>" + html + "</table>";
        }

        Document document = Jsoup.parse(html);
        Element element = document.getElementsByAttributeValue(TRACK_ATTR_NAME,
                TRACK_ATTR_VALUE).first();
        element.removeAttr(TRACK_ATTR_NAME);
        if (element.attr("href") == null) {
            element.attr("href", element.attr("data-cke-saved-href"));
        }
        return new de.fu_berlin.inf.ag_se.widgets.browser.extended.html.Element(
                element);
    }

    private BrowserUtils() {
    }

    /**
     * Creates a random name for a JavaScript function. This is especially handy for callback functions injected by {@link
     * org.eclipse.swt.browser.BrowserFunction}.
     *
     * @return
     */
    public static String createRandomFunctionName() {
        return "_"
                + de.fu_berlin.inf.ag_se.utils.StringUtils.createRandomString(32);
    }

    /**
     * Returns a Base64-encoded {@link String} data URI that can be used for the <code>src</code> attribute of an HTML <code>img</code>.
     *
     * @param file must point to a readable image file
     * @return
     */
    public static String createDataUri(File file) throws IOException {
        return createDataUri(ImageIO.read(file));
    }

    /**
     * Returns a Base64-encoded {@link String} data URI that can be used for the <code>src</code> attribute of an HTML <code>img</code>.
     *
     * @param image
     * @return
     */
    public static String createDataUri(Image image) {
        return createDataUri(image.getImageData());
    }

    /**
     * Returns a Base64-encoded {@link String} data URI that can be used for the <code>src</code> attribute of an HTML <code>img</code>.
     *
     * @param data
     * @return
     */
    public static String createDataUri(ImageData data) {
        return createDataUri(ImageUtils.convertToAWT(data));
    }

    /**
     * Returns a Base64-encoded {@link String} data URI that can be used for the <code>src</code> attribute of an HTML <code>img</code>.
     *
     * @param image
     * @return
     */
    public static String createDataUri(BufferedImage image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String encodedImage = DatatypeConverter.printBase64Binary(baos.toByteArray());
        return "data:image/png;base64," + encodedImage;
    }

    /**
     * Returns a script that - if executes - forward all thrown browser script exceptions to the given callback. <p> The arguments passed by
     * the browser to the given callback function can be processed using {@link #parseJavaScriptException(Object[])}.
     *
     * @return
     */
    public static String getExceptionForwardingScript(String callbackName) {
        return "window.onerror = function(detail, filename, lineNumber, columnNumber) { if ( typeof window['"
                + callbackName
                + "'] !== 'function') return; return window['"
                + callbackName
                + "'](filename ? filename : 'unknown file', lineNumber ? lineNumber : null, columnNumber ? columnNumber : null, detail ? detail : 'unknown detail'); }";
    }

    /**
     * This method creates an {@link de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException} out of the arguments passed by
     * the {@link org.eclipse.swt.browser.Browser} to the callback specified using {@link #getExceptionForwardingScript(String)}.
     *
     * @param arguments
     * @return
     */
    public static JavaScriptException parseJavaScriptException(
            Object[] arguments) {
        String filename = (String) arguments[0];
        Long lineNumber = Math.round((Double) arguments[1]);
        Long columnNumber = Math.round((Double) arguments[2]);
        String detail = (String) arguments[3];

        return new JavaScriptException(null, filename, lineNumber,
                columnNumber, detail);
    }

    /**
     * Modifies the given script in the way that an eventually thrown error will be catched and returned. <p> Passing the return value of
     * {@link org.eclipse.swt.browser.Browser#evaluate(String)} to {@link #assertException(String, Object)} will raise an appropriate {@link
     * de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException} if such one was thrown within the {@link
     * org.eclipse.swt.browser.Browser}.
     *
     * @param script
     * @return
     */
    public static String getExecutionReturningScript(String script) {
        return "try { return new Function('"
                + StringEscapeUtils.escapeJavaScript(script)
                + "')(); } catch(e) { return [ '"
                + ERROR_RETURN_MARKER
                + "', e.sourceURL, e.line, e.column-6/* reduce column by the exception catching code */, e.name + \": \" + e.message ]; }";
    }

    /**
     * Checks a the return value of {@link org.eclipse.swt.browser.Browser#evaluate(String)} for a caught exception. If an exception was
     * found, an appropriate JavaScriptException is thrown. <p> This feature only works if the evaluated script was returned by {@link
     * #getExecutionReturningScript}.
     *
     * @param script
     * @param returnValue
     * @throws de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException
     */
    public static void assertException(final String script, Object returnValue)
            throws JavaScriptException {
        // exception handling
        if (returnValue instanceof Object[]) {
            Object[] rt = (Object[]) returnValue;
            if (rt.length == 5 && rt[0] != null
                    && rt[0].equals(ERROR_RETURN_MARKER)) {
                throw new JavaScriptException(script, (String) rt[1],
                        rt[2] != null ? Math.round((Double) rt[2]) : null,
                        rt[3] != null ? Math.round((Double) rt[3]) : null,
                        (String) rt[4]);
            }
        }
    }
}
