package org.geysermc.hurricane.paper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ProjectResourcesTest {
    @Test
    void declaresHurricaneAsAFoliaSupportedPlugin() throws IOException {
        String plugin = resource("plugin.yml");

        assertTrue(plugin.contains("name: Hurricane"));
        assertTrue(plugin.contains("main: org.geysermc.hurricane.paper.HurricanePaper"));
        assertTrue(plugin.contains("api-version: '1.20'"));
        assertTrue(plugin.contains("folia-supported: true"));
        assertTrue(plugin.contains("softdepend: [Geyser-Spigot, floodgate]"));
        assertFalse(plugin.lines().anyMatch(line -> line.strip().startsWith("depend:")));
        assertFalse(plugin.contains("Better" + "Hurricane"));
        assertFalse(plugin.contains("better" + "hurricane"));
    }

    @Test
    void enablesBothCollisionFixesByDefault() throws IOException {
        String config = resource("config.yml");

        assertTrue(config.contains("bamboo: true"));
        assertTrue(config.contains("pointed-dripstone: true"));
    }

    private static String resource(String name) throws IOException {
        try (InputStream stream = ProjectResourcesTest.class.getClassLoader()
                .getResourceAsStream(name)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
