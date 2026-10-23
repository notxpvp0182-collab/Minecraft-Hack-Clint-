package bench.client.modules.movement;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.DoubleSetting;
import bench.client.core.EnumSetting;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class Fly extends Module {
    public enum FlyMode { VANILLA, GLIDE, PACKET }

    public final DoubleSetting speed = add(new DoubleSetting("Speed", "Horizontal flight speed.", 0.15, 0.05, 0.50));
    public final DoubleSetting verticalSpeed = add(new DoubleSetting("Vertical Speed", "Ascend/descend speed.", 0.30, 0.05, 1.0));
    public final EnumSetting<FlyMode> mode = add(new EnumSetting<>("Mode", "Flight physics mode.", FlyMode.VANILLA));
    public final BoolSetting antiKick = add(new BoolSetting("AntiKick", "Slight downward dip every 35 ticks.", true));

    private int kickTicks = 0;

    public Fly() {
        super("Fly", Category.MOVEMENT, "Creative-style flight with Vanilla, Glide, and Packet modes plus AntiKick.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        kickTicks++;
        switch (mode.getValue()) {
            case VANILLA -> {
                mc.player.getAbilities().allowFlying = true;
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().setFlySpeed(speed.getValue().floatValue());
            }
            case GLIDE -> {
                Vec3d v = mc.player.getVelocity();
                double vy = mc.options.jumpKey.isPressed() ? verticalSpeed.getValue() : (mc.options.sneakKey.isPressed() ? -verticalSpeed.getValue() : -0.04);
                mc.player.setVelocity(v.x, vy, v.z);
            }
            case PACKET -> {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.0, v.z);
                if (mc.getNetworkHandler() != null) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                }
            }
        }
        if (antiKick.getValue() && kickTicks >= 35) {
            kickTicks = 0;
            Vec3d v = mc.player.getVelocity();
            mc.player.setVelocity(v.x, -0.04, v.z);
        }
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
