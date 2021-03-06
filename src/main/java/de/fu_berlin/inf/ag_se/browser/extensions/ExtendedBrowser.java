package de.fu_berlin.inf.ag_se.browser.extensions;

import de.fu_berlin.inf.ag_se.browser.Browser;
import de.fu_berlin.inf.ag_se.browser.IBrowser;
import de.fu_berlin.inf.ag_se.browser.InternalBrowserWrapper;
import de.fu_berlin.inf.ag_se.browser.JavascriptString;
import de.fu_berlin.inf.ag_se.browser.utils.IConverter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * This {@link IBrowser} behaves like the {@link Browser} but allows {@link IBrowserExtension}s
 * to be automatically loaded when the requested {@link java.net.URI} was loaded.
 *
 * @author bkahlert
 */
public class ExtendedBrowser extends Browser {

    private static final Logger LOGGER = Logger
            .getLogger(ExtendedBrowser.class);

    private final Iterable<BrowserExtension> extensions;

    public ExtendedBrowser(InternalBrowserWrapper internalWrapper, Iterable<BrowserExtension> extensions) {
        super(internalWrapper);
        this.extensions = extensions;

        /*
         * TODO FIX BUG: afterCompletion is called after the DOMReady scripts.
         * PageLoad might need to access something loaded through extensions.
         */
        executeAfterCompletion(new Runnable() {
            @Override
            public void run() {
                for (BrowserExtension extension : ExtendedBrowser.this.extensions) {
                    if (!hasExtension(extension)) {
                        if (!addExtension(extension)) {
                            LOGGER.error("Error loading " + extension);
                        }
                    }
                }
            }
        });
    }

    private Boolean hasExtension(BrowserExtension extension) {
        return runImmediately(extension.getVerificationScript(), IConverter.CONVERTER_BOOLEAN);
    }

    private Boolean addExtension(BrowserExtension extension) {
        for (BrowserExtension dependency : extension.getDependencies()) {
            if (!this.addExtension(dependency)) {
                LOGGER.error("Dependency "
                        + dependency.getName()
                        + " could not be loaded.");
                return false;
            }
        }

        for (File jsExtension : extension.getJsExtensions()) {
            // by running the extension directly we execute it synchronously
            // otherwise a loader library would be necessary to satisfy the
            // loading dependencies
            try {
                if (!runImmediately(JavascriptString.embedContentsIntoScriptTag(jsExtension), IConverter.CONVERTER_BOOLEAN)) {
                    LOGGER.error("Could not load the JS extension \"" + extension.getName() + "\".");
                    return false;
                }
            } catch (RuntimeException e) {
                LOGGER.error("Could not load the JS extension \"" + extension.getName() + "\".", e);
                return false;
            } catch (IOException e) {
                LOGGER.error("Could not load the JS extension \"" + extension.getName() + "\".", e);
            }
            LOGGER.info("Loaded \"" + jsExtension + " successfully.");
        }

        for (File cssExtension : extension.getCssExtensions()) {
            try {
                runImmediately(JavascriptString.createCssFileInjectionScript(cssExtension), IConverter.CONVERTER_BOOLEAN);
            } catch (RuntimeException e) {
                LOGGER.error("Could not load the JS extension \"" + extension.getName() + "\".", e.getCause());
                return false;
            } catch (IOException e) {
                LOGGER.error("Could not load the JS extension \"" + extension.getName() + "\".", e.getCause());
                return false;
            }
            LOGGER.info("Loaded \"" + cssExtension + " successfully.");
        }

        return true;
    }
}
