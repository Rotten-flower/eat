package dev.flower.eat.game;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
public final class EatConfig {
    private static final System.Logger LOGGER = System.getLogger("eat.config");
    private static final Pattern EXCLUDED_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private static final String DEFAULT_JSON = """
            {
              "groundFeeding": true,
              "playerFeeding": true,
              "excludedEntities": []
            }
            """;
    private static volatile Snapshot snapshot = Snapshot.DEFAULT;
    private EatConfig() {}
    public static void load(Path file) {
        if (Files.notExists(file)) {
            try {
                Path parent = file.toAbsolutePath().getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(file, DEFAULT_JSON, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to create default config " + file, e);
            }
            snapshot = Snapshot.DEFAULT;
            return;
        }
        // 保留无效配置原文，方便用户修复。
        // Preserve invalid files so the user can correct them.
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to read config " + file, e);
            snapshot = Snapshot.DEFAULT;
            return;
        }
        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                LOGGER.log(System.Logger.Level.WARNING, "Config root is not an object: " + file);
                snapshot = Snapshot.DEFAULT;
                return;
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Invalid JSON in config " + file, e);
            snapshot = Snapshot.DEFAULT;
            return;
        }
        boolean groundFeeding = readBoolean(root, "groundFeeding", true, file);
        boolean playerFeeding = readBoolean(root, "playerFeeding", true, file);
        Set<String> excludedEntities = readExcludedEntities(root, file);
        snapshot = new Snapshot(groundFeeding, playerFeeding, excludedEntities);
    }
    public static boolean allowsGround(Mob mob) {
        Snapshot current = snapshot;
        if (!current.groundFeeding()) return false;
        if (current.excludedEntities().isEmpty()) return true;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        return !current.excludedEntities().contains(id);
    }
    public static boolean allowsPlayers() {
        return snapshot.playerFeeding();
    }
    private static boolean readBoolean(JsonObject root, String key, boolean fallback, Path file) {
        JsonElement value = root.get(key);
        if (value == null) return fallback;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean();
        LOGGER.log(System.Logger.Level.WARNING, "Config field '" + key + "' must be a boolean in " + file + "; using " + fallback);
        return fallback;
    }
    private static Set<String> readExcludedEntities(JsonObject root, Path file) {
        JsonElement value = root.get("excludedEntities");
        if (value == null) return Set.of();
        if (!value.isJsonArray()) {
            LOGGER.log(System.Logger.Level.WARNING, "Config field 'excludedEntities' must be an array in " + file + "; using empty set");
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement entry : value.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                LOGGER.log(System.Logger.Level.WARNING, "Skipping non-string excluded entity in " + file);
                continue;
            }
            String id = entry.getAsString();
            if (EXCLUDED_ID.matcher(id).matches()) result.add(id);
            else LOGGER.log(System.Logger.Level.WARNING, "Skipping invalid excluded entity id '" + id + "' in " + file);
        }
        return result;
    }
    private record Snapshot(boolean groundFeeding, boolean playerFeeding, Set<String> excludedEntities) {
        private Snapshot {
            excludedEntities = Set.copyOf(excludedEntities);
        }
        private static final Snapshot DEFAULT = new Snapshot(true, true, Set.of());
    }
}
