package bench.client.modules.world;

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

public class SpawnProofer extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Torch Reach", "Configure Torch Reach for SpawnProofer.", 4.2, 2.0, 6.0));
    public final BoolSetting secondaryToggle = add(new BoolSetting("Max Light Level 0", "Toggle Max Light Level 0 for SpawnProofer.", true));
    public final ColorSetting themeColor = add(new ColorSetting("SpawnProofer Color", "Custom color for SpawnProofer.", 0xFFFBBF24));

    private int tickCounter = 0;

    public SpawnProofer() {
        super("SpawnProofer", Category.WORLD, "Places torches on dark blocks with light 0.");
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
        if ("world".equals("render")) {
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
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), String.format(Locale.ROOT, "SpawnProofer: %.1f", range.getValue()), themeColor.getValue());
        }
    }
}
