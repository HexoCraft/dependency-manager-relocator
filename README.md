Dependency Manager Relocator
================

**Java dependencies loader and relocator**

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

DMRelocator downloads java dependencies from url or repository, then relocate them using [jar-relocator](https://github.com/lucko/jar-relocator) like you would do whith the maven-shade-plugin.
This way, it prevent from creating "uber" jar file.

## Usage

```java
public class Example {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // Define the folder where the relocated jar will be placed
        File libsDir = new File("libs");

        // Define a cache folder where jar dependencies will be downloaded
        File cacheDir = new File(libsDir, "cache");

        // Get DMRelocator instance
        try {
            DMRelocator.Relocator(Example.class.getClassLoader())
                    // The folder which will contains the relocated dependencies
                    .libDir(libsDir)
                    // The folder which will contains downloaded dependencies
                    .cacheDir(cacheDir)
                    // (default to false)
                    .useFlatDir(true)
                    // (default to false)
                    .forceDownload(true)
                    // (default to false)
                    .forceRelocate(true)
                    // (default to false)
                    .ignoreHash(false)
                    // logger
                    .logger(Example::log)
                    // Add artifacts
                    .addArtifact(new Artifact("com.google.code.gson", "gson", "2.8.6"))
                    .addArtifact(new Artifact("org.apache.commons", "commons-lang3", "3.11"))
                    .addArtifact(new Artifact("commons-io", "commons-io", "2.8.0"))
                    // Add relocations
                    .addRelocation(new Relocation("com.google.gson", "libs.gson"))
                    .addRelocation(new Relocation("org.apache.commons.lang3", "libs.commons-lang3"))
                    .addRelocation(new Relocation("org.apache.commons.io", "libs.commons-io"))
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
The main goal of **Dependency Manager Relocator** is to prevent from creating  "uber" jar file, as such, it come pack in one file that you can download from the [release page](https://github.com/hexocraft-lib/dependency-manager-relocator/releases).
You can also use the script below to download the file and to update the package name :

```bash
wget https://github.com/hexocraft-lib/dependency-manager-relocator/releases/download/1.0/DMRelocator.java
sed -i -e "s|com.github.hexocraft.lib|com.packahe.my|" DMRelocator.java
```