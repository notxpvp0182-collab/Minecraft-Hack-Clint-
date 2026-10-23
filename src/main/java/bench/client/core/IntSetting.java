package bench.client.core;

public class IntSetting extends Setting<Integer> {
    private final int min;
    private final int max;

    public IntSetting(String name, String description, int defaultValue, int min, int max) {
        super(name, description, clamp(defaultValue, min, max));
        this.min = min;
        this.max = max;
    }

    @Override
    public void setValue(Integer value) {
        super.setValue(clamp(value, min, max));
    }

    @Override
    public void setValueSilent(Integer value) {
        super.setValueSilent(clamp(value, min, max));
    }

    public void step(int delta) {
        setValue(getValue() + delta);
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
