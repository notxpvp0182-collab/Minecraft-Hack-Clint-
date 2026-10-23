package bench.client.modules.combat;

import bench.client.core.Category;
import bench.client.core.DoubleSetting;
import bench.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public class AutoTotem extends Module {
    public final DoubleSetting health = add(new DoubleSetting("Health", "HP threshold to swap totem.", 16.0, 2.0, 20.0));

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT, "Keeps Totem of Undying in offhand; swaps from inventory.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.getValue()) return;
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                int slot = (i < 9) ? (i + 36) : i;
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
                break;
            }
        }
    }
}
