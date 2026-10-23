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

public class Backtrack extends Module {
    public final IntSetting maxLatencyMs = add(new IntSetting("Window (ms)", "Backtrack position history window.", 200, 50, 600));
    public final ColorSettingtrailColor = add(new ColorSetting("Marker Color", "Backtrack ghost marker color.", 0xFFEF4444));
    public final BoolSetting renderGhost = add(new BoolSetting("Render Ghost", "Show recorded position marker.", true));

    private int tickTimer = 0;
    private boolean keyHeldState = false;

    public Backtrack() {
        super("Backtrack", Category.COMBAT, "Records recent enemy positions within a latency window and renders ghost markers.");
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        tickTimer++;

    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!renderGhost.getValue()) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && p.isAlive() && mc.player.squaredDistanceTo(p) <= 256.0) {
                Vec3d past = new Vec3d(p.lastRenderX, p.lastRenderY + 1.0, p.lastRenderZ);
                int[] scr = ModuleUtils.worldToScreen(ctx, past);
                if (scr != null) ctx.fill(scr[0] - 4, scr[1] - 6, scr[0] + 4, scr[1] + 6, trailColor.getValue());
            }
        }
    }
}
