package net.coms.abilitysgorwmod.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityDefinition {
    /** Extensible rarity tier used by abilities and weighted selection. */
    public static final class Rarity {
        private static final Map<String, Rarity> BY_ID = new ConcurrentHashMap<>();
        private static final List<Rarity> VALUES = new ArrayList<>();

        public static final Rarity COMMON = builtin("common", 0, 0xFFFFFF, 70.0);
        public static final Rarity RARE = builtin("rare", 1, 0x55FFFF, 22.0);
        public static final Rarity EPIC = builtin("epic", 2, 0xAA00FF, 7.0);
        public static final Rarity LEGENDARY = builtin("legendary", 3, 0xFFFF55, 1.0);

        private final String id;
        private final String legacyName;
        private final Component displayName;
        private final int sortOrder;
        private final int color;
        private volatile double rollWeight;

        private Rarity(String id, String legacyName, Component displayName,
                       int sortOrder, int color, double rollWeight) {
            this.id = id;
            this.legacyName = legacyName;
            this.displayName = displayName;
            this.sortOrder = sortOrder;
            this.color = color;
            this.rollWeight = rollWeight;
        }

        private static Rarity builtin(String path, int order, int color, double weight) {
            String id = "abilitysgorwmod:" + path;
            Rarity rarity = new Rarity(id, path.toUpperCase(Locale.ROOT),
                    Component.translatable("rarity.abilitysgorwmod." + path), order, color, weight);
            BY_ID.put(id, rarity);
            BY_ID.put(path, rarity); // Keeps JSON and Java code using COMMON/RARE/etc compatible.
            VALUES.add(rarity);
            return rarity;
        }

        /** Register a custom rarity before registering abilities that use it. */
        public static synchronized Rarity register(String id, Component displayName,
                                                   int sortOrder, int color, double rollWeight) {
            if (id == null || id.isBlank() || !id.equals(id.toLowerCase(Locale.ROOT))
                    || !id.contains(":") || ResourceLocation.tryParse(id) == null)
                throw new IllegalArgumentException("Rarity ID must use lowercase namespace:path format");
            if (displayName == null) throw new IllegalArgumentException("Rarity display name cannot be null");
            if (sortOrder < 0 || sortOrder > 10000)
                throw new IllegalArgumentException("Rarity sort order must be between 0 and 10000");
            validateWeight(rollWeight);
            if (BY_ID.containsKey(id)) throw new IllegalArgumentException("Duplicate rarity ID: " + id);

            Rarity rarity = new Rarity(id, id.toUpperCase(Locale.ROOT), displayName,
                    sortOrder, color, rollWeight);
            BY_ID.put(id, rarity);
            VALUES.add(rarity);
            return rarity;
        }

        public static Rarity valueOf(String name) {
            if (name == null) throw new IllegalArgumentException("Rarity name cannot be null");
            String normalized = name.trim().toLowerCase(Locale.ROOT);
            Rarity rarity = BY_ID.get(normalized);
            if (rarity == null) throw new IllegalArgumentException("Unknown rarity: " + name);
            return rarity;
        }

        public static synchronized Rarity[] values() {
            return VALUES.toArray(new Rarity[0]);
        }

        public String getId() { return id; }
        public String name() { return legacyName; }
        public int ordinal() { return sortOrder; }
        public int getColor() { return color; }
        public double getRollWeight() { return rollWeight; }
        public Component getDisplayName() { return displayName; }

        public void setRollWeight(double rollWeight) {
            validateWeight(rollWeight);
            this.rollWeight = rollWeight;
        }

        public boolean isAtLeast(Rarity minimum) {
            return minimum != null && sortOrder >= minimum.sortOrder;
        }

        private static void validateWeight(double weight) {
            if (!Double.isFinite(weight) || weight < 0 || weight > 1_000_000)
                throw new IllegalArgumentException("Rarity roll weight must be between 0 and 1000000");
        }

        @Override
        public String toString() { return id; }
    }

    private final Rarity rarity;
    private final String id;
    private final String name;
    private final String description;
    private final boolean obtainable;
    private final boolean toggleable;

    public AbilityDefinition(
            String id,
            String name,
            String description,
            boolean obtainable,
            boolean toggleable
    ) {
        this(id, name, description, obtainable, toggleable, Rarity.COMMON);
    }

    public AbilityDefinition(
            String id,
            String name,
            String description,
            boolean obtainable,
            boolean toggleable,
            Rarity rarity
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.obtainable = obtainable;
        this.toggleable = toggleable;
        this.rarity = rarity == null ? Rarity.COMMON : rarity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Component getNameComponent() {
        String key = localTranslationKey("");
        return key == null ? Component.literal(name) : Component.translatable(key);
    }

    public Component getDescriptionComponent() {
        String key = localTranslationKey(".description");
        return key == null ? Component.literal(description) : Component.translatable(key);
    }

    /**
     * Built-in abilities use the mod's language file. External abilities keep
     * the display name and description supplied by their ability pack.
     */
    private String localTranslationKey(String suffix) {
        String path = id;
        int separator = path.indexOf(':');
        if (separator >= 0) {
            if (!"abilitysgorwmod".equals(path.substring(0, separator))) {
                return null;
            }
            path = path.substring(separator + 1);
        }
        if (!path.matches("[a-z0-9_./-]+")) {
            return null;
        }
        return "ability.abilitysgorwmod." + path + suffix;
    }

    public boolean isObtainable() {
        return obtainable;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public Rarity getRarity() {
        return rarity;
    }
}