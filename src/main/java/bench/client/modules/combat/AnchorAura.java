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

public class AnchorAura extends Module {
    public final IntSetting chargeCount = add(new IntSetting("Glowstone Charges", "Number of charges before detonation.", 1, 1, 3));
    public final IntSetting stepDelay = add(new IntSetting("Step Delay (ticks)", "Delay between charge and detonate.", 2, 1, 10));
    public final BoolSetting restoreSlot = add(new BoolSetting("Restore Slot", "Restore weapon slot after detonation.", true));

    private int tickTimer = 0;
    private boolean keyHeldState = false;

    public AnchorAura() {
        super("AnchorAura", Category.COMBAT, "Places, charges with Glowstone, and detonates Respawn Anchors.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        tickTimer++;
        if (mc.interactionManager == null || mc.currentScreen != null || tickTimer < stepDelay.getValue()) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = bhr.getBlockPos();
        BlockState st = mc.world.getBlockState(pos);
        PlayerInventory inv = mc.player.getInventory();
        int orig = inv.getSelectedSlot();
        if (st.isOf(Blocks.RESPAWN_ANCHOR)) {
            int c = st.get(RespawnAnchorBlock.CHARGES);
            if (c < chargeCount.getValue()) {
                int gs = ModuleUtils.findHotbarItem(inv, Items.GLOWSTONE);
                if (gs != -1) { inv.setSelectedSlot(gs); mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr); tickTimer = 0; }
            } else {
                int anc = ModuleUtils.findHotbarItem(inv, Items.RESPAWN_ANCHOR);
                if (anc != -1) inv.setSelectedSlot(anc);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                if (restoreSlot.getValue()) inv.setSelectedSlot(orig);
                tickTimer = 0;
            }
        }
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

    }
}
