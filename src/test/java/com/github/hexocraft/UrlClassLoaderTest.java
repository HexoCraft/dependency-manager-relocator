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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class UrlClassLoaderTest {

    private static Path tmpDir;
    private static List<DMRelocator.Repository> repositories;

    @BeforeAll
    static void init() throws IOException {
        // Temporary folder
        tmpDir = Files.createTempDirectory("DMR_");
        // Maven central repository
        repositories = new LinkedList<>(Collections.singletonList(new DMRelocator.Repository(new URL("https://repo1.maven.org/maven2/")).name("Maven Central")));
    }

    @AfterAll
    static void end() {
        Assertions.assertThrows(IOException.class, () -> {
            DMRelocator.Downloader.deleteDir(tmpDir);
        });
    }

    @Test
    void AddToClassLoader() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator.Artifact artifact = new DMRelocator.Artifact("com.google.code.gson", "gson", "2.8.6");
            DMRelocator.Downloader.download(artifact, repositories, tmpDir);
            DMRelocator.UrlClassLoader.addToClassLoader(UrlClassLoaderTest.class.getClassLoader(), tmpDir.resolve(artifact.toPath()).toFile());

            Assertions.assertDoesNotThrow(() -> { Class.forName("com.google.gson.Gson"); });
        });
    }
}