package net.coms.abilitysgorwmod.ability.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** Prevents continuously refreshed ability buffs from stacking on every tick. */
public final class BuffEffectHelper {
    private BuffEffectHelper() {}

    public static void apply(Player player, MobEffect effect, int duration, int baseAmplifier) {
        if (player.level().isClientSide) return;
        String effectKey = effect.getDescriptionId().replace('.', '_');
        String tagKey = "abilitysgorwmod_buff_level_" + effectKey;
        MobEffectInstance current = player.getEffect(effect);
        int targetAmplifier;

        if (current == null) {
            targetAmplifier = baseAmplifier;
        } else {
            int previousTarget = player.getPersistentData().getInt(tagKey) - 1;
            if (previousTarget >= 0 && current.getAmplifier() == previousTarget) {
                targetAmplifier = current.getAmplifier();
            } else {
                targetAmplifier = Math.max(baseAmplifier, current.getAmplifier() + 1);
            }
        }

        player.getPersistentData().putInt(tagKey, targetAmplifier + 1);
        player.addEffect(new MobEffectInstance(effect, duration, targetAmplifier, true, false, false));
    }
}
