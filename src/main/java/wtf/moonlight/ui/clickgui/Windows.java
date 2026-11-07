package wtf.moonlight.ui.clickgui;

import imgui.ImGui;
import imgui.flag.ImGuiChildFlags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class Windows {
    private static final List<Node> NODES = new ArrayList<>();
    private static final Deque<Node> STACK = new ArrayDeque<>();

    private Windows() {
    }

    static void reset() {
        NODES.clear();
        STACK.clear();
    }

    static boolean begin(String name, int flags) {
        boolean visible = ImGui.begin(name, flags);
        push(false, 0, Popups.Motion.DROPDOWN);
        return visible;
    }

    static void end() {
        STACK.pop();
        ImGui.end();
    }

    static boolean beginChild(String id, float width, float height, int flags) {
        boolean visible = ImGui.beginChild(id, width, height, ImGuiChildFlags.None, flags);
        push(false, 0, Popups.Motion.DROPDOWN);
        return visible;
    }

    static void endChild() {
        STACK.pop();
        ImGui.endChild();
    }

    static boolean beginPopup(String id, int flags, Popups.Motion motion) {
        int key = ImGui.getID(id);
        if (!ImGui.beginPopup(id, flags)) return false;
        push(true, key, motion);
        return true;
    }

    static void endPopup() {
        STACK.pop();
        ImGui.endPopup();
    }

    static int popupKey() {
        return current().key;
    }

    static Popups.Motion popupMotion() {
        return current().motion;
    }

    static List<Long> subtree() {
        Node root = current();
        List<Long> lists = new ArrayList<>();
        for (Node node : NODES) {
            for (Node at = node; at != null; at = at.parent) {
                if (at == root) {
                    lists.add(node.list);
                    break;
                }
            }
        }
        return lists;
    }

    static List<Long> all() {
        List<Long> lists = new ArrayList<>(NODES.size());
        for (Node node : NODES) lists.add(node.list);
        return lists;
    }

    private static Node current() {
        Node node = STACK.peek();
        if (node == null) throw new IllegalStateException("No tracked window");
        return node;
    }

    private static void push(boolean popup, int key, Popups.Motion motion) {
        Node node = new Node(ImGui.getWindowDrawList().ptr, popup ? null : STACK.peek(), key, motion);
        NODES.add(node);
        STACK.push(node);
    }

    private record Node(long list, @Nullable Node parent, int key, Popups.Motion motion) {
    }
}
