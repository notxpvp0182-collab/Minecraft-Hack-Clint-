package bench.client.modules.combat;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ClientDataManager;
import bench.client.core.DoubleSetting;
import bench.client.core.EnumSetting;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class AimAssist extends Module {
    public enum Priority { DISTANCE, ANGLE, HEALTH }

    public final DoubleSetting range = add(new DoubleSetting("Range", "Max target distance.", 4.2, 1.5, 6.0));
    public final DoubleSetting fov = add(new DoubleSetting("FOV", "FOV cone in degrees.", 90.0, 15.0, 180.0));
    public final DoubleSetting yawSpeed = add(new DoubleSetting("Yaw Speed", "Horizontal assist speed.", 0.18, 0.05, 1.0));
    public final DoubleSetting pitchSpeed = add(new DoubleSetting("Pitch Speed", "Vertical assist speed.", 0.12, 0.05, 1.0));
    public final DoubleSetting maxStep = add(new DoubleSetting("Max Step", "Maximum degrees per tick.", 18.0, 2.0, 45.0));
    public final BoolSetting vertical = add(new BoolSetting("Vertical", "Adjust vertical pitch.", true));
    public final BoolSetting jitter = add(new BoolSetting("Jitter", "Enable Gaussian human jitter.", true));
    public final DoubleSetting jitterAmount = add(new DoubleSetting("Jitter Amount", "Gaussian jitter scale.", 0.55, 0.0, 2.0));
    public final BoolSetting onlyOnAttack = add(new BoolSetting("Only On Attack", "Only assist while attack key is held.", true));
    public final BoolSetting lineOfSight = add(new BoolSetting("Line of Sight", "Require direct visibility.", true));
    public final EnumSetting<Priority> priority = add(new EnumSetting<>("Priority", "Target selection priority.", Priority.ANGLE));
    public final BoolSetting hysteresis = add(new BoolSetting("Hysteresis", "Retain last target with 0.75x score bonus.", true));

    private LivingEntity lastTarget = null;
    private final Random random = new Random();

    public AimAssist() {
        super("AimAssist", Category.COMBAT, "Smooth FOV-cone rotation that yields to MSST, with hysteresis and jitter.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.currentScreen != null) {
            return;
        }
        // Yield immediately if MSST holds a lock
        if (MSST.holdingLock()) {
            return;
        }
        if (onlyOnAttack.getValue() && !mc.options.attackKey.isPressed()) {
            return;
        }

        LivingEntity target = selectTarget(mc);
        if (target == null) {
            lastTarget = null;
            return;
        }
        lastTarget = target;

        Vec3d diff = target.getEyePos().subtract(mc.player.getEyePos());
        double horiz = Math.hypot(diff.x, diff.z);

        float goalYaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0f;
        float goalPitch = (float) (-(MathHelper.atan2(diff.y, horiz) * (180.0 / Math.PI)));

        float yawDelta = MathHelper.wrapDegrees(goalYaw - mc.player.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(goalPitch - mc.player.getPitch());

        float jScale = jitter.getValue() ? jitterAmount.getValue().floatValue() : 0.0f;
        float maxDeg = maxStep.getValue().floatValue();

        float stepYaw = yawDelta * yawSpeed.getValue().floatValue() + (float) (random.nextGaussian() * jScale);
        stepYaw = MathHelper.clamp(stepYaw, -maxDeg, maxDeg);
        float newYaw = mc.player.getYaw() + stepYaw;

        float newPitch = mc.player.getPitch();
        if (vertical.getValue()) {
            float stepPitch = pitchDelta * pitchSpeed.getValue().floatValue() + (float) (random.nextGaussian() * jScale * 0.6f);
            stepPitch = MathHelper.clamp(stepPitch, -maxDeg, maxDeg);
            newPitch = MathHelper.clamp(mc.player.getPitch() + stepPitch, -89.0f, 89.0f);
        }

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
        mc.player.headYaw = newYaw;
        mc.player.bodyYaw = newYaw;

        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                newYaw,
                newPitch,
                mc.player.isOnGround(),
                mc.player.horizontalCollision
        ));
    }

    private LivingEntity selectTarget(MinecraftClient mc) {
        double maxSq = range.getValue() * range.getValue();
        double maxAngle = fov.getValue();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity liv) || !liv.isAlive() || liv.isDead()) continue;
            if (lineOfSight.getValue() && !mc.player.canSee(liv)) continue;
            if (liv instanceof PlayerEntity p) {
                if (ClientDataManager.isFriend(p.getName().getString()) || mc.player.isTeammate(p)) continue;
                if (mc.getNetworkHandler().getPlayerListEntry(p.getUuid()) == null) continue;
            }
            double dSq = mc.player.squaredDistanceTo(liv);
            if (dSq > maxSq) continue;
            double angle = MSST.getAngleTo(mc, liv);
            if (angle > maxAngle) continue;

            double score = switch (priority.getValue()) {
                case DISTANCE -> Math.sqrt(dSq);
                case ANGLE -> angle;
                case HEALTH -> liv.getHealth();
            };
            if (hysteresis.getValue() && liv == lastTarget) {
                score *= 0.75;
            }
            if (score < bestScore) {
                bestScore = score;
                best = liv;
            }
        }
        return best;
    }
}
