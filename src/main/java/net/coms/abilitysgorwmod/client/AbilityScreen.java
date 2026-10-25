package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.api.AbilityApi;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.coms.abilitysgorwmod.network.AbilityRequestSyncPacket;
import net.coms.abilitysgorwmod.network.AbilityTogglePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AbilityScreen extends Screen {

    private int scrollOffset = 0;
    private EditBox searchBox;
    private Button sortButton;
    private boolean sortByRarity;
    private boolean sortDescending;

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 282;
    private static final int ROW_HEIGHT = 42;
    private static final int VISIBLE_ROWS = 4;

    public AbilityScreen() {
        super(Component.translatable("screen.abilitysgorwmod.abilities"));
    }

    private static Component abilityName(AbilityDefinition ability) {
        return ability.getNameComponent();
    }

    private static Component abilityDescription(AbilityDefinition ability) {
        return ability.getDescriptionComponent();
    }
    private static Component rarityName(AbilityDefinition.Rarity rarity) {
        return rarity.getDisplayName();
    }

    @Override
    protected void init() {
        AbilityNetwork.sendToServer(new AbilityRequestSyncPacket());

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        searchBox = new EditBox(
                font,
                left + 8,
                top + 29,
                176,
                16,
                Component.translatable("screen.abilitysgorwmod.search")
        );

        searchBox.setHint(
                Component.translatable("screen.abilitysgorwmod.search_hint")
        );
        searchBox.setMaxLength(100);
        searchBox.setResponder(value -> scrollOffset = 0);
        addRenderableWidget(searchBox);

        sortButton = Button.builder(
                Component.translatable(getSortKey()),
                button -> {
                    if (!sortByRarity && !sortDescending) {
                        sortDescending = true;
                    } else if (!sortByRarity) {
                        sortByRarity = true;
                        sortDescending = false;
                    } else if (!sortDescending) {
                        sortDescending = true;
                    } else {
                        sortByRarity = false;
                        sortDescending = false;
                    }

                    scrollOffset = 0;
                    button.setMessage(Component.translatable(getSortKey()));
                }
        ).bounds(left + 190, top + 29, 102, 16).build();

        addRenderableWidget(sortButton);

        addRenderableWidget(Button.builder(Component.translatable("screen.abilitysgorwmod.dancing_settings"), button ->
                minecraft.setScreen(new DancingAnimationScreen(this)))
                .bounds(left + 8, top + PANEL_HEIGHT - 62, 120, 20).build());
    }

    private List<AbilityDefinition> getOwnedAbilities() {
        List<AbilityDefinition> result = new ArrayList<>();

        if (minecraft == null || minecraft.player == null) {
            return result;
        }

        String query = searchBox == null
                ? ""
                : searchBox.getValue().trim().toLowerCase(Locale.ROOT);

        for (String id : PlayerAbilityData.getOwnedIds(minecraft.player)) {
            AbilityDefinition definition = AbilityRegistry.get(id);

            if (definition == null) {
                continue;
            }

            String name = abilityName(definition).getString()
                    .toLowerCase(Locale.ROOT);
            String description = abilityDescription(definition).getString()
                    .toLowerCase(Locale.ROOT);

            if (!query.isEmpty()
                    && !name.contains(query)
                    && !description.contains(query)) {
                continue;
            }

            result.add(definition);
        }

        Comparator<AbilityDefinition> nameComparator =
                Comparator.comparing(
                        ability -> abilityName(ability).getString()
                                .toLowerCase(Locale.ROOT)
                );

        if (sortByRarity) {
            Comparator<AbilityDefinition> rarityComparator =
                    Comparator.comparingInt(
                            ability -> ability.getRarity().ordinal()
                    );

            if (sortDescending) {
                rarityComparator = rarityComparator.reversed();
            }

            result.sort(rarityComparator.thenComparing(nameComparator));
        } else {
            result.sort(
                    sortDescending
                            ? nameComparator.reversed()
                            : nameComparator
            );
        }

        return result;
    }

    private String getSortKey() {
        if (sortByRarity) {
            return sortDescending
                    ? "screen.abilitysgorwmod.sort.rarity_desc"
                    : "screen.abilitysgorwmod.sort.rarity_asc";
        }

        return sortDescending
                ? "screen.abilitysgorwmod.sort.name_desc"
                : "screen.abilitysgorwmod.sort.name_asc";
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics);

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        graphics.fill(
                left, top,
                left + PANEL_WIDTH, top + PANEL_HEIGHT,
                0xEE202020
        );

        graphics.drawCenteredString(
                font,
                Component.translatable("screen.abilitysgorwmod.abilities"),
                width / 2,
                top + 10,
                0xFFFFFF
        );

        List<AbilityDefinition> abilities = getOwnedAbilities();

        if (abilities.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            "screen.abilitysgorwmod.no_abilities"
                    ),
                    width / 2,
                    top + 60,
                    0xAAAAAA
            );
        }

        int listTop = top + 50;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = scrollOffset + i;

            if (index >= abilities.size()) {
                break;
            }

            AbilityDefinition ability = abilities.get(index);
            int rowTop = listTop + i * ROW_HEIGHT;

            boolean active = AbilityApi.isAbilityEnabled(minecraft.player, ability.getId());

            graphics.fill(
                    left + 8,
                    rowTop,
                    left + PANEL_WIDTH - 8,
                    rowTop + ROW_HEIGHT - 3,
                    0xFF353535
            );

            graphics.drawString(
                    font,
                    abilityName(ability),
                    left + 16,
                    rowTop + 5,
                    0xFFFFFF
            );

            int rarityColor = ability.getRarity().getColor();

            graphics.drawString(
                    font,
                    rarityName(ability.getRarity()),
                    left + 16,
                    rowTop + 20,
                    rarityColor
            );

            String description = abilityDescription(ability).getString();

            if (description.length() > 22) {
                description = description.substring(0, 22) + "...";
            }

            graphics.drawString(
                    font,
                    description,
                    left + 88,
                    rowTop + 20,
                    0xFFBBBBBB
            );

            int buttonLeft = left + PANEL_WIDTH - 74;
            int buttonRight = left + PANEL_WIDTH - 14;

            graphics.fill(
                    buttonLeft,
                    rowTop + 8,
                    buttonRight,
                    rowTop + 29,
                    active ? 0xFF287A42 : 0xFF666666
            );

            graphics.drawCenteredString(
                    font,
                    Component.translatable(
                            active
                                    ? "screen.abilitysgorwmod.on"
                                    : "screen.abilitysgorwmod.off"
                    ),
                    (buttonLeft + buttonRight) / 2,
                    rowTop + 14,
                    0xFFFFFF
            );
        }

        graphics.drawCenteredString(
                font,
                Component.translatable(
                        "screen.abilitysgorwmod.controls"
                ),
                width / 2,
                top + PANEL_HEIGHT - 15,
                0xAAAAAA
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            int left = (width - PANEL_WIDTH) / 2;
            int top = (height - PANEL_HEIGHT) / 2;
            int listTop = top + 50;

            List<AbilityDefinition> abilities = getOwnedAbilities();

            for (int i = 0; i < VISIBLE_ROWS; i++) {
                int index = scrollOffset + i;

                if (index >= abilities.size()) {
                    break;
                }

                int rowTop = listTop + i * ROW_HEIGHT;
                int buttonLeft = left + PANEL_WIDTH - 74;
                int buttonRight = left + PANEL_WIDTH - 14;

                if (mouseX >= buttonLeft
                        && mouseX <= buttonRight
                        && mouseY >= rowTop + 8
                        && mouseY <= rowTop + 29) {

                    AbilityDefinition ability = abilities.get(index);

                    if (ability.isToggleable()) {
                        boolean currentlyActive =
                                PlayerAbilityData.isActive(
                                        minecraft.player,
                                        ability.getId()
                                );

                        AbilityNetwork.sendToServer(
                                new AbilityTogglePacket(
                                        ability.getId(),
                                        !currentlyActive
                                )
                        );
                    }

                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        int maxOffset = Math.max(
                0,
                getOwnedAbilities().size() - VISIBLE_ROWS
        );

        scrollOffset -= (int) Math.signum(delta);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}