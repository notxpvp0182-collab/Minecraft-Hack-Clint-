package bench.client.modules.combat;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ClientDataManager;
import bench.client.core.DoubleSetting;
import bench.client.core.EnumSetting;
import bench.client.core.IntSetting;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class KillAura extends Module {
    public enum RotationMode { SILENT, CLIENT }
    public enum TargetType { PLAYERS, MOBS, ALL }

    public final DoubleSetting range = add(new DoubleSetting("Range", "Attack range in blocks.", 4.0, 2.0, 6.0));
    public final IntSetting cps = add(new IntSetting("CPS", "Attacks per second.", 10, 1, 20));
    public final EnumSetting<RotationMode> rotations = add(new EnumSetting<>("Rotations", "Silent packet or client view rotation.", RotationMode.SILENT));
    public final EnumSetting<TargetType> targetMode = add(new EnumSetting<>("Target Mode", "Entities to attack.", TargetType.ALL));
    public final BoolSetting autoBlock = add(new BoolSetting("AutoBlock", "Raise offhand shield between hits.", false));
    public final BoolSetting multiTarget = add(new BoolSetting("MultiTarget", "Attack all valid targets in range.", false));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", "Allow attacking targets behind walls.", false));

    private int tickCounter = 0;

    public KillAura() {
        super("KillAura", Category.COMBAT, "Auto-attacks entities in range with configurable rotations, multi-target, and CPS.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null || mc.currentScreen != null) {
            return;
        }
        tickCounter++;
        int interval = Math.max(1, 20 / cps.getValue());
        if (tickCounter < interval || mc.player.getAttackCooldownProgress(0.5f) < 0.90f) {
            return;
        }

        double maxSq = range.getValue() * range.getValue();
        boolean attacked = false;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity liv) || !liv.isAlive() || liv.isDead()) continue;
            if (!throughWalls.getValue() && !mc.player.canSee(liv)) continue;
            if (targetMode.getValue() == TargetType.PLAYERS && !(liv instanceof PlayerEntity)) continue;
            if (targetMode.getValue() == TargetType.MOBS && liv instanceof PlayerEntity) continue;
            if (liv instanceof PlayerEntity p && ClientDataManager.isFriend(p.getName().getString())) continue;
            if (mc.player.squaredDistanceTo(liv) > maxSq) continue;

            Vec3d diff = liv.getEyePos().subtract(mc.player.getEyePos());
            float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0f;
            float pitch = (float) (-(MathHelper.atan2(diff.y, Math.hypot(diff.x, diff.z)) * (180.0 / Math.PI)));

            if (rotations.getValue() == RotationMode.CLIENT) {
                ModuleUtils.smoothAimAt(mc.player, liv, 0.45f);
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
            }

            mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(liv, mc.player.isSprinting()));
            mc.interactionManager.attackEntity(mc.player, liv);
            mc.player.swingHand(Hand.MAIN_HAND);
            ClientDataManager.recordLeftClick();
            attacked = true;

            if (!multiTarget.getValue()) {
                break;
            }
        }

        if (attacked) {
            tickCounter = 0;
            if (autoBlock.getValue()) {
                mc.options.useKey.setPressed(true);
            }
        }
    }
}
