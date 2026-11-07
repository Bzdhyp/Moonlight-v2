package wtf.moonlight.bind;

public interface Bindable {
    int getKey();

    void onBind(boolean pressed);

    default boolean triggersOnRelease() {
        return false;
    }
}
