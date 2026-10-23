package bench.client.core;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;

    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }

    public E[] getValues() {
        return values;
    }

    public void cycle() {
        int next = (getValue().ordinal() + 1) % values.length;
        setValue(values[next]);
    }

    public void setByName(String name) {
        for (E constant : values) {
            if (constant.name().equalsIgnoreCase(name)) {
                setValueSilent(constant);
                return;
            }
        }
    }
}
