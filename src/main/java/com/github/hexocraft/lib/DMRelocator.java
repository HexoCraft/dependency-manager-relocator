package com.github.hexocraft.lib;

/**
 * Copyright 2020 hexosse <hexosse@gmail.com>
 * <p>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;

/**
 *
 */
public class DMRelocator {

    private static final String VERSION = "1.0";

    // The class loader
    private final ClassLoader classLoader;
    // The folder which will contains the relocated dependencies
    private Path libDir = Paths.get(".", "libs", "relocated").toAbsolutePath();
    // The folder which will contains downloaded dependencies
    private Path cacheDir = Paths.get(".", "libs", "cache").toAbsolutePath();
    // Maven repositories used to resolve artifacts
    private final List<Repository> repositories = new LinkedList<>();
    // Artifacts to download and relocate
    private final List<Artifact> artifacts = new LinkedList<>();
    // Relocation's to apply
    private final List<Relocation> relocations = new LinkedList<>();
    // Default ASM artifact version to use
    private Artifact asmArtifact = new Artifact("org.ow2.asm", "asm", "9.0");
    // Default ASM Commons artifact version to use
    private Artifact asmCommonsArtifact = new Artifact("org.ow2.asm", "asm-commons", "9.0");
    // Default jar relocator version
    private Artifact jarRelocatorArtifact = new Artifact("me.lucko", "jar-relocator", "1.4");

    // Hierarchy tree will not be preserved while downloading and relocating
    protected static boolean useFlatDir = false;
    // Existing file will removed and downloaded
    protected static boolean forceDownload = false;
    // Existing file will removed and relocated
    protected static boolean forceRelocate = false;
    // Ignore artifact hash result
    protected static boolean ignoreHash = false;

    // Logger
    protected static Consumer<String> logger = System.out::println;


    /**
     * Instanciate DMRelocator from ClassLoader
     *
     * @param classLoader class loader
     * @return Instance on DMRelocator
     */
    public static DMRelocator Relocator(ClassLoader classLoader) {
        return new DMRelocator(classLoader).init();
    }

    /**
     * Instanciate DMRelocator from Class
     *
     * @param clazz class loader class
     * @return Instance on DMRelocator
     */
    public static DMRelocator Relocator(Class<?> clazz) {
        return new DMRelocator(clazz).init();
    }

