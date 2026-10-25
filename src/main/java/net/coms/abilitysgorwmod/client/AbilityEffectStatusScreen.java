package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.coms.abilitysgorwmod.network.AbilityEffectStatsRequestPacket;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AbilityEffectStatusScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 330;
    private static final int ROW_HEIGHT = 34;

    private final Screen parent;
    private int tab;
    private int scrollOffset;

    public AbilityEffectStatusScreen(Screen parent) {
        super(Component.translatable("screen.abilitysgorwmod.effect_status.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AbilityEffectStatsClientData.beginRequest();
        AbilityNetwork.sendToServer(new AbilityEffectStatsRequestPacket());

        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight()) / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("screen.abilitysgorwmod.effect_status.tab_chance"),
                button -> { tab = 0; scrollOffset = 0; }
        ).bounds(left + 12, top + 32, 118, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.abilitysgorwmod.effect_status.tab_effects"),
                button -> { tab = 1; scrollOffset = 0; }
        ).bounds(left + 134, top + 32, 118, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.abilitysgorwmod.effect_status.refresh"),
                button -> {
                    AbilityEffectStatsClientData.beginRequest();
                    AbilityNetwork.sendToServer(new AbilityEffectStatsRequestPacket());
                }
        ).bounds(left + panelWidth - 62, top + 32, 50, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.abilitysgorwmod.effect_status.back"),
                button -> onClose()
        ).bounds(left + panelWidth - 62, top + PANEL_HEIGHT - 25, 50, 18).build());
    }

    private int panelWidth() {
        return Math.min(PANEL_WIDTH, Math.max(260, width - 16));
    }

    private int panelHeight() {
        return Math.min(PANEL_HEIGHT, Math.max(210, height - 16));
    }

    private List<MobEffectInstance> getBeneficialEffects() {
        if (minecraft == null || minecraft.player == null) return List.of();
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (effect.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) effects.add(effect);
        }
        effects.sort(Comparator.comparing(effect ->
                effect.getEffect().getDisplayName().getString().toLowerCase(Locale.ROOT)));
        return effects;
    }

    private int visibleRows(int panelHeight) {
        return Math.max(1, (panelHeight - 126) / ROW_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0202020);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);

        int listTop = top + 76;
        int rowsToShow = visibleRows(panelHeight);
        int listBottom = top + panelHeight - 36;

        if (tab == 0) {
            graphics.drawString(font,
                    Component.translatable("screen.abilitysgorwmod.effect_status.chance_note"),
                    left + 12, top + 59, 0xFFCCCCCC);

            if (!AbilityEffectStatsClientData.isLoaded()) {
                graphics.drawCenteredString(font,
                        Component.translatable("screen.abilitysgorwmod.effect_status.loading"),
                        width / 2, listTop + 24, 0xFFCCCCCC);
            } else {
                List<AbilityEffectStatsClientData.ChanceRow> rows = AbilityEffectStatsClientData.getChances();
                if (rows.isEmpty()) {
                    graphics.drawCenteredString(font,
                            Component.translatable("screen.abilitysgorwmod.effect_status.no_chances"),
                            width / 2, listTop + 24, 0xFFCCCCCC);
                }
                for (int i = 0; i < rowsToShow; i++) {
                    int index = scrollOffset + i;
                    if (index >= rows.size()) break;
                    AbilityEffectStatsClientData.ChanceRow row = rows.get(index);
                    int rowTop = listTop + i * ROW_HEIGHT;
                    if (rowTop + ROW_HEIGHT > listBottom) break;

                    graphics.fill(left + 10, rowTop, left + panelWidth - 10,
                            rowTop + ROW_HEIGHT - 2, 0xFF343434);
                    AbilityDefinition definition = AbilityRegistry.get(row.abilityId());
                    Component name = definition == null
                            ? Component.literal(row.abilityId())
                            : definition.getNameComponent();
                    graphics.drawString(font, name, left + 17, rowTop + 4, 0xFFFFFFFF);
                    graphics.drawString(font,
                            Component.translatable(row.active()
                                    ? "screen.abilitysgorwmod.effect_status.active"
                                    : "screen.abilitysgorwmod.effect_status.inactive"),
                            left + panelWidth - 56, rowTop + 4, row.active() ? 0xFF66DD88 : 0xFFAAAAAA);

                    String base = formatPercent(row.baseChance());
                    String effective = formatPercent(row.effectiveChance());
                    String delta = String.format(Locale.ROOT, "%+.0f", (row.effectiveChance() - row.baseChance()) * 100.0F);
                    graphics.drawString(font,
                            Component.translatable("screen.abilitysgorwmod.effect_status.chance_summary",
                                    base, effective, delta),
                            left + 17, rowTop + 19, 0xFFFFD875);
                }
            }
        } else {
            graphics.drawString(font,
                    Component.translatable("screen.abilitysgorwmod.effect_status.effects_note"),
                    left + 12, top + 59, 0xFFCCCCCC);
            List<MobEffectInstance> effects = getBeneficialEffects();
            if (effects.isEmpty()) {
                graphics.drawCenteredString(font,
                        Component.translatable("screen.abilitysgorwmod.effect_status.no_effects"),
                        width / 2, listTop + 24, 0xFFCCCCCC);
            }
            for (int i = 0; i < rowsToShow; i++) {
                int index = scrollOffset + i;
                if (index >= effects.size()) break;
                int rowTop = listTop + i * ROW_HEIGHT;
                if (rowTop + ROW_HEIGHT > listBottom) break;
                MobEffectInstance effect = effects.get(index);
                graphics.fill(left + 10, rowTop, left + panelWidth - 10,
                        rowTop + ROW_HEIGHT - 2, 0xFF343434);
                graphics.drawString(font,
                        Component.translatable("screen.abilitysgorwmod.effect_status.effect_level",
                                effect.getEffect().getDisplayName(), effect.getAmplifier() + 1),
                        left + 17, rowTop + 4, 0xFFFFFFFF);
                graphics.drawString(font,
                        Component.translatable("screen.abilitysgorwmod.effect_status.effect_duration",
                                formatDuration(effect.getDuration())),
                        left + 17, rowTop + 19, 0xFFB8E5B8);
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String formatPercent(float value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0F);
    }

    private static String formatDuration(int ticks) {
        if (ticks < 0) return "∞";
        int seconds = Math.max(0, ticks / 20);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int count = tab == 0
                ? AbilityEffectStatsClientData.getChances().size()
                : getBeneficialEffects().size();
        int maxOffset = Math.max(0, count - visibleRows(panelHeight()));
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null && parent != null) minecraft.setScreen(parent);
        else super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
