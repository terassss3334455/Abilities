package net.coms.abilitysgorwmod.network;
import net.coms.abilitysgorwmod.data.PlayerAbilityData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.UUID;
import java.util.function.Supplier;
public final class AnimationPlayRequestPacket {
    private final UUID playerId; private final String resource; private final String animation; private final String hash; private final boolean stop;
    public AnimationPlayRequestPacket(UUID id,String resource,String animation,String hash,boolean stop){this.playerId=id;this.resource=resource;this.animation=animation;this.hash=hash;this.stop=stop;}
    public static void encode(AnimationPlayRequestPacket p,FriendlyByteBuf b){b.writeUUID(p.playerId);b.writeUtf(p.resource,256);b.writeUtf(p.animation,64);b.writeUtf(p.hash,64);b.writeBoolean(p.stop);}
    public static AnimationPlayRequestPacket decode(FriendlyByteBuf b){return new AnimationPlayRequestPacket(b.readUUID(),b.readUtf(256),b.readUtf(64),b.readUtf(64),b.readBoolean());}
    private static boolean canDance(ServerPlayer player){return (PlayerAbilityData.hasAbility(player,"dancing")&&PlayerAbilityData.isActive(player,"dancing"))||(PlayerAbilityData.hasAbility(player,"example:dancing")&&PlayerAbilityData.isActive(player,"example:dancing"));}
    public static void handle(AnimationPlayRequestPacket p,Supplier<NetworkEvent.Context> s){NetworkEvent.Context c=s.get();c.enqueueWork(()->{
        ServerPlayer sender=c.getSender();if(sender==null||!sender.getUUID().equals(p.playerId)||!canDance(sender))return;
        ResourceLocation id=ResourceLocation.tryParse(p.resource);if(id==null||!id.getNamespace().equals("abilitysgorwmod")||!id.getPath().startsWith("animations/"))return;
        if(!p.stop&&!p.animation.matches("[A-Za-z0-9_.-]{1,64}"))return; if(p.hash==null||!p.hash.matches("[0-9a-f]{64}"))return;
        AbilityNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->sender),new AnimationPlayPacket(sender.getUUID(),id.toString(),p.animation,p.hash,p.stop));
    });c.setPacketHandled(true);}
}