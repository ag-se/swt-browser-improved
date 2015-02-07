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
}
