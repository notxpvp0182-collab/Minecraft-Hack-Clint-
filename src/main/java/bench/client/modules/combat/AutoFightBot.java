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
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;
import java.util.Locale;

public class AutoFightBot extends Module {
    public final BoolSetting playersOnly = add(new BoolSetting("PlayersOnly", "Target only players.", true));
    public final KeybindSetting lockKey = add(new KeybindSetting("LockKey", GLFW.GLFW_KEY_R));
    public final DoubleSetting reach = add(new DoubleSetting("HitReach", "Attack reach.", 3.0, 2.5, 4.5));
    public final DoubleSetting scanRange = add(new DoubleSetting("ScanRange", "Target search range.", 16.0, 6.0, 32.0));
    public final DoubleSetting aimSpeed = add(new DoubleSetting("AimSpeed", "360 smooth aim speed.", 0.38, 0.1, 1.0));
    public final BoolSetting autoWASD = add(new BoolSetting("AutoWASD", "Automatically chase and strafe target.", true));
    public final BoolSetting autoJumpCrit = add(new BoolSetting("AutoJumpCrit", "Jump for critical hits.", true));
    private LivingEntity lockedTarget = null;
    private boolean lockHeld = false;
    private int strafeDir = 1, strafeTicks = 0;

    public AutoFightBot() {
        super("AutoFightBot", Category.COMBAT, "Full autonomous combat state machine with 360 aim, WASD strafe, and crits.");
    }

    @Override
    public void onDisable() {
        lockedTarget = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false);
        }
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        int code = lockKey.getValue();
        if (code > 0 && InputUtil.isKeyPressed(mc.getWindow(), code)) {
            if (!lockHeld) {
                lockHeld = true;
                LivingEntity t = ModuleUtils.findNearestTarget(mc, scanRange.getValue(), playersOnly.getValue());
                if (t != null) { lockedTarget = t; NotificationManager.push("AutoFightBot", "Locked: " + t.getName().getString(), true); }
            }
        } else lockHeld = false;

        if (lockedTarget != null && (!lockedTarget.isAlive() || lockedTarget.isDead() || mc.player.squaredDistanceTo(lockedTarget) > scanRange.getValue() * scanRange.getValue())) lockedTarget = null;
        if (lockedTarget == null) lockedTarget = ModuleUtils.findNearestTarget(mc, scanRange.getValue(), playersOnly.getValue());
        if (lockedTarget == null) return;

        ModuleUtils.smoothAimAt(mc.player, lockedTarget, aimSpeed.getValue().floatValue());
        double dist = Math.sqrt(mc.player.squaredDistanceTo(lockedTarget));
        if (autoWASD.getValue()) {
            mc.options.forwardKey.setPressed(dist > 1.4);
            mc.options.backKey.setPressed(dist < 0.9);
            mc.player.setSprinting(true);
            if (dist <= reach.getValue() + 1.2) {
                if (++strafeTicks > 18 || mc.player.horizontalCollision) { strafeDir = -strafeDir; strafeTicks = 0; }
                mc.options.leftKey.setPressed(strafeDir < 0); mc.options.rightKey.setPressed(strafeDir > 0);
            }
            if (autoJumpCrit.getValue() && mc.player.isOnGround() && dist <= reach.getValue() + 1.5) mc.player.jump();
        }
        if (dist <= reach.getValue() && mc.player.getAttackCooldownProgress(0.5f) >= 0.92f) {
            mc.interactionManager.attackEntity(mc.player, lockedTarget);
            mc.player.swingHand(Hand.MAIN_HAND);
            ClientDataManager.recordLeftClick();
        }
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        if (lockedTarget != null && lockedTarget.isAlive()) {
            ModuleUtils.drawHudBadge(ctx, 8, ctx.getScaledWindowHeight() - 48, String.format(Locale.ROOT, "Target: %s (%.1f HP)", lockedTarget.getName().getString(), lockedTarget.getHealth()), 0xFFDC2626);
        }
    }
}
