package de.fu_berlin.inf.ag_se.widgets.browser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class JavascriptString {


    public static String embedContentsIntoScriptTag(File scriptFile) throws IOException {
        String scriptContent = FileUtils.readFileToString(scriptFile);
        return "var script=document.createElement(\"script\"); script.type=\"text/javascript\"; script.text=\""
                + StringEscapeUtils.escapeJavaScript(scriptContent)
                + "\"; document.getElementsByTagName(\"head\")[0].appendChild(script);";
    }


    public static String createJSForInjection(String callbackFunctionName, URI script, boolean removeAfterExecution) {
        String js = "var h = document.getElementsByTagName(\"head\")[0]; var s = document.createElement(\"script\");s.type = \"text/javascript\";s.src = \""
                + script.toString()
                + "\"; s.onload=function(e){";
        if (removeAfterExecution) {
            js += "h.removeChild(s);";
        }
        js += callbackFunctionName + "();";
        js += "};h.appendChild(s);";
        return js;
    }

    static String createWaitForConditionJavascript(String condition,
                                                   String callbackFunctionName) {
        return "(function() { function test() { if(" + condition + ") { "
                + callbackFunctionName
                + "(); } else { window.setTimeout(test, 50); } } "
                + "test(); })()";
    }

    static String createJsFileInjectionScript(File file) {
        return
                "var script=document.createElement(\"script\"); script.type=\"text/javascript\"; script.src=\""
                        + file.toURI()
                        + "\"; document.getElementsByTagName(\"head\")[0].appendChild(script);";
    }

    static String createCssFileInjectionScript(URI uri) {
        return "if(document.createStyleSheet){document.createStyleSheet(\""
                + uri.toString()
                + "\")}else{var link=document.createElement(\"link\"); link.rel=\"stylesheet\"; link.type=\"text/css\"; link.href=\""
                + uri.toString()
                + "\"; document.getElementsByTagName(\"head\")[0].appendChild(link); }";
    }

    static String createCssInjectionScript(String css) {
        return
                "(function(){var style=document.createElement(\"style\");style.appendChild(document.createTextNode(\""
                        + css
                        + "\"));(document.getElementsByTagName(\"head\")[0]||document.documentElement).appendChild(style)})()";
    }

    static String createCssToDisableTextSelection() {
        return "* { -webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none; }";
    }

    protected static String createJavascriptForInsertingHTML(String html) {
        String escapedHtml = escape(html);
        return "if(['input','textarea'].indexOf(document.activeElement.tagName.toLowerCase()) != -1) { document.activeElement.value = '"
        + escapedHtml
        + "';} else { var t,n;if(window.getSelection){t=window.getSelection();if(t.getRangeAt&&t.rangeCount){n=t.getRangeAt(0);n.deleteContents();var r=document.createElement(\"div\");r.innerHTML='"
        + escapedHtml
        + "';var i=document.createDocumentFragment(),s,o;while(s=r.firstChild){o=i.appendChild(s)}n.insertNode(i);if(o){n=n.cloneRange();n.setStartAfter(o);n.collapse(true);t.removeAllRanges();t.addRange(n)}}}else if(document.selection&&document.selection.type!=\"Control\"){document.selection.createRange().pasteHTML('"
        + escapedHtml
                + "')}}";
    }

    public static String escape(String html) {
        return html.replace("\n", "<br>").replace("&#xD;", "").replace("\r", "")
                   .replace("\"", "\\\"").replace("'", "\\'");
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
     * Modifies the given script in the way that an eventually thrown error will be caught and returned.
     * Passing the return value of
     * {@link org.eclipse.swt.browser.Browser#evaluate(String)} to {@link #assertException(String, Object)} will raise an appropriate
     * {@link de.fu_berlin.inf.ag_se.widgets.browser.exception.JavaScriptException} if such one was thrown within the browser.
     *
     * @param script
     * @return
     */
    public static String getExceptionReturningScript(String script) {
        return "try { return new Function('"
                + StringEscapeUtils.escapeJavaScript(script)
                + "')(); } catch(e) { return [ '"
                + BrowserUtils.ERROR_RETURN_MARKER
                + "', e.sourceURL, e.line, e.column-6/* reduce column by the exception catching code */, e.name + \": \" + e.message ]; }";
    }
}
