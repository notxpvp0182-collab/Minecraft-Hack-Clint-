package bench.client.modules.render;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ColorSetting;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public class NoRender extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Fire Opacity", "Configure Fire Opacity for NoRender.", 0.0, 0.0, 1.0));
    public final BoolSetting secondaryToggle = add(new BoolSetting("Hide Weather", "Toggle Hide Weather for NoRender.", true));
    public final ColorSetting themeColor = add(new ColorSetting("NoRender Color", "Custom color for NoRender.", 0xFF71717A));

    private int tickCounter = 0;

    public NoRender() {
        super("NoRender", Category.RENDER, "Disables selected overlay clutter.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        tickCounter++;
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if ("render".equals("render")) {
            double maxSq = range.getValue() * range.getValue();
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || mc.player.squaredDistanceTo(e) > maxSq) continue;
                if (e instanceof PlayerEntity || (secondaryToggle.getValue() && (e instanceof LivingEntity || e instanceof ItemEntity))) {
                    int[] scr = ModuleUtils.worldToScreen(ctx, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()));
                    if (scr != null) {
                        ctx.fill(scr[0] - 4, scr[1] - 4, scr[0] + 4, scr[1] + 4, themeColor.getValue());
                    }
                }
            }
        } else if (secondaryToggle.getValue()) {
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), String.format(Locale.ROOT, "NoRender: %.1f", range.getValue()), themeColor.getValue());
        }
    }
}
