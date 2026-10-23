package bench.client.modules.render;

import bench.client.core.Category;
import bench.client.core.ColorSetting;
import bench.client.core.Module;
import bench.client.core.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class ActiveModulesHUD extends Module {
    public final ColorSetting accent = add(new ColorSetting("Accent Color", "ArrayList accent color.", 0xFFDC2626));

    public ActiveModulesHUD() {
        super("ActiveModulesHUD", Category.RENDER, "Displays all enabled modules sorted in the top-right corner.");
        loadStateFromConfig(true);
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        List<Module> active = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (m.isEnabled() && m != this) {
                active.add(m);
            }
        }
        active.sort((a, b) -> Integer.compare(mc.textRenderer.getWidth(b.getName()), mc.textRenderer.getWidth(a.getName())));
        int sw = ctx.getScaledWindowWidth();
        int y = 6;
        int col = accent.getValue();
        for (Module m : active) {
            String name = m.getName();
            int w = mc.textRenderer.getWidth(name);
            ctx.fill(sw - w - 10, y - 1, sw - 2, y + 10, 0xAA0A0A0E);
            ctx.fill(sw - 2, y - 1, sw, y + 10, col);
            ctx.drawTextWithShadow(mc.textRenderer, name, sw - w - 6, y, 0xFFFFFFFF);
            y += 11;
        }
    }
}
