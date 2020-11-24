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

class AttifactTest {

    @BeforeAll
    static void init() {
    }

    @AfterAll
    static void end() {
    }

    @Test
    void AddArtifact() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator relocator = DMRelocator.Relocator(AttifactTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("com.google.com.gson", "gson", "2.8.6"));
            Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("com.google.com.gson:gson:2.8.6")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
        });
    }

    @Test
    void AsmVersion() {
        DMRelocator relocator = DMRelocator.Relocator(AttifactTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("org.ow2.asm", "asm", "9.0"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("org.ow2.asm:asm:9.0")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
    }

    @Test
    void AsmCommonsVersion() {
        DMRelocator relocator = DMRelocator.Relocator(AttifactTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("org.ow2.asm", "asm-commons", "9.0"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("org.ow2.asm:asm-commons:9.0")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
    }

    @Test
    void JarRelocatorVersion() {
        DMRelocator relocator = DMRelocator.Relocator(AttifactTest.class.getClassLoader()).addArtifact(new DMRelocator.Artifact("me.lucko", "jar-relocator", "1.3"));
        Assertions.assertNotNull(relocator.getArtifacts().stream().filter(a -> a.toString().equals("me.lucko:jar-relocator:1.3")).map(DMRelocator.Artifact::toString).findFirst().orElse(null));
    }
}