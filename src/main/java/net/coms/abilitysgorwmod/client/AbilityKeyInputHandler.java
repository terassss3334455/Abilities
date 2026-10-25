package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.coms.abilitysgorwmod.network.DimensionShiftPacket;
import net.coms.abilitysgorwmod.network.OpenVaultPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = abilitysgorwmod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
public final class AbilityKeyInputHandler {

    private AbilityKeyInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        while (AbilityScreenKeyHandler.OPEN_ABILITY_GUI.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new AbilityScreen());
            }
        }
        while (AbilityScreenKeyHandler.DIMENSION_SHIFT.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                // The server validates ownership and ON state. Do not gate this on
                // client persistent data, which can be stale after dimension changes.
                AbilityNetwork.sendToServer(new DimensionShiftPacket());
            }
        }
        while (AbilityScreenKeyHandler.OPEN_VAULT.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null
                    && PlayerAbilityData.isActive(minecraft.player, "vault_inventory")) {
                AbilityNetwork.sendToServer(new OpenVaultPacket());
            }
        }
    }
}