package bench.client.modules.movement;

import bench.client.core.Category;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;

public class Fly extends Module {
    public final DoubleSetting speed = add(new DoubleSetting("Speed", "Flight speed multiplier.", 0.12, 0.05, 0.50));

    public Fly() {
        super("Fly", Category.MOVEMENT, "Creative-style flight with configurable speed.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlySpeed(speed.getValue().floatValue());
        mc.player.fallDistance = 0.0f;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                mc.player.getAbilities().allowFlying = false;
                mc.player.getAbilities().flying = false;
            }
            mc.player.getAbilities().setFlySpeed(0.05f);
        }
    }
}
