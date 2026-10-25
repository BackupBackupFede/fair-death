package net.emeraude.fairdeath.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

/**
 * Server configuration, read from {@code config/fairdeath.json}. Missing keys keep their default.
 * The {@code kept} switches are also set from {@code /fairdeath settings}.
 */
public final class Config {

    /** What is kept on death, by category. Gear on, the rest off, as the defaults of {@link Category}. */
    public static final class Kept {
        public boolean armour = Category.ARMOUR.defaultKept;
        public boolean toolsAndWeapons = Category.TOOLS_AND_WEAPONS.defaultKept;
        public boolean food = Category.FOOD.defaultKept;
        public boolean ores = Category.ORES.defaultKept;
        public boolean utilities = Category.UTILITIES.defaultKept;

        public boolean get(Category category) {
            return switch (category) {
                case ARMOUR -> armour;
                case TOOLS_AND_WEAPONS -> toolsAndWeapons;
                case FOOD -> food;
                case ORES -> ores;
                case UTILITIES -> utilities;
            };
        }

        public void set(Category category, boolean kept) {
            switch (category) {
                case ARMOUR -> armour = kept;
                case TOOLS_AND_WEAPONS -> toolsAndWeapons = kept;
                case FOOD -> food = kept;
                case ORES -> ores = kept;
                case UTILITIES -> utilities = kept;
            }
        }

        public Set<Category> switchedOn() {
            Set<Category> on = EnumSet.noneOf(Category.class);
            for (Category category : Category.values()) if (get(category)) on.add(category);
            return on;
        }
    }

    public Kept kept = new Kept();
    /** Make the items that do drop glow, so they are easy to find again. */
    public boolean glowingDrops = true;
    /** Tell the dead player, and only them, where they died. */
    public boolean deathCoordinates = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Reads the file, writing it with every default filled in when it is missing or incomplete. */
    public static Config load(Path file) throws IOException {
        Config config = null;
        if (Files.exists(file)) {
            try {
                config = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Config.class);
            } catch (JsonParseException e) {
                throw new IOException("unreadable " + file + ": " + e.getMessage(), e);
            }
        }
        if (config == null) config = new Config();
        if (config.kept == null) config.kept = new Kept();
        config.save(file);
        return config;
    }

    public void save(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.writeString(file, GSON.toJson(this), StandardCharsets.UTF_8);
    }
}
