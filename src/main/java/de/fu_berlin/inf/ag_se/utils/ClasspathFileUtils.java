package de.fu_berlin.inf.ag_se.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class ClasspathFileUtils {

    private static final Logger LOGGER = Logger.getLogger(ClasspathFileUtils.class);

    /**
     * Gets a file object for a file path which is given relative to the location of a loaded class.
     *
     * @param clazz             the class file used as reference for the relative path
     * @param clazzRelativePath the relative path of the file
     * @return a file object
     */
    public static File getFile(Class<?> clazz, String clazzRelativePath) {
        return new File(getFileUri(clazz, clazzRelativePath));
    }

    public static URI getFileUri(Class<?> clazz, String clazzRelativePath) {
        return getFileUrl(clazz, clazzRelativePath, "");
    }

    public static File getFile(String relativePath) {
        URL resource = ClasspathFileUtils.class.getResource(relativePath);
        if (resource.toString().contains("!/")) {
            InputStream in = ClasspathFileUtils.class.getResourceAsStream(relativePath);
            File tempFile;
            try {
                tempFile = File.createTempFile(FilenameUtils.getName(relativePath), FilenameUtils.getExtension(relativePath));
                FileWriter output = new FileWriter(tempFile);
                IOUtils.copy(in, output);
                in.close();
                output.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return tempFile;

        }
        return new File(resource.getFile());
    }

    public static URI getFileUri(String relativePath) {
        return getFile(relativePath).toURI();
    }

    private static URI getFileUrl(Class<?> clazz, String clazzRelativePath, String suffix) {
        try {
            URL classContainer = getURLForClass(clazz);
            String parent = FilenameUtils.getFullPath(classContainer.getFile());
            String path = FilenameUtils.concat(parent, clazzRelativePath + suffix);
            return new File(path).toURI();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the container url for this class. This varies based on whether or not the class files are in a zip/jar or not, so this method
     * standardizes that. The method may return null, if the class is a dynamically generated class (perhaps with asm, or a proxy class)
     *
     * @param c The class to find the container for
     * @return
     */
    private static URL getURLForClass(Class<?> c) {
        if (c == null) {
            throw new NullPointerException(
                    "The Class passed to this method may not be null");
        }
        try {
            while (c.isMemberClass() || c.isAnonymousClass()) {
                c = c.getEnclosingClass(); // Get the actual enclosing file
            }
            if (c.getProtectionDomain().getCodeSource() == null) {
                // This is a proxy or other dynamically generated class, and has
                // no physical container,
                // so just return null.
                return null;
            }
            String packageRoot;
            try {
                // This is the full path to THIS file, but we need to get the
                // package root.
                String thisClass = c.getResource(c.getSimpleName() + ".class")
                                    .toString();
                packageRoot = org.apache.commons.lang.StringUtils.replace(
                        thisClass,
                        Pattern.quote(c.getName().replaceAll("\\.", "/")
                                + ".class"), "");
                if (packageRoot.endsWith("!/")) {
                    packageRoot = org.apache.commons.lang.StringUtils.replace(packageRoot, "!/", "");
                }
            } catch (Exception e) {
                // Hmm, ok, try this then
                packageRoot = c.getProtectionDomain().getCodeSource()
                               .getLocation().toString();
            }
            packageRoot = URLDecoder.decode(packageRoot, "UTF-8");
            return new URL(packageRoot);
        } catch (Exception e) {
            throw new RuntimeException("While interrogating " + c.getName()
                    + ", an unexpected exception was thrown.", e);
        }
    }
}
