package bench.client.modules.movement;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;

public class Sprint extends Module {
    public final BoolSetting omni = add(new BoolSetting("OmniSprint", "Sprint in all directions.", false));

    public Sprint() {
        super("Sprint", Category.MOVEMENT, "Permanent sprint while moving.");
        loadStateFromConfig(true);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.currentScreen != null) return;
        Vec2f m = mc.player.input.getMovementInput();
        boolean moving = omni.getValue() ? (m.x != 0 || m.y != 0) : (m.y > 0);
        if (moving && !mc.player.isSneaking() && !mc.player.horizontalCollision) {
            mc.player.setSprinting(true);
        }
    }
}
