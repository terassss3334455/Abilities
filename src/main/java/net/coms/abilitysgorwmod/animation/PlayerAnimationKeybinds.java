package net.coms.abilitysgorwmod.animation;

import com.mojang.blaze3d.platform.InputConstants;
import net.coms.abilitysgorwmod.network.AbilityNetwork;
import net.coms.abilitysgorwmod.network.AnimationPlayRequestPacket;
import net.coms.abilitysgorwmod.client.AnimationResourceHash;
import net.coms.abilitysgorwmod.client.DancingAnimationConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid=PlayerAnimationHandler.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class PlayerAnimationKeybinds {
    private static final String CATEGORY="ダンシング";
    public static final KeyMapping PLAY_ANIMATION=new KeyMapping("待機アニメーション再生",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,CATEGORY);
    public static final KeyMapping PLAY_ATTACK=new KeyMapping("攻撃アニメーション再生",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,CATEGORY);
    public static final KeyMapping STOP_ANIMATION=new KeyMapping("アニメーション停止",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_UNKNOWN,CATEGORY);
    private PlayerAnimationKeybinds() {}
    @SubscribeEvent public static void registerKeyMappings(RegisterKeyMappingsEvent e){e.register(PLAY_ANIMATION);e.register(PLAY_ATTACK);e.register(STOP_ANIMATION);}
    @Mod.EventBusSubscriber(modid=PlayerAnimationHandler.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
    public static final class KeyInputHandler {
        private KeyInputHandler() {}
        @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END)return;
            while(PLAY_ANIMATION.consumeClick())send("abilitysgorwmod:animations/player.animation.json", "idle",false);
            while(PLAY_ATTACK.consumeClick())send("abilitysgorwmod:animations/player.animation.json", "attack",false);
            while(STOP_ANIMATION.consumeClick())send("abilitysgorwmod:animations/player.animation.json", "",true);
            DancingAnimationConfig.tickKeys();
        }
        private static void send(String resource,String clip,boolean stop){
            Minecraft mc=Minecraft.getInstance();
            if(mc.player!=null&&mc.screen==null){String hash=AnimationResourceHash.sha256(resource);if(hash!=null)AbilityNetwork.sendToServer(new AnimationPlayRequestPacket(mc.player.getUUID(),resource,clip,hash,stop));}
        }
    }
}