package com.github.hexocraft;

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

import com.github.hexocraft.DMRelocator.Artifact;
import com.github.hexocraft.DMRelocator.Downloader;
import com.github.hexocraft.DMRelocator.Repository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


class DownloaderTest {

    private static Path tmpDir;
    private static List<Repository> repositories;

    @BeforeAll
    static void init() throws IOException {
        // Temporary folder
        tmpDir = Files.createTempDirectory("DMR_");
        Assertions.assertTrue(tmpDir.toFile().exists());
        // Maven central repository
        repositories = new LinkedList<>(Arrays.asList(
                new Repository(new URL("https://repo.maven.org/maven2/")).name("Fake repository")
                , new Repository(new URL("https://repo1.maven.org/maven2/")).name("Maven Central")));
    }

    @AfterAll
    static void end() {
        Assertions.assertDoesNotThrow(() -> {
            Downloader.deleteDir(tmpDir);
            Assertions.assertFalse(tmpDir.toFile().exists());
        });
    }

    @Test
    void ArtifactFromUrl() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar");
            Artifact artifact = new Artifact("com.google.code.gson", "gson", "2.8.6").url(url);
            Downloader downloader = new Downloader(DMRelocator.Relocator(this.getClass()));
            downloader.download(artifact, null, tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromUrlWithSha1() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar");
            Artifact artifact = new Artifact("com.google.code.gson", "gson", "2.8.6").url(url).sha1("9180733b7df8542621dc12e21e87557e8c99b8cb");
            Downloader downloader = new Downloader(DMRelocator.Relocator(this.getClass()));
            downloader.download(artifact, null, tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromRepositoriesRelease() {
        Assertions.assertDoesNotThrow(() -> {
            Artifact artifact = new Artifact("com.google.code.gson", "gson", "2.8.6");
            Downloader downloader = new Downloader(DMRelocator.Relocator(this.getClass()));
            downloader.download(artifact, repositories, tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath()).toFile().exists());
        });
    }

    @Test
    void ArtifactFromRepositoriesSnapshot() {
        Assertions.assertDoesNotThrow(() -> {
            Artifact artifact = new Artifact("co.aikar", "acf-core", "0.5.0-SNAPSHOT");
            Repository repository = new Repository(new URL("https://repo.aikar.co/content/groups/aikar")).name("aikar");
            Downloader downloader = new Downloader(DMRelocator.Relocator(this.getClass()));
            downloader.download(artifact, Collections.singletonList(repository), tmpDir);

            Assertions.assertTrue(tmpDir.resolve(artifact.toPath()).toFile().exists());
        });
    }

}