package net.coms.abilitysgorwmod.item;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.api.AbilityApi;
import net.coms.abilitysgorwmod.api.AbilityRarityApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AbilityObtainItem extends Item {
    private final AbilityDefinition.Rarity maximumRarity;

    public AbilityObtainItem(Properties properties) {
        this(properties, AbilityDefinition.Rarity.COMMON);
    }

    public AbilityObtainItem(Properties properties, AbilityDefinition.Rarity maximumRarity) {
        super(properties);
        this.maximumRarity = maximumRarity == null ? AbilityDefinition.Rarity.COMMON : maximumRarity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            AbilityDefinition selected = AbilityRarityApi.rollRandomObtainableAbility(
                    player, null, maximumRarity);
            if (selected == null) {
                player.displayClientMessage(Component.translatable(
                        "message.abilitysgorwmod.no_available_abilities").withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.success(stack);
            }
            if (AbilityApi.grantAbility(player, selected.getId())) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                player.displayClientMessage(Component.translatable(
                        "message.abilitysgorwmod.ability_obtained",
                        selected.getNameComponent(), selected.getRarity().getDisplayName())
                        .withStyle(ChatFormatting.GREEN), false);
                net.coms.abilitysgorwmod.network.AbilityNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new net.coms.abilitysgorwmod.network.AbilityAcquiredToastPacket(
                                selected.getId(), selected.getRarity().getId()));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}