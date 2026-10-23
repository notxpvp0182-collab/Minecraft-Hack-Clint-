package bench.client.modules.movement;

import bench.client.core.Category;
import bench.client.core.DoubleSetting;
import bench.client.core.EnumSetting;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public class NoFall extends Module {
    public enum NoFallMode { PACKET, VOID }

    public final EnumSetting<NoFallMode> mode = add(new EnumSetting<>("Mode", "Packet ground spoof or Void bounce.", NoFallMode.PACKET));
    public final DoubleSetting threshold = add(new DoubleSetting("Fall Distance Threshold", "Minimum fall distance to activate.", 2.5, 1.5, 6.0));

    public NoFall() {
        super("NoFall", Category.MOVEMENT, "Negates fall damage via ground packet spoof or void arrest.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.player.isOnGround()) return;
        if (mc.player.fallDistance >= threshold.getValue()) {
            if (mode.getValue() == NoFallMode.VOID && mc.player.getY() < -60) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, 0.0, v.z);
            }
            mc.player.fallDistance = 0.0f;
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
            }
        }
    }
}
