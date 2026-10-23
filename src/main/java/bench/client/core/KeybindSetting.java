package bench.client.core;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String name, int defaultKeyCode) {
        super(name, "Custom action keybind for " + name + ".", defaultKeyCode);
    }
    public String getKeyName() {
        int code = getValue();
        if (code == GLFW.GLFW_KEY_UNKNOWN || code <= 0) return "NONE";
        try { return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString().toUpperCase(); }
        catch (Exception e) { return "KEY_" + code; }
    }
}
