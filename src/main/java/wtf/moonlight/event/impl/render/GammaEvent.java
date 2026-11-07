package wtf.moonlight.event.impl.render;

import wtf.moonlight.event.Event;

public final class GammaEvent extends Event {
    private double gamma;

    public GammaEvent(double gamma) {
        this.gamma = gamma;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }
}