    /**
     * Instanciate DMRelocator from ClassLoader
     *
     * @param classLoader class loader
     */
    private DMRelocator(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Instanciate DMRelocator from Class
     *
     * @param clazz class loader class
     */
    private DMRelocator(Class<?> clazz) {
        this.classLoader = clazz.getClassLoader();
    }

    /**
     * Initialize default values
     */
    private DMRelocator init() {
        // Add Maven Central repository
        addMavenCentral();

        return this;
    }

    /**
     * @param folder folder which will contains the relocated dependencies
     * @return instance of DMRelocator
     */
    public DMRelocator libDir(Path folder) {
        this.libDir = folder.toAbsolutePath();
        return this;
    }

    /**
     * @param cache folder which will contains downloaded dependencies
     * @return instance of DMRelocator
     */
    public DMRelocator cacheDir(Path cache) {
        this.cacheDir = cache.toAbsolutePath();
        return this;
    }

    /**
     * Hierarchy tree will not be preserved while downloading and relocating
     * (Default to false)
     */
    public DMRelocator useFlatDir(Boolean flatDir) {
        DMRelocator.useFlatDir = flatDir;
        return this;
    }

    /**
     * Existing file will removed and downloaded
     * (Default to false)
     */
    public DMRelocator forceDownload(Boolean force) {
        DMRelocator.forceDownload = force;
        return this;
    }

    /**
     * Existing file will removed and relocated
     * (Default to false)
     */
    public DMRelocator forceRelocate(Boolean force) {
        DMRelocator.forceRelocate = force;
        return this;
    }

    /**
     * Ignore artifact hash result
     * (Default to false)
     */
    public DMRelocator ignoreHash(Boolean ignore) {
        DMRelocator.ignoreHash = ignore;
        return this;
    }

    public DMRelocator logger(Consumer<String> onInfo) {
        DMRelocator.logger = onInfo;
        return this;
    }


    /**
     * Adds a repository.
     * <p>
     * Artifacts will be resolved using this repository when attempts to locate
     * the artifact through previously added repositories are all unsuccessful.
     *
     * @param repository Repository to add
     */
    public DMRelocator addRepository(Repository repository) {
        Objects.requireNonNull(repository, "repository cannot be null");
        // Remove existing repository
        repositories.removeIf(r -> (r.url() != null && r.url().equals(repository.url())) || (r.basedir() != null && r.basedir().equals(repository.basedir())));
        // Add to the repositories list
        repositories.add(repository);

        return this;
    }

    /**
     * Adds the current user's local Maven repository.
     */
    public DMRelocator addMavenLocal() {
        return addRepository(new Repository(Paths.get(System.getProperty("user.home")).resolve(".m2/repository").toAbsolutePath()).name("Maven local"));
    }

    /**
     * Adds the Maven Central repository.
     */
    public DMRelocator addMavenCentral() {
        try {
            URL url = new URL("https://repo1.maven.org/maven2/");
            return addRepository(new Repository(url).name("Maven Central"));
        } catch (MalformedURLException ignored) {
        }
        return this;
    }

    protected List<Repository> getRepositories() {
        return repositories;
    }

    /**
     * Add an artifact to the list of artifacts to download and relocate.
     * <p>
     * Artifacts will be resolved using repositories.
     *
     * @param artifact Artifact to add
     */
    public DMRelocator addArtifact(Artifact artifact) {
        Objects.requireNonNull(artifact, "artifact cannot be null");
        // Remove existing artifact
        artifacts.removeIf(a -> a.groupId().equals(artifact.groupId()) && a.artifactId().equals(artifact.artifactId()));
        // Add to the artifacts list
        artifacts.add(artifact);

        return this;
    }

    protected List<Artifact> getArtifacts() {
        return artifacts;
    }

    protected Artifact getArtifact(String groupId, String artifactId) {
        return artifacts.stream().filter(a -> a.groupId.equals(groupId) && a.artifactId.equals(artifactId)).findFirst().orElse(null);
    }

    /**
     * Adds a relocation.
     *
     * @param relocation Relocation to add
     */
    public DMRelocator addRelocation(Relocation relocation) {
        Objects.requireNonNull(relocation, "relocation cannot be null");
        // Remove existing relocation
        relocations.removeIf(r -> r.pattern().equals(relocation.pattern()));
        // Add to the artifacts list
        relocations.add(relocation);

        return this;
    }

    public List<Relocation> getRelocations() {
        return relocations;
    }

    /**
     * Download artifacts and relocate them
     */
    public DMRelocator relocate() throws IOException {

        // Download DMRelocator dependencies
        // (asm, asm-commons and jar-relocator)
        Downloader.download(asmArtifact, repositories, cacheDir);
        Downloader.download(asmCommonsArtifact, repositories, cacheDir);
        Downloader.download(jarRelocatorArtifact, repositories, cacheDir);

        // Download artifacts
        for (Artifact artifact : artifacts) {
            Downloader.download(artifact, repositories, cacheDir);
        }

        // Inject DMRelocator dependencies
        // (asm, asm-commons and jar-relocator)
        UrlClassLoader.addToClassLoader(classLoader, cacheDir.resolve(asmArtifact.toPath()).toFile());
        UrlClassLoader.addToClassLoader(classLoader, cacheDir.resolve(asmCommonsArtifact.toPath()).toFile());
        UrlClassLoader.addToClassLoader(classLoader, cacheDir.resolve(jarRelocatorArtifact.toPath()).toFile());

        // Relocate and inject dependencies
        for (Artifact artifact : artifacts) {
            Relocator.relocate(classLoader, artifact, relocations, cacheDir, libDir);
        }

        return this;
    }


    /**
     * Represent an artifact to add to the class loader
     * <p>
     * Optional parameters:
     * - sha1: if present, any downloaded file will be checked against it.
     * - path: the path where the file will be downloaded into the cache folder
     */
    public static class Artifact {

        // Artifact group id
        private final String groupId;
        // Artifact id
        private final String artifactId;
        // Artifact version
        private final String version;
        // Artifact hash value
        private String sha1;
        // Artifact url
        // If set, the artifact will be downloaded from this url
        // else, it will be downloaded from the repository list.
        private URL url;
        // Artifact name
        // If set, the relocated file will be renamed
        private String name;

        /**
         * Construct an Artifact
         *
         * @param groupId    Artifact group id
         * @param artifactId Artifact
         * @param version    Artifact version
         */
        public Artifact(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        /**
         * @param sha1 Artifact hash value
         */
        public Artifact sha1(String sha1) {
            this.sha1 = sha1;
            return this;
        }

        /**
         * @param url Artifact url
         */
        public Artifact url(URL url) {
            this.url = url;
            return this;
        }

        /**
         * @param name Artifact name
         */
        public Artifact name(String name) {
            this.name = name;
            return this;
        }

        /**
         * @return Artifact group id
         */
        public String groupId() {
            return groupId;
        }

        /**
         * @return Artifact id
         */
        public String artifactId() {
            return artifactId;
        }

        /**
         * @return Artifact version
         */
        public String version() {
            return version;
        }

        /**
         * @return Artifact hash value
         */
        public String sha1() {
            return sha1;
        }

        /**
         * @return Artifact url
         */
        public URL url() {
            return url;
        }

        /**
         * @return Artifact name
         */
        public String name() {
            if (name != null) {
                return name;
            } else {
                return artifactId + "-" + version;
            }
        }

        public File toFile() {
            if (DMRelocator.useFlatDir) {
                return Paths.get(name() + ".jar")
                        .toFile();
            } else {
                return Paths.get(groupId.replace(".", "/"))
                        .resolve(artifactId.replace(".", "/"))
                        .resolve(version)
                        .resolve(name() + ".jar")
                        .toFile();
            }
        }

        public Path toPath() {
            return toFile().toPath();
        }

        public URL toURL(URL root) throws RuntimeException {
            try {
                URI uri = root.toURI();
                String path = uri.getPath()
                        + "/" + groupId.replace(".", "/")
                        + "/" + artifactId.replace(".", "/")
                        + "/" + version
                        + "/" + artifactId + "-" + URLEncoder.encode(version, "UTF-8") + ".jar";
                return uri.resolve(path.replace("//", "/")).toURL();
            } catch (MalformedURLException | URISyntaxException | UnsupportedEncodingException e) {
                throw new RuntimeException("Cannot create artifact url for : " + this.toString(), e);
            }
        }

        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }


    /**
     * Represent a repository from where an Artifact can be downloaded
     * <p>
     * It can be a remote repository or a local repository
     */
    public static class Repository {

        // Repository url
        private URL url;
        // The base directory of the repository
        private Path basedir;
        // Repository name
        private String name;

        /**
         * Construct a remote Repository
         *
         * @param url Repository url
         */
        public Repository(URL url) {
            this.url = url;
            this.basedir = null;
        }

        /**
         * Construct a local Repository
         *
         * @param basedir Repository base directory
         */
        public Repository(Path basedir) {
            this.url = null;
            this.basedir = basedir;
        }

        /**
         * Name the Repository
         *
         * @param name Repository name
         */
        public Repository name(String name) {
            this.name = name;
            return this;
        }

        /**
         * @return Repository url
         */
        public URL url() {
            return url;
        }

        /**
         * @return Repository base directory
         */
        public Path basedir() {
            return basedir;
        }

        /**
         * @return Repository name
         */
        public String name() {
            return name;
        }

        /**
         * @return true if the repository is a remote repository, false if the repository is a locale repository
         */
        public boolean isRemote() {
            return url != null;
        }

        /**
         * @return true if the repository is a locale repository, false if the repository is a remote repository
         */
        public boolean isLocale() {
            return url == null;
        }

        public String toString() {
            return url.toString();
        }
    }


    /**
     * Relocation rule
     */
    public static class Relocation {
        private final String pattern;
        private final String relocatedPattern;

        public Relocation(String pattern, String relocatedPattern) {
            this.pattern = pattern;
            this.relocatedPattern = relocatedPattern;
        }

        public String pattern() {
            return pattern;
        }

        public String getRelocatedPattern() {
            return relocatedPattern;
        }
    }


    /**
     * Download files (jar, pom, ...) from urls
     */
    static class Downloader {
        private Downloader() {
        }

        static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.75 Safari/535.7";

        /**
         * Download file from url
         *
         * @param artifact     artifact to download
         * @param repositories List of repositories to download the artifact from
         * @param output       Where to download the artifact
         */
        static void download(Artifact artifact, List<Repository> repositories, Path output) throws IOException, RuntimeException {
            Objects.requireNonNull(artifact, "artifact cannot be null.");
            Objects.requireNonNull(output, "output cannot be null.");

            // Output file to download to
            File file = output.resolve(artifact.toPath()).toFile();

            // Make sure output directory exist
            makeDir(file.getParentFile().toPath());

            // Delete file if exist
            if (DMRelocator.forceDownload) {
                logger.accept("Deleting file: " + file.toPath());
                Files.deleteIfExists(file.toPath());
            }

            // Check if file already exist
            if (file.exists()) {
                logger.accept("Not downloading " + file.getName() + "(already exists)");
            }
            // Download artifact from url
            else if (artifact.url() != null) {
                downloadFile(artifact.url(), file);
            }
            // Download artifact from repositories
            else if (repositories != null) {
                downloadFile(artifact, repositories, file);
            }
            // Throw an error if no url or repositories are defined
            else {
                throw new IOException("Artifact cannot be downloaded");
            }

            // Check sha1 hash value
            if (!DMRelocator.ignoreHash) {
                if (artifact.sha1() != null && !artifact.sha1().isEmpty()) {
                    if (!checkHash(file, artifact.sha1())) {
                        throw new RuntimeException("Artifact hash mismatch for file : " + file.getName());
                    }
                }
            }
        }

        /**
         * Create the directory if not exist
         *
         * @param dir Directory to create
         */
        static void makeDir(Path dir) throws IOException {
            Objects.requireNonNull(dir, "dir cannot be null.");

            if (Files.exists(dir)) {
                if (!Files.isDirectory(dir)) {
                    Files.delete(dir);
                }
            } else {
                Files.createDirectories(dir);
            }
        }

        /**
         * Delete a directory with all its content
         *
         * @param dir Directory to delete
         * @throws IOException if an error occur while deleting files and folders
         */
        static void deleteDir(Path dir) throws IOException {
            Objects.requireNonNull(dir, "dir cannot be null.");
            File[] contents = dir.toFile().listFiles();
            if (contents != null) {
                for (File f : contents) {
                    if (!Files.isSymbolicLink(f.toPath())) {
                        deleteDir(f.toPath());
                    }
                }
            }
            Files.delete(dir);
        }

        /**
         * Check if the file hash
         *
         * @param file File to check
         * @param sha1 Hash value
         */
        static boolean checkHash(File file, String sha1) throws IllegalArgumentException {
            Objects.requireNonNull(file, "file cannot be null.");
            Objects.requireNonNull(sha1, "file cannot be null.");
            if (sha1.isEmpty()) {
                throw new IllegalArgumentException("file cannot be empty.");
            }
            return sha1.equalsIgnoreCase(FileSha1.sha1Code(file));
        }

        /**
         * Download file from url
         *
         * @param url    File url
         * @param output output file
         * @throws IOException If the file cannot be downloaded
         */
        static void downloadFile(URL url, File output) throws IOException {
            logger.accept("Downloading file: " + url);
            ReadableByteChannel readableByteChannel = Channels.newChannel(openStream(url));
            FileOutputStream fileOutputStream = new FileOutputStream(output);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            fileOutputStream.close();
        }

        /**
         * Download artifact from repository list
         *
         * @param artifact     artifact to download
         * @param repositories repositories to look for the artifact file
         * @param output       output file
         * @throws IOException If the file cannot be downloaded
         */
        static void downloadFile(Artifact artifact, List<Repository> repositories, File output) throws IOException {
            Objects.requireNonNull(artifact, "artifact cannot be null.");
            Objects.requireNonNull(repositories, "repositories cannot be null.");
            Objects.requireNonNull(output, "file output be null.");

            boolean exist = Files.exists(output.toPath());

            for (Repository repository : repositories) {
                try {
                    // Don't check in the repository if the file already exist
                    if (exist) continue;
                    // Create url from repository
                    URL url = artifact.toURL(repository.url());
                    // Create connection and check the response
                    HttpURLConnection conn = openConnection(url);
                    int status = conn.getResponseCode();
                    // The file is present
                    if ((status >= 200 && status < 300) || status == 304) {
                        downloadFile(url, output);
                        exist = Files.exists(output.toPath());
                    }
                } catch (IOException ignored) {
                }
            }

            if (!exist) {
                throw new IOException("Could download artifact '" + artifact.toString() + "' from any of the repositories");
            }
        }

        static HttpURLConnection openConnection(URL url) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            boolean redirect = status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER;

            if (redirect) {
                String newUrl = conn.getHeaderField("Location");
                return openConnection(new URL(newUrl));
            }

            return conn;
        }

        static InputStream openStream(URL url) throws IOException {
            HttpURLConnection connection = openConnection(url);
            return connection.getInputStream();
        }
    }


    /**
     * Helper class doing all the relocation process
     * <p>
     * It use jar-relocator (https://github.com/lucko/jar-relocator)
     */
    static class Relocator {
        private Relocator() {
        }

        private static final Class<?> CLASS_JAR_RELOCATOR;
        private static final Class<?> CLASS_RELOCATION;

        static {
            Class<?> classJarRelocator = null;
            Class<?> classRelocation = null;
            try {
                classJarRelocator = Class.forName("me.lucko.jarrelocator.JarRelocator");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not found class me.lucko.jarrelocator.JarRelocator", e);
            }
            try {
                classRelocation = Class.forName("me.lucko.jarrelocator.Relocation");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not found class me.lucko.jarrelocator.Relocation", e);
            }
            CLASS_JAR_RELOCATOR = classJarRelocator;
            CLASS_RELOCATION = classRelocation;
        }

        static void relocate(ClassLoader classLoader, Artifact artifact, Collection<Relocation> relocations, Path from, Path to) throws IOException, RuntimeException {
            // All parameters must not be null
            Objects.requireNonNull(classLoader);
            Objects.requireNonNull(artifact);
            Objects.requireNonNull(relocations);
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);

            // Jar-relocateor constructor parameters
            final File input = from.resolve(artifact.toPath()).toFile();
            final File output = to.resolve(artifact.toPath()).toFile();
            List<Object> rules = new LinkedList<>();

            // Delete file if exist
            if (DMRelocator.forceRelocate) {
                logger.accept("Deleting file: " + output.toPath());
                Files.deleteIfExists(output.toPath());
            }

            // Jar-relocateor Relocation instances
            try {
                for (Relocation relocation : relocations) {
                    Constructor<?> constructor = CLASS_RELOCATION.getConstructor(String.class, String.class);
                    Object instance = constructor.newInstance(relocation.pattern, relocation.relocatedPattern);
                    rules.add(instance);
                }
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException("Cannot instantiate Relocation class", e);
            }

            // Jar-relocateor instances
            if (!output.exists()) {
                // Make sure output directory exist
                Downloader.makeDir(output.getParentFile().toPath());

                //
                logger.accept("Relocating file: " + input.toPath() + " to: " + output.toPath());

                // Run jar locator
                try {
                    Constructor<?> constructor = CLASS_JAR_RELOCATOR.getConstructor(File.class, File.class, Collection.class);
                    Object instance = constructor.newInstance(input, output, rules);
                    Method run = CLASS_JAR_RELOCATOR.getMethod("run");
                    run.invoke(instance);

                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new RuntimeException("Cannot instantiate JarRelocator class", e);
                }

            }

            // Add to class loader
            UrlClassLoader.addToClassLoader(classLoader, output);
        }
    }


