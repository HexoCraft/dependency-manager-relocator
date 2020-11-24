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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.hexocraft.DMRelocator.*;

class RelocatorTest {

    private static final Path cacheDir = Paths.get("target", "relocator", "cache");
    private static final Path libDir = Paths.get("target", "relocator", "libs");

    @BeforeAll
    static void init() {
    }

    @AfterAll
    static void end() {
    }

    @Test
    void Relocate() {
        Assertions.assertDoesNotThrow(() -> {
            Relocator(RelocatorTest.class.getClassLoader())
                    .cacheDir(cacheDir)
                    .libDir(libDir)
                    .addArtifact(new Artifact("com.google.code.gson", "gson", "2.8.6"))
                    .addArtifact(new Artifact("org.apache.commons", "commons-lang3", "3.11"))
                    .addRelocation(new Relocation("com.google.gson", "com.github.hexocraft.libs.gson"))
                    .addRelocation(new Relocation("org.apache.commons.lang3", "com.github.hexocraft.libs.commons-lang3"))
                    .relocate();

            Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName("com.google.gson.Gson"));
            Assertions.assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.commons.lang3.ClassUtils"));
            Assertions.assertDoesNotThrow(() -> Class.forName("com.github.hexocraft.libs.gson.Gson"));
            Assertions.assertDoesNotThrow(() -> Class.forName("com.github.hexocraft.libs.commons-lang3.ClassUtils"));
        });
    }

    @Test
    void RelocateSnapshotArtifact() {
        Assertions.assertDoesNotThrow(() -> {
            Relocator(RelocatorTest.class.getClassLoader())
                    .cacheDir(cacheDir)
                    .libDir(libDir)
                    // Add Maven repositories
                    .addRepository(new Repository(new URL("https://repo.aikar.co/content/groups/aikar")).name("aikar"))
                    // Add artifacts
                    .addArtifact(new Artifact("co.aikar", "acf-core", "0.5.0-SNAPSHOT"))
                    // Add relocations
                    .addRelocation(new Relocation("co.aikar", "com.github.hexocraft.libs.aikar"))
                    // relocate artifacts
                    .relocate();

            Assertions.assertDoesNotThrow(() -> Class.forName("com.github.hexocraft.libs.aikar.commands.Annotations"));
        });
    }
}