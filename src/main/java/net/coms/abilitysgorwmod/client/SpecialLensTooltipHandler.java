package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = abilitysgorwmod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
public final class SpecialLensTooltipHandler {

    private SpecialLensTooltipHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(
                abilitysgorwmod.SPECIAL_LENS.get()
        )) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        event.getToolTip().add(
                Component.literal("所持能力")
                        .withStyle(ChatFormatting.GOLD)
        );

        for (String id : PlayerAbilityData.getOwnedIds(minecraft.player)) {
            AbilityDefinition ability = AbilityRegistry.get(id);

            if (ability == null) {
                continue;
            }

            boolean active = PlayerAbilityData.isActive(
                    minecraft.player,
                    id
            );
            if (!active) {
                continue;
            }

            event.getToolTip().add(
                    Component.literal(
                            ability.getName() + " [ON]"
                    ).withStyle(ChatFormatting.GREEN)
            );

            event.getToolTip().add(
                    Component.literal(ability.getDescription())
                            .withStyle(ChatFormatting.GRAY)
            );
        }
    }
}