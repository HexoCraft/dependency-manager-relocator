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

import com.github.hexocraft.DMRelocator;
import com.github.hexocraft.DMRelocator.Artifact;
import com.github.hexocraft.DMRelocator.Relocation;
import com.github.hexocraft.DMRelocator.Repository;

import java.io.IOException;
import java.net.URL;
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
