package net.coms.abilitysgorwmod.ability;

import net.coms.abilitysgorwmod.api.AbilityApi;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ChargeAbility {
    public static final String COOLDOWN_KEY = "abilitysgorwmod_charge_cooldown";
    public static final int COOLDOWN_TICKS = 100;
    private static final String DASH_TICKS = "abilitysgorwmod_charge_dash_ticks";
    private static final String MOB_COOLDOWN = "abilitysgorwmod_mob_charge_cooldown";
    private static final String HIT_TARGETS = "abilitysgorwmod_charge_hit_targets";
    private static final int DASH_DURATION_TICKS = 8;

    private ChargeAbility() {}

    public static boolean activate(ServerPlayer player) {
        if (!AbilityApi.isAbilityEnabled(player, "charge")
                || player.getPersistentData().getInt(COOLDOWN_KEY) > 0) {
            return false;
        }
        beginDash(player, player.getLookAngle(), COOLDOWN_KEY, COOLDOWN_TICKS, 2.25D);
        return true;
    }

    public static void tickPlayer(Player player) {
        decrement(player, COOLDOWN_KEY);
        tickActiveDash(player);
    }

    public static void tickMob(Mob mob) {
        int cooldown = mob.getPersistentData().getInt(MOB_COOLDOWN);
        if (cooldown > 0) {
            cooldown--;
            mob.getPersistentData().putInt(MOB_COOLDOWN, cooldown);
        }
        if (tickActiveDash(mob)) return;
        if (cooldown > 0 || !HardModeCombatAbilityData.hasAbility(mob, "charge")) return;

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || mob.distanceToSqr(target) > 24.0D * 24.0D) return;

        Vec3 aimPoint = target.position().add(0.0D, target.getBbHeight() * 0.35D, 0.0D);
        Vec3 origin = mob.position().add(0.0D, mob.getBbHeight() * 0.35D, 0.0D);
        Vec3 direction = aimPoint.subtract(origin);
        if (direction.lengthSqr() < 1.0E-5D) return;

        mob.getNavigation().stop();
        beginDash(mob, direction.normalize(), MOB_COOLDOWN, COOLDOWN_TICKS, 1.85D);
    }

    private static void decrement(LivingEntity entity, String key) {
        int cooldown = entity.getPersistentData().getInt(key);
        if (cooldown > 0) entity.getPersistentData().putInt(key, cooldown - 1);
    }

    private static void beginDash(LivingEntity entity, Vec3 direction, String cooldownKey,
                                  int cooldownTicks, double speed) {
        if (direction.lengthSqr() < 1.0E-5D) direction = entity.getLookAngle();
        entity.getPersistentData().putInt(cooldownKey, cooldownTicks);
        entity.getPersistentData().putInt(DASH_TICKS, DASH_DURATION_TICKS);
        entity.getPersistentData().put(HIT_TARGETS, new ListTag());
        entity.setDeltaMovement(direction.normalize().scale(speed));
        entity.hurtMarked = true;
        AbilityFeedback.play(entity, "entity.player.attack.sweep");
        AbilityFeedback.spawn(entity.level(), ParticleTypes.CLOUD,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.4D, entity.getZ(),
                8, 0.25D, 0.2D, 0.25D, 0.02D);
    }

    /** Returns true while this entity is completing a dash. */
    private static boolean tickActiveDash(LivingEntity attacker) {
        int ticks = attacker.getPersistentData().getInt(DASH_TICKS);
        if (ticks <= 0) return false;
        hitAlongPath(attacker);
        if (ticks == 1) {
            attacker.getPersistentData().remove(DASH_TICKS);
            attacker.getPersistentData().remove(HIT_TARGETS);
        } else {
            attacker.getPersistentData().putInt(DASH_TICKS, ticks - 1);
        }
        return true;
    }

    private static void hitAlongPath(LivingEntity attacker) {
        if (attacker.level().isClientSide) return;
        Vec3 previousStep = attacker.getDeltaMovement().scale(-1.0D);
        AABB sweep = attacker.getBoundingBox().expandTowards(previousStep).inflate(0.7D, 0.45D, 0.7D);
        List<LivingEntity> targets = attacker.level().getEntitiesOfClass(
                LivingEntity.class, sweep,
                target -> target != attacker && target.isAlive() && canHit(attacker, target));
        for (LivingEntity target : targets) {
            if (wasAlreadyHit(attacker, target)) continue;
            rememberHit(attacker, target);
            DamageSource source;
            if (attacker instanceof Player player) {
                source = attacker.damageSources().playerAttack(player);
            } else if (attacker instanceof Mob mob) {
                source = attacker.damageSources().mobAttack(mob);
            } else {
                continue;
            }
            float damage = (float) Math.max(0.0D, attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
            if (damage > 0.0F && target.hurt(source, damage)) {
                AbilityFeedback.particles(target, ParticleTypes.CRIT, 8,
                        target.getBbHeight() * 0.5D, 0.35D);
            }
        }
    }

    private static boolean canHit(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof Player) return target instanceof Mob;
        return attacker instanceof Mob mob && target == mob.getTarget();
    }

    private static boolean wasAlreadyHit(LivingEntity attacker, LivingEntity target) {
        ListTag hitTargets = attacker.getPersistentData().getList(HIT_TARGETS, Tag.TAG_STRING);
        String id = target.getUUID().toString();
        for (int i = 0; i < hitTargets.size(); i++) {
            if (id.equals(hitTargets.getString(i))) return true;
        }
        return false;
    }

    private static void rememberHit(LivingEntity attacker, LivingEntity target) {
        attacker.getPersistentData().getList(HIT_TARGETS, Tag.TAG_STRING)
                .add(StringTag.valueOf(target.getUUID().toString()));
    }
}