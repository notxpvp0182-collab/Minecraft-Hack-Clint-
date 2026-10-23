package bench.client.modules.render;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ColorSetting;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class BetterBeacons extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Range", "Effective range or intensity for BetterBeacons.", 4.5, 1.0, 16.0));
    public final BoolSetting hudIndicator = add(new BoolSetting("HUD Status", "Show status badge when active.", false));
    public final ColorSetting color = add(new ColorSetting("Accent Color", "Highlight color for BetterBeacons.", 0xFFDC2626));

    private int activeTicks = 0;

    public BetterBeacons() {
        super("BetterBeacons", Category.RENDER, "Displays beacon range and effect info.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        activeTicks++;
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        if (hudIndicator.getValue()) {
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), "BetterBeacons [" + activeTicks + "t]", color.getValue());
        }
    }
}
