package wtf.moonlight.bind;

public interface Bindable {
    void onKey(int code, boolean pressed);

    void reconcile();
}
