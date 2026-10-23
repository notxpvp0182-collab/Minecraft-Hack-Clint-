package bench.client.modules.fun;

import bench.client.core.Category;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Jetpack extends Module {
    public final DoubleSetting thrust = add(new DoubleSetting("Thrust", "Upward jetpack acceleration.", 0.08, 0.03, 0.20));

    public Jetpack() {
        super("Jetpack", Category.FUN, "Jetpack-style upward flying movement when holding Jump.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player != null && mc.options.jumpKey.isPressed()) {
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, Math.min(0.42, v.y + thrust.getValue()), v.z);
            mc.player.fallDistance = 0.0f;
        }
    }
}
