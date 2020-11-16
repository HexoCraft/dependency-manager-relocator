import com.github.hexocraft.lib.DMRelocator;
import com.github.hexocraft.lib.DMRelocator.Artifact;
import com.github.hexocraft.lib.DMRelocator.Relocation;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
                    .addArtifact(new Artifact("org.apache.commons", "commons-lang3", "3.11").name("apache.commons.lang3-3.11"))
                    .addArtifact(new Artifact("commons-io", "commons-io", "2.8.0").name("apache.commons.io-2.8.0"))
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
