package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.coms.abilitysgorwmod.network.VaultActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class VaultScreen extends Screen {
    private int page;
    private int tab; // 0=収納、1=強化
    private boolean sortedByName;
    private EditBox searchBox;
    private long lastShiftClickTime;
    private int lastShiftClickSlot = -1;
    private int lastShiftClickSide = -1; // 0=vault, 1=player inventory
    private ItemStack lastShiftClickStack = ItemStack.EMPTY;
    private static final long DOUBLE_SHIFT_CLICK_MILLIS = 300L;
    private static final int GUI_WIDTH = 350;
    private static final int GUI_HEIGHT = 230;
    private static final int SLOT = 18;

    public VaultScreen() {
        super(Component.literal("無限収納"));
    }

    @Override
    protected void init() {
        int left = (width - GUI_WIDTH) / 2;
        int top = (height - GUI_HEIGHT) / 2;
        searchBox = new EditBox(font, left + 8, top + 35, 180, 14,
                Component.literal("収納内を検索"));
        searchBox.setHint(Component.literal("アイテム名で検索"));
        searchBox.setMaxLength(100);
        searchBox.setResponder(value -> page = 0);
        addRenderableWidget(searchBox);
    }

    private List<Integer> getVisibleStorageIndices() {
        List<Integer> indices = new ArrayList<>();
        int unlocked = Math.max(0, VaultClientSync.getUnlockedSlots());
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < unlocked; i++) {
            ItemStack stack = VaultClientSync.getStorage(i);
            if (!query.isEmpty()) {
                if (stack.isEmpty() || !stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    continue;
                }
            }
            indices.add(i);
        }
        if (sortedByName) {
            indices.sort(Comparator.comparing(i -> {
                ItemStack stack = VaultClientSync.getStorage(i);
                return stack.isEmpty() ? "\uffff" : stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            }));
        }
        return indices;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = (width - GUI_WIDTH) / 2;
        int top = (height - GUI_HEIGHT) / 2;
        graphics.fill(left, top, left + GUI_WIDTH, top + GUI_HEIGHT, 0xEE20242A);
        graphics.drawCenteredString(font, "無限収納", width / 2, top + 5, 0xFFFFFF);

        drawTab(graphics, left + 8, top + 18, left + 62, top + 33, "収納", tab == 0);
        drawTab(graphics, left + 66, top + 18, left + 120, top + 33, "強化", tab == 1);
        if (searchBox != null) searchBox.visible = tab == 0;

        if (tab == 0) {
            List<Integer> visibleIndices = getVisibleStorageIndices();
            int pageCount = Math.max(1, (visibleIndices.size() + 26) / 27);
            page = Math.min(page, pageCount - 1);
            graphics.drawString(font, "ページ " + (page + 1) + "/" + pageCount, left + 130, top + 22, 0xFFFFFF);
            drawButton(graphics, left + 198, top + 36, left + 292, top + 51, sortedByName ? "名前順: ON" : "名前順: OFF");
            for (int i = 0; i < 27; i++) {
                int x = left + 8 + (i % 9) * SLOT;
                int y = top + 54 + (i / 9) * SLOT;
                drawSlot(graphics, x, y);
                int viewIndex = page * 27 + i;
                if (viewIndex < visibleIndices.size()) {
                    drawStack(graphics, VaultClientSync.getStorage(visibleIndices.get(viewIndex)), x, y);
                }
            }
            drawButton(graphics, left + 296, top + 72, left + 316, top + 92, "◀");
            drawButton(graphics, left + 320, top + 72, left + 340, top + 92, "▶");
        } else {
            graphics.drawString(font, "チェスト1個につき27枠増加（最大10個）", left + 8, top + 38, 0xFFFFFF);
            for (int i = 0; i < 10; i++) {
                int x = left + 8 + (i % 5) * SLOT;
                int y = top + 50 + (i / 5) * SLOT;
                drawSlot(graphics, x, y);
                drawStack(graphics, VaultClientSync.getUpgrade(i), x, y);
            }
            graphics.drawString(font, "挿入したチェストは取り出せません", left + 110, top + 54, 0xFFDD88);
        }

        int autoX = left + 198, autoY = top + 72;
        graphics.fill(autoX, autoY, autoX + 94, autoY + 20, VaultClientSync.isAutoPickup() ? 0xFF286B42 : 0xFF5B5B5B);
        graphics.drawCenteredString(font, "自動回収 " + (VaultClientSync.isAutoPickup() ? "ON" : "OFF"), autoX + 47, autoY + 6, 0xFFFFFF);

        graphics.drawString(font, "プレイヤーインベントリ", left + 8, top + 114, 0xFFFFFF);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = left + 8 + col * SLOT;
                int y = top + 126 + row * SLOT;
                drawSlot(graphics, x, y);
                int invIndex = 9 + row * 9 + col;
                drawStack(graphics, minecraft.player.getInventory().getItem(invIndex), x, y);
            }
        }
        for (int col = 0; col < 9; col++) {
            int x = left + 8 + col * SLOT;
            int y = top + 184;
            drawSlot(graphics, x, y);
            drawStack(graphics, minecraft.player.getInventory().getItem(col), x, y);
        }

        ItemStack carried = minecraft.player.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
        }

        ItemStack hovered = getHoveredStack(mouseX, mouseY, left, top);
        if (!hovered.isEmpty()) graphics.renderTooltip(font, hovered, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTab(GuiGraphics graphics, int x1, int y1, int x2, int y2, String label, boolean selected) {
        graphics.fill(x1, y1, x2, y2, selected ? 0xFF416A8A : 0xFF454545);
        graphics.drawCenteredString(font, label, (x1 + x2) / 2, y1 + 3, 0xFFFFFF);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF777777);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF252525);
    }

    private void drawStack(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    private void drawButton(GuiGraphics graphics, int x1, int y1, int x2, int y2, String text) {
        graphics.fill(x1, y1, x2, y2, 0xFF555555);
        graphics.drawCenteredString(font, text, (x1 + x2) / 2, y1 + 6, 0xFFFFFF);
    }

    private ItemStack getHoveredStack(double mx, double my, int left, int top) {
        if (tab == 0) {
            int slot = slotAt(mx, my, left + 8, top + 54, 9, 3);
            if (slot >= 0) {
                List<Integer> visible = getVisibleStorageIndices();
                int viewIndex = page * 27 + slot;
                return viewIndex < visible.size() ? VaultClientSync.getStorage(visible.get(viewIndex)) : ItemStack.EMPTY;
            }
        } else {
            int slot = slotAt(mx, my, left + 8, top + 50, 5, 2);
            if (slot >= 0) return VaultClientSync.getUpgrade(slot);
        }
        int main = slotAt(mx, my, left + 8, top + 126, 9, 3);
        if (main >= 0) return minecraft.player.getInventory().getItem(9 + main);
        int hotbar = slotAt(mx, my, left + 8, top + 184, 9, 1);
        if (hotbar >= 0) return minecraft.player.getInventory().getItem(hotbar);
        return ItemStack.EMPTY;
    }

    private int slotAt(double mx, double my, int x, int y, int columns, int rows) {
        if (mx < x || my < y || mx >= x + columns * SLOT || my >= y + rows * SLOT) return -1;
        int col = (int) ((mx - x) / SLOT);
        int row = (int) ((my - y) / SLOT);
        return row * columns + col;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return super.mouseClicked(mouseX, mouseY, button);
        int left = (width - GUI_WIDTH) / 2;
        int top = (height - GUI_HEIGHT) / 2;

        if (mouseY >= top + 18 && mouseY <= top + 33) {
            if (mouseX >= left + 8 && mouseX <= left + 62) { tab = 0; return true; }
            if (mouseX >= left + 66 && mouseX <= left + 120) { tab = 1; return true; }
        }

        if (tab == 0) {
            int vaultSlot = slotAt(mouseX, mouseY, left + 8, top + 54, 9, 3);
            if (vaultSlot >= 0) {
                List<Integer> visible = getVisibleStorageIndices();
                int viewIndex = page * 27 + vaultSlot;
                if (viewIndex < visible.size()) {
                    int actualSlot = visible.get(viewIndex);
                    if (hasShiftDown()) {
                        ItemStack clicked = VaultClientSync.getStorage(actualSlot).copy();
                        if (isMatchingShiftDoubleClick(0, actualSlot, clicked)) {
                            AbilityNetwork.sendToServer(new VaultActionPacket(4, actualSlot, button, lastShiftClickStack));
                            clearShiftClick();
                        } else {
                            AbilityNetwork.sendToServer(new VaultActionPacket(6, actualSlot, button));
                            rememberShiftClick(0, actualSlot, clicked);
                        }
                    } else {
                        clearShiftClick();
                        AbilityNetwork.sendToServer(new VaultActionPacket(0, actualSlot, button));
                    }
                }
                return true;
            }
            if (mouseY >= top + 36 && mouseY <= top + 51 && mouseX >= left + 198 && mouseX <= left + 292) {
                sortedByName = !sortedByName;
                page = 0;
                return true;
            }
            if (mouseY >= top + 72 && mouseY <= top + 92 && mouseX >= left + 296 && mouseX <= left + 316) {
                page = Math.max(0, page - 1);
                return true;
            }
            if (mouseY >= top + 72 && mouseY <= top + 92 && mouseX >= left + 320 && mouseX <= left + 340) {
                int maxPage = Math.max(0, (getVisibleStorageIndices().size() - 1) / 27);
                page = Math.min(maxPage, page + 1);
                return true;
            }
        } else {
            int upgradeSlot = slotAt(mouseX, mouseY, left + 8, top + 50, 5, 2);
            if (upgradeSlot >= 0) {
                AbilityNetwork.sendToServer(new VaultActionPacket(2, upgradeSlot, button));
                return true;
            }
        }

        if (mouseX >= left + 198 && mouseX <= left + 292 && mouseY >= top + 72 && mouseY <= top + 92) {
            AbilityNetwork.sendToServer(new VaultActionPacket(3, 0, button));
            return true;
        }

        int main = slotAt(mouseX, mouseY, left + 8, top + 126, 9, 3);
        if (main >= 0) {
            int slot = 9 + main;
            if (hasShiftDown()) {
                ItemStack clicked = minecraft.player.getInventory().getItem(slot).copy();
                if (isMatchingShiftDoubleClick(1, slot, clicked)) {
                    AbilityNetwork.sendToServer(new VaultActionPacket(5, slot, button, lastShiftClickStack));
                    clearShiftClick();
                } else {
                    AbilityNetwork.sendToServer(new VaultActionPacket(7, slot, button));
                    rememberShiftClick(1, slot, clicked);
                }
            } else {
                clearShiftClick();
                AbilityNetwork.sendToServer(new VaultActionPacket(1, slot, button));
            }
            return true;
        }
        int hotbar = slotAt(mouseX, mouseY, left + 8, top + 184, 9, 1);
        if (hotbar >= 0) {
            if (hasShiftDown()) {
                ItemStack clicked = minecraft.player.getInventory().getItem(hotbar).copy();
                if (isMatchingShiftDoubleClick(1, hotbar, clicked)) {
                    AbilityNetwork.sendToServer(new VaultActionPacket(5, hotbar, button, lastShiftClickStack));
                    clearShiftClick();
                } else {
                    AbilityNetwork.sendToServer(new VaultActionPacket(7, hotbar, button));
                    rememberShiftClick(1, hotbar, clicked);
                }
            } else {
                clearShiftClick();
                AbilityNetwork.sendToServer(new VaultActionPacket(1, hotbar, button));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMatchingShiftDoubleClick(int side, int slot, ItemStack clicked) {
        long now = System.currentTimeMillis();
        return !lastShiftClickStack.isEmpty()
                && lastShiftClickSide == side
                && lastShiftClickSlot == slot
                && now - lastShiftClickTime <= DOUBLE_SHIFT_CLICK_MILLIS
                && (clicked.isEmpty() || ItemStack.isSameItemSameTags(lastShiftClickStack, clicked));
    }

    private void rememberShiftClick(int side, int slot, ItemStack stack) {
        lastShiftClickTime = System.currentTimeMillis();
        lastShiftClickSide = side;
        lastShiftClickSlot = slot;
        lastShiftClickStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private void clearShiftClick() {
        lastShiftClickTime = 0L;
        lastShiftClickSide = -1;
        lastShiftClickSlot = -1;
        lastShiftClickStack = ItemStack.EMPTY;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
