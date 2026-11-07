package wtf.moonlight.ui.hud;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import wtf.moonlight.event.EventPhase;
import wtf.moonlight.event.Subscribe;
import wtf.moonlight.event.impl.client.TickEvent;
import wtf.moonlight.event.impl.client.WorldChangeEvent;
import wtf.moonlight.event.impl.network.PacketReceiveEvent;

import java.util.function.Supplier;

public final class Tracker implements Supplier<Snapshot> {
    private static final int SAMPLES = 6;
    private static final long STALE = 10_000_000_000L;

    private final long[] times = new long[SAMPLES];
    private final long[] ticks = new long[SAMPLES];
    private int count;
    private volatile float speed = -1;

    @Subscribe
    private void onPacket(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof ClientboundSetTimePacket packet)) return;
        long now = System.nanoTime();
        synchronized (this) {
            if (count > 0 && (packet.gameTime() <= ticks[count - 1] || now - times[count - 1] > STALE)) count = 0;
            if (count == SAMPLES) {
                System.arraycopy(times, 1, times, 0, SAMPLES - 1);
                System.arraycopy(ticks, 1, ticks, 0, SAMPLES - 1);
                count--;
            }
            times[count] = now;
            ticks[count] = packet.gameTime();
            count++;
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (event.getPhase() != EventPhase.POST) return;
        LocalPlayer player = Minecraft.getInstance().player;
        speed = player == null ? -1 : (float) Math.hypot(player.getX() - player.xo, player.getZ() - player.zo) * 20;
    }

    @Subscribe
    private void onWorld(WorldChangeEvent event) {
        synchronized (this) {
            count = 0;
        }
    }

    @Override
    public Snapshot get() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ServerData data = mc.getCurrentServer();
        String server = data != null ? data.ip : mc.hasSingleplayerServer() ? "Singleplayer" : "--";
        ClientPacketListener connection = mc.getConnection();
        PlayerInfo info = player != null && connection != null ? connection.getPlayerInfo(player.getUUID()) : null;
        String coordinates = player == null ? "--"
                : (int) Math.floor(player.getX()) + ", " + (int) Math.floor(player.getY()) + ", " + (int) Math.floor(player.getZ());
        return new Snapshot(server, info != null ? info.getLatency() : -1, tps(), coordinates, player != null ? speed : -1,
                SharedConstants.getCurrentVersion().name());
    }

    private synchronized float tps() {
        if (count < 2 || System.nanoTime() - times[count - 1] > STALE) return -1;
        double seconds = (times[count - 1] - times[0]) / 1.0e9;
        return seconds > 0.05 ? (float) Math.clamp((ticks[count - 1] - ticks[0]) / seconds, 0, 20) : -1;
    }
}