    /**
     * Helper class to add jar file to the class loader
     */
    static class UrlClassLoader {
        private UrlClassLoader() {
        }

        private final static Method METHOD_ADD_URL;

        static {
            Method methodAddUrl = null;
            try {
                methodAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                methodAddUrl.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Could not found method addURL from URLClassLoader", e);
            }
            METHOD_ADD_URL = methodAddUrl;
        }

        public static void addToClassLoader(ClassLoader classLoader, File input) throws RuntimeException {
            Objects.requireNonNull(classLoader, "classLoader cannot be null.");
            Objects.requireNonNull(input, "input cannot be null.");

            if (classLoader instanceof URLClassLoader) {
                try {
                    METHOD_ADD_URL.invoke(classLoader, new URL("jar:file:" + input.getPath() + "!/"));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException e) {
                    throw new RuntimeException("Error while adding jar file : " + input.getName() + " to the class loader", e);
                }
            } else {
                throw new RuntimeException("Unknown classloader: " + classLoader.getClass());
            }
        }
    }


    /**
     * Helper class to get sha1 hash value from file
     */
    static class FileSha1 {
        private FileSha1() {
        }

        /**
         * Generate a file's sha1 hash value.
         */
        static String sha1Code(File file) throws RuntimeException {
            try {
                if (file == null) {
                    throw new IllegalArgumentException("file cannot be null.");
                }
                FileInputStream fileInputStream = new FileInputStream(file);
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest);
                byte[] bytes = new byte[1024];
                // read all file content
                while (digestInputStream.read(bytes) > 0) ;
                byte[] resultByteArr = digest.digest();
                digestInputStream.close();
                fileInputStream.close();
                return bytesToHexString(resultByteArr);
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to generate sha1 hash value for file : " + file.getName(), e);
            }
        }

        /**
         * Convert a array of byte to hex String.
         * <p>
         * Each byte is covert a two character of hex String. That is
         * if byte of int is less than 16, then the hex String will append
         * a character of '0'.
         *
         * @param bytes array of byte
         * @return hex String represent the array of byte
         */
        static String bytesToHexString(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                int value = b & 0xFF;
                if (value < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toHexString(value).toUpperCase());
            }
            return sb.toString();
        }
    }
}
