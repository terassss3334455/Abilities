package net.coms.abilitysgorwmod.client;

import net.coms.abilitysgorwmod.abilitysgorwmod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = abilitysgorwmod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT
)
public final class AbilityScreenKeyHandler {

    public static final KeyMapping OPEN_ABILITY_GUI =
            new KeyMapping(
                    "key.abilitysgorwmod.open_ability_gui",
                    GLFW.GLFW_KEY_K,
                    "key.categories.abilitysgorwmod"
            );

    public static final KeyMapping DIMENSION_SHIFT = new KeyMapping(
            "境界渡り", GLFW.GLFW_KEY_H,
            "key.categories.abilitysgorwmod");

    public static final KeyMapping OPEN_VAULT = new KeyMapping(
            "無限収納", GLFW.GLFW_KEY_B,
            "key.categories.abilitysgorwmod");

    private AbilityScreenKeyHandler() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ABILITY_GUI);
        event.register(DIMENSION_SHIFT);
        event.register(OPEN_VAULT);
    }
}