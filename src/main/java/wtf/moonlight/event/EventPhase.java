package wtf.moonlight.event;

public enum EventPhase {
    PRE,
    POST;

    public boolean isPre() {
        return this == PRE;
    }
}
