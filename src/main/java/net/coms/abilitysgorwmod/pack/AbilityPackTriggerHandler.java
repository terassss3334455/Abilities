package net.coms.abilitysgorwmod.pack;

import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = abilitysgorwmod.MODID)
public final class AbilityPackTriggerHandler {
    private AbilityPackTriggerHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity attacker = event.getSource().getEntity();
        if (event.getEntity() instanceof Player victim) {
            AbilityTriggerRules.fire(victim, "on_hurt",
                    new AbilityTriggerRules.Context(event.getEntity(), attacker, event, ItemStack.EMPTY));
        }
        if (attacker instanceof Player player) {
            AbilityTriggerRules.fire(player, "on_attack",
                    new AbilityTriggerRules.Context(event.getEntity(), attacker, event, ItemStack.EMPTY));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Player player) {
            AbilityTriggerRules.fire(player, "on_kill",
                    new AbilityTriggerRules.Context(event.getEntity(), attacker, null, ItemStack.EMPTY));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        AbilityTriggerRules.fire(event.player, "on_tick",
                new AbilityTriggerRules.Context(event.player, null, null, ItemStack.EMPTY));
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) return;
        AbilityTriggerRules.fire(player, "on_jump",
                new AbilityTriggerRules.Context(player, null, null, ItemStack.EMPTY));
    }

    @SubscribeEvent
    public static void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem();
        AbilityTriggerRules.fire(player, "on_item_use",
                new AbilityTriggerRules.Context(player, null, null, stack));
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        ItemStack stack = event.getItem().getItem();
        AbilityTriggerRules.fire(player, "on_pickup",
                new AbilityTriggerRules.Context(event.getItem(), null, null, stack));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) return;
        AbilityTriggerRules.fire(player, "on_block_break",
                new AbilityTriggerRules.Context(null, null, null, ItemStack.EMPTY));
    }
}