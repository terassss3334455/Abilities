package net.coms.abilitysgorwmod.data;

import net.coms.abilitysgorwmod.ability.AbilityDefinition;
import net.coms.abilitysgorwmod.ability.AbilityRegistry;
import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = abilitysgorwmod.MODID)
public final class AbilityAssignmentHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!PlayerAbilityData.isInitialized(player)) {
            List<AbilityDefinition> abilities =
                    AbilityRegistry.getObtainableAbilities();

            if (!abilities.isEmpty()) {
                AbilityDefinition selected = abilities.get(
                        player.getRandom().nextInt(abilities.size())
                );

                if (PlayerAbilityData.addAbility(player, selected.getId())) {
                    AbilityNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new net.coms.abilitysgorwmod.network.AbilityAcquiredToastPacket(
                                    selected.getName(), selected.getRarity().name())
                    );
                }
                PlayerAbilityData.setInitialized(player, true);
            }
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