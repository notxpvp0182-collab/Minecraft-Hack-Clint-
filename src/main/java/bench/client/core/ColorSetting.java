package bench.client.core;

import java.util.Locale;

public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultColor) {
        super(name, description, 0xFF000000 | (defaultColor & 0xFFFFFF));
    }

    public int getRed() {
        return (getValue() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public void setRgb(int r, int g, int b) {
        int clampedR = Math.max(0, Math.min(255, r));
        int clampedG = Math.max(0, Math.min(255, g));
        int clampedB = Math.max(0, Math.min(255, b));
        setValue((0xFF << 24) | (clampedR << 16) | (clampedG << 8) | clampedB);
    }

    public String getHexString() {
        return String.format(Locale.ROOT, "#%06X", getValue() & 0xFFFFFF);
    }
}
