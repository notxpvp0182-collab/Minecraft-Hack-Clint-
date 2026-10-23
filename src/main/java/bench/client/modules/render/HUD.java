package bench.client.modules.render;

import bench.client.core.Category;
import bench.client.core.ClientDataManager;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import java.util.Locale;

public class HUD extends Module {
    public HUD() {
        super("HUD", Category.RENDER, "On-screen info: coords, FPS, speed, armor, totem count.");
        setHudPosSilent(8, 8);
        loadStateFromConfig(true);
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int x = getHudX(), y = getHudY();
        ModuleUtils.drawHudBadge(ctx, x, y, "Eternal BenchClient v3.0 | FPS: " + mc.getCurrentFps(), 0xFFDC2626);
        String dir = mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT);
        ModuleUtils.drawHudBadge(ctx, x, y + 16, String.format(Locale.ROOT, "XYZ: %.1f, %.1f, %.1f [%s]", mc.player.getX(), mc.player.getY(), mc.player.getZ(), dir), 0xFFDC2626);
        double bps = Math.hypot(mc.player.getX() - mc.player.lastRenderX, mc.player.getZ() - mc.player.lastRenderZ) * 20.0;
        int totems = ModuleUtils.countItem(mc.player.getInventory(), Items.TOTEM_OF_UNDYING);
        ModuleUtils.drawHudBadge(ctx, x, y + 32, String.format(Locale.ROOT, "Speed: %.2f b/s | CPS: %d | Totems: %d", bps, ClientDataManager.getLeftCps(1000), totems), 0xFFDC2626);
    }
}
