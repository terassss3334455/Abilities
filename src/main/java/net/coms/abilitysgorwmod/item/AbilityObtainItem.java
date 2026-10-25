package net.coms.abilitysgorwmod.item;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class AbilityObtainItem extends Item {
    private final AbilityDefinition.Rarity minimumRarity;

    public AbilityObtainItem(Properties properties) {
        this(properties, AbilityDefinition.Rarity.COMMON);
    }

    public AbilityObtainItem(Properties properties, AbilityDefinition.Rarity minimumRarity) {
        super(properties);
        this.minimumRarity = minimumRarity == null
                ? AbilityDefinition.Rarity.COMMON : minimumRarity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            List<AbilityDefinition> available =
                    AbilityRegistry.getUnownedObtainableAbilities(player, minimumRarity);

            if (available.isEmpty()) {
                player.displayClientMessage(
                        Component.literal("取得できる能力がありません。")
                                .withStyle(ChatFormatting.RED),
                        true
                );

                return InteractionResultHolder.success(stack);
            }

            AbilityDefinition selected = available.get(
                    player.getRandom().nextInt(available.size())
            );

            if (PlayerAbilityData.addAbility(player, selected.getId())) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                player.displayClientMessage(
                        Component.literal("能力を取得した：「"
                                        + selected.getName() + "」 [" + selected.getRarity().name() + "]")
                                .withStyle(ChatFormatting.GREEN),
                        false
                );

                AbilityNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new net.coms.abilitysgorwmod.network.AbilityAcquiredToastPacket(
                                selected.getName(), selected.getRarity().name())
                );
                AbilityNetwork.syncPlayer(serverPlayer);
            }
        }

        return InteractionResultHolder.sidedSuccess(
                stack,
                level.isClientSide
        );
    }
}