package bench.client.modules.combat;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ClientDataManager;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

public class KillAura extends Module {
    public final DoubleSetting range = add(new DoubleSetting("Range", "Attack range in blocks.", 3.8, 2.0, 6.0));
    public final BoolSetting playersOnly = add(new BoolSetting("PlayersOnly", "Target players only.", false));
    public final BoolSetting smoothAim = add(new BoolSetting("SmoothAim", "Rotate smoothly toward target.", true));

    public KillAura() {
        super("KillAura", Category.COMBAT, "Auto-attacks entities in range; swing, crit, multi-target.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
        LivingEntity target = ModuleUtils.findNearestTarget(mc, range.getValue(), playersOnly.getValue());
        if (target != null) {
            if (smoothAim.getValue()) {
                ModuleUtils.smoothAimAt(mc.player, target, 0.45f);
            }
            if (mc.player.getAttackCooldownProgress(0.5f) >= 0.92f) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                ClientDataManager.recordLeftClick();
            }
        }
    }
}
