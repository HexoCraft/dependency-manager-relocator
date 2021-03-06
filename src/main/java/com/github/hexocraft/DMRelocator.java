/**
 *    Copyright 2020 hexosse <hexosse@gmail.com>
 *
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.github.hexocraft;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 *
 */
@SuppressWarnings("unused")
public class DMRelocator {

    public static final String VERSION = "1.6";

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
    private final Artifact asmArtifact = new Artifact("org.ow2.asm", "asm", "9.0");
    // Default ASM Commons artifact version to use
    private final Artifact asmCommonsArtifact = new Artifact("org.ow2.asm", "asm-commons", "9.0");
    // Default jar-relocator version
    private final Artifact jarRelocatorArtifact = new Artifact("me.lucko", "jar-relocator", "1.4");

    // Ignore artifact hash result
    private boolean ignoreHash = false;

    // Logger
    private Consumer<String> logger = System.out::println;


    /**
     * Instantiate DMRelocator from ClassLoader
     *
     * @param classLoader class loader
     * @return Instance on DMRelocator
     */
    public static DMRelocator Relocator(ClassLoader classLoader) {
        return new DMRelocator(classLoader).init();
    }

    /**
     * Instantiate DMRelocator from Class
     *
     * @param clazz class loader class
     * @return Instance on DMRelocator
     */
    public static DMRelocator Relocator(Class<?> clazz) {
        return new DMRelocator(clazz).init();
    }

