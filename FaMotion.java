package wtf.carnation.ui.fatality;

/** Frame-rate independent exponential approach; quick to settle, never overshoots. */
final class FaMotion {
    private float value;

    FaMotion(float initial) {
        value = initial;
    }

    /** Moves toward {@code target}; {@code speed} is roughly how many times per second the gap shrinks by e. */
    float toward(float target, float dt, float speed) {
        float t = 1.0f - (float) Math.exp(-Math.max(0.0f, dt) * speed);
        value += (target - value) * t;
        if (Math.abs(target - value) < 0.001f) value = target;
        return value;
    }

    float value() {
        return value;
    }

    void snap(float value) {
        this.value = value;
    }
}
