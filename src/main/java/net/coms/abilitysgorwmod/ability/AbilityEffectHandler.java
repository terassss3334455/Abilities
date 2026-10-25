
package net.coms.abilitysgorwmod.ability;

import net.coms.abilitysgorwmod.ability.effects.AutoSmeltAbility;
import net.coms.abilitysgorwmod.ability.effects.BuffEffectHelper;
import net.coms.abilitysgorwmod.ability.effects.SpeedBoostAbility;
import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.RecipeType;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@SuppressWarnings({"resource", "deprecation"})
@Mod.EventBusSubscriber(modid = abilitysgorwmod.MODID)
public final class AbilityEffectHandler {

    private static final String PROJECTILE_PHASE_COOLDOWN =
            "abilitysgorwmod_projectile_phase_cooldown";

    private static final double PROJECTILE_DODGE_DISTANCE = 4.0D;
    private static final double CREEPER_DISAPPEAR_DISTANCE = 8.0D;
    private static final UUID ATTACK_SPEED_MODIFIER_ID = UUID.fromString("a2c7fcd3-8e42-4dbe-9db5-48d49d1a4f11");
    private static boolean processingRetaliation = false;

    private AbilityEffectHandler() {}

    private static boolean active(Player player, String id) {
        return PlayerAbilityData.isActive(player, id);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide) {
            return;
        }

        Player p = event.player;
        Level level = p.level();

        // 瞬間転位のクールダウン
        int projectileCooldown = p.getPersistentData()
                .getInt(PROJECTILE_PHASE_COOLDOWN);

        if (projectileCooldown > 0) {
            p.getPersistentData().putInt(
                    PROJECTILE_PHASE_COOLDOWN,
                    projectileCooldown - 1);
        }
        int dimensionCooldown = p.getPersistentData().getInt("abilitysgorwmod_dimension_shift_cooldown");
        if (dimensionCooldown > 0) {
            p.getPersistentData().putInt("abilitysgorwmod_dimension_shift_cooldown", dimensionCooldown - 1);
        }

        if (active(p, "speed_boost")) {
            SpeedBoostAbility.apply(p);
        }


