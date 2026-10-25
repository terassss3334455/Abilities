package net.coms.abilitysgorwmod.network;
import net.coms.abilitysgorwmod.animation.PlayerAnimationHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.UUID;
import java.util.function.Supplier;
public final class AnimationPlayPacket {
    final UUID playerId; final String resource; final String animation; final String hash; final boolean stop;
    public AnimationPlayPacket(UUID id,String resource,String animation,String hash,boolean stop){this.playerId=id;this.resource=resource;this.animation=animation;this.hash=hash;this.stop=stop;}
    public static void encode(AnimationPlayPacket p,FriendlyByteBuf b){b.writeUUID(p.playerId);b.writeUtf(p.resource,256);b.writeUtf(p.animation,64);b.writeUtf(p.hash,64);b.writeBoolean(p.stop);}
    public static AnimationPlayPacket decode(FriendlyByteBuf b){return new AnimationPlayPacket(b.readUUID(),b.readUtf(256),b.readUtf(64),b.readUtf(64),b.readBoolean());}
    public static void handle(AnimationPlayPacket p,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PlayerAnimationHandler.receiveAnimation(p.playerId,p.resource,p.animation,p.hash,p.stop)));c.setPacketHandled(true);}
}