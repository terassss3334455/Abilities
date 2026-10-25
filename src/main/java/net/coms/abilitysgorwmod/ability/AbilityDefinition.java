package net.coms.abilitysgorwmod.ability;

public class AbilityDefinition {
    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY;

        public boolean isAtLeast(Rarity minimum) {
            return ordinal() >= minimum.ordinal();
        }
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