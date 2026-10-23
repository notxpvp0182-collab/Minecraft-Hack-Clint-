package bench.client.modules.misc;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ColorSetting;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ConfigManager extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Range", "Effective range or intensity for ConfigManager.", 4.5, 1.0, 16.0));
    public final BoolSetting hudIndicator = add(new BoolSetting("HUD Status", "Show status badge when active.", false));
    public final ColorSetting color = add(new ColorSetting("Accent Color", "Highlight color for ConfigManager.", 0xFFDC2626));
    private int activeTicks = 0;

    public ConfigManager() {
        super("ConfigManager", Category.MISC, "Saves and loads JSON configuration profiles.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player != null && mc.world != null) activeTicks++;
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        if (hudIndicator.getValue()) {
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), "ConfigManager [" + activeTicks + "t]", color.getValue());
        }
    }
}
