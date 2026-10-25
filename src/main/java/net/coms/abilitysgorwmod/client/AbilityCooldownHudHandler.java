package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = abilitysgorwmod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AbilityCooldownHudHandler {
    private static final int MAX_ROWS = 4;
    private static final int BAR_WIDTH = 86;
    private static final int ROW_HEIGHT = 17;

    private AbilityCooldownHudHandler() {}

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.ARMOR_LEVEL.type()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        List<AbilityCooldownHudData.CooldownRow> rows = AbilityCooldownHudData.getRows();
        if (rows.isEmpty()) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int guiWidth = event.getWindow().getGuiScaledWidth();
        int guiHeight = event.getWindow().getGuiScaledHeight();
        int left = guiWidth / 2 - 91;
        int firstTop = guiHeight - 73;
        int count = Math.min(rows.size(), MAX_ROWS);

        for (int i = 0; i < count; i++) {
            AbilityCooldownHudData.CooldownRow row = rows.get(i);
            int top = firstTop - i * ROW_HEIGHT;
            int remaining = row.remainingTicks();
            String name = abilityName(row.abilityId());
            String time = remaining == 0
                    ? Component.translatable("hud.abilitysgorwmod.cooldown.ready").getString()
                    : Component.translatable("hud.abilitysgorwmod.cooldown.seconds",
                            (remaining + 19) / 20).getString();

            graphics.fill(left, top, left + BAR_WIDTH, top + 15, 0xA0000000);
            graphics.drawString(minecraft.font, name, left + 2, top + 1, 0xFFFFFFFF, true);
            graphics.drawString(minecraft.font, time, left + BAR_WIDTH - 2
                    - minecraft.font.width(time), top + 1, 0xFFFFFFFF, true);

            int barLeft = left + 2;
            int barTop = top + 10;
            int barWidth = BAR_WIDTH - 4;
            graphics.fill(barLeft, barTop, barLeft + barWidth, barTop + 3, 0xFF292929);
            float progress = 1.0F - Math.min(1.0F, remaining / (float) row.totalTicks());
            int filled = Math.round(barWidth * progress);
            if (filled > 0) graphics.fill(barLeft, barTop, barLeft + filled, barTop + 3,
                    remaining == 0 ? 0xFF55DD77 : 0xFF42B9FF);
        }
    }

    private static String abilityName(String abilityId) {
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        return definition == null ? abilityId : definition.getNameComponent().getString();
    }
}