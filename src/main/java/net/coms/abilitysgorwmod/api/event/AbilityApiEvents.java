package net.coms.abilitysgorwmod.api.event;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.pack.AbilityTriggerRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Cancelable;

/** Public Forge events emitted by the ability API. All are server-side events. */
public final class AbilityApiEvents {
    private AbilityApiEvents() {}

    public abstract static class PlayerAbilityEvent extends Event {
        private final Player player;
        private final AbilityDefinition ability;

        protected PlayerAbilityEvent(Player player, AbilityDefinition ability) {
            this.player = player;
            this.ability = ability;
        }

        public Player getPlayer() { return player; }
        public AbilityDefinition getAbility() { return ability; }
        public String getAbilityId() { return ability.getId(); }
    }

    public static final class AbilityAcquiredEvent extends PlayerAbilityEvent {
        public AbilityAcquiredEvent(Player player, AbilityDefinition ability) { super(player, ability); }
    }

    public static final class AbilityRemovedEvent extends PlayerAbilityEvent {
        private final CompoundTag previousAbilityData;

        public AbilityRemovedEvent(Player player, AbilityDefinition ability, CompoundTag previousAbilityData) {
            super(player, ability);
            this.previousAbilityData = previousAbilityData.copy();
        }

        public CompoundTag getPreviousAbilityData() { return previousAbilityData.copy(); }
    }

    public static final class AbilityToggledEvent extends PlayerAbilityEvent {
        private final boolean active;

        public AbilityToggledEvent(Player player, AbilityDefinition ability, boolean active) {
            super(player, ability);
            this.active = active;
        }

        public boolean isActive() { return active; }
    }

    /** Fired when another mod asks this system to activate an owned, enabled ability. */
    @Cancelable
    public static final class AbilityActivationEvent extends PlayerAbilityEvent {
        private final String source;

        public AbilityActivationEvent(Player player, AbilityDefinition ability, String source) {
            super(player, ability);
            this.source = source == null ? "api" : source;
        }

        public String getSource() { return source; }
    }

    /** Fired when a JSON trigger matches; listeners can cancel its configured actions. */
    @Cancelable
    public static final class AbilityTriggeredEvent extends PlayerAbilityEvent {
        private final String trigger;
        private final AbilityTriggerRules.Context context;

        public AbilityTriggeredEvent(Player player, AbilityDefinition ability, String trigger,
                                     AbilityTriggerRules.Context context) {
            super(player, ability);
            this.trigger = trigger;
            this.context = context;
        }

        public String getTrigger() { return trigger; }
        public AbilityTriggerRules.Context getContext() { return context; }
    }

    /** Fired immediately before a built-in chance-based ability rolls its probability. */
    public static final class AbilityChanceEvent extends Event {
        private final Player player;
        private final String abilityId;
        private final float baseChance;
        private float chance;

        public AbilityChanceEvent(Player player, String abilityId, float baseChance) {
            this.player = player;
            this.abilityId = abilityId;
            this.baseChance = baseChance;
            this.chance = baseChance;
        }

        public Player getPlayer() { return player; }
        public String getAbilityId() { return abilityId; }
        public float getBaseChance() { return baseChance; }
        public float getChance() { return chance; }
        public void setChance(float chance) { this.chance = chance; }
        public void addChance(float amount) { this.chance += amount; }
    }
}