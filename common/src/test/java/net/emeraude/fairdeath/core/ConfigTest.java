package net.emeraude.fairdeath.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    @TempDir
    Path dir;

    @Test
    void missingFileIsWrittenWithEveryDefault() throws IOException {
        Path file = dir.resolve("fairdeath.json");
        Config config = Config.load(file);
        assertTrue(config.glowingDrops && config.deathCoordinates);
        assertEquals(EnumSet.of(Category.ARMOUR, Category.TOOLS_AND_WEAPONS), config.kept.switchedOn());
        String written = Files.readString(file, StandardCharsets.UTF_8);
        for (String key : new String[]{"kept", "armour", "toolsAndWeapons", "food", "ores", "utilities", "glowingDrops", "deathCoordinates"}) {
            assertTrue(written.contains(key), key);
        }
    }

    @Test
    void missingKeysKeepTheirDefault() throws IOException {
        Path file = dir.resolve("fairdeath.json");
        Files.writeString(file, "{ \"glowingDrops\": false, \"kept\": { \"ores\": true } }", StandardCharsets.UTF_8);
        Config config = Config.load(file);
        assertFalse(config.glowingDrops);
        assertTrue(config.deathCoordinates);
        assertTrue(config.kept.ores);
        assertTrue(config.kept.armour, "a kept block without armour keeps its default");
        assertFalse(config.kept.food);
    }

    @Test
    void switchesSurviveASave() throws IOException {
        Path file = dir.resolve("fairdeath.json");
        Config config = Config.load(file);
        config.kept.set(Category.ARMOUR, false);
        config.kept.set(Category.UTILITIES, true);
        config.save(file);
        Config again = Config.load(file);
        assertEquals(EnumSet.of(Category.TOOLS_AND_WEAPONS, Category.UTILITIES), again.kept.switchedOn());
    }

    @Test
    void everyCategoryHasItsOwnSwitch() {
        Config.Kept kept = new Config.Kept();
        for (Category category : Category.values()) {
            assertEquals(category.defaultKept, kept.get(category), category.name());
            kept.set(category, !category.defaultKept);
            assertEquals(!category.defaultKept, kept.get(category), category.name());
        }
    }

    @Test
    void brokenFileIsRefusedAndLeftAlone() throws IOException {
        Path file = dir.resolve("fairdeath.json");
        Files.writeString(file, "{ kept: ", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> Config.load(file));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).startsWith("{ kept"));
    }
}
