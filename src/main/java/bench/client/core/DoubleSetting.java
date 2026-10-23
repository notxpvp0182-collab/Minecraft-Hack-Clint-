package bench.client.core;

public class DoubleSetting extends Setting<Double> {
    private final double min, max;
    public DoubleSetting(String name, String description, double defaultValue, double min, double max) {
        super(name, description, Math.max(min, Math.min(max, defaultValue)));
        this.min = min;
        this.max = max;
    }
    @Override public void setValue(Double value) { super.setValue(Math.max(min, Math.min(max, value))); }
    @Override public void setValueSilent(Double value) { super.setValueSilent(Math.max(min, Math.min(max, value))); }
    public void step(double delta) { setValue(Math.round((getValue() + delta) * 100.0) / 100.0); }
    public double getMin() { return min; }
    public double getMax() { return max; }
}
