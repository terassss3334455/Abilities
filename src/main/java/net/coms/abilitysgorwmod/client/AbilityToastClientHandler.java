package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class AbilityToastClientHandler {

    private AbilityToastClientHandler() {}

    public static void show(AbilityDefinition ability) {
        if (ability == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        SystemToast.add(
                minecraft.getToasts(),
                SystemToast.SystemToastIds.TUTORIAL_HINT,
                Component.translatable(
                        "toast.abilitysgorwmod.acquired"
                ),
                Component.translatable(
                        "toast.abilitysgorwmod.acquired.detail",
                        ability.getNameComponent(),
                        ability.getRarity().getDisplayName()
                )
        );
    }

    /**
     * 既存コードとの互換性を保つためのメソッド。
     * abilityName が登録済みの能力名なら翻訳表示する。
     */
    public static void show(String abilityName, String rarityName) {
        for (AbilityDefinition ability : AbilityRegistry.getAll()) {
            if (ability.getId().equals(abilityName)
                    || ability.getName().equals(abilityName)) {
                show(ability);
                return;
            }
        }

        Minecraft minecraft = Minecraft.getInstance();

        SystemToast.add(
                minecraft.getToasts(),
                SystemToast.SystemToastIds.TUTORIAL_HINT,
                Component.translatable(
                        "toast.abilitysgorwmod.acquired"
                ),
                Component.literal(
                        abilityName + " [" + rarityName + "]"
                )
        );
    }
}