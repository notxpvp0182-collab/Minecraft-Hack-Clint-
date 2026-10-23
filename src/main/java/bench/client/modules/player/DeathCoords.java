package bench.client.modules.player;

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

public class DeathCoords extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Alert Duration (s)", "Configure Alert Duration (s) for DeathCoords.", 10.0, 3.0, 30.0));
    public final BoolSetting secondaryToggle = add(new BoolSetting("Copy To Chat", "Toggle Copy To Chat for DeathCoords.", true));
    public final ColorSetting themeColor = add(new ColorSetting("DeathCoords Color", "Custom color for DeathCoords.", 0xFFEF4444));

    private int tickCounter = 0;

    public DeathCoords() {
        super("DeathCoords", Category.PLAYER, "Prints and notifies death coordinates.");
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
        if ("player".equals("render")) {
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
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), String.format(Locale.ROOT, "DeathCoords: %.1f", range.getValue()), themeColor.getValue());
        }
    }
}
