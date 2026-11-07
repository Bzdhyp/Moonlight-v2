package wtf.moonlight.ui.imgui;

import imgui.ImDrawData;
import imgui.ImGuiStyle;
import org.lwjgl.system.MemoryUtil;

public final class Natives {
    public static final int VERTEX_SIZE = 20;
    public static final int COMMAND_SIZE = 72;
    public static final int COMMAND_TEXTURE_DATA = 16;
    public static final int COMMAND_TEXTURE_ID = 24;
    public static final int COMMAND_VERTEX_OFFSET = 32;
    public static final int COMMAND_INDEX_OFFSET = 36;
    public static final int COMMAND_ELEMENTS = 40;
    public static final int COMMAND_CALLBACK = 48;
    public static final int TEXTURE_ID = 16;

    static final int TEXTURE_STATUS = 4;
    static final int TEXTURE_WIDTH = 28;
    static final int TEXTURE_HEIGHT = 32;
    static final int TEXTURE_PIXELS = 40;
    static final int TEXTURE_UPDATES = 64;
    static final int TEXTURE_UNUSED_FRAMES = 80;
    static final int TEXTURE_DESTROY_NEXT_FRAME = 87;

    private static final int STYLE_SIZE = 1340;
    private static final int STYLE_TOOLTIP_NAV_FLAGS = 1328;

    private Natives() {
    }

    public static int commandCount(long list) {
        return MemoryUtil.memGetInt(list);
    }

    public static long commands(long list) {
        return MemoryUtil.memGetAddress(list + 8);
    }

    public static int indexCount(long list) {
        return MemoryUtil.memGetInt(list + 16);
    }

    public static long indices(long list) {
        return MemoryUtil.memGetAddress(list + 24);
    }

    public static int vertexCount(long list) {
        return MemoryUtil.memGetInt(list + 32);
    }

    public static long vertices(long list) {
        return MemoryUtil.memGetAddress(list + 40);
    }

    public static long textureId(long command) {
        long data = MemoryUtil.memGetAddress(command + COMMAND_TEXTURE_DATA);
        return data != 0 ? MemoryUtil.memGetLong(data + TEXTURE_ID) : MemoryUtil.memGetLong(command + COMMAND_TEXTURE_ID);
    }

    public static void transform(long list, float pivotX, float pivotY, float zoom, float shiftX, float shiftY, float alpha) {
        long vertex = vertices(list);
        for (int i = vertexCount(list); i > 0; i--, vertex += VERTEX_SIZE) {
            MemoryUtil.memPutFloat(vertex, pivotX + (MemoryUtil.memGetFloat(vertex) - pivotX) * zoom + shiftX);
            MemoryUtil.memPutFloat(vertex + 4, pivotY + (MemoryUtil.memGetFloat(vertex + 4) - pivotY) * zoom + shiftY);
            int color = MemoryUtil.memGetInt(vertex + 16);
            MemoryUtil.memPutInt(vertex + 16, withAlpha(color, (int) ((color >>> 24) * alpha)));
        }
        long command = commands(list);
        for (int i = commandCount(list); i > 0; i--, command += COMMAND_SIZE) {
            for (int corner = 0; corner < 16; corner += 8) {
                MemoryUtil.memPutFloat(command + corner, pivotX + (MemoryUtil.memGetFloat(command + corner) - pivotX) * zoom + shiftX);
                MemoryUtil.memPutFloat(command + corner + 4, pivotY + (MemoryUtil.memGetFloat(command + corner + 4) - pivotY) * zoom + shiftY);
            }
        }
    }

    public static void gradient(long list, int firstVertex, float x0, float y0, float x1, float y1, int from, int to) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Math.max(1.0E-6F, dx * dx + dy * dy);
        long vertex = vertices(list) + (long) firstVertex * VERTEX_SIZE;
        for (int i = vertexCount(list) - firstVertex; i > 0; i--, vertex += VERTEX_SIZE) {
            float t = Math.clamp(((MemoryUtil.memGetFloat(vertex) - x0) * dx + (MemoryUtil.memGetFloat(vertex + 4) - y0) * dy) / length, 0.0F, 1.0F);
            int fringe = MemoryUtil.memGetInt(vertex + 16) >>> 24;
            int out = 0;
            for (int shift = 0; shift < 32; shift += 8) {
                out |= Math.round((from >>> shift & 0xFF) + ((to >>> shift & 0xFF) - (from >>> shift & 0xFF)) * t) << shift;
            }
            MemoryUtil.memPutInt(vertex + 16, withAlpha(out, (out >>> 24) * fringe / 255));
        }
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24);
    }

    static int listCount(ImDrawData data) {
        return MemoryUtil.memGetInt(data.ptr + 4);
    }

    static long list(ImDrawData data, int index) {
        return MemoryUtil.memGetAddress(MemoryUtil.memGetAddress(data.ptr + 24) + 8L * index);
    }

    static int totalVertices(ImDrawData data) {
        return MemoryUtil.memGetInt(data.ptr + 12);
    }

    static int totalIndices(ImDrawData data) {
        return MemoryUtil.memGetInt(data.ptr + 8);
    }

    static long textures(ImDrawData data) {
        return MemoryUtil.memGetAddress(data.ptr + 64);
    }

    static long allocateStyle() {
        return MemoryUtil.nmemAlloc(STYLE_SIZE);
    }

    static void copyStyle(long from, long to) {
        MemoryUtil.memCopy(from, to, STYLE_SIZE);
    }

    static boolean matchesStyle(ImGuiStyle style) {
        int previous = style.getHoverFlagsForTooltipNav();
        style.setHoverFlagsForTooltipNav(0x5A5A1234);
        boolean matches = MemoryUtil.memGetInt(style.ptr + STYLE_TOOLTIP_NAV_FLAGS) == 0x5A5A1234;
        style.setHoverFlagsForTooltipNav(previous);
        return matches;
    }

    static boolean matchesDrawData(ImDrawData data) {
        if (ImDrawData.sizeOfImDrawVert() != VERTEX_SIZE || listCount(data) != data.getCmdListsCount()) return false;
        for (int n = 0; n < data.getCmdListsCount(); n++) {
            long list = list(data, n);
            if (commandCount(list) != data.getCmdListCmdBufferSize(n)) return false;
            if (vertexCount(list) != data.getCmdListVtxBufferSize(n) || indexCount(list) != data.getCmdListIdxBufferSize(n)) return false;
            long command = commands(list);
            for (int c = 0; c < data.getCmdListCmdBufferSize(n); c++, command += COMMAND_SIZE) {
                if (MemoryUtil.memGetInt(command + COMMAND_ELEMENTS) != data.getCmdListCmdBufferElemCount(n, c)) return false;
                if (MemoryUtil.memGetInt(command + COMMAND_INDEX_OFFSET) != data.getCmdListCmdBufferIdxOffset(n, c)) return false;
                if (MemoryUtil.memGetInt(command + COMMAND_VERTEX_OFFSET) != data.getCmdListCmdBufferVtxOffset(n, c)) return false;
            }
        }
        return true;
    }
}
