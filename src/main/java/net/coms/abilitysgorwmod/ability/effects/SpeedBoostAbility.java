package net.coms.abilitysgorwmod.ability.effects;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public final class SpeedBoostAbility {
    private SpeedBoostAbility() {}

    public static void apply(Player player) {
        BuffEffectHelper.apply(player, MobEffects.MOVEMENT_SPEED, 3, 0);
    }
}
