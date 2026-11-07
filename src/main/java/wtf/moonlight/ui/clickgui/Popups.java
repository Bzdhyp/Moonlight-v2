package wtf.moonlight.ui.clickgui;

import imgui.ImDrawData;
import imgui.ImDrawList;
import imgui.ImGui;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import wtf.moonlight.ui.imgui.Natives;

import java.util.ArrayList;
import java.util.List;

final class Popups {
    private static @Nullable Popups active;

    private final List<Entry> entries = new ArrayList<>();

    static void setActive(@Nullable Popups popups) {
        active = popups;
    }

    static void capture(int key, float x, float y, float width, float height, float reveal, Motion motion, List<Long> lists) {
        if (active == null) return;
        Entry entry = null;
        for (Entry candidate : active.entries) {
            if (candidate.key == key) entry = candidate;
        }
        if (entry == null) {
            entry = new Entry(key);
            active.entries.add(entry);
        }
        entry.frame = ImGui.getFrameCount();
        entry.reveal = reveal;
        entry.motion = motion;
        entry.x = x;
        entry.y = y;
        entry.width = width;
        entry.height = height;
        entry.lists.clear();
        for (long list : lists) entry.lists.add(Snapshot.of(list));
    }

    static void transform(long list, float x, float y, float width, float height, float reveal, Motion motion) {
        float e = ease(reveal, motion);
        if (motion == Motion.ACCOUNT) {
            Natives.transform(list, 0, 0, 1, 0, 0, e);
        } else {
            Natives.transform(list, x + width * .5f, y + height * .5f, .96f + (1 - .96f) * e, Widgets.scale(4) * (1 - e), 0, e);
        }
    }

    void reset() {
        entries.clear();
    }

    void drawClosing() {
        ImDrawList out = ImGui.getForegroundDrawList();
        for (Entry entry : entries) {
            if (entry.frame == ImGui.getFrameCount()) continue;
            entry.reveal *= (float) Math.exp(-(entry.motion == Motion.ACCOUNT ? 17 : 20) * ImGui.getIO().getDeltaTime());
            float e = ease(entry.reveal, entry.motion);
            if (entry.reveal < .002f) continue;
            for (Snapshot list : entry.lists) list.replay(out, entry, e);
        }
        entries.removeIf(entry -> entry.reveal < .002f);
    }

    private static float ease(float reveal, Motion motion) {
        return motion == Motion.ACCOUNT ? reveal : Controls.popupEase(reveal);
    }

    enum Motion {
        DROPDOWN,
        ACCOUNT
    }

    private static final class Entry {
        private final int key;
        private final List<Snapshot> lists = new ArrayList<>();
        private int frame;
        private float reveal;
        private float x;
        private float y;
        private float width;
        private float height;
        private Motion motion = Motion.DROPDOWN;

        private Entry(int key) {
            this.key = key;
        }

        private float mapX(float px, float e) {
            if (motion == Motion.ACCOUNT) return px;
            float pivot = x + width * .5f;
            return pivot + (px - pivot) * (.96f + (1 - .96f) * e) + Widgets.scale(4) * (1 - e);
        }

        private float mapY(float py, float e) {
            if (motion == Motion.ACCOUNT) return py;
            float pivot = y + height * .5f;
            return pivot + (py - pivot) * (.96f + (1 - .96f) * e);
        }
    }

    private record Snapshot(float[] clips, long[] textures, int[] offsets, long[] callbacks, float[] vertices, int[] colors, int[] indices) {
        private static Snapshot of(long list) {
            int commandCount = Natives.commandCount(list);
            float[] clips = new float[commandCount * 4];
            long[] textures = new long[commandCount];
            int[] offsets = new int[commandCount * 3];
            long[] callbacks = new long[commandCount];
            long command = Natives.commands(list);
            for (int c = 0; c < commandCount; c++, command += Natives.COMMAND_SIZE) {
                for (int i = 0; i < 4; i++) clips[c * 4 + i] = MemoryUtil.memGetFloat(command + 4L * i);
                textures[c] = Natives.textureId(command);
                offsets[c * 3] = MemoryUtil.memGetInt(command + Natives.COMMAND_VERTEX_OFFSET);
                offsets[c * 3 + 1] = MemoryUtil.memGetInt(command + Natives.COMMAND_INDEX_OFFSET);
                offsets[c * 3 + 2] = MemoryUtil.memGetInt(command + Natives.COMMAND_ELEMENTS);
                callbacks[c] = MemoryUtil.memGetAddress(command + Natives.COMMAND_CALLBACK);
            }
            int vertexCount = Natives.vertexCount(list);
            float[] vertices = new float[vertexCount * 4];
            int[] colors = new int[vertexCount];
            long vertex = Natives.vertices(list);
            for (int v = 0; v < vertexCount; v++, vertex += Natives.VERTEX_SIZE) {
                for (int i = 0; i < 4; i++) vertices[v * 4 + i] = MemoryUtil.memGetFloat(vertex + 4L * i);
                colors[v] = MemoryUtil.memGetInt(vertex + 16);
            }
            int indexCount = Natives.indexCount(list);
            int[] indices = new int[indexCount];
            long index = Natives.indices(list);
            boolean wide = ImDrawData.sizeOfImDrawIdx() == 4;
            for (int i = 0; i < indexCount; i++) {
                indices[i] = wide ? MemoryUtil.memGetInt(index + 4L * i) : MemoryUtil.memGetShort(index + 2L * i) & 0xFFFF;
            }
            return new Snapshot(clips, textures, offsets, callbacks, vertices, colors, indices);
        }

        private void replay(ImDrawList out, Entry entry, float e) {
            for (int c = 0; c < textures.length; c++) {
                int elements = offsets[c * 3 + 2];
                if (callbacks[c] != 0 || elements == 0) continue;
                out.pushClipRect(entry.mapX(clips[c * 4], e), entry.mapY(clips[c * 4 + 1], e),
                        entry.mapX(clips[c * 4 + 2], e), entry.mapY(clips[c * 4 + 3], e), false);
                out.pushTextureID(textures[c]);
                int vertexOffset = offsets[c * 3];
                int indexOffset = offsets[c * 3 + 1];
                for (int offset = 0; offset < elements; ) {
                    int count = Math.min(3000, elements - offset);
                    out.primReserve(count, count);
                    for (int i = 0; i < count; i++) {
                        int v = vertexOffset + indices[indexOffset + offset + i];
                        out.primVtx(entry.mapX(vertices[v * 4], e), entry.mapY(vertices[v * 4 + 1], e),
                                vertices[v * 4 + 2], vertices[v * 4 + 3],
                                Natives.withAlpha(colors[v], (int) ((colors[v] >>> 24) * e)));
                    }
                    offset += count;
                }
                out.popTextureID();
                out.popClipRect();
            }
        }
    }
}
