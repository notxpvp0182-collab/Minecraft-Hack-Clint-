package bench.client.core;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    private final E[] values;
    public EnumSetting(String name, String description, E defaultValue) {
        super(name, description, defaultValue);
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
    }
    public void cycle() { setValue(values[(getValue().ordinal() + 1) % values.length]); }
    public void setByName(String name) {
        for (E c : values) if (c.name().equalsIgnoreCase(name)) { setValueSilent(c); return; }
    }
}
