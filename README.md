Dependency Manager Relocator
================

**Java dependencies loader and relocator**

[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hexocraft/DependencyManagerRelocator?label=stable&color=%23f6cf17)][Maven Central]
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.github.hexocraft/DependencyManagerRelocator?label=dev&server=https%3A%2F%2Foss.sonatype.org)


DMRelocator downloads java dependencies from url or repository, then relocate them using [jar-relocator](https://github.com/lucko/jar-relocator) like you would do whith the maven-shade-plugin.
This way, it prevents from creating "uber" jar file.

**Dependency Manager Relocator** can be used to relocate any libraries from any repositories. It can be a release version of your favorite library or a snapshot version. In case of snapshot version, Dependency Manager Relocator will always check that you use the latest updated snapshot version.

## Usage

```java
public class Example {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Define the folder where the relocated jar will be placed
        Path libsDir = Paths.get("libs");

        // Define a cache folder where jar dependencies will be downloaded
        Path cacheDir = Paths.get(libsDir.toString(), "cache");

        // Get DMRelocator instance
        try {
            DMRelocator.Relocator(Example.class.getClassLoader())
                    // The folder which will contains the relocated dependencies
                    .libDir(libsDir)
                    // The folder which will contains downloaded dependencies
                    .cacheDir(cacheDir)
                    // (default to false)
                    .ignoreHash(false)
                    // logger
                    .logger(Example::log)
                    // Add repositories
                    .addMavenCentral()
                    .addRepository(new Repository(new URL("https://jcenter.bintray.com/")).name("jcenter"))
                    .addRepository(new Repository(new URL("https://repo.aikar.co/content/groups/aikar")).name("aikar"))
                    // Add artifacts
                    .addArtifact(new Artifact("com.google.code.gson", "gson", "2.8.6"))
                    .addArtifact(new Artifact("org.apache.commons", "commons-lang3", "3.11"))
                    .addArtifact(new Artifact("commons-io", "commons-io", "2.8.0"))
                    .addArtifact(new Artifact("co.aikar", "acf-core", "0.5.0-SNAPSHOT"))
                    // Add relocations
                    .addRelocation(new Relocation("com.google.gson", "libs.gson"))
                    .addRelocation(new Relocation("org.apache.commons.lang3", "libs.commons-lang3"))
                    .addRelocation(new Relocation("org.apache.commons.io", "libs.commons-io"))
                    .addRelocation(new Relocation("co.aikar", "libs.aikar"))
                    // relocate artifacts
                    .relocate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        log("Loaded and/or downloaded all dependencies in " + (System.currentTimeMillis() - start) + " ms!");
    }

    private static void log(String message) {
        System.out.println("[DMRelocator]: " + message);
    }
}
```

## How to setup
The main goal of **Dependency Manager Relocator** is to prevent from creating  "uber" jar file, as such, it come pack in one file that can be downloaded from the [release page](https://github.com/hexocraft-lib/dependency-manager-relocator/releases).
You can also use the script below to download the file and to update the package name :

```bash
wget https://github.com/hexocraft-lib/dependency-manager-relocator/releases/download/1.1/DMRelocator.java
sed -i -e "s|com.github.hexocraft|com.package.my|" DMRelocator.java
```

or download the current dev version from the rpository :

```bash
wget https://raw.githubusercontent.com/HexoCraft/dependency-manager-relocator/master/src/main/java/com/github/hexocraft/DMRelocator.java
sed -i -e "s|com.github.hexocraft|com.package.my|" DMRelocator.java
```

[Maven Central]: https://search.maven.org/search?q=g:com.github.hexocraft%20AND%20a:DependencyManagerRelocator*