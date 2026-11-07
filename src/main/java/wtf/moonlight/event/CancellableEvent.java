package wtf.moonlight.event;

public abstract class CancellableEvent extends Event {
    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        cancelled = true;
    }
}
