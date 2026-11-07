package wtf.moonlight.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import wtf.moonlight.Moonlight;
import wtf.moonlight.event.impl.network.PacketReceiveEvent;
import wtf.moonlight.event.impl.network.PacketSendEvent;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow
    public abstract PacketFlow getReceiving();

    @WrapOperation(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V")
    )
    private void moonlight$receive(Packet<?> packet, PacketListener listener, Operation<Void> original) {
        if (getReceiving() != PacketFlow.CLIENTBOUND || !Moonlight.EVENT_BUS.hasListeners(PacketReceiveEvent.class)) {
            original.call(packet, listener);
            return;
        }
        PacketReceiveEvent event = Moonlight.EVENT_BUS.post(new PacketReceiveEvent(packet));
        if (!event.isCancelled()) {
            original.call(event.getPacket(), listener);
        }
    }

    @WrapOperation(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V")
    )
    private void moonlight$send(Connection connection, Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, Operation<Void> original) {
        if (getReceiving() != PacketFlow.CLIENTBOUND || !Moonlight.EVENT_BUS.hasListeners(PacketSendEvent.class)) {
            original.call(connection, packet, listener, flush);
            return;
        }
        PacketSendEvent event = Moonlight.EVENT_BUS.post(new PacketSendEvent(packet));
        if (!event.isCancelled()) {
            original.call(connection, event.getPacket(), listener, flush);
        }
    }
}
