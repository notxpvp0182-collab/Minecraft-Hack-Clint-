package bench.client.core;

public class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max) {
        super(name, description, clamp(defaultValue, min, max));
        this.min = min;
        this.max = max;
    }

    @Override
    public void setValue(Double value) {
        super.setValue(clamp(value, min, max));
    }

    @Override
    public void setValueSilent(Double value) {
        super.setValueSilent(clamp(value, min, max));
    }

    public void step(double delta) {
        double next = Math.round((getValue() + delta) * 100.0) / 100.0;
        setValue(next);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
