package wtf.moonlight.event.impl.network;

import net.minecraft.network.protocol.Packet;
import wtf.moonlight.event.CancellableEvent;

public final class PacketSendEvent extends CancellableEvent {
    private final Packet<?> packet;

    public PacketSendEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }
}
