package net.coms.abilitysgorwmod.ability;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/** Small server/client-safe helpers for ability feedback effects. */
public final class AbilityFeedback {
    private AbilityFeedback() {}

    public static void play(Entity entity, String soundId) {
        play(entity, soundId, 0.8F, 1.0F);
    }

    public static void play(Entity entity, String soundId, float volume, float pitch) {
        if (entity == null) return;
        if (entity.level().isClientSide) {
            if (entity instanceof Player) {
                SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(soundId));
                if (sound != null) entity.playSound(sound, volume, pitch);
            }
        } else if (entity.level() instanceof ServerLevel serverLevel) {
            playAt(serverLevel, entity.getX(), entity.getY(), entity.getZ(), soundId, volume, pitch);
        }
    }

    public static void playAt(ServerLevel level, double x, double y, double z, String soundId) {
        playAt(level, x, y, z, soundId, 0.8F, 1.0F);
    }

    public static void playAt(ServerLevel level, double x, double y, double z,
                              String soundId, float volume, float pitch) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(soundId));
        if (sound != null) {
            level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
        }
    }

    public static void particles(Entity entity, ParticleOptions particle, int count,
                                 double yOffset, double spread) {
        if (entity == null) return;
        spawn(entity.level(), particle, entity.getX(), entity.getY() + yOffset, entity.getZ(),
                count, spread, spread, spread, 0.01D);
    }

    public static void spawn(Level level, ParticleOptions particle,
                             double x, double y, double z, int count,
                             double spreadX, double spreadY, double spreadZ, double speed) {
        if (level == null || particle == null || count <= 0) return;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, count,
                    spreadX, spreadY, spreadZ, speed);
            return;
        }
        for (int i = 0; i < count; i++) {
            level.addParticle(particle,
                    x + (level.random.nextDouble() - 0.5D) * spreadX * 2.0D,
                    y + (level.random.nextDouble() - 0.5D) * spreadY * 2.0D,
                    z + (level.random.nextDouble() - 0.5D) * spreadZ * 2.0D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    /** Returns true once when the supplied condition changes from false to true. */
    public static boolean entered(Player player, String id, boolean condition) {
        String key = "abilitysgorwmod_feedback_active_" + id;
        boolean previous = player.getPersistentData().getBoolean(key);
        player.getPersistentData().putBoolean(key, condition);
        return condition && !previous;
    }

    /** Rate limits repeated status-prevention feedback for effects reapplied every tick. */
    public static boolean allow(Player player, String id, int intervalTicks) {
        String key = "abilitysgorwmod_feedback_tick_" + id;
        int now = player.tickCount;
        int previous = player.getPersistentData().getInt(key);
        if (previous > 0 && now >= previous && now - previous < intervalTicks) return false;
        player.getPersistentData().putInt(key, now);
        return true;
    }
}