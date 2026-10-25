package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class AbilityClientSync {

    private AbilityClientSync() {
    }

    public static void apply(CompoundTag data) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player != null) {
            PlayerAbilityData.applySyncData(
                    minecraft.player,
                    data
            );
        }
    }
}