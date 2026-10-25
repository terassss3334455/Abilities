package net.coms.abilitysgorwmod.animation;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = PlayerAnimationHandler.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PlayerAnimationKeybinds {

    public static final KeyMapping PLAY_ANIMATION =
            new KeyMapping(
                    "テストアニメーション",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "テストアニメーション"
            );


    private PlayerAnimationKeybinds() {
    }

    public static final KeyMapping PLAY_ATTACK =
            new KeyMapping(
                    "アタックアニメーション",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "テストアニメーション"
            );


    @SubscribeEvent
    public static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {
        event.register(PLAY_ANIMATION);
        event.register(PLAY_ATTACK);
    }


    @Mod.EventBusSubscriber(
            modid = PlayerAnimationHandler.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.FORGE,
            value = Dist.CLIENT
    )
    public static final class KeyInputHandler {

        private KeyInputHandler() {
        }

        @SubscribeEvent
        public static void onClientTick(
                TickEvent.ClientTickEvent event
        ) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            while (PLAY_ANIMATION.consumeClick()) {
                PlayerAnimationHandler.playAnimation(
                        "abilitysgorwmod:animations/player.animation.json",
                        "idle"
                );
            }
            while (PLAY_ATTACK.consumeClick()) {
                PlayerAnimationHandler.playAnimation(
                        "abilitysgorwmod:animations/player.animation.json",
                        "attack"
                );
            }
        }
    }
}