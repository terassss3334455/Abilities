package net.coms.abilitysgorwmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class AbilityToastClientHandler {
    private AbilityToastClientHandler() {}

    public static void show(String abilityName, String rarityName) {
        Minecraft minecraft = Minecraft.getInstance();
        SystemToast.add(minecraft.getToasts(), SystemToast.SystemToastIds.TUTORIAL_HINT,
                Component.literal("能力を初めて獲得！"),
                Component.literal(abilityName + "  [" + rarityName + "]"));
    }
}
