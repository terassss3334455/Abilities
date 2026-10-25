package net.coms.abilitysgorwmod.data;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.api.AbilityRarityApi;
import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = abilitysgorwmod.MODID)
public final class AbilityAssignmentHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!PlayerAbilityData.isInitialized(player)) {
            AbilityDefinition selected = AbilityRarityApi.rollRandomObtainableAbility(player, null, null);
            if (selected != null && PlayerAbilityData.addAbility(player, selected.getId())) {
                AbilityNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new net.coms.abilitysgorwmod.network.AbilityAcquiredToastPacket(
                                selected.getId(), selected.getRarity().getId())
                );
            }
            PlayerAbilityData.setInitialized(player, true);
        }

        AbilityNetwork.syncPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            PlayerAbilityData.copyData(
                    event.getOriginal(),
                    event.getEntity()
            );
            VaultInventoryData.copyData(event.getOriginal(), event.getEntity());
        }
    }
}