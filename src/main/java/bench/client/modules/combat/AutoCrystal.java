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

public class AutoCrystal extends Module {
    public final DoubleSetting placeRange = add(new DoubleSetting("Place Range", "Max crystal placement range.", 4.5, 2.0, 6.0));
    public final DoubleSetting breakRange = add(new DoubleSetting("Break Range", "Max crystal detonation range.", 4.5, 2.0, 6.0));
    public final IntSetting delayTicks = add(new IntSetting("Action Delay", "Ticks between crystal actions.", 2, 1, 10));
    public final BoolSetting autoSwitch = add(new BoolSetting("Auto Switch", "Switch to End Crystal in hotbar.", true));

    private int tickTimer = 0;
    private boolean keyHeldState = false;

    public AutoCrystal() {
        super("AutoCrystal", Category.COMBAT, "Places and detonates End Crystals on nearby obsidian or bedrock.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        tickTimer++;
        if (mc.interactionManager == null || mc.currentScreen != null || tickTimer < delayTicks.getValue()) return;
        for (Entity e : mc.world.getEntities()) {
            if (e.getType() == net.minecraft.entity.EntityType.END_CRYSTAL && mc.player.squaredDistanceTo(e) <= breakRange.getValue() * breakRange.getValue()) {
                mc.interactionManager.attackEntity(mc.player, e);
                mc.player.swingHand(Hand.MAIN_HAND);
                tickTimer = 0;
                return;
            }
        }
        if (mc.crosshairTarget instanceof BlockHitResult bhr && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.world.getBlockState(bhr.getBlockPos());
            if (st.isOf(Blocks.OBSIDIAN) || st.isOf(Blocks.BEDROCK)) {
                int cSlot = ModuleUtils.findHotbarItem(mc.player.getInventory(), Items.END_CRYSTAL);
                if (cSlot != -1 && autoSwitch.getValue()) {
                    mc.player.getInventory().setSelectedSlot(cSlot);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    tickTimer = 0;
                }
            }
        }
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

    }
}
