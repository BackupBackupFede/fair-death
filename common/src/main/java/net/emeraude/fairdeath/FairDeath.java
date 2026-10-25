package net.emeraude.fairdeath;

import net.emeraude.fairdeath.core.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Loader-agnostic entry point — TIER A (server-side) logic goes here.
 *
 * <p>Both loader modules compile this source in directly and call {@link #init(Path)}. Anything
 * that runs on a dedicated server belongs in this class or below it; nothing here may touch
 * client-only Minecraft classes, or a dedicated server will crash on class load.
 */
public final class FairDeath {

    public static final String MOD_ID = "fairdeath";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fair Death");

    private static boolean initialized = false;
    private static Path configDir = Path.of("config");
    private static Config config = new Config();

    private FairDeath() {}

    /** Called once from each loader's entry point, as early as possible, with the loader's config folder. */
    public static synchronized void init(Path configDir) {
        if (initialized) return;
        initialized = true;
        FairDeath.configDir = configDir;
        reloadConfig();

        LOGGER.info("{} loaded{}", MOD_ID, debug() ? " (DEBUG ON)" : "");
    }

    public static Config config() {
        return config;
    }

    /** Re-reads {@code config/fairdeath.json}. A broken file keeps the previous settings. */
    public static String reloadConfig() {
        try {
            config = Config.load(configDir.resolve(MOD_ID + ".json"));
            return null;
        } catch (IOException e) {
            LOGGER.error("Could not read the config, keeping the previous settings", e);
            return e.getMessage();
        }
    }

    /** Writes the current settings back to {@code config/fairdeath.json}; returns why it failed, or null. */
    public static String saveConfig() {
        try {
            config.save(configDir.resolve(MOD_ID + ".json"));
            return null;
        } catch (IOException e) {
            LOGGER.error("Could not save the config", e);
            return e.getMessage();
        }
    }

    /** For the self-test's witness run only. */
    static void useConfig(Config replacement) {
        config = replacement;
    }

    public static Path configDir() {
        return configDir;
    }

    /**
     * Test-only features (a debug command, a fake second player...). Off in normal use, on when any
     * of these is present:
     * <ul>
     *   <li>the self-test switch ({@code <MODID>_SELFTEST=true}) — which also HALTS the server, so it
     *       is no use for playing;</li>
     *   <li>{@code <MODID>_DEBUG=true} in the environment — a Gradle run inherits it;</li>
     *   <li>{@code -D<modid>.debug=true} on the JVM;</li>
     *   <li>an empty file {@code config/<modid>.debug} — for a launcher that cannot take JVM
     *       arguments.</li>
     * </ul>
     * Gate debug commands behind an operator level as well: the file can be left behind on a server.
     */
    public static boolean debug() {
        String env = MOD_ID.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "") + "_DEBUG";
        return FairDeathSelfTest.requested()
            || "true".equalsIgnoreCase(System.getenv(env))
            || Boolean.getBoolean(MOD_ID + ".debug")
            || Files.exists(configDir.resolve(MOD_ID + ".debug"));
    }
}
