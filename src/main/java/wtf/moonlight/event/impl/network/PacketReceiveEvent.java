package wtf.moonlight.event.impl.network;

import net.minecraft.network.protocol.Packet;
import wtf.moonlight.event.CancellableEvent;

public final class PacketReceiveEvent extends CancellableEvent {
    private final Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}
