package bench.client.modules.player;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ColorSetting;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class MiddleClick extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Range", "Effective range or intensity for MiddleClick.", 4.5, 1.0, 16.0));
    public final BoolSetting hudIndicator = add(new BoolSetting("HUD Status", "Show status badge when active.", false));
    public final ColorSetting color = add(new ColorSetting("Accent Color", "Highlight color for MiddleClick.", 0xFFDC2626));

    private int activeTicks = 0;

    public MiddleClick() {
        super("MiddleClick", Category.PLAYER, "Middle-click action for Pearl throw or Friend toggle.");
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
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), "MiddleClick [" + activeTicks + "t]", color.getValue());
        }
    }
}