        // バフは固定レベルで更新し、毎tickレベルを積み上げない。
        if (active(p, "night_vision")) {
            BuffEffectHelper.apply(p, MobEffects.NIGHT_VISION, 220, 0);
        }
        if (active(p, "entity_glow") && p.tickCount % 20 == 0) {
            for (Entity nearby : level.getEntities(p, p.getBoundingBox().inflate(12.0D))) {
                if (nearby instanceof LivingEntity living && living != p) {
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, false));
                }
            }
        }
        if (active(p, "weakness_immunity")) p.removeEffect(MobEffects.WEAKNESS);
        if (active(p, "wither_immunity")) p.removeEffect(MobEffects.WITHER);
        if (active(p, "blindness_immunity")) p.removeEffect(MobEffects.BLINDNESS);
        if (active(p, "slowness_immunity")) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (active(p, "hunger_immunity")) p.removeEffect(MobEffects.HUNGER);
        if (active(p, "mining_fatigue_immunity")) p.removeEffect(MobEffects.DIG_SLOWDOWN);
        if (active(p, "nausea_immunity")) p.removeEffect(MobEffects.CONFUSION);
        if (active(p, "darkness_immunity")) p.removeEffect(MobEffects.DARKNESS);
        if (active(p, "unluck_immunity")) p.removeEffect(MobEffects.UNLUCK);

        AttributeInstance attackSpeed = p.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            if (active(p, "attack_speed")) {
                if (attackSpeed.getModifier(ATTACK_SPEED_MODIFIER_ID) == null) {
                    attackSpeed.addTransientModifier(new AttributeModifier(
                            ATTACK_SPEED_MODIFIER_ID, "Ability attack speed", 0.5D,
                            AttributeModifier.Operation.MULTIPLY_BASE));
                }
            } else if (attackSpeed.getModifier(ATTACK_SPEED_MODIFIER_ID) != null) {
                attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_ID);
            }
        }

        if (active(p, "water_breathing")
                && p.isEyeInFluid(FluidTags.WATER)) {
            BuffEffectHelper.apply(p, MobEffects.WATER_BREATHING, 40, 0);
        }

        if (active(p, "poison_immunity")) {
            p.removeEffect(MobEffects.POISON);
        }

        if (active(p, "levitation_immunity")) {
            p.removeEffect(MobEffects.LEVITATION);
        }

        if (active(p, "freeze_immunity")) {
            p.setTicksFrozen(0);
        }

        // 火炎支配：火炎耐性エフェクトを付与
        if (active(p, "fire_immunity")) {
            BuffEffectHelper.apply(p, MobEffects.FIRE_RESISTANCE, 40, 0);
        }

        if (active(p, "no_hunger") && p instanceof ServerPlayer sp) {
            sp.getFoodData().setFoodLevel(20);
            sp.getFoodData().setSaturation(20.0F);
        }

        if (active(p, "slow_fall")
                && !p.onGround()
                && p.getDeltaMovement().y < 0) {
            BuffEffectHelper.apply(p, MobEffects.SLOW_FALLING, 25, 0);
        }

        if (active(p, "night_power")
                && (!level.isDay()
                || level.getMaxLocalRawBrightness(p.blockPosition()) <= 7)) {
            BuffEffectHelper.apply(p, MobEffects.DAMAGE_BOOST, 40, 0);
            BuffEffectHelper.apply(p, MobEffects.MOVEMENT_SPEED, 40, 0);
        }

        if (active(p, "dark_invisibility")
                && (!level.isDay()
                || level.getMaxLocalRawBrightness(p.blockPosition()) <= 7)) {
            BuffEffectHelper.apply(p, MobEffects.INVISIBILITY, 40, 0);
        }

        if (active(p, "water_speed")
                && p.isEyeInFluid(FluidTags.WATER)) {
            BuffEffectHelper.apply(p, MobEffects.DOLPHINS_GRACE, 40, 0);
        }

        if (active(p, "treasure_fishing")
                && (p.getMainHandItem().is(Items.FISHING_ROD)
                || p.getOffhandItem().is(Items.FISHING_ROD))) {
            BuffEffectHelper.apply(p, MobEffects.LUCK, 40, 1);
        }

        if (active(p, "jump_boost")) {
            BuffEffectHelper.apply(p, MobEffects.JUMP, 25, 1);
        }

        if (active(p, "mining_speed")) {
            BuffEffectHelper.apply(p, MobEffects.DIG_SPEED, 25, 1);
        }

        // 飛行能力
        if (p instanceof ServerPlayer sp) {
            boolean wantsFlight = active(p, "flight");
            boolean granted = sp.getPersistentData()
                    .getBoolean("abilitysgorwmod_flight_granted");

            if (wantsFlight && !sp.isCreative() && !sp.isSpectator()) {
                if (!sp.getAbilities().mayfly) {
                    sp.getAbilities().mayfly = true;
                }

                sp.getPersistentData().putBoolean(
                        "abilitysgorwmod_flight_granted", true);
                sp.onUpdateAbilities();

            } else if (!wantsFlight && granted) {
                sp.getAbilities().flying = false;
                sp.getAbilities().mayfly = false;
                sp.getPersistentData().remove(
                        "abilitysgorwmod_flight_granted");
                sp.onUpdateAbilities();
            }
        }

        // チェスト・樽探知
        if (active(p, "chest_finder")
                && p instanceof ServerPlayer chestPlayer
                && p.tickCount % 20 == 0) {

            BlockPos origin = p.blockPosition();

            for (BlockPos chestPos : BlockPos.betweenClosed(
                    origin.offset(-16, -8, -16),
                    origin.offset(16, 8, 16))) {

                BlockState chestState = level.getBlockState(chestPos);

                if (chestState.is(Blocks.CHEST)
                        || chestState.is(Blocks.TRAPPED_CHEST)
                        || chestState.is(Blocks.BARREL)) {

                    ((ServerLevel) level).sendParticles(
                            chestPlayer,
                            ParticleTypes.END_ROD,
                            true,
                            chestPos.getX() + 0.5,
                            chestPos.getY() + 1.15,
                            chestPos.getZ() + 0.5,
                            2,
                            0.15, 0.15, 0.15,
                            0.005);
                }
            }
        }

        // クリーパー消滅・ピグリン敵対解除
        if (p.tickCount % 10 == 0) {

            if (active(p, "creeper_repellent")) {
                for (Entity e : level.getEntities(
                        p, p.getBoundingBox().inflate(
                                CREEPER_DISAPPEAR_DISTANCE))) {

                    if (e instanceof Creeper creeper
                            && creeper.distanceToSqr(p)
                            <= CREEPER_DISAPPEAR_DISTANCE
                            * CREEPER_DISAPPEAR_DISTANCE) {

                        // 白いモヤを出してクリーパーを消滅させる
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(
                                    ParticleTypes.CLOUD,
                                    creeper.getX(),
                                    creeper.getY() + creeper.getBbHeight() * 0.5,
                                    creeper.getZ(),
                                    20,
                                    0.35, 0.45, 0.35,
                                    0.03);
                        }

                        creeper.discard();
                    }
                }
            }

            if (active(p, "phantom_repellent")) {
                for (Entity e : level.getEntities(p, p.getBoundingBox().inflate(12.0D))) {
                    if (e instanceof Phantom phantom) {
                        if (level instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.CLOUD,
                                    phantom.getX(), phantom.getY() + 0.5D, phantom.getZ(),
                                    18, 0.3D, 0.4D, 0.3D, 0.02D);
                        }
                        phantom.discard();
                    }
                }
            }

            if (active(p, "piglin_peace")) {
                for (Entity e : level.getEntities(
                        p, p.getBoundingBox().inflate(8))) {

                    if (e instanceof Piglin piglin
                            && piglin.getTarget() == p) {
                        piglin.setTarget(null);
                    }
                }
            }

            if (active(p, "friendly_aura")) {
                for (Entity e : level.getEntities(
                        p, p.getBoundingBox().inflate(8))) {

                    if (e instanceof Mob mob
                            && !(mob instanceof Monster)
                            && mob.getTarget() == null
                            && mob.distanceToSqr(p) > 4) {
                        mob.getNavigation().moveTo(p, 1.05);
                    }
                }
            }
        }

        // 瞬間転位：接近してくる飛び道具を検知してプレイヤー自身が回避
        if (active(p, "projectile_phase")
                && p instanceof ServerPlayer serverPlayer
                && p.getPersistentData().getInt(
                PROJECTILE_PHASE_COOLDOWN) <= 0) {

            for (Entity e : level.getEntities(
                    p, p.getBoundingBox().inflate(6.0D))) {

                if (!(e instanceof Projectile projectile)
                        || projectile.getOwner() == p
                        || projectile.isRemoved()) {
                    continue;
                }

                double distanceSqr = projectile.distanceToSqr(p);

                if (distanceSqr > PROJECTILE_DODGE_DISTANCE
                        * PROJECTILE_DODGE_DISTANCE) {
                    continue;
                }

                // 飛び道具の進行方向がプレイヤーに向いているか判定
                net.minecraft.world.phys.Vec3 toPlayer =
                        p.position().subtract(projectile.position());

                net.minecraft.world.phys.Vec3 velocity =
                        projectile.getDeltaMovement();

                if (velocity.lengthSqr() < 0.0001D
                        || velocity.dot(toPlayer) <= 0) {
                    continue;
                }

                if (teleportToSafeSpot(serverPlayer, (ServerLevel) level)) {
                    p.getPersistentData().putInt(
                            PROJECTILE_PHASE_COOLDOWN, 10);
                    break;
                }
            }
        }
    }

    /**
     * プレイヤーを現在地の周囲にある安全な場所へテレポートさせる。
     */
    private static boolean teleportToSafeSpot(
            ServerPlayer player,
            ServerLevel level) {

        double oldX = player.getX();
        double oldY = player.getY();
        double oldZ = player.getZ();

        // 回避前の位置にポータル粒子を表示
        level.sendParticles(
                ParticleTypes.PORTAL,
                oldX,
                oldY + player.getBbHeight() * 0.5,
                oldZ,
                24,
                0.35, 0.5, 0.35,
                0.15);

        // 現在地から3～6ブロック離れた場所を探す
        for (int radius = 3; radius <= 6; radius++) {
            for (int angle = 0; angle < 360; angle += 15) {
                double radians = Math.toRadians(angle);

                double targetX = oldX + Math.cos(radians) * radius;
                double targetZ = oldZ + Math.sin(radians) * radius;

                for (int yOffset : new int[]{0, 1, -1, 2}) {
                    double targetY = Math.floor(oldY) + yOffset;

                    BlockPos feetPos = BlockPos.containing(
                            targetX, targetY, targetZ);

                    BlockState feetState = level.getBlockState(feetPos);
                    BlockState headState = level.getBlockState(
                            feetPos.above());
                    BlockState floorState = level.getBlockState(
                            feetPos.below());

                    // 足元と頭上に空間があり、床がある場所だけを使う
                    if (!feetState.isAir()
                            || !headState.isAir()
                            || !floorState.isFaceSturdy(
                            level, feetPos.below(),
                            net.minecraft.core.Direction.UP)) {
                        continue;
                    }

                    double dx = targetX - oldX;
                    double dy = targetY - oldY;
                    double dz = targetZ - oldZ;

                    if (!level.noCollision(
                            player,
                            player.getBoundingBox().move(dx, dy, dz))) {
                        continue;
                    }

                    player.teleportTo(targetX, targetY, targetZ);

                    // テレポート先にも粒子を表示
                    level.sendParticles(
                            ParticleTypes.PORTAL,
                            targetX,
                            targetY + player.getBbHeight() * 0.5,
                            targetZ,
                            24,
                            0.35, 0.5, 0.35,
                            0.15);

                    return true;
                }
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity source = event.getSource().getEntity();

        if (event.getEntity() instanceof Player victim) {
            if (!victim.level().isClientSide && active(victim, "auto_counterattack")
                    && source instanceof net.minecraft.world.entity.LivingEntity attacker
                    && attacker != victim && !processingRetaliation
                    && victim.getRandom().nextFloat() < 0.20F) {
                double attackDamage = victim.getAttributeValue(Attributes.ATTACK_DAMAGE);
                if (attackDamage > 0.0D) {
                    processingRetaliation = true;
                    try {
                        attacker.hurt(victim.damageSources().playerAttack(victim), (float) attackDamage);
                    } finally {
                        processingRetaliation = false;
                    }
                }
            }

            if (active(victim, "fall_negation")
                    && event.getSource().is(DamageTypes.FALL)) {
                event.setAmount(0);
                return;
            }

            // fire_immunity は火炎耐性エフェクトで処理する

            if (active(victim, "explosion_immunity")
                    && event.getSource().is(DamageTypes.EXPLOSION)) {
                event.setAmount(0);
                return;
            }

            if (active(victim, "fatal_negation")
                    && event.getAmount() >= victim.getMaxHealth()) {
                event.setAmount(0);
                return;
            }

            if (active(victim, "damage_dodge")
                    && victim.getRandom().nextFloat() < 0.20F) {
                event.setAmount(0);
                return;
            }
        }

        if (source instanceof Player attacker) {

            if (active(attacker, "undead_slayer")
                    && event.getEntity().getMobType() == MobType.UNDEAD) {
                event.setAmount(event.getAmount() * 2.0F);
            }

            if (active(attacker, "critical_strike")
                    && attacker.getRandom().nextFloat() < 0.20F) {
                event.setAmount(event.getAmount() * 2.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p
                && active(p, "fall_negation")) {
            event.setDistance(0);
            event.setDamageMultiplier(0);
        }
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player p
                && active(p, "knockback_immunity")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (active(event.getEntity(), "mining_speed")) {
            event.setNewSpeed(event.getNewSpeed() * 2.0F);
        }
    }

    // 作物を右クリックしたとき、25%の確率で成長させる
    @SubscribeEvent
    public static void onCropUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()
                || !active(event.getEntity(), "crop_growth")) {
            return;
        }

        if (event.getEntity().getRandom().nextFloat() >= 0.25F) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);

        if (state.getBlock() instanceof CropBlock crop
                && crop.isMaxAge(state)) {
            return;
        }

        if (state.getBlock() instanceof BonemealableBlock growable
                && growable.isValidBonemealTarget(
                serverLevel, pos, state, false)
                && growable.isBonemealSuccess(
                serverLevel, serverLevel.random, pos, state)) {

            growable.performBonemeal(
                    serverLevel, serverLevel.random, pos, state);

            serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5,
                    5,
                    0.25, 0.2, 0.25,
                    0);
        }
    }

    // 植えたとき、25%の確率で成長させる
    @SubscribeEvent
    public static void onPlant(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Player p)
                || !active(p, "growth_on_plant")) {
            return;
        }

        if (p.getRandom().nextFloat() >= 0.25F) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof BonemealableBlock growable
                && growable.isValidBonemealTarget(
                level, pos, state, false)
                && growable.isBonemealSuccess(
                level, level.random, pos, state)) {

            growable.performBonemeal(
                    level, level.random, pos, state);
        }
    }

    // 地面アイテムの取得処理はここに集約し、精錬と独自収納が競合しないようにする。
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player.level() instanceof ServerLevel level)) return;

        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;

        boolean alreadyProcessed = itemEntity.getPersistentData()
                .getBoolean("abilitysgorwmod_auto_smelt_processed");
        boolean autoSmelt = active(player, "auto_smelt");
        boolean autoCook = active(player, "auto_cook_food") && stack.isEdible();
        boolean transformed = false;

        if (!alreadyProcessed && (autoSmelt || autoCook)) {
            ItemStack result = AutoSmeltAbility.smeltOneStep(stack, level);
            transformed = !ItemStack.isSameItemSameTags(result, stack)
                    || result.getCount() != stack.getCount();
            if (transformed) {
                itemEntity.setItem(result);
                itemEntity.getPersistentData().putBoolean(
                        "abilitysgorwmod_auto_smelt_processed", true);
                stack = itemEntity.getItem();
            }
        }

        // 収納への自動回収を先に処理する。満杯の場合は残量を地面に残す。
        if (active(player, "vault_inventory")
                && net.coms.abilitysgorwmod.data.VaultInventoryData.isAutoPickup(player)) {
            int originalCount = stack.getCount();
            ItemStack remainder = net.coms.abilitysgorwmod.data.VaultInventoryData.insertStack(player, stack);
            event.setCanceled(true);
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remainder);
            }
            if (remainder.getCount() != originalCount) {
                net.coms.abilitysgorwmod.data.VaultInventoryData.syncIfServerPlayer(player);
            }
            return;
        }

        // ItemEntity の内部処理はイベント前の ItemStack 参照を保持することがある。
        // 精錬結果が変わった場合は通常インベントリへの追加もここで完結させ、二重回収を防ぐ。
        if (transformed) {
            event.setCanceled(true);
            ItemStack remainder = stack.copy();
            player.getInventory().add(remainder);
            player.getInventory().setChanged();
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remainder);
            }
        }
    }

    // ブロック破壊時に、草からの鉱石抽選と精錬可能ドロップの追加分を生成する。
    // Forge 1.20.1 の BreakEvent はドロップ一覧を持たないため、同じブロック状態・道具でドロップを計算する。
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();

        if (active(player, "grass_mineral")
                && (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN))
                && player.getRandom().nextFloat() < 0.03F) {
            ItemStack[] ores = {
                    new ItemStack(Items.COAL), new ItemStack(Items.RAW_IRON),
                    new ItemStack(Items.RAW_GOLD), new ItemStack(Items.RAW_COPPER),
                    new ItemStack(Items.REDSTONE), new ItemStack(Items.LAPIS_LAZULI),
                    new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD),
                    new ItemStack(Items.QUARTZ)
            };
            spawnExtraDrop(level, pos, ores[player.getRandom().nextInt(ores.length)]);
        }

        if (!active(player, "triple_luck") || player.getAbilities().instabuild) return;
        List<ItemStack> calculatedDrops = Block.getDrops(
                state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem());
        for (ItemStack stack : calculatedDrops) {
            if (stack.isEmpty()) continue;
            SimpleContainer input = new SimpleContainer(1);
            input.setItem(0, stack.copyWithCount(1));
            if (level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).isEmpty()) continue;
            // Vanilla が1回分を落とすので、追加で2回分を落として合計3倍にする。
            spawnExtraDrop(level, pos, stack.copy());
            spawnExtraDrop(level, pos, stack.copy());
        }
    }

    private static void spawnExtraDrop(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity item = new ItemEntity(level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    // モブドロップ25%倍増。
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
                || player.level().isClientSide || !active(player, "double_mob_drops")
                || player.getRandom().nextFloat() >= 0.25F) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            ItemEntity extra = new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), stack.copy());
            extra.setDefaultPickUpDelay();
            drop.level().addFreshEntity(extra);
        }
    }

    // 自動反撃とは独立した、命中時の回復能力。
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player player
                && active(player, "lifesteal") && event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            player.heal(2.0F);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player, "death_explosion")) {
            player.level().explode(player, player.getX(), player.getY(), player.getZ(),
                    6.0F, Level.ExplosionInteraction.MOB);
        }
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || !active(player, "random_food_effect") || !event.getItem().isEdible()) return;
        MobEffectInstance[] effects = {
                new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 0),
                new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 0),
                new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 0),
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0),
                new MobEffectInstance(MobEffects.JUMP, 20 * 30, 0),
                new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60, 0),
                new MobEffectInstance(MobEffects.ABSORPTION, 20 * 45, 0)
        };
        player.addEffect(new MobEffectInstance(effects[player.getRandom().nextInt(effects.length)]));
    }

    @SubscribeEvent
    public static void onFurnaceOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !active(player, "auto_furnace_fuel")
                || !(event.getContainer() instanceof FurnaceMenu menu) || !menu.getSlot(1).getItem().isEmpty()) return;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(Items.COAL)) {
                stack.shrink(1);
                menu.getSlot(1).set(new ItemStack(Items.COAL));
                menu.broadcastChanges();
                break;
            }
        }
    }

    // 村人の好感度を上げ、取引価格の割引を得る。
    @SubscribeEvent
    public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getTarget() instanceof Villager villager)
                || !active(event.getEntity(), "villager_discount")) return;
        Player player = event.getEntity();
        String appliedKey = "abilitysgorwmod_discount_" + player.getUUID();
        // 取引画面を開いたり村人を何度もクリックしたりしても、同じ相手への割引を積み増さない。
        if (villager.getPersistentData().getBoolean(appliedKey)) {
            return;
        }
        villager.getGossips().add(player.getUUID(), GossipType.MAJOR_POSITIVE, 25);
        villager.getPersistentData().putBoolean(appliedKey, true);
    }

    // 釣りの宝物判定を補助
    @SubscribeEvent
    public static void onFishing(ItemFishedEvent event) {
        Player player = event.getEntity();

        if (!active(player, "treasure_fishing")) {
            return;
        }

        BuffEffectHelper.apply(player, MobEffects.LUCK, 100, 1);
    }

    // エンチャントのレベル消費を25%の確率で無効化
    @SubscribeEvent
    public static void onExperienceLevelChange(
            PlayerXpEvent.LevelChange event) {

        Player player = event.getEntity();

        if (event.getLevels() < 0
                && active(player, "free_enchant")
                && player.getRandom().nextFloat() < 0.25F) {
            event.setLevels(0);
        }
    }


}
