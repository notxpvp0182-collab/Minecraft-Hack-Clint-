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

public class Surround extends Module {
    public final BoolSetting onlyOnGround = add(new BoolSetting("Only On Ground", "Place only when grounded.", true));
    public final BoolSetting centerPlayer = add(new BoolSetting("Center On Block", "Nudge player to block center.", true));
    public final BoolSetting disableOnJump = add(new BoolSetting("Disable On Jump", "Turn off when player jumps.", true));

    private int tickTimer = 0;
    private boolean keyHeldState = false;

    public Surround() {
        super("Surround", Category.COMBAT, "Places obsidian around player feet for crystal blast protection.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        tickTimer++;
        if (mc.interactionManager == null || mc.currentScreen != null) return;
        if (disableOnJump.getValue() && !mc.player.isOnGround()) { setEnabled(false); return; }
        if (onlyOnGround.getValue() && !mc.player.isOnGround()) return;
        int obby = ModuleUtils.findHotbarItem(mc.player.getInventory(), Items.OBSIDIAN);
        if (obby == -1) return;
        if (centerPlayer.getValue()) {
            double cx = Math.floor(mc.player.getX()) + 0.5, cz = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setVelocity((cx - mc.player.getX()) * 0.2, mc.player.getVelocity().y, (cz - mc.player.getZ()) * 0.2);
        }
        int prev = mc.player.getInventory().getSelectedSlot();
        BlockPos feet = mc.player.getBlockPos();
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockPos adj = feet.offset(d);
            if (mc.world.getBlockState(adj).isReplaceable()) {
                mc.player.getInventory().setSelectedSlot(obby);
                BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(adj), Direction.UP, adj.down(), false);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            }
        }
        mc.player.getInventory().setSelectedSlot(prev);
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

    }
}
