package bench.client.core;

public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    private final T defaultValue;

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T getValue() { return value; }
    public T getDefaultValue() { return defaultValue; }
    public void setValue(T value) { this.value = value; SettingManager.save(); }
    public void setValueSilent(T value) { this.value = value; }
    public void resetToDefault() { setValue(defaultValue); }
}
