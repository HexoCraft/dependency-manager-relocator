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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static com.github.hexocraft.DMRelocator.Relocator;
import static com.github.hexocraft.DMRelocator.Repository;

class RepositoryTest {

    @Test
    void AddRepository() {
        Assertions.assertDoesNotThrow(() -> {
            DMRelocator relocator = Relocator(ArtifactTest.class.getClassLoader()).addRepository(new Repository(new URL("https://repo.hexocube.fr/release")));
            Assertions.assertNotNull(relocator.getRepositories().stream().map(Repository::toString).filter(s -> s.equals("https://repo.hexocube.fr/release")).findFirst().orElse(null));
        });
    }

    @Test
    void MavenCentralRepositoryTest() {
        Assertions.assertDoesNotThrow(() -> {
            URL url = new URL("https://repo1.maven.org/maven2/");
            Repository repository = new Repository(url).name("Maven Central");

            Assertions.assertEquals("Maven Central", repository.name());
            Assertions.assertTrue(repository.isRemote());
            Assertions.assertFalse(repository.isLocale());
        });
    }

}