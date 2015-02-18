package de.fu_berlin.inf.ag_se.widgets.browser.extended.extensions;

import de.fu_berlin.inf.ag_se.utils.ClasspathFileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * TODO Currently the loading of external resources does not work reliably.
 * This can lead to waiting threads which are never notified.
 */
public enum BrowserExtension {

    JQUERY_EXTENSION("jQuery 1.9.0", "return typeof jQuery !== 'undefined';",
            Arrays.asList(ClasspathFileUtils.getFile("/jquery/jquery-1.9.0.js")),
            Collections.<File>emptyList(),
            Collections.<BrowserExtension>emptyList()),

    BOOTSTRAP_EXTENSION("Bootstrap 3.0.0",
            "return (typeof window.jQuery !== 'undefined') && (typeof $().modal == 'function');",
            Arrays.asList(ClasspathFileUtils.getFile("/bootstrap/js/bootstrap.min.js")),
            Arrays.asList(ClasspathFileUtils.getFile("/bootstrap/css/bootstrap.min.css")),
            Arrays.asList(JQUERY_EXTENSION)),

    EVENT_CATCH_EXTENSION("Event Catch Functionality", "return window[\"__eventsCatchInjected\"];",
            Arrays.asList(ClasspathFileUtils.getFile("/events.js"), ClasspathFileUtils.getFile("/dnd.js")),
            Arrays.asList(ClasspathFileUtils.getFile("/dnd.css")),
            Collections.<BrowserExtension>emptyList()
            );

    private final String name;
    private final String verificationScript;
    private final List<File> jsExtensions;
    private final List<File> cssExtensions;
    private final List<BrowserExtension> dependencies;

    /**
     * This constructor allows adding a multiple JS and CSS files.
     *
     * @param name
     * @param verificationScript
     * @param jsExtensions
     * @param cssExtensions
     * @param dependencies
     */
    private BrowserExtension(String name, String verificationScript,
                             List<File> jsExtensions, List<File> cssExtensions,
                             List<BrowserExtension> dependencies) {
        checkNotNull(name);
        checkNotNull(verificationScript);
        checkNotNull(jsExtensions);
        checkNotNull(cssExtensions);
        checkNotNull(dependencies);
        this.name = name;
        this.verificationScript = verificationScript;
        this.jsExtensions = Collections.unmodifiableList(jsExtensions);
        this.cssExtensions = Collections.unmodifiableList(cssExtensions);
        this.dependencies = Collections.unmodifiableList(dependencies);
    }

    public String getName() {
        return this.name;
    }

    public String getVerificationScript() {
        return this.verificationScript;
    }

    public List<File> getJsExtensions() {
        return this.jsExtensions;
    }

    public List<File> getCssExtensions() {
        return this.cssExtensions;
    }

    public List<BrowserExtension> getDependencies() {
        return this.dependencies;
    }
}
