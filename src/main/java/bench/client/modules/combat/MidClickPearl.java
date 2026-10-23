package bench.client.modules.combat;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ClientDataManager;
import bench.client.core.ColorSetting;
import bench.client.core.DoubleSetting;
import bench.client.core.EnumSetting;
import bench.client.core.IntSetting;
import bench.client.core.KeybindSetting;
import bench.client.core.Module;
import bench.client.core.ModuleManager;
import bench.client.core.NotificationManager;
import bench.client.modules.ModuleUtils;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MidClickPearl extends Module {
    public final KeybindSetting throwKey = add(new KeybindSetting("Pearl Key", GLFW.GLFW_KEY_Z));
    public final BoolSetting statusAlerts = add(new BoolSetting("Action Bar Alerts", "Show cooldown/missing alerts.", true));

    private int tickTimer = 0;
    private boolean keyHeldState = false;

    public MidClickPearl() {
        super("MidClickPearl", Category.COMBAT, "Throws a hotbar Ender Pearl on keybind press and restores original slot.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        tickTimer++;
        if (mc.currentScreen != null || mc.interactionManager == null) { keyHeldState = false; return; }
        int code = throwKey.getValue();
        if (code > 0 && InputUtil.isKeyPressed(mc.getWindow(), code)) {
            if (!keyHeldState) {
                keyHeldState = true;
                int p = ModuleUtils.findHotbarItem(mc.player.getInventory(), Items.ENDER_PEARL);
                if (p == -1) {
                    if (statusAlerts.getValue()) mc.player.sendMessage(Text.literal("No Ender Pearl!").formatted(Formatting.RED), true);
                    return;
                }
                int orig = mc.player.getInventory().getSelectedSlot();
                mc.player.getInventory().setSelectedSlot(p);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.getInventory().setSelectedSlot(orig);
            }
        } else keyHeldState = false;
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

    }
}
