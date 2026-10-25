package net.coms.abilitysgorwmod.pack;

import com.google.gson.*;
import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Loads data-only abilities from config/abilitysgorwmod/ability_packs/*.json. */
public final class AbilityPackLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("abilitysgorwmod/AbilityPacks");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Set<String> EFFECTS = Set.of(
            "speed", "haste", "strength", "jump_boost", "night_vision",
            "fire_resistance", "water_breathing", "slow_falling",
            "regeneration", "resistance", "invisibility", "luck"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private AbilityPackLoader() {}

    public static void loadInto(Map<String, AbilityDefinition> registry) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("abilitysgorwmod/ability_packs");
        try {
            Files.createDirectories(dir);
            writeIfMissing(dir.resolve("README_最初にお読みください.txt"),
                    "abilitysgorwmod 外部能力パック\n*.json をこのフォルダに配置して起動してください。\n仕様は同じフォルダの README_能力パック.md を参照してください。");
            writeIfMissing(dir.resolve("template.json"), """
                    {
                      "abilities": [
                        {
                          "id": "yourpack:your_ability",
                          "name": "能力名",
                          "description": "能力の説明。",
                          "rarity": "COMMON",
                          "obtainable": true,
                          "toggleable": true,
                          "triggers": [],
                          "effect": { "type": "speed", "amplifier": 0, "duration": 100 }
                        }
                      ]
                    }
                    """);
            writeIfMissing(dir.resolve("sample.json"), """
                    {
                      "abilities": [
                        {
                          "id": "example:swift",
                          "name": "疾風",
                          "description": "能力が有効な間、移動速度上昇を得る。",
                          "rarity": "RARE",
                          "obtainable": true,
                          "toggleable": true,
                          "triggers": [],
                          "effect": { "type": "speed", "amplifier": 0, "duration": 100 }
                        },
                        {
                          "id": "example:night_sight",
                          "name": "夜目",
                          "description": "能力が有効な間、暗視を得る。",
                          "rarity": "COMMON",
                          "obtainable": true,
                          "toggleable": true,
                          "effect": { "type": "night_vision", "amplifier": 0, "duration": 300 }
                        }
                      ]
                    }
                    """);
            copyGuideIfMissing(dir.resolve("README_能力パック.md"));
            copyGuideIfMissing(dir.resolve("README_イベント能力.md"));
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*.json")) {
                for (Path file : files) {
                    if (file.getFileName().toString().equals("template.json")) continue;
                    try {
                        JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                        JsonArray abilities = root.getAsJsonArray("abilities");
                        if (abilities == null) throw new JsonParseException("必須配列 'abilities' がありません");
                        for (JsonElement element : abilities) {
                            try {
                                JsonObject a = element.getAsJsonObject();
                                String id = requiredString(a, "id");
                                if (!ID.matcher(id).matches()) throw new IllegalArgumentException("id は namespace:path 形式にしてください: " + id);
                                if (registry.containsKey(id)) throw new IllegalArgumentException("ID が既存能力または他パックと重複しています: " + id);
                                String name = requiredString(a, "name");
                                String description = requiredString(a, "description");
                                AbilityDefinition.Rarity rarity;
                                try { rarity = AbilityDefinition.Rarity.valueOf(a.has("rarity") ? a.get("rarity").getAsString().toUpperCase(Locale.ROOT) : "COMMON"); }
                                catch (IllegalArgumentException ex) { throw new IllegalArgumentException("不正な rarity。COMMON/RARE/EPIC/LEGENDARY のいずれかが必要です"); }
                                JsonObject effect = null;
                                if (a.has("effect") && !a.get("effect").isJsonNull()) {
                                    if (!a.get("effect").isJsonObject()) throw new IllegalArgumentException("effect はオブジェクトで指定してください");
                                    effect = a.getAsJsonObject("effect");
                                }
                                if (effect != null) {
                                    String type = requiredString(effect, "type");
                                    if (!EFFECTS.contains(type)) throw new IllegalArgumentException("未対応 effect.type: " + type);
                                    validateEffect(effect);
                                }
                                JsonArray triggers = null;
                                if (a.has("triggers") && !a.get("triggers").isJsonNull()) {
                                    if (!a.get("triggers").isJsonArray()) throw new IllegalArgumentException("triggers は配列で指定してください");
                                    triggers = a.getAsJsonArray("triggers");
                                    AbilityTriggerRules.validate(triggers);
                                }
                                registry.put(id, new AbilityDefinition(id, name, description,
                                        !a.has("obtainable") || a.get("obtainable").getAsBoolean(),
                                        !a.has("toggleable") || a.get("toggleable").getAsBoolean(), rarity));
                                if (effect != null) AbilityEffectDefinitions.register(id, effect);
                                if (triggers != null) AbilityTriggerRules.register(id, triggers);
                                LOGGER.info("Loaded external ability '{}' from {}", id, file.getFileName());
                            } catch (Exception ex) {
                                LOGGER.error("能力定義エラー: {} / {}", file.getFileName(), ex.getMessage());
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("能力パック '{}' を読み込めません: {}", file.getFileName(), ex.getMessage(), ex);
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.error("能力パックフォルダを準備できません: {}", dir, ex);
        }
    }
    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || object.get(key).getAsString().isBlank())
            throw new IllegalArgumentException("必須文字列フィールドがありません: " + key);
        return object.get(key).getAsString().trim();
    }
    private static void validateEffect(JsonObject effect) {
        for (String key : effect.keySet()) {
            if (!Set.of("type", "amplifier", "duration").contains(key)) throw new IllegalArgumentException("未対応 effect 項目です: " + key);
        }
        int amplifier = optionalInt(effect, "amplifier", 0);
        int duration = optionalInt(effect, "duration", 100);
        if (amplifier < 0 || amplifier > 10) throw new IllegalArgumentException("effect.amplifier は 0～10 にしてください");
        if (duration < 1 || duration > 1200) throw new IllegalArgumentException("effect.duration は 1～1200 tick にしてください");
    }
    private static int optionalInt(JsonObject object, String key, int fallback) {
        if (!object.has(key)) return fallback;
        if (!object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) throw new IllegalArgumentException("effect." + key + " は整数で指定してください");
        double value = object.get(key).getAsDouble();
        if (value != Math.rint(value)) throw new IllegalArgumentException("effect." + key + " は整数で指定してください");
        return (int) value;
    }
    private static void copyGuideIfMissing(Path target) throws IOException {
        String fileName = target.getFileName().toString();
        String resourcePath = "/ability_packs/" + fileName;
        try (java.io.InputStream in = AbilityPackLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                if (!Files.exists(target)) Files.writeString(target, "同梱の " + fileName + " を参照してください。", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                return;
            }
            byte[] contents = in.readAllBytes();
            if (!Files.exists(target)) {
                Files.write(target, contents, StandardOpenOption.CREATE_NEW);
                return;
            }
            // The earlier event guide mistakenly copied README_能力パック.md. Replace only that exact stale copy.
            if (fileName.equals("README_イベント能力.md")) {
                try (java.io.InputStream oldGuide = AbilityPackLoader.class.getResourceAsStream("/ability_packs/README_能力パック.md")) {
                    if (oldGuide != null && java.util.Arrays.equals(Files.readAllBytes(target), oldGuide.readAllBytes()))
                        Files.write(target, contents, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
        }
    }
    private static void writeIfMissing(Path path, String content) throws IOException {
        if (!Files.exists(path)) Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
}
