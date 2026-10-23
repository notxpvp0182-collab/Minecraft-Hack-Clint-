package bench.client.modules.render;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.DoubleSetting;
import bench.client.core.IntSetting;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;

public class XRay extends Module {
    public final BoolSetting showOres = add(new BoolSetting("Ores List", "Highlight Diamond, Iron, Gold, Ancient Debris.", true));
    public final IntSetting opacity = add(new IntSetting("Opacity", "Block visibility opacity level.", 120, 0, 255));
    public final BoolSetting showHidden = add(new BoolSetting("Show Hidden Blocks", "Include unexposed ore blocks.", true));
    public final DoubleSetting range = add(new DoubleSetting("Range", "Render chunk reload radius.", 64.0, 16.0, 128.0));

    public XRay() {
        super("XRay", Category.RENDER, "Shows selected ores and storage blocks; reloads chunk meshes on toggle.");
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
    }
}
