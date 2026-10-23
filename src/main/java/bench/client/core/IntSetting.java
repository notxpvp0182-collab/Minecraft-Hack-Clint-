package bench.client.core;

public class IntSetting extends Setting<Integer> {
    private final int min, max;
    public IntSetting(String name, String description, int defaultValue, int min, int max) {
        super(name, description, Math.max(min, Math.min(max, defaultValue)));
        this.min = min;
        this.max = max;
    }
    @Override public void setValue(Integer value) { super.setValue(Math.max(min, Math.min(max, value))); }
    @Override public void setValueSilent(Integer value) { super.setValueSilent(Math.max(min, Math.min(max, value))); }
    public void step(int delta) { setValue(getValue() + delta); }
    public int getMin() { return min; }
    public int getMax() { return max; }
}
