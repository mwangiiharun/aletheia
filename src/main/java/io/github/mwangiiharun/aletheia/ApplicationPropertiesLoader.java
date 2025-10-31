package io.github.mwangiiharun.aletheia;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * Loads configuration from application*.properties files.
 *
 * Primary sources (in order of precedence):
 *  1. System property: -Daletheia.properties.dir=/path
 *  2. Working directory (System.getProperty("user.dir"))
 *  3. Classpath (application*.properties)
 *  4. [Tests only] java.io.tmpdir (for JUnit temp files)
 */
public final class ApplicationPropertiesLoader {

    private ApplicationPropertiesLoader() {}

    public static Properties load() {
        Properties props = new Properties();

        // 1️⃣ Optional explicit directory
        String explicitDir = System.getProperty("aletheia.properties.dir");
        if (explicitDir != null && !explicitDir.isBlank()) {
            loadFromDirectory(new File(explicitDir), props);
        }

        // 2️⃣ Working directory
        File userDir = new File(System.getProperty("user.dir", "."));
        loadFromDirectory(userDir, props);

        // 3️⃣ Classpath resources (always relevant for Spring / Quarkus)
        loadFromClasspath(props);

        // 4️⃣ Temp directory (ONLY if running under test)
        if (isRunningUnderTest()) {
            File tmp = new File(System.getProperty("java.io.tmpdir"));
            loadFromDirectory(tmp, props);
        }

        return props;
    }

    private static void loadFromDirectory(File dir, Properties props) {
        if (dir == null || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) ->
                name.startsWith("application") && name.endsWith(".properties"));
        if (files == null) return;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties p = new Properties();
                p.load(in);
                props.putAll(p); // later keys override earlier ones
            } catch (IOException ignored) {}
        }
    }

    private static void loadFromClasspath(Properties props) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            for (String name : new String[]{
                    "application.properties",
                    "application-dev.properties",
                    "application-prod.properties"
            }) {
                Enumeration<URL> resources = cl.getResources(name);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    try (InputStream in = url.openStream()) {
                        Properties p = new Properties();
                        p.load(in);
                        props.putAll(p);
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static boolean isRunningUnderTest() {
        // Maven Surefire and JUnit set this property internally
        String testProp = System.getProperty("java.test");
        return "true".equalsIgnoreCase(testProp)
                || Thread.currentThread().getStackTrace().toString().contains("org.junit");
    }
}