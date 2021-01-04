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

    //@Test
    void RelocateTest() {
        Assertions.assertDoesNotThrow(() -> {
            String RELOCATION_ROOT = "com.github.hexocraft.libs.";
            Relocator(RelocatorTest.class.getClassLoader())
                    .cacheDir(cacheDir)
                    .libDir(libDir)
                    // Add Maven repositories
                    .addRepository(new Repository(new URL("https://repo.aikar.co/content/groups/aikar")).name("aikar"))
                    .addRepository(new Repository(new URL("http://nexus.hc.to/content/repositories/pub_releases")).name("vault"))
                    // Add artifacts
                    // --> commons
                    .addArtifact(new Artifact("com.google.code.gson", "gson", "2.8.6"))
                    .addArtifact(new Artifact("com.google.guava", "guava", "30.0-jre"))
                    .addArtifact(new Artifact("com.typesafe", "config", "1.4.1"))
                    .addArtifact(new Artifact("org.yaml", "snakeyaml", "1.27"))
                    // --> configuration
                    .addArtifact(new Artifact("org.spongepowered", "configurate-core", "4.0.0"))
                    .addArtifact(new Artifact("org.spongepowered", "configurate-hocon", "4.0.0"))
                    .addArtifact(new Artifact("org.spongepowered", "configurate-yaml", "4.0.0"))
                    .addArtifact(new Artifact("com.github.hexocraft", "configuration-core", "1.0.0-SNAPSHOT"))
                    .addArtifact(new Artifact("com.github.hexocraft", "configuration-hocon", "1.0.0-SNAPSHOT"))
                    .addArtifact(new Artifact("com.github.hexocraft", "configuration-yaml", "1.0.0-SNAPSHOT"))
                    // --> Annotation Command Framework
                    .addArtifact(new Artifact("co.aikar", "acf-core", "0.5.0-SNAPSHOT"))
                    .addArtifact(new Artifact("co.aikar", "acf-bukkit", "0.5.0-SNAPSHOT"))
                    .addArtifact(new Artifact("co.aikar", "acf-paper", "0.5.0-SNAPSHOT"))
                    // --> Vault API
                    .addArtifact(new Artifact("net.milkbowl.vault", "VaultAPI", "1.7"))
                    // Add relocations
                    // --> commons
                    .addRelocation(new Relocation("com.google", RELOCATION_ROOT + "google"))
                    .addRelocation(new Relocation("com.typesafe.config", RELOCATION_ROOT + "typesafe.config"))
                    .addRelocation(new Relocation("org.yaml.snakeyaml", RELOCATION_ROOT + "snakeyaml"))
                    // --> configuration
                    .addRelocation(new Relocation("org.spongepowered.configurate", RELOCATION_ROOT + "configurate"))
                    .addRelocation(new Relocation("com.github.hexocraft.configuration", RELOCATION_ROOT + "configuration"))
                    // --> Annotation Command Framework
                    .addRelocation(new Relocation("co.aikar.commands", RELOCATION_ROOT + "acf"))
                    .addRelocation(new Relocation("co.aikar.locales", RELOCATION_ROOT + "locales"))
                    // --> Vault API
                    .addRelocation(new Relocation("net.milkbowl.vault", RELOCATION_ROOT + "vault"))
                    // relocate artifacts
                    .relocate();

            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "google.gson.Gson"));

            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "configurate.ConfigurateException"));
            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "configurate.hocon.HoconConfigurationLoader"));
            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "configurate.yaml.YamlConfigurationLoader"));

            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "configuration.AbstractConfiguration"));
            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "configuration.hocon.HoconConfiguration"));
            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "configuration.yaml.YamlConfiguration"));

            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "acf.CommandContexts"));
            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "acf.BukkitCommandContexts"));
            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "acf.PaperCommandContexts"));
            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "locales.LocaleManager"));

            Assertions.assertDoesNotThrow(() -> Class.forName(RELOCATION_ROOT + "vault.economy.Economy"));
        });
    }
}