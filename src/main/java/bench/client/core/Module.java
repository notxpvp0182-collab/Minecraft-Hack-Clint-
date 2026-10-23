package bench.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private boolean favorite;
    private int keyBind;
    private boolean keyPressedLastTick;
    private int hudX = 8;
    private int hudY = 8;
    private int hudW = 96;
    private int hudH = 16;
    private int hudLayer = 0;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, Category category, String description) {
        this(name, category, description, GLFW.GLFW_KEY_UNKNOWN);
    }

    public Module(String name, Category category, String description, int keyBind) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.keyBind = keyBind;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            try {
                if (enabled) {
                    onEnable();
                } else {
                    onDisable();
                }
            } catch (Exception ignored) {
            }
            NotificationManager.push(this.name, enabled ? "Enabled" : "Disabled", enabled);
            SettingManager.save();
        }
    }

    public void loadStateFromConfig(boolean savedEnabled) {
        this.enabled = savedEnabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
        SettingManager.save();
    }

    public void setFavoriteSilent(boolean favorite) {
        this.favorite = favorite;
    }

    public void toggleFavorite() {
        setFavorite(!favorite);
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
        SettingManager.save();
    }

    public void setKeyBindSilent(int keyBind) {
        this.keyBind = keyBind;
    }

    public String getKeyBindName() {
        if (keyBind == GLFW.GLFW_KEY_UNKNOWN || keyBind <= 0) {
            return "NONE";
        }
        try {
            return InputUtil.Type.KEYSYM.createFromCode(keyBind).getLocalizedText().getString().toUpperCase();
        } catch (Exception e) {
            return "KEY_" + keyBind;
        }
    }

    public int getHudX() {
        return hudX;
    }

    public int getHudY() {
        return hudY;
    }

    public int getHudW() {
        return hudW;
    }

    public int getHudH() {
        return hudH;
    }

    public int getHudLayer() {
        return hudLayer;
    }

    public void setHudLayer(int layer) {
        this.hudLayer = layer;
        SettingManager.save();
    }

    public void setHudLayerSilent(int layer) {
        this.hudLayer = layer;
    }

    public void setHudBoundsSilent(int x, int y, int w, int h) {
        this.hudX = x;
        this.hudY = y;
        this.hudW = Math.max(40, w);
        this.hudH = Math.max(14, h);
    }

    public void setHudPos(int x, int y) {
        this.hudX = x;
        this.hudY = y;
        SettingManager.save();
    }

    public void setHudPosSilent(int x, int y) {
        this.hudX = x;
        this.hudY = y;
    }

    public boolean wasKeyPressedLastTick() {
        return keyPressedLastTick;
    }

    public void setKeyPressedLastTick(boolean keyPressedLastTick) {
        this.keyPressedLastTick = keyPressedLastTick;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick(MinecraftClient client) {
    }

    public void onRenderHud(DrawContext context) {
    }
}