    /**
     * Instantiate DMRelocator from ClassLoader
     *
     * @param classLoader class loader
     */
    private DMRelocator(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Instantiate DMRelocator from Class
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
        // Add Maven Snapshots repository
        addMavenSnapshots();

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
     * Ignore artifact hash result
     * (Default to false)
     */
    public DMRelocator ignoreHash(Boolean ignore) {
        this.ignoreHash = ignore;
        return this;
    }

    public DMRelocator logger(Consumer<String> onInfo) {
        this.logger = onInfo;
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
        requireNonNull(repository, "repository cannot be null");
        // Remove existing repository
        repositories.removeIf(r -> (r.url() != null && repository.url() != null && r.url().toString().equals(repository.url().toString())) || (r.basedir() != null && r.basedir().equals(repository.basedir())));
        // Add to the repositories list
        repositories.add(repository);

        return this;
    }

    /**
     * Adds the Maven Central repository.
     */
    public DMRelocator addMavenCentral() {
        try {
            URL url = new URL("https://repo1.maven.org/maven2/");
            return addRepository(new Repository(url).name("Maven Central"));
        } catch (MalformedURLException ignored) {
            // Exception ignored
        }
        return this;
    }

    /**
     * Adds the Maven Central repository.
     */
    public DMRelocator addMavenSnapshots() {
        try {
            URL url = new URL("https://oss.sonatype.org/content/repositories/snapshots/");
            return addRepository(new Repository(url).name("Maven Snapshots"));
        } catch (MalformedURLException ignored) {
            // Exception ignored
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
        requireNonNull(artifact, "artifact cannot be null");
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
        requireNonNull(relocation, "relocation cannot be null");
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
        // Create downloader
        Downloader downloader = new Downloader(this);

        // Download DMRelocator dependencies
        // (asm, asm-commons and jar-relocator)
        downloader.download(asmArtifact, repositories, cacheDir);
        downloader.download(asmCommonsArtifact, repositories, cacheDir);
        downloader.download(jarRelocatorArtifact, repositories, cacheDir);

        // Download artifacts
        for (Artifact artifact : artifacts) {
            downloader.download(artifact, repositories, cacheDir);
        }

        // Inject DMRelocator dependencies
        // (asm, asm-commons and jar-relocator)
        UrlClassLoader.addToClassLoader(classLoader, asmArtifact.toFile(cacheDir));
        UrlClassLoader.addToClassLoader(classLoader, asmCommonsArtifact.toFile(cacheDir));
        UrlClassLoader.addToClassLoader(classLoader, jarRelocatorArtifact.toFile(cacheDir));

        // Create relocator
        Relocator relocator = new Relocator(this);

        // Relocate and inject dependencies
        for (Artifact artifact : artifacts) {
            relocator.relocate(classLoader, artifact, relocations, cacheDir, libDir);
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

        String name() {
            if (name != null) {
                return name;
            } else {
                return artifactId + "-" + version;
            }
        }

        void name(String name) {
            this.name = name;
        }

        public Path toPath(Path root) {
            return root.resolve(toPath());
        }

        Path toPath() {
            return Paths.get(groupId.replace(".", "/"))
                    .resolve(artifactId.replace(".", "/"))
                    .resolve(version)
                    .resolve(name() + ".jar");
        }

        public File toFile(Path root) {
            return toPath(root).toFile();
        }

        File toFile() {
            return toPath().toFile();
        }

        URL getBaseUrl(URL root) {
            try {
                URI uri = root.toURI();
                String path = String.join("/"
                        , uri.getPath()
                        , groupId.replace(".", "/")
                        , artifactId.replace(".", "/")
                        , URLEncoder.encode(version, "UTF-8")
                );
                return uri.resolve(path.replace("//", "/")).toURL();
            } catch (MalformedURLException | UnsupportedEncodingException | URISyntaxException e) {
                throw new RelocatorException("Cannot create base url for : " + this.toString(), e);
            }
        }

        URL getArtifactUrl(URL root) {
            try {
                URI uri = getBaseUrl(root).toURI();
                String path = uri.getPath() + "/" + URLEncoder.encode(name(), "UTF-8") + ".jar";
                return uri.resolve(path.replace("//", "/")).toURL();
            } catch (MalformedURLException | URISyntaxException | UnsupportedEncodingException e) {
                throw new RelocatorException("Cannot create artifact base url for : " + this.toString(), e);
            }
        }

        URL getMetaDataUrl(URL root) {
            try {
                URI uri = getBaseUrl(root).toURI();
                String path = uri.getPath() + "/maven-metadata.xml";
                return uri.resolve(path.replace("//", "/")).toURL();
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RelocatorException("Cannot create maven-metadata base url for : " + this.toString(), e);
            }
        }

        File getMetaDataFile(Path root) {
            return Paths.get(root.toString())
                    .resolve(groupId.replace(".", "/"))
                    .resolve(artifactId.replace(".", "/"))
                    .resolve(version)
                    .resolve("maven-metadata.xml")
                    .toFile();
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
        private final URL url;
        // The base directory of the repository
        private final Path basedir;
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
            return isRemote() ? url.toString() : "";
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

        //
        private final DMRelocator dmRelocator;
        private Proxy proxy;

        static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.75 Safari/535.7";

        public Downloader(DMRelocator dmRelocator) {
            requireNonNull(dmRelocator, "dmRelocator cannot be null");
            this.dmRelocator = dmRelocator;
        }

        /**
         * Download file from url
         *
         * @param artifact     artifact to download
         * @param repositories List of repositories to download the artifact from
         * @param output       Where to download the artifact
         */
        void download(Artifact artifact, List<Repository> repositories, Path output) throws IOException {
            requireNonNull(artifact, "artifact cannot be null.");
            requireNonNull(output, "output cannot be null.");

            // Make sure output directory exist
            makeDir(artifact.toFile(output).getParentFile().toPath());

            // The file already exist
            if (!artifact.toFile(output).exists()) {
                // Download artifact from url
                if (artifact.url() != null) {
                    downloadFile(artifact.url(), artifact.toFile(output));
                }
                // Download artifact from repositories
                else if (repositories != null) {
                    downloadFile(artifact, repositories, output);
                }
                // Throw an error if no url or repositories are defined
                else {
                    throw new RelocatorException("Artifact cannot be downloaded");
                }
            }

            // Check sha1 hash value
            if (!dmRelocator.ignoreHash
                    && artifact.sha1() != null && !artifact.sha1().isEmpty()
                    && !checkHash(artifact.toFile(output), artifact.sha1())) {
                throw new RelocatorException("Artifact hash mismatch for file : " + artifact.toFile(output).getName());
            }
        }

        /**
         * Create the directory if not exist
         *
         * @param dir Directory to create
         */
        static void makeDir(Path dir) throws IOException {
            requireNonNull(dir, "dir cannot be null.");

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
            requireNonNull(dir, "dir cannot be null.");
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
        boolean checkHash(File file, String sha1) {
            requireNonNull(file, "file cannot be null.");
            requireNonNull(sha1, "sha1 cannot be null.");
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
        void downloadFile(URL url, File output) throws IOException {
            dmRelocator.logger.accept("Downloading file: " + url);

            try (
                    ReadableByteChannel readableByteChannel = Channels.newChannel(openStream(url));
                    FileOutputStream fileOutputStream = new FileOutputStream(output)
            ) {
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                requireNonNull(fileOutputStream).close();
                requireNonNull(readableByteChannel).close();
            }
        }

        /**
         * Download artifact from repository list
         *
         * @param artifact     artifact to download
         * @param repositories repositories to look for the artifact file
         * @param output       output file
         */
        void downloadFile(Artifact artifact, List<Repository> repositories, Path output) {
            requireNonNull(artifact, "artifact cannot be null.");
            requireNonNull(repositories, "repositories cannot be null.");
            requireNonNull(output, "file output be null.");

            // Will be updated when the artifact is found
            boolean found = false;

            //
            HttpURLConnection conn;
            int status;

            // Loop throw all repositories
            for (Repository repository : repositories) {
                try {
                    // Don't check in the repository if the artifact is already found
                    if (found) continue;

                    // Firstly, try to download the file corresponding to the artifact
                    // --------------------------------------------------------------
                    URL artifactUrl = artifact.getArtifactUrl(repository.url());
                    // Create connection and check the response
                    conn = openConnection(artifactUrl);
                    status = conn.getResponseCode();
                    // The file is present in the repository
                    if ((status >= 200 && status < 300) || status == 304) {
                        downloadFile(artifactUrl, artifact.toFile(output));
                        found = Files.exists(artifact.toPath(output));
                        continue;
                    }

                    // If the file does not exist, try to download the maven-metadata.xml file
                    // -----------------------------------------------------------------------
                    // Create meta data url from repository
                    URL metaDataUrl = artifact.getMetaDataUrl(repository.url());
                    // meta data file to store
                    File metaDataFile = artifact.getMetaDataFile(output);
                    // Create connection and check the response
                    conn = openConnection(metaDataUrl);
                    status = conn.getResponseCode();
                    // The file is present in the repository
                    if ((status >= 200 && status < 300) || status == 304) {
                        // Download MetaData.xml from repository
                        downloadFile(metaDataUrl, metaDataFile);
                        // Use MetaDataHelper to get the latest jar version
                        MetaDataHelper metaDataHelper = new MetaDataHelper(metaDataFile);
                        if (!metaDataHelper.isValid()) {
                            continue;
                        }
                        // Update artifact
                        artifact.name(artifact.artifactId() + "-" + metaDataHelper.getLatest());
                        artifactUrl = artifact.getArtifactUrl(repository.url());
                        // Download artifact
                        if (!artifact.toFile(output).exists()) {
                            downloadFile(artifactUrl, artifact.toFile(output));
                        }
                        found = Files.exists(artifact.toPath(output));
                    }
                } catch (Exception ignored) {
                    // Exception ignored
                }
            }

            if (!found) {
                throw new RelocatorException("Could not download artifact '" + artifact.toString() + "' from any of the repositories");
            }
        }

        /**
         * Get the proxy used by the JVM
         */
        Proxy getProxy() {

            // Return already foud proxy
            if (proxy != null) {
                return proxy;
            }

            // Try to find the first proxy used by the JVM
            try {
                ProxySelector selector = ProxySelector.getDefault();
                List<Proxy> proxyList = selector.select(new URI("http://foo/bar"));

                if (proxyList != null && !proxyList.isEmpty()) {
                    proxy = proxyList.get(0);
                    return proxy;
                }
            } catch (Exception ignored) {
                // Exception ignored
            }
            return null;
        }

        HttpURLConnection openConnection(URL url) throws IOException {
            Proxy p = getProxy();
            final HttpURLConnection conn = (HttpURLConnection) (p != null ? url.openConnection(p) : url.openConnection());
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

        InputStream openStream(URL url) throws IOException {
            HttpURLConnection connection = openConnection(url);
            return connection.getInputStream();
        }

        static class MetaDataHelper {

            private final Metadata metaData;

            public MetaDataHelper(File metaDataFile) {
                try {
                    JAXBContext jaxbContext = JAXBContext.newInstance(Metadata.class);
                    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                    this.metaData = (Metadata) jaxbUnmarshaller.unmarshal(metaDataFile);
                }
                catch(Exception e) {
                    throw new RelocatorException(e);
                }
            }

            boolean isValid() {
                return metaData != null && !metaData.groupId.isEmpty() && !metaData.artifactId.isEmpty() && !metaData.version.isEmpty();
            }

            String getLatest() {
                for (SnapshotVersion snapshotVersion : metaData.getVersioning().getSnapshotVersions().getSnapshotVersion()) {
                    if ((snapshotVersion.classifier == null || snapshotVersion.classifier.isEmpty()) && snapshotVersion.extension.equals("jar")) {
                        return snapshotVersion.value;
                    }
                }
                return "";
            }

            @XmlRootElement
            static class Metadata {
                private String groupId;
                private String artifactId;
                private String version;
                private Versioning versioning;

                public Metadata() {
                }

                public Metadata(String groupId, String artifactId, String version, Versioning versioning) {
                    super();
                    this.groupId = groupId;
                    this.artifactId = artifactId;
                    this.version = version;
                    this.versioning = versioning;
                }

                @XmlElement
                public String getGroupId() {
                    return groupId;
                }

                public void setGroupId(String groupId) {
                    this.groupId = groupId;
                }

                @XmlElement
                public String getArtifactId() {
                    return artifactId;
                }

                public void setArtifactId(String artifactId) {
                    this.artifactId = artifactId;
                }

                @XmlElement
                public String getVersion() {
                    return version;
                }

                public void setVersion(String version) {
                    this.version = version;
                }

                @XmlElement
                public Versioning getVersioning() {
                    return versioning;
                }

                public void setVersioning(Versioning versioning) {
                    this.versioning = versioning;
                }
            }

            static class Versioning {
                private String lastUpdated;
                private SnapshotVersions snapshotVersions;

                public Versioning() {
                }

                public Versioning(String lastUpdated, SnapshotVersions snapshotVersions) {
                    super();
                    this.lastUpdated = lastUpdated;
                    this.snapshotVersions = snapshotVersions;
                }

                @XmlElement
                public String getLastUpdated() {
                    return lastUpdated;
                }

                public void setLastUpdated(String lastUpdated) {
                    this.lastUpdated = lastUpdated;
                }

                @XmlElement
                public SnapshotVersions getSnapshotVersions() {
                    return snapshotVersions;
                }

                public void setSnapshotVersions(SnapshotVersions snapshotVersions) {
                    this.snapshotVersions = snapshotVersions;
                }
            }

            static class SnapshotVersions {

                private List<SnapshotVersion> versions;

                public SnapshotVersions() {
                }

                public SnapshotVersions(List<SnapshotVersion> versions) {
                    super();
                    this.versions = versions;
                }

                @XmlElement
                public List<SnapshotVersion> getSnapshotVersion() {
                    return versions;
                }

                public void setSnapshotVersion(List<SnapshotVersion> snapshotVersions) {
                    this.versions = snapshotVersions;
                }
            }

            static class SnapshotVersion {
                private String classifier;
                private String extension;
                private String value;
                private String updated;

                public SnapshotVersion() {
                }

                public SnapshotVersion(String classifier, String extension, String value, String updated) {
                    super();
                    this.classifier = classifier;
                    this.extension = extension;
                    this.value = value;
                    this.updated = updated;
                }

                @XmlElement
                public String getClassifier() {
                    return classifier;
                }

                public void setClassifier(String classifier) {
                    this.classifier = classifier;
                }

                @XmlElement
                public String getExtension() {
                    return extension;
                }

                public void setExtension(String extension) {
                    this.extension = extension;
                }

                @XmlElement
                public String getValue() {
                    return value;
                }

                public void setValue(String value) {
                    this.value = value;
                }

                @XmlElement
                public String getUpdated() {
                    return updated;
                }

                public void setUpdated(String updated) {
                    this.updated = updated;
                }
            }
        }
    }


    /**
     * Helper class doing all the relocation process
     * <p>
     * It use jar-relocator (https://github.com/lucko/jar-relocator)
     */
    static class Relocator {

        private final DMRelocator dmRelocator;

        public Relocator(DMRelocator dmRelocator) {
            this.dmRelocator = dmRelocator;
        }

        private static final Class<?> CLASS_JAR_RELOCATOR;
        private static final Class<?> CLASS_RELOCATION;

        static {
            Class<?> classJarRelocator;
            Class<?> classRelocation;
            try {
                classJarRelocator = Class.forName("me.lucko.jarrelocator.JarRelocator");
            } catch (ClassNotFoundException e) {
                throw new RelocatorException("Could not found class me.lucko.jarrelocator.JarRelocator", e);
            }
            try {
                classRelocation = Class.forName("me.lucko.jarrelocator.Relocation");
            } catch (ClassNotFoundException e) {
                throw new RelocatorException("Could not found class me.lucko.jarrelocator.Relocation", e);
            }
            CLASS_JAR_RELOCATOR = classJarRelocator;
            CLASS_RELOCATION = classRelocation;
        }

        void relocate(ClassLoader classLoader, Artifact artifact, Collection<Relocation> relocations, Path from, Path to) {
            // All parameters must not be null
            requireNonNull(classLoader);
            requireNonNull(artifact);
            requireNonNull(relocations);
            requireNonNull(from);
            requireNonNull(to);

            // Jar-relocator constructor parameters
            final File input = from.resolve(artifact.toPath(from)).toFile();
            final File output = to.resolve(artifact.toPath(to)).toFile();
            List<Object> rules = new LinkedList<>();

            if (!output.exists()) {
                // Jar-relocator Relocation instances
                try {
                    for (Relocation relocation : relocations) {
                        Constructor<?> constructor = CLASS_RELOCATION.getConstructor(String.class, String.class);
                        Object instance = constructor.newInstance(relocation.pattern, relocation.relocatedPattern);
                        rules.add(instance);
                    }
                } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    throw new RelocatorException("Cannot instantiate Relocation class", e);
                }

                // Make sure output directory exist
                try {
                    Downloader.makeDir(output.getParentFile().toPath());
                } catch (IOException e) {
                    throw new RelocatorException("Cannot create output folder", e);
                }

                // Run jar locator
                try {
                    dmRelocator.logger.accept("Relocating file: " + input.toPath() + " to: " + output.toPath());

                    Constructor<?> constructor = CLASS_JAR_RELOCATOR.getConstructor(File.class, File.class, Collection.class);
                    Object instance = constructor.newInstance(input, output, rules);
                    Method run = CLASS_JAR_RELOCATOR.getMethod("run");
                    run.invoke(instance);

                } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    throw new RelocatorException("Cannot instantiate JarRelocator class", e);
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

        private static final Method METHOD_ADD_URL;

        static {
            Method methodAddUrl;
            try {
                methodAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                methodAddUrl.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RelocatorException("Could not found method addURL from URLClassLoader", e);
            }
            METHOD_ADD_URL = methodAddUrl;
        }

        public static void addToClassLoader(ClassLoader classLoader, File input) {
            requireNonNull(classLoader, "classLoader cannot be null.");
            requireNonNull(input, "input cannot be null.");

            if (classLoader instanceof URLClassLoader) {
                try {
                    METHOD_ADD_URL.invoke(classLoader, new URL("jar:file:" + input.getPath() + "!/"));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException e) {
                    throw new RelocatorException("Error while adding jar file : " + input.getName() + " to the class loader", e);
                }
            } else {
                throw new RelocatorException("Unknown classloader: " + classLoader.getClass());
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
        static String sha1Code(File file) {
            requireNonNull(file, "file cannot be null.");

            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");

                try (FileInputStream fileInputStream = new FileInputStream(file);
                     DigestInputStream digestInputStream = new DigestInputStream(fileInputStream, digest)
                ) {
                    byte[] bytes = new byte[1024];
                    // read all file content
                    while (digestInputStream.read(bytes) > 0) ;
                    byte[] resultByteArr = digest.digest();
                    // then return
                    return bytesToHexString(resultByteArr);
                }

            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RelocatorException("Unable to generate sha1 hash value for file : " + file.getName(), e);
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

    public static class RelocatorException extends RuntimeException {

        static final long serialVersionUID = 3641868074431532124L;

        public RelocatorException() {
        }

        public RelocatorException(String message) {
            super(message);
        }

        public RelocatorException(Throwable cause) {
            super(cause);
        }

        public RelocatorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
