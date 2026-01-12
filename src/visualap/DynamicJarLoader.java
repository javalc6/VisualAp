package visualap;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class DynamicJarLoader {

    /**
     * Creates a new URLClassLoader capable of loading classes from the specified files (JARs/directories).
     * The parent ClassLoader is set to the current thread's context class loader.
     *
     * @param filePaths A list of String paths pointing to the JARs or directories to load.
     * @return A new URLClassLoader instance.
     * @throws IOException if any of the file paths cannot be converted to a URL.
     */
    public static ClassLoader createLoaderForJars(List<String> filePaths) throws IOException {
        List<URL> urls = new ArrayList<>();
        
        for (String path : filePaths) {
            File f = new File(path);
            if (!f.exists()) {
                System.err.println("Warning: Path does not exist: " + path);
                continue;
            }
            // Converts the File object to a URL, handling path separators correctly.
            urls.add(f.toURI().toURL());
        }

        URL[] urlArray = urls.toArray(new URL[0]);

        // Create the new URLClassLoader. We pass the current thread's context
        // ClassLoader as the parent, ensuring it can still load core Java classes.
        return new URLClassLoader(urlArray, Thread.currentThread().getContextClassLoader());
    }
}