package net.coms.abilitysgorwmod.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

/** Persistent combat ability grants for hostile mobs spawned in Hard difficulty. */
public final class HardModeCombatAbilityData {
    private static final String PROCESSED_TAG = "abilitysgorwmod_hard_combat_checked";
    private static final String ABILITIES_TAG = "abilitysgorwmod_hard_combat_abilities";

    private static final List<String> COMBAT_ABILITIES = List.of(
            "critical_strike",
            "damage_dodge",
            "auto_counterattack",
            "undead_slayer",
            "lifesteal",
            "fatal_negation",
            "explosion_immunity",
            "knockback_immunity",
            "fall_negation",
            "death_explosion",
            "fire_immunity",
            "speed_boost",
            "jump_boost",
            "attack_speed",
            "charge",
            "resentment"
    );

    private HardModeCombatAbilityData() {}

    public static boolean wasSpawnProcessed(Mob mob) {
        return mob.getPersistentData().getBoolean(PROCESSED_TAG);
    }

    public static void markSpawnProcessed(Mob mob) {
        mob.getPersistentData().putBoolean(PROCESSED_TAG, true);
    }

    public static List<String> assignRandomAbilities(Mob mob, int count) {
        List<String> available = new ArrayList<>(COMBAT_ABILITIES);
        ListTag assigned = new ListTag();
        List<String> result = new ArrayList<>();
        int amount = Math.min(Math.max(count, 0), available.size());

        for (int i = 0; i < amount; i++) {
            int index = mob.getRandom().nextInt(available.size());
            String abilityId = available.remove(index);
            assigned.add(StringTag.valueOf(abilityId));
            result.add(abilityId);
        }

        mob.getPersistentData().put(ABILITIES_TAG, assigned);
        return List.copyOf(result);
    }

    public static boolean hasAbility(LivingEntity entity, String abilityId) {
        if (!(entity instanceof Mob mob) || abilityId == null) return false;
        ListTag assigned = mob.getPersistentData().getList(ABILITIES_TAG, Tag.TAG_STRING);
        for (int i = 0; i < assigned.size(); i++) {
            if (abilityId.equals(assigned.getString(i))) return true;
        }
        return false;
    }

    public static boolean hasAnyAbility(LivingEntity entity) {
        return entity instanceof Mob mob
                && !mob.getPersistentData().getList(ABILITIES_TAG, Tag.TAG_STRING).isEmpty();
    }
}