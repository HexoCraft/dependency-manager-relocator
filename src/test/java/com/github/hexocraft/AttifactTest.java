package com.github.hexocraft;

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