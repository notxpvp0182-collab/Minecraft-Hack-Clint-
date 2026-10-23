package bench.client.modules;

import bench.client.core.ClientDataManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public final class ModuleUtils {
    private ModuleUtils() {}

    public static LivingEntity findNearestTarget(MinecraftClient mc, double maxRange, boolean playersOnly) {
        if (mc.player == null || mc.world == null) return null;
        double maxSq = maxRange * maxRange, bestSq = maxSq;
        LivingEntity best = null;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity liv) || !liv.isAlive() || liv.isDead()) continue;
            if (playersOnly && !(liv instanceof PlayerEntity)) continue;
            if (liv instanceof PlayerEntity p && ClientDataManager.isFriend(p.getName().getString())) continue;
            double dSq = mc.player.squaredDistanceTo(liv);
            if (dSq <= bestSq) { bestSq = dSq; best = liv; }
        }
        return best;
    }

    public static void smoothAimAt(ClientPlayerEntity player, Entity target, float speed) {
        Vec3d eye = player.getEyePos();
        Vec3d tPos = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.68, target.getZ());
        Vec3d diff = tPos.subtract(eye);
        double horiz = Math.hypot(diff.x, diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(MathHelper.atan2(diff.y, horiz) * (180.0 / Math.PI)));
        float dYaw = MathHelper.wrapDegrees(yaw - player.getYaw());
        float dPitch = MathHelper.wrapDegrees(pitch - player.getPitch());
        float s = MathHelper.clamp(speed, 0.05f, 1.0f);
        float newYaw = player.getYaw() + dYaw * s;
        float newPitch = MathHelper.clamp(player.getPitch() + dPitch * s, -89.0f, 89.0f);
        player.setYaw(newYaw); player.setPitch(newPitch);
        player.headYaw = newYaw; player.bodyYaw = newYaw;
    }

    public static int findHotbarItem(PlayerInventory inv, Item item) {
        for (int i = 0; i < 9; i++) if (inv.getStack(i).isOf(item)) return i;
        return -1;
    }

    public static int countItem(PlayerInventory inv, Item item) {
        int c = 0;
        for (int i = 0; i < inv.size(); i++) if (inv.getStack(i).isOf(item)) c += inv.getStack(i).getCount();
        return c;
    }

    public static int[] worldToScreen(DrawContext ctx, Vec3d worldPos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.gameRenderer == null) return null;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d rel = worldPos.subtract(mc.player.getEyePos());
        Vector3f v = new Vector3f((float) rel.x, (float) rel.y, (float) rel.z).rotate(new Quaternionf(cam.getRotation()).conjugate());
        if (v.z >= -0.1f) return null;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        double f = (sh / 2.0) / Math.tan(Math.toRadians(mc.options.getFov().getValue() / 2.0)), d = -v.z;
        return new int[]{(int) (sw / 2.0 + (v.x / d) * f), (int) (sh / 2.0 - (v.y / d) * f), (int) Math.max(1.0, d)};
    }

    public static void drawHudBadge(DrawContext ctx, int x, int y, String txt, int col) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.textRenderer.getWidth(txt) + 12;
        ctx.fill(x, y, x + w, y + 14, 0xCC0A0A0E);
        ctx.fill(x, y, x + 2, y + 14, col);
        ctx.drawTextWithShadow(mc.textRenderer, txt, x + 6, y + 3, 0xFFFFFFFF);
    }

    public static List<BlockPos> get3x3Positions(BlockPos c, Direction f) {
        List<BlockPos> l = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (f.getAxis() == Direction.Axis.Y) l.add(c.add(a, 0, b));
                else if (f.getAxis() == Direction.Axis.Z) l.add(c.add(a, b, 0));
                else l.add(c.add(0, b, a));
            }
        }
        return l;
    }
}
