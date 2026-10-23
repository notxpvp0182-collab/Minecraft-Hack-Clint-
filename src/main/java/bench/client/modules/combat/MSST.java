package bench.client.modules.combat;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ClientDataManager;
import bench.client.core.DoubleSetting;
import bench.client.core.KeybindSetting;
import bench.client.core.Module;
import bench.client.core.NotificationManager;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Random;

public class MSST extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Range", "Max target lock distance.", 4.2, 1.5, 6.0));
    public final DoubleSetting fov = add(new DoubleSetting("FOV", "Field of view cone in degrees.", 90.0, 15.0, 180.0));
    public final DoubleSetting smoothing = add(new DoubleSetting("Smoothing", "Interpolation factor (lower = more human).", 0.18, 0.05, 1.0));
    public final DoubleSetting yawSpeed = add(new DoubleSetting("Yaw Speed", "Horizontal rotation speed.", 0.18, 0.05, 1.0));
    public final DoubleSetting pitchSpeed = add(new DoubleSetting("Pitch Speed", "Vertical rotation speed.", 0.12, 0.05, 1.0));
    public final DoubleSetting maxStep = add(new DoubleSetting("Max Step", "Maximum angle step per tick.", 18.0, 2.0, 45.0));
    public final BoolSetting vertical = add(new BoolSetting("Vertical", "Aim vertically (pitch).", true));
    public final DoubleSetting jitterAmount = add(new DoubleSetting("Jitter Amount", "Gaussian human aim jitter.", 0.55, 0.0, 2.0));
    public final BoolSetting lineOfSight = add(new BoolSetting("Line of Sight", "Require clear line of sight.", true));
    public final BoolSetting autoAttack = add(new BoolSetting("Auto Attack", "Attack automatically when in reach.", true));
    public final BoolSetting criticals = add(new BoolSetting("Criticals", "Jump-timed critical attacks.", true));
    public final DoubleSetting reach = add(new DoubleSetting("Reach", "Attack reach distance (clamped <= 4.5).", 3.0, 2.0, 4.5));
    public final KeybindSetting lockBind = add(new KeybindSetting("Target Lock Bind", GLFW.GLFW_KEY_R));
    public final BoolSetting antiBot = add(new BoolSetting("AntiBot", "Filter out unverified server bots.", true));
    public final BoolSetting teams = add(new BoolSetting("Teams", "Ignore teammates on same scoreboard team.", false));
    public final BoolSetting friends = add(new BoolSetting("Friends", "Ignore players on Friends list.", true));

    private static LivingEntity lockedTarget = null;
    private static MSST instance;
    private final Random random = new Random();
    private boolean lockKeyHeld = false;

    public MSST() {
        super("MSST", Category.COMBAT, "Smooth aimbot + target lock with Gaussian jitter and packet rotation.");
        instance = this;
    }

    public static boolean holdingLock() {
        return instance != null && instance.isEnabled() && lockedTarget != null && lockedTarget.isAlive();
    }

    public static LivingEntity getLockedTarget() {
        return lockedTarget;
    }

    public static void lockTarget(LivingEntity target) {
        lockedTarget = target;
    }

    public static void unlockTarget() {
        lockedTarget = null;
    }

    @Override
    public void onDisable() {
        lockedTarget = null;
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            lockedTarget = null;
            return;
        }

        if (mc.currentScreen == null) {
            int code = lockBind.getValue();
            if (code > 0 && InputUtil.isKeyPressed(mc.getWindow(), code)) {
                if (!lockKeyHeld) {
                    lockKeyHeld = true;
                    if (lockedTarget != null) {
                        lockedTarget = null;
                        NotificationManager.push("MSST", "Target Unlocked", false);
                    } else {
                        lockedTarget = findCandidate(mc);
                        if (lockedTarget != null) {
                            NotificationManager.push("MSST", "Locked: " + lockedTarget.getName().getString(), true);
                        }
                    }
                }
            } else {
                lockKeyHeld = false;
            }
        }

        if (lockedTarget != null) {
            double maxSq = range.getValue() * range.getValue();
            if (!lockedTarget.isAlive() || lockedTarget.isDead() || lockedTarget.isRemoved()
                    || mc.player.squaredDistanceTo(lockedTarget) > maxSq
                    || (lineOfSight.getValue() && !mc.player.canSee(lockedTarget))) {
                lockedTarget = null;
            }
        }

        if (lockedTarget == null) {
            return;
        }

        Vec3d eye = mc.player.getEyePos();
        Vec3d targetEye = lockedTarget.getEyePos();
        Vec3d diff = targetEye.subtract(eye);
        double horiz = Math.hypot(diff.x, diff.z);

        float goalYaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0f;
        float goalPitch = (float) (-(MathHelper.atan2(diff.y, horiz) * (180.0 / Math.PI)));

        float yawDelta = MathHelper.wrapDegrees(goalYaw - mc.player.getYaw());
        float pitchDelta = MathHelper.wrapDegrees(goalPitch - mc.player.getPitch());

        float smoothFactor = smoothing.getValue().floatValue();
        float ySpd = yawSpeed.getValue().floatValue();
        float pSpd = pitchSpeed.getValue().floatValue();
        float jitter = jitterAmount.getValue().floatValue();
        float maxDeg = maxStep.getValue().floatValue();

        float stepYaw = yawDelta * Math.min(1.0f, smoothFactor + ySpd * 0.5f) + (float) (random.nextGaussian() * jitter);
        stepYaw = MathHelper.clamp(stepYaw, -maxDeg, maxDeg);
        float newYaw = mc.player.getYaw() + stepYaw;

        float newPitch = mc.player.getPitch();
        if (vertical.getValue()) {
            float stepPitch = pitchDelta * Math.min(1.0f, smoothFactor + pSpd * 0.5f) + (float) (random.nextGaussian() * jitter * 0.6f);
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

        if (autoAttack.getValue()) {
            double dist = Math.sqrt(mc.player.squaredDistanceTo(lockedTarget));
            double clampedReach = Math.min(4.5, reach.getValue());
            if (dist <= clampedReach) {
                if (criticals.getValue() && mc.player.isOnGround() && !mc.player.isSubmergedInWater()) {
                    mc.player.jump();
                }
                boolean critReady = !criticals.getValue()
                        || mc.player.fallDistance > 0.05f
                        || mc.player.isSubmergedInWater();
                if (mc.player.getAttackCooldownProgress(0.5f) >= 0.92f && critReady) {
                    mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(lockedTarget, mc.player.isSprinting()));
                    mc.interactionManager.attackEntity(mc.player, lockedTarget);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    ClientDataManager.recordLeftClick();
                }
            }
        }
    }

    private LivingEntity findCandidate(MinecraftClient mc) {
        double maxSq = range.getValue() * range.getValue();
        double halfFov = fov.getValue();
        LivingEntity best = null;
        double bestSq = maxSq;

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity liv) || !liv.isAlive() || liv.isDead()) continue;
            if (!isValidTarget(mc, liv)) continue;
            double dSq = mc.player.squaredDistanceTo(liv);
            if (dSq > bestSq) continue;
            if (getAngleTo(mc, liv) > halfFov) continue;
            bestSq = dSq;
            best = liv;
        }
        return best;
    }

    public boolean isValidTarget(MinecraftClient mc, LivingEntity liv) {
        if (lineOfSight.getValue() && !mc.player.canSee(liv)) return false;
        if (liv instanceof PlayerEntity p) {
            if (friends.getValue() && ClientDataManager.isFriend(p.getName().getString())) return false;
            if (teams.getValue() && mc.player.isTeammate(p)) return false;
            if (antiBot.getValue() && mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
                if (entry == null) return false;
            }
        }
        return true;
    }

    public static double getAngleTo(MinecraftClient mc, Entity entity) {
        Vec3d diff = entity.getEyePos().subtract(mc.player.getEyePos());
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0f;
        return Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        if (holdingLock()) {
            String txt = String.format(Locale.ROOT, "MSST Lock: %s (%.1f HP)", lockedTarget.getName().getString(), lockedTarget.getHealth());
            ModuleUtils.drawHudBadge(ctx, getHudX(), getHudY(), txt, 0xFFDC2626);
        }
    }
}
