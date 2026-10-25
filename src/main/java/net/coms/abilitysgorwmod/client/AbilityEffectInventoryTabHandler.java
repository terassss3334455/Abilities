package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = abilitysgorwmod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AbilityEffectInventoryTabHandler {
    private AbilityEffectInventoryTabHandler() {}

    @SubscribeEvent
    public static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory)) return;
        event.addListener(Button.builder(
                Component.translatable("screen.abilitysgorwmod.effect_status.inventory_button"),
                button -> net.minecraft.client.Minecraft.getInstance()
                        .setScreen(new AbilityEffectStatusScreen(inventory))
        ).bounds(inventory.getGuiLeft() + 132, inventory.getGuiTop() + 2, 40, 14).build());
    }
}