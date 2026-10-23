package bench.client.core;

import java.util.Locale;

public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultColor) {
        super(name, description, 0xFF000000 | (defaultColor & 0xFFFFFF));
    }
    public int getRed() { return (getValue() >> 16) & 0xFF; }
    public int getGreen() { return (getValue() >> 8) & 0xFF; }
    public int getBlue() { return getValue() & 0xFF; }
    public void setRgb(int r, int g, int b) {
        int cr = Math.max(0, Math.min(255, r));
        int cg = Math.max(0, Math.min(255, g));
        int cb = Math.max(0, Math.min(255, b));
        setValue((0xFF << 24) | (cr << 16) | (cg << 8) | cb);
    }
    public String getHexString() { return String.format(Locale.ROOT, "#%06X", getValue() & 0xFFFFFF); }
}
