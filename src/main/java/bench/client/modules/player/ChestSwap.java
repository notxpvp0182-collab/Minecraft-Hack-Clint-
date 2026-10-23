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

public class ChestSwap extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Swap Delay (ms)", "Configure Swap Delay (ms) for ChestSwap.", 40.0, 0.0, 200.0));
    public final BoolSetting secondaryToggle = add(new BoolSetting("Prefer Netherite", "Toggle Prefer Netherite for ChestSwap.", true));
    public final ColorSetting themeColor = add(new ColorSetting("ChestSwap Color", "Custom color for ChestSwap.", 0xFFF59E0B));

    private int tickCounter = 0;

    public ChestSwap() {
        super("ChestSwap", Category.PLAYER, "Swaps chestplate and Elytra with a single toggle.");
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
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), String.format(Locale.ROOT, "ChestSwap: %.1f", range.getValue()), themeColor.getValue());
        }
    }
}
