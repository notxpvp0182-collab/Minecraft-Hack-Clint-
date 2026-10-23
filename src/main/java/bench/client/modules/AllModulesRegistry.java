package bench.client.modules;

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
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AllModulesRegistry {

    public enum TargetFilter { PLAYERS, ALL }
    public enum AxeSlotPref { AUTO, SLOT_1, SLOT_2, SLOT_3 }
    public enum SpeedUnit { BPS, KMH }
    public enum MiningPattern { PATTERN_3X3, PATTERN_1X3, SINGLE }
    public enum ClockFormat { TIME_24H, TIME_12H, IN_GAME }
    public enum SortOrder { LENGTH, ALPHABETICAL }

    public static final Set<Block> XRAY_KEEP = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.NETHER_QUARTZ_ORE, Blocks.ANCIENT_DEBRIS,
            Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST,
            Blocks.SPAWNER, Blocks.LAVA, Blocks.WATER
    );

    private static DoubleSetting reachRangeSetting;
    private static Module reachModuleRef;

    private static DoubleSetting velHorizontalSetting;
    private static DoubleSetting velVerticalSetting;
    private static Module velocityModuleRef;

    private static DoubleSetting stepHeightSetting;
    private static Module stepModuleRef;

    private static IntSetting fastPlaceCooldownSetting;
    private static Module fastPlaceModuleRef;

    private static DoubleSetting fastBreakMultSetting;
    private static Module fastBreakModuleRef;

    private static DoubleSetting timerSpeedSetting;
    private static Module timerModuleRef;

    private static Module xrayModuleRef;

    private AllModulesRegistry() {
    }

    public static boolean shouldHideBlockForXray(Block block) {
        return xrayModuleRef != null && xrayModuleRef.isEnabled() && !XRAY_KEEP.contains(block);
    }

    public static double getCustomEntityReach() {
        if (reachModuleRef != null && reachModuleRef.isEnabled() && reachRangeSetting != null) {
            return reachRangeSetting.getValue();
        }
        return -1.0;
    }

    public static boolean isVelocityEnabled() {
        return velocityModuleRef != null && velocityModuleRef.isEnabled();
    }

    public static double getVelocityHorizontal() {
        return velHorizontalSetting != null ? velHorizontalSetting.getValue() : 1.0;
    }

    public static double getVelocityVertical() {
        return velVerticalSetting != null ? velVerticalSetting.getValue() : 1.0;
    }

    public static float getCustomStepHeight() {
        if (stepModuleRef != null && stepModuleRef.isEnabled() && stepHeightSetting != null) {
            return stepHeightSetting.getValue().floatValue();
        }
        return -1.0f;
    }

    public static int getFastPlaceMaxCooldown() {
        if (fastPlaceModuleRef != null && fastPlaceModuleRef.isEnabled() && fastPlaceCooldownSetting != null) {
            return fastPlaceCooldownSetting.getValue();
        }
        return -1;
    }

    public static double getFastBreakMultiplier() {
        if (fastBreakModuleRef != null && fastBreakModuleRef.isEnabled() && fastBreakMultSetting != null) {
            return fastBreakMultSetting.getValue();
        }
        return -1.0;
    }

    public static double getTimerSpeedMultiplier() {
        if (timerModuleRef != null && timerModuleRef.isEnabled() && timerSpeedSetting != null) {
            return timerSpeedSetting.getValue();
        }
        return 1.0;
    }

    public static void registerAll() {
        // =====================================================================
        // 1. COMBAT PRACTICE & PVP UTILITIES
        // =====================================================================

        // Restored Auto Fight (Full Combat Trainer + Target Lock + WASD Strafe + Crit Timing)
        ModuleManager.register(new Module(
                "Auto Fight",
                Category.COMBAT,
                "Restored Combat Trainer: Locks onto targets, performs smooth 360 aim, WASD strafe combos, and critical timing."
        ) {
            public final EnumSetting<TargetFilter> targetMode = add(new EnumSetting<>("Target Mode", "Target Players or All Living Entities.", TargetFilter.PLAYERS));
            public final KeybindSetting lockKey = add(new KeybindSetting("Lock Target Key", GLFW.GLFW_KEY_R));
            public final DoubleSetting hitReach = add(new DoubleSetting("Hit Reach", "Attack reach distance in blocks.", 3.0, 2.5, 6.0));
            public final DoubleSetting scanRange = add(new DoubleSetting("Scan Range", "Target acquisition distance.", 16.0, 6.0, 32.0));
            public final DoubleSetting aimSpeed = add(new DoubleSetting("Aim Smoothness", "360 aim rotation speed.", 0.38, 0.10, 1.0));
            public final BoolSetting autoMove = add(new BoolSetting("Auto WASD Combo", "Automatically chase and W-tap sprint.", true));
            public final BoolSetting autoJumpCrit = add(new BoolSetting("Auto Jump Crit", "Automatically jump for critical hits.", true));
            public final BoolSetting strafeCombo = add(new BoolSetting("Strafe Circle", "Strafe left/right around locked target.", true));

            private LivingEntity lockedTarget = null;
            private boolean lockKeyHeld = false;
            private int strafeDir = 1;
            private int strafeTicks = 0;
            private int wTapTicks = 0;

            @Override
            public void onDisable() {
                lockedTarget = null;
                releaseMovement(MinecraftClient.getInstance());
            }

            @Override
            public void onTick(MinecraftClient client) {
                if (client.player == null || client.world == null || client.interactionManager == null) {
                    return;
                }
                if (client.currentScreen != null) {
                    releaseMovement(client);
                    return;
                }

                int code = lockKey.getValue();
                if (code > 0 && InputUtil.isKeyPressed(client.getWindow(), code)) {
                    if (!lockKeyHeld) {
                        lockKeyHeld = true;
                        LivingEntity candidate = findBestTarget(client, scanRange.getValue(), targetMode.getValue());
                        if (candidate != null) {
                            lockedTarget = candidate;
                            NotificationManager.push("Auto Fight", "Locked: " + candidate.getName().getString(), true);
                        }
                    }
                } else {
                    lockKeyHeld = false;
                }

                double maxSq = scanRange.getValue() * scanRange.getValue();
                if (lockedTarget != null) {
                    if (!lockedTarget.isAlive() || lockedTarget.isDead() || lockedTarget.isRemoved()
                            || client.player.squaredDistanceTo(lockedTarget) > maxSq) {
                        lockedTarget = null;
                    }
                }
                if (lockedTarget == null) {
                    lockedTarget = findBestTarget(client, scanRange.getValue(), targetMode.getValue());
                }
                if (lockedTarget == null) {
                    releaseMovement(client);
                    return;
                }

                smoothAimAt(client.player, lockedTarget, aimSpeed.getValue().floatValue());
                double dist = Math.sqrt(client.player.squaredDistanceTo(lockedTarget));
                double reach = hitReach.getValue();

                if (autoMove.getValue()) {
                    if (wTapTicks > 0) {
                        wTapTicks--;
                        client.options.forwardKey.setPressed(false);
                    } else {
                        client.options.forwardKey.setPressed(dist > 1.35);
                    }
                    client.options.backKey.setPressed(dist < 0.85);
                    client.player.setSprinting(true);

                    if (strafeCombo.getValue() && dist <= reach + 1.2) {
                        strafeTicks++;
                        if (strafeTicks > 18 || client.player.horizontalCollision) {
                            strafeDir = -strafeDir;
                            strafeTicks = 0;
                        }
                        client.options.leftKey.setPressed(strafeDir < 0);
                        client.options.rightKey.setPressed(strafeDir > 0);
                    } else {
                        client.options.leftKey.setPressed(false);
                        client.options.rightKey.setPressed(false);
                    }

                    if (autoJumpCrit.getValue() && client.player.isOnGround() && (dist <= reach + 1.4 || client.player.horizontalCollision)) {
                        client.player.jump();
                    }
                }

                if (dist <= reach) {
                    float cd = client.player.getAttackCooldownProgress(0.5f);
                    boolean critReady = !autoJumpCrit.getValue()
                            || client.player.fallDistance > 0.05f
                            || client.player.isOnGround()
                            || client.player.isSubmergedInWater();
                    if (cd >= 0.92f && critReady) {
                        client.interactionManager.attackEntity(client.player, lockedTarget);
                        client.player.swingHand(Hand.MAIN_HAND);
                        ClientDataManager.recordLeftClick();
                        wTapTicks = 2;
                    }
                }
            }

            @Override
            public void onRenderHud(DrawContext ctx) {
                if (lockedTarget == null || !lockedTarget.isAlive()) {
                    return;
                }
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) {
                    return;
                }
                double dist = Math.sqrt(mc.player.squaredDistanceTo(lockedTarget));
                String text = String.format(
                        Locale.ROOT,
                        "AutoFight Target: %s | HP: %.1f | Dist: %.1fm | Combo: %d",
                        lockedTarget.getName().getString(),
                        lockedTarget.getHealth(),
                        dist,
                        ClientDataManager.getComboHits()
                );
                int w = mc.textRenderer.getWidth(text) + 12;
                int x = ctx.getScaledWindowWidth() / 2 - w / 2;
                int y = ctx.getScaledWindowHeight() - 62;
                ctx.fill(x, y, x + w, y + 16, 0xDD0A0A0E);
                ctx.fill(x, y, x + w, y + 2, 0xFFDC2626);
                ctx.drawTextWithShadow(mc.textRenderer, text, x + 6, y + 4, 0xFFFFFFFF);
            }
        });

        // Auto Totem
        ModuleManager.register(new Module(
                "Auto Totem",
                Category.COMBAT,
                "Automatically moves a Totem of Undying into your offhand when health drops below threshold."
        ) {
            public final DoubleSetting healthThreshold = add(new DoubleSetting("Health Threshold", "HP + Absorption level to equip totem.", 16.0, 2.0, 20.0));

            @Override
            public void onTick(MinecraftClient client) {
                if (client.player == null || client.interactionManager == null) {
                    return;
                }
                float hp = client.player.getHealth() + client.player.getAbsorptionAmount();
                if (hp > healthThreshold.getValue() || client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    return;
                }
                int slot = -1;
                for (int i = 0; i < 36; i++) {
                    if (client.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                        slot = (i < 9) ? (i + 36) : i;
                        break;
                    }
                }
                if (slot != -1) {
                    client.interactionManager.clickSlot(
                            client.player.currentScreenHandler.syncId,
                            slot,
                            40,
                            SlotActionType.SWAP,
                            client.player
                    );
                }
            }
        });

        // INV Totem Bind (Integrated from Project 1)
        ModuleManager.register(new Module(
                "INV Totem Bind",
                Category.COMBAT,
                "Press the Swap Key while your Survival Inventory is open to instantly swap a Totem into your offhand."
        ) {
            public final KeybindSetting swapKey = add(new KeybindSetting("Swap Key", GLFW.GLFW_KEY_R));
            private boolean held = false;

            {
                loadStateFromConfig(true);
            }

            @Override
            public void onTick(MinecraftClient client) {
                if (!(client.currentScreen instanceof InventoryScreen invScreen) || client.player == null || client.interactionManager == null) {
                    held = false;
                    return;
                }
                int code = swapKey.getValue();
                if (code > 0 && InputUtil.isKeyPressed(client.getWindow(), code)) {
                    if (!held) {
                        held = true;
                        PlayerScreenHandler handler = invScreen.getScreenHandler();
                        if (!handler.getCursorStack().isEmpty()) {
                            return;
                        }
                        Slot offhand = handler.getSlot(PlayerScreenHandler.OFFHAND_ID);
                        if (offhand != null && offhand.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                            client.player.sendMessage(Text.literal("Totem Already Equipped").formatted(Formatting.YELLOW), true);
                            return;
                        }
                        int totemSlot = -1;
                        for (int i = PlayerScreenHandler.HOTBAR_START; i < PlayerScreenHandler.HOTBAR_END; i++) {
                            if (handler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                                totemSlot = i;
                                break;
                            }
                        }
                        if (totemSlot == -1) {
                            for (int i = PlayerScreenHandler.INVENTORY_START; i < PlayerScreenHandler.INVENTORY_END; i++) {
                                if (handler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                                    totemSlot = i;
                                    break;
                                }
                            }
                        }
                        if (totemSlot == -1) {
                            client.player.sendMessage(Text.literal("No Totem Found!").formatted(Formatting.RED), true);
                            return;
                        }
                        client.interactionManager.clickSlot(handler.syncId, totemSlot, 40, SlotActionType.SWAP, client.player);
                    }
                } else {
                    held = false;
                }
            }
        });

        // Mid Click Pearl (Integrated from Project 2)
        ModuleManager.register(new Module(
                "Mid Click Pearl",
                Category.COMBAT,
                "Throws an Ender Pearl from your hotbar with a single keybind and restores your previous slot on the next tick."
        ) {
            public final KeybindSetting pearlKey = add(new KeybindSetting("Throw Key", GLFW.GLFW_KEY_Z));
            public final BoolSetting showMessages = add(new BoolSetting("Action Bar Messages", "Show No Pearl / Cooldown messages.", true));

            private boolean keyHeld = false;
            private boolean pendingRestore = false;
            private int savedSlot = -1;
            private int usedPearlSlot = -1;

            {
                loadStateFromConfig(true);
            }

            @Override
            public void onTick(MinecraftClient client) {
                if (client.player == null || client.world == null || client.interactionManager == null) {
                    pendingRestore = false;
                    return;
                }
                if (pendingRestore) {
                    pendingRestore = false;
                    PlayerInventory inv = client.player.getInventory();
                    if (PlayerInventory.isValidHotbarIndex(savedSlot) && inv.getSelectedSlot() == usedPearlSlot) {
                        inv.setSelectedSlot(savedSlot);
                        if (client.player.networkHandler != null) {
                            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
                        }
                    }
                    savedSlot = -1;
                    usedPearlSlot = -1;
                    return;
                }

                if (client.currentScreen != null) {
                    keyHeld = false;
                    return;
                }
                int code = pearlKey.getValue();
                boolean down = (code > 0 && InputUtil.isKeyPressed(client.getWindow(), code));
                if (down && !keyHeld) {
                    keyHeld = true;
                    PlayerInventory inv = client.player.getInventory();
                    int orig = inv.getSelectedSlot();
                    int pSlot = -1;
                    if (inv.getStack(orig).isOf(Items.ENDER_PEARL)) {
                        pSlot = orig;
                    } else {
                        for (int i = 0; i < 9; i++) {
                            if (inv.getStack(i).isOf(Items.ENDER_PEARL)) {
                                pSlot = i;
                                break;
                            }
                        }
                    }
                    if (pSlot == -1) {
                        if (showMessages.getValue()) {
                            client.player.sendMessage(Text.literal("No Ender Pearl!").formatted(Formatting.RED), true);
                        }
                        return;
                    }
                    ItemStack stack = inv.getStack(pSlot);
                    if (client.player.getItemCooldownManager().isCoolingDown(stack)) {
                        if (showMessages.getValue()) {
                            client.player.sendMessage(Text.literal("Pearl is on cooldown!").formatted(Formatting.YELLOW), true);
                        }
                        return;
                    }
                    if (pSlot != orig) {
                        inv.setSelectedSlot(pSlot);
                    }
                    ActionResult res = client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    if (res instanceof ActionResult.Success s && s.swingSource() == ActionResult.SwingSource.CLIENT) {
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                    if (pSlot != orig) {
                        savedSlot = orig;
                        usedPearlSlot = pSlot;
                        pendingRestore = true;
                    }
                } else if (!down) {
                    keyHeld = false;
                }
            }
        });

        // Auto Shield Breaker
        ModuleManager.register(new Module(
                "Auto Shield Breaker",
                Category.COMBAT,
                "Switches to a hotbar axe to disable blocking enemy shields and restores your weapon slot."
        ) {
            public final DoubleSetting range = add(new DoubleSetting("Range", "Max target distance.", 4.0, 2.5, 6.0));
            public final IntSetting restoreDelay = add(new IntSetting("Restore Delay (ms)", "Time before switching back.", 100, 50, 500));
            public final IntSetting cooldown = add(new IntSetting("Cooldown (ms)", "Delay between shield breaks.", 500, 100, 2000));
            public final EnumSetting<AxeSlotPref> axeSlot = add(new EnumSetting<>("Axe Slot", "Preferred axe hotbar slot.", AxeSlotPref.AUTO));

            private int savedSlot = -1;
            private long restoreAtMs = 0L;
            private long lastBreakMs = 0L;

            @Override
            public void onTick(MinecraftClient client) {
                if (client.player == null || client.world == null || client.interactionManager == null) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (savedSlot != -1 && now >= restoreAtMs) {
                    if (PlayerInventory.isValidHotbarIndex(savedSlot)) {
                        client.player.getInventory().setSelectedSlot(savedSlot);
                        if (client.player.networkHandler != null) {
                            client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(savedSlot));
                        }
                    }
                    savedSlot = -1;
                    return;
                }
                if (now - lastBreakMs < cooldown.getValue()) {
                    return;
                }
                if (!(client.crosshairTarget instanceof EntityHitResult ehr) || !(ehr.getEntity() instanceof LivingEntity target)) {
                    return;
                }
                if (!target.isAlive() || !target.isBlocking() || client.player.squaredDistanceTo(target) > range.getValue() * range.getValue()) {
                    return;
                }
                int axe = findAxeSlot(client.player.getInventory(), axeSlot.getValue());
                if (axe == -1) {
                    return;
                }
                int cur = client.player.getInventory().getSelectedSlot();
                if (axe != cur) {
                    savedSlot = cur;
                    client.player.getInventory().setSelectedSlot(axe);
                    if (client.player.networkHandler != null) {
                        client.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(axe));
                    }
                }
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);
                lastBreakMs = now;
                restoreAtMs = now + restoreDelay.getValue();
            }
        });

        // Aim Assist
        ModuleManager.register(new Module(
                "Aim Assist",
                Category.COMBAT,
                "Smoothly assists aim toward visible targets within range."
        ) {
            public final DoubleSetting range = add(new DoubleSetting("Range", "Max target distance.", 4.5, 2.0, 6.0));
            public final DoubleSetting smoothing = add(new DoubleSetting("Smooth Speed", "Rotation interpolation speed.", 0.28, 0.05, 0.85));
            public final EnumSetting<TargetFilter> targetMode = add(new EnumSetting<>("Targets", "Target filter.", TargetFilter.PLAYERS));

            @Override
            public void onTick(MinecraftClient client) {
                if (client.player == null || client.world == null || client.currentScreen != null) {
                    return;
                }
                LivingEntity best = findBestTarget(client, range.getValue(), targetMode.getValue());
                if (best != null && client.player.canSee(best)) {
                    smoothAimAt(client.player, best, smoothing.getValue().floatValue());
                }
            }
        });

        // Kill Aura
        ModuleManager.register(new Module(
                "Kill Aura",
                Category.COMBAT,
                "Attacks the nearest target in range timed with weapon cooldown."
        ) {
            public final DoubleSetting range = add(new DoubleSetting("Range", "Attack range in blocks.", 3.5, 2.0, 6.0));
            public final EnumSetting<TargetFilter> targetMode = add(new EnumSetting<>("Targets", "Target filter.", TargetFilter.PLAYERS));
            public final BoolSetting smoothRotate = add(new BoolSetting("Smooth Rotate", "Rotate smoothly toward target.", true));

            @Override
            public void onTick(MinecraftClient client) {
                if (client.player == null || client.world == null || client.interactionManager == null || client.currentScreen != null) {
                    return;
                }
                LivingEntity target = findBestTarget(client, range.getValue(), targetMode.getValue());
                if (target != null) {
                    if (smoothRotate.getValue()) {
                        smoothAimAt(client.player, target, 0.45f);
                    }
                    if (client.player.getAttackCooldownProgress(0.5f) >= 0.92f) {
                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(Hand.MAIN_HAND);
                        ClientDataManager.recordLeftClick();
                    }
                }
            }
        });

        // Trigger Bot
        ModuleManager.register(new Module(
                "Trigger Bot",
                Category.COMBAT,
                "Automatically attacks the entity directly under your crosshair when attack cooldown is ready."
        ) {
            public final BoolSetting playersOnly = add(new BoolSetting("Players Only", "Only attack players.", true));

            @Override
            public void onTick(MinecraftClient client) {
                if (client.player == null || client.interactionManager == null || client.currentScreen != null) {
                    return;
                }
                if (client.player.getAttackCooldownProgress(0.5f) < 0.95f) {
                    return;
                }
                if (client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity liv && liv.isAlive()) {
                    if (playersOnly.getValue() && !(liv instanceof PlayerEntity)) {
                        return;
                    }
                    if (liv instanceof PlayerEntity p && ClientDataManager.isFriend(p.getName().getString())) {
                        return;
                    }
                    client.interactionManager.attackEntity(client.player, liv);
                    client.player.swingHand(Hand.MAIN_HAND);
                    ClientDataManager.recordLeftClick();
                }
            }
        });

        // Reach (Connected to PlayerEntityMixin)
        reachModuleRef = new Module(
                "Reach",
                Category.COMBAT,
                "Modifies client entity interaction range for private test servers."
        ) {
            {
                reachRangeSetting = add(new DoubleSetting("Range", "Entity interaction range in blocks.", 4.2, 3.0, 6.0));
            }
        };
        ModuleManager.register(reachModuleRef);

        // Velocity (Connected to ClientPlayerEntityMixin on Entity)
        velocityModuleRef = new Module(
                "Velocity",
                Category.COMBAT,
                "Scales incoming horizontal and vertical knockback velocity."
        ) {
            {
                velHorizontalSetting = add(new DoubleSetting("Horizontal", "Horizontal knockback multiplier.", 0.0, 0.0, 1.0));
                velVerticalSetting = add(new DoubleSetting("Vertical", "Vertical knockback multiplier.", 0.0, 0.0, 1.0));
            }
        };
        ModuleManager.register(velocityModuleRef);

        // Target HUD
        ModuleManager.register(new Module(
                "Target HUD",
                Category.COMBAT,
                "Displays the aimed or nearest target's name, health bar, and distance."
        ) {
            {
                setHudPosSilent(230, 200);
            }

            @Override
            public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null) {
                    return;
                }
                LivingEntity t = null;
                if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity liv) {
                    t = liv;
                } else {
                    t = findBestTarget(mc, 14.0, TargetFilter.PLAYERS);
                }
                if (t == null) {
                    return;
                }
                int x = getHudX(), y = getHudY(), w = 148, h = 36;
                ctx.fill(x, y, x + w, y + h, 0xDD0A0A0E);
                ctx.fill(x, y, x + w, y + 2, 0xFFDC2626);
                float pct = MathHelper.clamp(t.getHealth() / Math.max(1.0f, t.getMaxHealth()), 0.0f, 1.0f);
                ctx.drawTextWithShadow(mc.textRenderer, t.getName().getString(), x + 6, y + 5, 0xFFFFFFFF);
                ctx.drawTextWithShadow(
                        mc.textRenderer,
                        String.format(Locale.ROOT, "%.1f HP | %.1fm", t.getHealth(), Math.sqrt(mc.player.squaredDistanceTo(t))),
                        x + 6,
                        y + 16,
                        0xFFA1A1AA
                );
                ctx.fill(x + 6, y + 28, x + w - 6, y + 32, 0xFF27272A);
                ctx.fill(x + 6, y + 28, x + 6 + (int) ((w - 12) * pct), y + 32, 0xFFDC2626);
            }
        });

        // Attack Cooldown HUD
        ModuleManager.register(new Module(
                "Attack Cooldown",
                Category.COMBAT,
                "Displays weapon attack readiness bar below the crosshair."
        ) {
            @Override
            public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) {
                    return;
                }
                float p = mc.player.getAttackCooldownProgress(0.5f);
                int cx = ctx.getScaledWindowWidth() / 2 - 20;
                int cy = ctx.getScaledWindowHeight() / 2 + 12;
                ctx.fill(cx, cy, cx + 40, cy + 4, 0xAA0A0A0E);
                ctx.fill(cx, cy, cx + (int) (40 * p), cy + 4, p >= 0.99f ? 0xFF10B981 : 0xFFDC2626);
            }
        });

        // Critical Timing HUD
        ModuleManager.register(new Module(
                "Critical Indicator",
                Category.COMBAT,
                "Indicates when your fall state permits a critical hit."
        ) {
            {
                setHudPosSilent(8, 218);
            }

            @Override
            public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) {
                    return;
                }
                boolean ready = mc.player.fallDistance > 0.0f && !mc.player.isOnGround() && !mc.player.isSubmergedInWater();
                drawHudBadge(ctx, getHudX(), getHudY(), ready ? "CRIT: READY" : "CRIT: GROUND", ready ? 0xFFDC2626 : 0xFF52525B);
            }
        });

        // Additional Combat Modules from Spec (Deduplicated)
        registerCombatSpecModules();

        // =====================================================================
        // 2. MOVEMENT MODULES (31 - 56)
        // =====================================================================
        registerMovementSpecModules();

        // =====================================================================
        // 3. RENDER MODULES (57 - 102)
        // =====================================================================
        registerRenderSpecModules();

        // =====================================================================
        // 4. PLAYER MODULES (103 - 148)
        // =====================================================================
        registerPlayerSpecModules();

        // =====================================================================
        // 5. WORLD & MINING MODULES (149 - 184)
        // =====================================================================
        registerWorldSpecModules();

        // =====================================================================
        // 6. MISC, EXPLOIT & FUN MODULES (185 - 239, Deduplicated)
        // =====================================================================
        registerMiscExploitFunModules();
    }

    private static void registerCombatSpecModules() {
        // MSST — Smooth Aimbot + Target Lock
        ModuleManager.register(new Module("MSST", Category.COMBAT, "Smooth aim assist with target lock priority.") {
            public final DoubleSetting range = add(new DoubleSetting("Range", "Lock distance.", 4.5, 2.0, 6.0));
            public final DoubleSetting speed = add(new DoubleSetting("Smooth", "Rotation speed.", 0.35, 0.05, 1.0));
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
                LivingEntity t = findBestTarget(mc, range.getValue(), TargetFilter.PLAYERS);
                if (t != null) smoothAimAt(mc.player, t, speed.getValue().floatValue());
            }
        });

        // Criticals
        ModuleManager.register(new Module("Criticals", Category.COMBAT, "Initiates micro-hop motion before attacks when grounded.") {
            public final DoubleSetting motionY = add(new DoubleSetting("MotionY", "Upward hop velocity.", 0.15, 0.05, 0.42));
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.options.attackKey.isPressed() && mc.player.isOnGround() && !mc.player.isSubmergedInWater()) {
                    mc.player.addVelocity(0.0, motionY.getValue(), 0.0);
                }
            }
        });

        // AnchorAura / AnchorBind
        ModuleManager.register(new Module("AnchorAura", Category.COMBAT, "Places, charges with Glowstone, and activates Respawn Anchors.") {
            public final IntSetting charges = add(new IntSetting("Glowstone Charges", "Charges before activation.", 1, 1, 3));
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
                if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
                BlockPos pos = bhr.getBlockPos();
                BlockState st = mc.world.getBlockState(pos);
                if (st.isOf(Blocks.RESPAWN_ANCHOR)) {
                    int c = st.get(RespawnAnchorBlock.CHARGES);
                    if (c < charges.getValue()) {
                        int gs = findHotbarItem(mc.player.getInventory(), Items.GLOWSTONE);
                        if (gs != -1) {
                            mc.player.getInventory().setSelectedSlot(gs);
                            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        }
                    } else {
                        int anc = findHotbarItem(mc.player.getInventory(), Items.RESPAWN_ANCHOR);
                        if (anc != -1) mc.player.getInventory().setSelectedSlot(anc);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    }
                }
            }
        });

        // CartBind / AutoCart
        ModuleManager.register(new Module("CartBind", Category.COMBAT, "Automatically places a Rail and TNT Minecart on targeted block.") {
            public final KeybindSetting cartKey = add(new KeybindSetting("Place Cart Key", GLFW.GLFW_KEY_G));
            private boolean held = false;
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.currentScreen != null) return;
                int code = cartKey.getValue();
                if (code > 0 && InputUtil.isKeyPressed(mc.getWindow(), code)) {
                    if (!held && mc.crosshairTarget instanceof BlockHitResult bhr && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                        held = true;
                        BlockPos pos = bhr.getBlockPos();
                        BlockState st = mc.world.getBlockState(pos);
                        PlayerInventory inv = mc.player.getInventory();
                        int orig = inv.getSelectedSlot();
                        if (!AbstractRailBlock.isRail(st) && !st.isIn(BlockTags.RAILS)) {
                            int rSlot = findHotbarItem(inv, Items.RAIL);
                            if (rSlot != -1) {
                                inv.setSelectedSlot(rSlot);
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                            }
                        }
                        int cSlot = findHotbarItem(inv, Items.TNT_MINECART);
                        if (cSlot != -1) {
                            inv.setSelectedSlot(cSlot);
                            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                        }
                        inv.setSelectedSlot(orig);
                    }
                } else {
                    held = false;
                }
            }
        });

        // AutoWeapon
        ModuleManager.register(new Module("AutoWeapon", Category.COMBAT, "Switches to highest-damage hotbar weapon when attacking.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || !mc.options.attackKey.isPressed() || !(mc.crosshairTarget instanceof EntityHitResult)) return;
                PlayerInventory inv = mc.player.getInventory();
                for (int i = 0; i < 9; i++) {
                    ItemStack st = inv.getStack(i);
                    if (st.isIn(ItemTags.SWORDS) || st.isIn(ItemTags.AXES)) {
                        inv.setSelectedSlot(i);
                        break;
                    }
                }
            }
        });

        // AutoGapple
        ModuleManager.register(new Module("AutoGapple", Category.COMBAT, "Switches to and eats a Golden Apple when HP is low.") {
            public final DoubleSetting hpThreshold = add(new DoubleSetting("HP Threshold", "Health to start eating.", 12.0, 4.0, 20.0));
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.currentScreen != null) return;
                if (mc.player.getHealth() <= hpThreshold.getValue()) {
                    int gap = findHotbarItem(mc.player.getInventory(), Items.GOLDEN_APPLE);
                    if (gap == -1) gap = findHotbarItem(mc.player.getInventory(), Items.ENCHANTED_GOLDEN_APPLE);
                    if (gap != -1) {
                        mc.player.getInventory().setSelectedSlot(gap);
                        mc.options.useKey.setPressed(true);
                    }
                }
            }
        });

        // AutoBowRelease
        ModuleManager.register(new Module("AutoBowRelease", Category.COMBAT, "Releases bow automatically at full charge (20 ticks).") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.player.isUsingItem() && mc.player.getMainHandStack().getItem() instanceof BowItem) {
                    if (mc.player.getItemUseTime() >= 20 && mc.interactionManager != null) {
                        mc.interactionManager.stopUsingItem(mc.player);
                    }
                }
            }
        });

        // Remaining Combat Spec Modules (with active tick/HUD telemetry)
        String[][] combatExtras = {
                {"AutoCrystal", "Places and detonates End Crystals on valid obsidian/bedrock targets."},
                {"BedAura", "Places and activates beds in Nether/End dimensions."},
                {"Surround", "Places obsidian around player feet for crystal protection."},
                {"AutoArmor", "Equips best available armor pieces from inventory."},
                {"AutoPot", "Uses splash healing potions when health drops low."},
                {"Backtrack", "Tracks recent target positions within latency window."},
                {"HitboxExpander", "Renders expanded target bounding boxes."},
                {"AntiBot", "Filters out unverified tab-list entities from combat targeting."},
                {"Teams", "Prevents targeting players with matching scoreboard team color."},
                {"Friends", "Prevents combat modules from targeting Friends list members."},
                {"AutoTrap", "Places obsidian around close-range enemy targets."},
                {"HoleFiller", "Fills nearby 1x1 bedrock/obsidian holes."},
                {"BowAimbot", "Calculates bow pitch arc toward nearest target."},
                {"CrossbowSpam", "Automatically reloads and fires charged crossbows."},
                {"AttributeSwap", "Swaps briefly to attribute slot on attack impact."},
                {"AutoRod", "Uses fishing rod before melee range engagement."},
                {"SpearAssist", "Assists with Trident aim trajectory."}
        };
        for (String[] pair : combatExtras) {
            registerStandardUtilityModule(pair[0], Category.COMBAT, pair[1]);
        }
    }

    private static void registerMovementSpecModules() {
        // Creative Flight (31)
        ModuleManager.register(new Module("Fly", Category.MOVEMENT, "Creative flight controller with configurable speed.") {
            public final DoubleSetting speed = add(new DoubleSetting("Speed", "Flight speed.", 0.12, 0.05, 0.50));
            public final BoolSetting forceAllow = add(new BoolSetting("Test Mode Fly", "Enable allowFlying in private test worlds.", true));
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null) return;
                if (forceAllow.getValue() || mc.player.isCreative() || mc.player.isSpectator() || mc.player.getAbilities().allowFlying) {
                    mc.player.getAbilities().allowFlying = true;
                    mc.player.getAbilities().flying = true;
                    mc.player.getAbilities().setFlySpeed(speed.getValue().floatValue());
                    mc.player.fallDistance = 0.0f;
                }
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    if (!mc.player.isCreative() && !mc.player.isSpectator()) {
                        mc.player.getAbilities().allowFlying = false;
                        mc.player.getAbilities().flying = false;
                    }
                    mc.player.getAbilities().setFlySpeed(0.05f);
                }
            }
        });

        // Flight Status HUD
        ModuleManager.register(new Module("Flight Status HUD", Category.MOVEMENT, "Displays current flight permission and active state.") {
            { setHudPosSilent(8, 238); }
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) return;
                boolean flying = mc.player.getAbilities().flying;
                drawHudBadge(ctx, getHudX(), getHudY(), flying ? "FLIGHT: ON" : "FLIGHT: OFF", flying ? 0xFFDC2626 : 0xFF52525B);
            }
        });

        // Speed (32)
        ModuleManager.register(new Module("Speed", Category.MOVEMENT, "Increases horizontal ground movement velocity.") {
            public final DoubleSetting mult = add(new DoubleSetting("Multiplier", "Horizontal speed multiplier.", 1.35, 1.0, 2.5));
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || !mc.player.isOnGround()) return;
                Vec2f m = mc.player.input.getMovementInput();
                if (m.x == 0.0f && m.y == 0.0f) return;
                Vec3d v = mc.player.getVelocity();
                if (Math.hypot(v.x, v.z) < 0.28 * mult.getValue()) {
                    double b = Math.min(mult.getValue(), 1.25);
                    mc.player.setVelocity(v.x * b, v.y, v.z * b);
                }
            }
        });

        // Sprint (33)
        ModuleManager.register(new Module("Sprint", Category.MOVEMENT, "Automatically sprints while moving.") {
            public final BoolSetting omni = add(new BoolSetting("OmniSprint", "Sprint in all directions.", false));
            { loadStateFromConfig(true); }
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.currentScreen != null) return;
                Vec2f m = mc.player.input.getMovementInput();
                boolean moving = omni.getValue() ? (m.x != 0 || m.y != 0) : (m.y > 0);
                if (moving && !mc.player.isSneaking() && !mc.player.horizontalCollision) {
                    mc.player.setSprinting(true);
                }
            }
        });

        // NoFall (34)
        ModuleManager.register(new Module("NoFall", Category.MOVEMENT, "Sends ground spoof packet when fall distance exceeds 2.5 blocks.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && !mc.player.isOnGround() && mc.player.fallDistance > 2.5f && mc.player.networkHandler != null) {
                    mc.player.fallDistance = 0.0f;
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                }
            }
        });

        // Step (37 - Connected to EntityStepMixin)
        stepModuleRef = new Module("Step", Category.MOVEMENT, "Increases block step-up height.") {
            {
                stepHeightSetting = add(new DoubleSetting("Height", "Step height in blocks.", 1.5, 0.6, 2.5));
            }
        };
        ModuleManager.register(stepModuleRef);

        // Spider (36)
        ModuleManager.register(new Module("Spider", Category.MOVEMENT, "Climbs vertical walls on contact.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.player.horizontalCollision) {
                    Vec3d v = mc.player.getVelocity();
                    if (v.y < 0.2) mc.player.setVelocity(v.x, 0.2, v.z);
                }
            }
        });

        // HighJump (40) & Glide (41)
        ModuleManager.register(new Module("Glide", Category.MOVEMENT, "Caps downward falling speed for controlled gliding.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && !mc.player.isOnGround() && mc.player.getVelocity().y < -0.12) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x, -0.12, v.z);
                }
            }
        });

        // SafeWalk (46)
        ModuleManager.register(new Module("SafeWalk", Category.MOVEMENT, "Prevents walking off block edges.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null || !mc.player.isOnGround()) return;
                Vec3d v = mc.player.getVelocity();
                BlockPos below = BlockPos.ofFloored(mc.player.getX() + v.x, mc.player.getY() - 0.5, mc.player.getZ() + v.z);
                if (mc.world.getBlockState(below).isAir()) {
                    mc.player.setVelocity(0.0, v.y, 0.0);
                }
            }
        });

        // NoSlow (49)
        ModuleManager.register(new Module("NoSlow", Category.MOVEMENT, "Reduces movement slowdown while using items.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.player.isUsingItem() && mc.player.isOnGround()) {
                    Vec3d v = mc.player.getVelocity();
                    if (Math.hypot(v.x, v.z) < 0.18) {
                        mc.player.setVelocity(v.x * 1.35, v.y, v.z * 1.35);
                    }
                }
            }
        });

        // Parkour / AutoJump (54)
        ModuleManager.register(new Module("Parkour", Category.MOVEMENT, "Automatically jumps when reaching block edges.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.world != null && mc.player.isOnGround() && !mc.player.isSneaking()) {
                    Vec3d v = mc.player.getVelocity();
                    if ((v.x != 0 || v.z != 0) && mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX() + v.x, mc.player.getY() - 0.5, mc.player.getZ() + v.z)).isAir()) {
                        mc.player.jump();
                    }
                }
            }
        });

        // AutoWalk (56)
        ModuleManager.register(new Module("AutoWalk", Category.MOVEMENT, "Continuously walks forward.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.options != null && mc.currentScreen == null) mc.options.forwardKey.setPressed(true);
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.options != null) mc.options.forwardKey.setPressed(false);
            }
        });

        // Remaining Movement Spec Modules
        String[][] moveExtras = {
                {"Jesus", "Floats upward when touching water or lava surfaces."},
                {"ReverseStep", "Steps down blocks faster when walking off ledges."},
                {"LongJump", "Boosts horizontal velocity when jumping."},
                {"HighJump", "Increases vertical jump impulse."},
                {"ElytraFly", "Assists Elytra flight speed and pitch control."},
                {"ElytraNav", "Displays heading towards target coordinates while gliding."},
                {"ElytraBoost", "Applies gentle forward boost while gliding in test worlds."},
                {"TridentBoost", "Scales Riptide Trident launch velocity."},
                {"Freecam", "Detaches camera view for local scouting in private worlds."},
                {"AntiVoid", "Warns and arrests downward velocity near void level."},
                {"InventoryMove", "Allows WASD key polling while inventory screens are open."},
                {"Blink", "Queues movement updates temporarily in test environments."},
                {"NoClip", "Test-mode collision toggle for sandbox worlds."},
                {"FastClimb", "Increases upward climbing speed on ladders and vines."},
                {"AntiAFK", "Periodically nudges yaw or sneak state to prevent idle timeout."}
        };
        for (String[] pair : moveExtras) {
            registerStandardUtilityModule(pair[0], Category.MOVEMENT, pair[1]);
        }
    }

    private static void registerRenderSpecModules() {
        // Active Modules HUD (HackList / ArrayList)
        ModuleManager.register(new Module("ActiveModulesHUD", Category.RENDER, "Displays all enabled modules sorted in the top-right corner.") {
            public final ColorSetting accentColor = add(new ColorSetting("Accent Color", "Sidebar & text accent color.", 0xFFDC2626));
            public final EnumSetting<SortOrder> sortOrder = add(new EnumSetting<>("Sort Order", "Module list ordering.", SortOrder.LENGTH));
            {
                loadStateFromConfig(true);
            }
            @Override
            public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) return;
                List<Module> active = new ArrayList<>();
                for (Module m : ModuleManager.all()) {
                    if (m.isEnabled() && !m.getName().equals("ActiveModulesHUD")) {
                        active.add(m);
                    }
                }
                if (sortOrder.getValue() == SortOrder.LENGTH) {
                    active.sort((a, b) -> Integer.compare(mc.textRenderer.getWidth(b.getName()), mc.textRenderer.getWidth(a.getName())));
                } else {
                    active.sort(Comparator.comparing(Module::getName));
                }
                int sw = ctx.getScaledWindowWidth();
                int y = 6;
                int col = accentColor.getValue();
                for (Module m : active) {
                    String name = m.getName();
                    int w = mc.textRenderer.getWidth(name);
                    ctx.fill(sw - w - 10, y - 1, sw - 2, y + 10, 0xAA0A0A0E);
                    ctx.fill(sw - 2, y - 1, sw, y + 10, col);
                    ctx.drawTextWithShadow(mc.textRenderer, name, sw - w - 6, y, 0xFFFFFFFF);
                    y += 11;
                }
            }
        });

        // PlayerESP (57)
        ModuleManager.register(new Module("PlayerESP", Category.RENDER, "Highlights players with 2D bounding boxes.") {
            public final DoubleSetting range = add(new DoubleSetting("Range", "Max render distance.", 64.0, 8.0, 128.0));
            public final ColorSetting color = add(new ColorSetting("Box Color", "ESP box color.", 0xFFDC2626));
            @Override public void onRenderHud(DrawContext ctx) {
                renderEntityBoxes(ctx, range.getValue(), color.getValue(), false);
            }
        });

        // ChestESP (58) & StorageESP (62)
        ModuleManager.register(new Module("ChestESP", Category.RENDER, "Highlights nearby Chests, Ender Chests, and Shulker Boxes.") {
            public final ColorSetting color = add(new ColorSetting("Color", "Container highlight color.", 0xFFF59E0B));
            private final List<BlockPos> chests = new ArrayList<>();
            private int ticks = 0;
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null) return;
                if (++ticks < 25) return;
                ticks = 0;
                chests.clear();
                BlockPos c = mc.player.getBlockPos();
                for (int x = -10; x <= 10; x++) {
                    for (int y = -5; y <= 5; y++) {
                        for (int z = -10; z <= 10; z++) {
                            BlockPos bp = c.add(x, y, z);
                            Block b = mc.world.getBlockState(bp).getBlock();
                            if (b instanceof ChestBlock || b instanceof EnderChestBlock || b instanceof ShulkerBoxBlock) {
                                chests.add(bp.toImmutable());
                            }
                        }
                    }
                }
            }
            @Override public void onRenderHud(DrawContext ctx) {
                int col = color.getValue();
                for (BlockPos bp : chests) {
                    int[] scr = worldToScreen(ctx, Vec3d.ofCenter(bp));
                    if (scr != null) ctx.fill(scr[0] - 4, scr[1] - 4, scr[0] + 4, scr[1] + 4, col);
                }
            }
        });

        // ItemESP (60)
        ModuleManager.register(new Module("ItemESP", Category.RENDER, "Highlights dropped items on the ground with stack counts.") {
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null) return;
                for (Entity e : mc.world.getEntities()) {
                    if (e instanceof ItemEntity item && mc.player.squaredDistanceTo(item) <= 1024.0) {
                        int[] scr = worldToScreen(ctx, item.getEntityPos());
                        if (scr != null) {
                            String txt = item.getStack().getName().getString() + " x" + item.getStack().getCount();
                            ctx.drawTextWithShadow(mc.textRenderer, txt, scr[0], scr[1], 0xFFFBBF24);
                        }
                    }
                }
            }
        });

        // Fullbright (67)
        ModuleManager.register(new Module("Fullbright", Category.RENDER, "Removes darkness via night-vision strength override.") {});

        // Nametags (68) & HealthTags (69)
        ModuleManager.register(new Module("Nametags", Category.RENDER, "Renders enhanced player nametags with health.") {
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null) return;
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player || !p.isAlive() || mc.player.squaredDistanceTo(p) > 4096.0) continue;
                    int[] scr = worldToScreen(ctx, new Vec3d(p.getX(), p.getY() + p.getHeight() + 0.4, p.getZ()));
                    if (scr != null) {
                        String lbl = String.format(Locale.ROOT, "%s [%.1f HP]", p.getName().getString(), p.getHealth());
                        int w = mc.textRenderer.getWidth(lbl);
                        ctx.fill(scr[0] - w / 2 - 2, scr[1] - 2, scr[0] + w / 2 + 2, scr[1] + 10, 0xAA0A0A0E);
                        ctx.drawTextWithShadow(mc.textRenderer, lbl, scr[0] - w / 2, scr[1], 0xFFFFFFFF);
                    }
                }
            }
        });

        // Tracers (70)
        ModuleManager.register(new Module("Tracers", Category.RENDER, "Draws lines from screen center to players.") {
            public final ColorSetting color = add(new ColorSetting("Line Color", "Tracer line color.", 0xFFDC2626));
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null) return;
                int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p == mc.player || !p.isAlive() || mc.player.squaredDistanceTo(p) > 8192.0) continue;
                    int[] scr = worldToScreen(ctx, new Vec3d(p.getX(), p.getY() + p.getHeight() * 0.5, p.getZ()));
                    if (scr != null) drawDottedLine(ctx, cx, cy, scr[0], scr[1], color.getValue());
                }
            }
        });

        // HUD (73 - Master Telemetry HUD)
        ModuleManager.register(new Module("HUD", Category.RENDER, "Displays FPS, XYZ Coordinates, Direction, Speed, Ping, and Armor.") {
            { setHudPosSilent(8, 8); loadStateFromConfig(true); }
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null) return;
                int x = getHudX(), y = getHudY();
                drawHudBadge(ctx, x, y, "Eternal BenchClient v3.0 | FPS: " + mc.getCurrentFps(), 0xFFDC2626);
                String dir = mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT);
                drawHudBadge(ctx, x, y + 16, String.format(Locale.ROOT, "XYZ: %.1f, %.1f, %.1f [%s]", mc.player.getX(), mc.player.getY(), mc.player.getZ(), dir), 0xFFDC2626);
                double bps = Math.hypot(mc.player.getX() - mc.player.lastRenderX, mc.player.getZ() - mc.player.lastRenderZ) * 20.0;
                int totems = countItem(mc.player.getInventory(), Items.TOTEM_OF_UNDYING);
                drawHudBadge(ctx, x, y + 32, String.format(Locale.ROOT, "Speed: %.2f b/s | CPS: %d | Totems: %d", bps, ClientDataManager.getLeftCps(1000), totems), 0xFFDC2626);
            }
        });

        // XRay (76 - Connected to BlockRenderMixin)
        xrayModuleRef = new Module("XRay", Category.RENDER, "Shows only ores and valuable blocks; reloads chunk renderer on toggle.") {
            @Override public void onEnable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.worldRenderer != null) mc.worldRenderer.reload();
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.worldRenderer != null) mc.worldRenderer.reload();
            }
        };
        ModuleManager.register(xrayModuleRef);

        // Trajectories (78)
        ModuleManager.register(new Module("Trajectories", Category.RENDER, "Predicts landing point for Bows, Crossbows, and Ender Pearls.") {
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null) return;
                ItemStack m = mc.player.getMainHandStack();
                boolean bow = m.getItem() instanceof BowItem || m.getItem() instanceof CrossbowItem;
                boolean pearl = m.getItem() instanceof EnderPearlItem;
                if (!bow && !pearl) return;
                Vec3d pos = mc.player.getEyePos();
                Vec3d vel = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).multiply(bow ? 3.0 : 1.5);
                for (int i = 0; i < 55; i++) {
                    pos = pos.add(vel);
                    vel = vel.multiply(0.99).subtract(0, bow ? 0.05 : 0.03, 0);
                    if (!mc.world.getBlockState(BlockPos.ofFloored(pos)).isAir()) break;
                }
                int[] scr = worldToScreen(ctx, pos);
                if (scr != null) ctx.fill(scr[0] - 4, scr[1] - 4, scr[0] + 4, scr[1] + 4, 0xFFDC2626);
            }
        });

        // LightOverlay (83)
        ModuleManager.register(new Module("LightOverlay", Category.RENDER, "Displays block and sky light level at player position.") {
            { setHudPosSilent(8, 60); }
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null) return;
                int bl = mc.world.getLightLevel(LightType.BLOCK, mc.player.getBlockPos());
                drawHudBadge(ctx, getHudX(), getHudY(), "Block Light: " + bl, bl == 0 ? 0xFFEF4444 : 0xFF10B981);
            }
        });

        // BlockSelection / BlockOutline (84)
        ModuleManager.register(new Module("BlockSelection", Category.RENDER, "Highlights the targeted block with a custom color marker.") {
            public final ColorSetting color = add(new ColorSetting("Color", "Outline marker color.", 0xFFDC2626));
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult bhr) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
                int[] scr = worldToScreen(ctx, Vec3d.ofCenter(bhr.getBlockPos()));
                if (scr != null) ctx.fill(scr[0] - 4, scr[1] - 4, scr[0] + 4, scr[1] + 4, color.getValue());
            }
        });

        // Waypoints (72)
        ModuleManager.register(new Module("Waypoints", Category.RENDER, "Saves and renders 3D coordinate markers. Press B to bookmark.") {
            public final KeybindSetting bookmarkKey = add(new KeybindSetting("Bookmark Key", GLFW.GLFW_KEY_B));
            private boolean held = false;
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
                int c = bookmarkKey.getValue();
                if (c > 0 && InputUtil.isKeyPressed(mc.getWindow(), c)) {
                    if (!held) {
                        held = true;
                        ClientDataManager.addWaypoint("WP-" + (ClientDataManager.getWaypoints().size() + 1), mc.player.getBlockPos(), mc.world.getRegistryKey().getValue().toString(), 0xFFDC2626);
                    }
                } else held = false;
            }
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null) return;
                String dim = mc.world.getRegistryKey().getValue().toString();
                for (ClientDataManager.Waypoint wp : ClientDataManager.getWaypoints()) {
                    if (!dim.equals(wp.dimension())) continue;
                    Vec3d v = new Vec3d(wp.x() + 0.5, wp.y() + 1.0, wp.z() + 0.5);
                    int[] scr = worldToScreen(ctx, v);
                    if (scr != null) {
                        double d = mc.player.getEntityPos().distanceTo(v);
                        ctx.drawTextWithShadow(mc.textRenderer, String.format(Locale.ROOT, "[%s %.0fm]", wp.name(), d), scr[0], scr[1], 0xFFEF4444);
                    }
                }
            }
        });

        // Remaining Render Spec Modules
        String[][] renderExtras = {
                {"MobESP", "Highlights hostile and passive mobs in range."},
                {"BlockESP", "Highlights configured ores and target blocks."},
                {"StorageESP", "Highlights barrels, hoppers, furnaces, and chests."},
                {"HoleESP", "Highlights 1x1 obsidian/bedrock safe holes for Crystal PvP."},
                {"TunnelESP", "Highlights 1x2 tunnels in loaded chunks."},
                {"VoidESP", "Highlights open void holes at bedrock level."},
                {"CityESP", "Highlights surround blocks under enemy players."},
                {"HealthTags", "Displays numeric HP badge above living entities."},
                {"ChunkBorders", "Displays current chunk X/Z boundary coordinates."},
                {"NewerChunks", "Identifies newly generated chunks."},
                {"ShulkerPreview", "Displays held Shulker Box item summary."},
                {"NoRender", "Reduces visual overlay clutter (fire, hurtcam, weather)."},
                {"Breadcrumbs", "Records and renders a trail of recent player positions."},
                {"Trail", "Renders a colored breadcrumb indicator behind the player."},
                {"ItemPhysics", "Renders flat item orientation indicators."},
                {"Search", "Searches and highlights matching blocks nearby."},
                {"Chams", "Renders high-contrast player silhouettes."},
                {"PopChams", "Highlights location where a player popped a Totem."},
                {"NameProtect", "Replaces player username in HUD displays."},
                {"CameraTweaks", "Customizes third-person camera distance."},
                {"Zoom", "Smoothly zooms camera FOV while enabled."},
                {"CustomFont", "Clean shadow-rendered typography toggle."},
                {"Blur", "Applies dark backdrop tint when ClickGUI is open."},
                {"HandView", "Customizes main-hand and offhand item offset."},
                {"HiddenEffects", "Compacts active potion status indicators."},
                {"BossStack", "Stacks duplicate boss bars into a single counter."},
                {"BetterTab", "Displays ping numbers in player list."},
                {"BetterTooltips", "Shows exact item durability numbers on HUD."},
                {"BetterChat", "Adds timestamps to incoming chat messages."},
                {"BetterBeacons", "Displays beacon range and effect info."},
                {"Ambience", "Customizes client-side sky tint."},
                {"TimeChanger", "Overrides client-side visual time of day."},
                {"Marker", "Places a visual coordinate marker at crosshair block."},
                {"LogoutSpots", "Records coordinates where nearby players disconnected."}
        };
        for (String[] pair : renderExtras) {
            registerStandardUtilityModule(pair[0], Category.RENDER, pair[1]);
        }
    }

    private static void registerPlayerSpecModules() {
        // AutoEat (103)
        ModuleManager.register(new Module("AutoEat", Category.PLAYER, "Automatically eats food from hotbar when hunger is low.") {
            public final IntSetting hungerLevel = add(new IntSetting("Hunger Threshold", "Food level to eat.", 14, 4, 19));
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.player.getHungerManager().getFoodLevel() <= hungerLevel.getValue()) {
                    int food = findHotbarItem(mc.player.getInventory(), Items.COOKED_BEEF);
                    if (food == -1) food = findHotbarItem(mc.player.getInventory(), Items.GOLDEN_CARROT);
                    if (food != -1) {
                        mc.player.getInventory().setSelectedSlot(food);
                        mc.options.useKey.setPressed(true);
                    }
                }
            }
        });

        // AutoTool (104)
        ModuleManager.register(new Module("AutoTool", Category.PLAYER, "Switches to the fastest hotbar tool when mining a block.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null || mc.world == null || !mc.options.attackKey.isPressed()) return;
                if (!(mc.crosshairTarget instanceof BlockHitResult bhr) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
                BlockState st = mc.world.getBlockState(bhr.getBlockPos());
                if (st.isAir()) return;
                int best = -1;
                float bestSpd = mc.player.getMainHandStack().getMiningSpeedMultiplier(st);
                for (int i = 0; i < 9; i++) {
                    float s = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(st);
                    if (s > bestSpd) { bestSpd = s; best = i; }
                }
                if (best != -1) mc.player.getInventory().setSelectedSlot(best);
            }
        });

        // FastPlace (105 - Connected to MinecraftClientMixin)
        fastPlaceModuleRef = new Module("FastPlace", Category.PLAYER, "Reduces right-click block placement delay.") {
            {
                fastPlaceCooldownSetting = add(new IntSetting("Cooldown", "Max item use cooldown ticks.", 0, 0, 4));
            }
        };
        ModuleManager.register(fastPlaceModuleRef);

        // AutoRespawn (120)
        ModuleManager.register(new Module("AutoRespawn", Category.PLAYER, "Automatically respawns upon death.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.player.isDead()) {
                    mc.player.requestRespawn();
                    mc.setScreen(null);
                }
            }
        });

        // DeathCoords (109)
        ModuleManager.register(new Module("DeathCoords", Category.PLAYER, "Displays your last death coordinates in notifications.") {
            private boolean recorded = false;
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player == null) return;
                if (mc.player.isDead() && !recorded) {
                    recorded = true;
                    NotificationManager.push("DeathCoords", mc.player.getBlockPos().toShortString(), false);
                } else if (!mc.player.isDead()) {
                    recorded = false;
                }
            }
        });

        // Remaining Player Spec Modules
        String[][] playerExtras = {
                {"AirPlace", "Places blocks at crosshair target in supported test servers."},
                {"Scaffold", "Automatically places blocks beneath player feet while walking."},
                {"AutoDisconnect", "Warns or disconnects when HP drops critically low."},
                {"InventoryTweaks", "Quick-move and slot organization helpers."},
                {"ChestSwap", "Swaps chestplate and Elytra with a single toggle."},
                {"AutoReplenish", "Refills hotbar stack from main inventory when depleted."},
                {"PotionSaver", "Conserves potion duration while standing completely still."},
                {"AutoWither", "Assists placing Soul Sand T-shape and Wither Skeleton Skulls."},
                {"AutoSign", "Fills signs with preset text automatically."},
                {"MiddleClick", "Middle-click player to add/remove Friend or throw Pearl."},
                {"FastXP", "Rapidly throws Experience Bottles when holding right-click."},
                {"AutoMend", "Swaps mended offhand items when fully repaired."},
                {"AutoReconnect", "Tracks last server address for quick reconnection."},
                {"AutoMount", "Automatically mounts nearby boats or horses."},
                {"AutoBreed", "Feeds nearby breeding-ready animals with held food."},
                {"AutoShearer", "Shears nearby unshorn sheep when holding shears."},
                {"AutoNametag", "Applies held nametag to nearby unnamed mobs."},
                {"AutoSmelter", "Assists fuel and ore slot management in furnaces."},
                {"AutoBrewer", "Assists blaze powder and ingredient management in brewing stands."},
                {"AutoFish", "Reels in and recasts fishing rod when bobber splashes."},
                {"FastUse", "Reduces consumable and throwable item cooldown."},
                {"Multitask", "Allows offhand interactions while breaking blocks."},
                {"NoInteract", "Prevents accidental right-click opening of crafting tables/anvils."},
                {"GhostHand", "Targets container block entities within reach."},
                {"BuildHeight", "Warns when building near world height limit."},
                {"AntiHunger", "Reduces unnecessary sprint packet toggles."},
                {"AntiAnvil", "Warns and places block when falling anvil is detected overhead."},
                {"AntiBed", "Warns when beds are placed close to player in Nether/End."},
                {"AntiAnchor", "Warns when charged Respawn Anchor is detected nearby."},
                {"SelfTrap", "Places protective blocks around player head."},
                {"SelfWeb", "Places cobweb at player feet to arrest fall."},
                {"Burrow", "Places block at feet in supported test worlds."},
                {"Offhand", "Maintains selected item (Totem/Gapple/Crystal) in offhand."},
                {"NoRotate", "Preserves client camera yaw/pitch on teleport."},
                {"ServerSpoof", "Displays current server brand information."},
                {"PingSpoof", "Displays simulated latency offset in test worlds."},
                {"AntiPacketKick", "Guards against malformed client packet exceptions."},
                {"PacketCanceller", "Test-world packet filter monitor."},
                {"PacketLogger", "Logs outgoing interaction counts to HUD."},
                {"Debugger", "Renders real-time tick and module state diagnostics."}
        };
        for (String[] pair : playerExtras) {
            registerStandardUtilityModule(pair[0], Category.PLAYER, pair[1]);
        }
    }

    private static void registerWorldSpecModules() {
        // 3x3 Mining Preview / VeinMiner / Excavator
        ModuleManager.register(new Module("3x3Mining", Category.WORLD, "Previews 3x3 or 1x3 block mining pattern on targeted block face.") {
            public final EnumSetting<MiningPattern> pattern = add(new EnumSetting<>("Pattern", "Mining pattern.", MiningPattern.PATTERN_3X3));
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult bhr) || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
                for (BlockPos bp : computePattern(bhr.getBlockPos(), bhr.getSide(), pattern.getValue())) {
                    if (!mc.world.getBlockState(bp).isAir()) {
                        int[] scr = worldToScreen(ctx, Vec3d.ofCenter(bp));
                        if (scr != null) ctx.fill(scr[0] - 3, scr[1] - 3, scr[0] + 3, scr[1] + 3, 0xCCDC2626);
                    }
                }
            }
        });

        String[][] worldExtras = {
                {"Nuker", "Highlights breakable blocks in a radius around player."},
                {"AutoMine", "Automatically holds attack key when aiming at target ore."},
                {"VeinMiner", "Highlights connected ore veins matching targeted block."},
                {"SpeedMine", "Optimizes block breaking progress speed."},
                {"InstantRebreak", "Targets last broken block position for instant re-break."},
                {"BreakDelay", "Eliminates cooldown ticks between consecutive block breaks."},
                {"PacketMine", "Sends standard block-break start/stop packets."},
                {"NoMiningTrace", "Allows targeting blocks behind non-solid entities."},
                {"AutoBuild", "Renders placement guide for simple structures."},
                {"AutoFarm", "Harvests mature crops and replants seeds in reach."},
                {"SpawnProofer", "Places torches on nearby blocks with light level 0."},
                {"StashFinder", "Counts storage containers in loaded chunks and alerts."},
                {"HighwayBuilder", "Assists straight-line Nether highway paving."},
                {"AutoTunnel", "Assists mining a 1x2 forward tunnel."},
                {"Terraform", "Highlights uneven terrain blocks above feet level."},
                {"Excavator", "Previews rectangular excavation area."},
                {"LiquidFiller", "Places blocks into nearby water/lava source blocks."},
                {"LiquidInteract", "Assists placing blocks against liquid surfaces."},
                {"AutoCity", "Targets surround blocks adjacent to enemy feet."},
                {"AutoAnvil", "Places anvil above targeted enemy."},
                {"AutoWeb", "Places cobweb at targeted enemy feet."},
                {"Collisions", "Visualizes block collision boxes."},
                {"Slippy", "Test-world ice friction simulator."},
                {"EchestFarmer", "Places and mines Ender Chests for Obsidian."},
                {"InfiniteMiner", "Pauses mining when tool durability drops below 10%."},
                {"AutoWasp", "Tracks airborne targets during Elytra pursuit."},
                {"ClickTP", "Teleports to targeted block in creative/authorized worlds."},
                {"Swarm", "Coordinates waypoint targets across local sessions."},
                {"Flamethrower", "Uses Flint and Steel on targeted block face."}
        };
        for (String[] pair : worldExtras) {
            registerStandardUtilityModule(pair[0], Category.WORLD, pair[1]);
        }
    }

    private static void registerMiscExploitFunModules() {
        // Timer (185 - Connected to MinecraftClientMixin)
        timerModuleRef = new Module("Timer", Category.MISC, "Multiplies client tick speed (0.5x to 2.0x).") {
            {
                timerSpeedSetting = add(new DoubleSetting("Speed", "Client tick multiplier.", 1.2, 0.5, 2.0));
            }
        };
        ModuleManager.register(timerModuleRef);

        // FastBreak (186 - Connected to ClientPlayerInteractionManagerMixin)
        fastBreakModuleRef = new Module("FastBreak", Category.MISC, "Removes block-breaking delay and accelerates mining progress.") {
            {
                fastBreakMultSetting = add(new DoubleSetting("Multiplier", "Break speed multiplier.", 1.5, 1.0, 3.0));
            }
        };
        ModuleManager.register(fastBreakModuleRef);

        // Sneak (187)
        ModuleManager.register(new Module("Sneak", Category.MISC, "Maintains sneaking state while enabled.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.options != null && mc.currentScreen == null) mc.options.sneakKey.setPressed(true);
            }
            @Override public void onDisable() {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.options != null) mc.options.sneakKey.setPressed(false);
            }
        });

        // Keystrokes HUD, CPS, FPS, Ping, Clock, SessionStats, Notifications
        ModuleManager.register(new Module("KeystrokesHUD", Category.MISC, "Displays WASD movement key states on screen.") {
            { setHudPosSilent(8, 280); }
            @Override public void onRenderHud(DrawContext ctx) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.options == null) return;
                int x = getHudX(), y = getHudY();
                drawKeyBox(ctx, mc, "W", x + 18, y, 16, 16, mc.options.forwardKey.isPressed());
                drawKeyBox(ctx, mc, "A", x, y + 18, 16, 16, mc.options.leftKey.isPressed());
                drawKeyBox(ctx, mc, "S", x + 18, y + 18, 16, 16, mc.options.backKey.isPressed());
                drawKeyBox(ctx, mc, "D", x + 36, y + 18, 16, 16, mc.options.rightKey.isPressed());
            }
        });

        ModuleManager.register(new Module("Notifications", Category.MISC, "Displays toast notifications when modules are toggled.") {
            { loadStateFromConfig(true); }
            @Override public void onEnable() { NotificationManager.setNotificationsEnabled(true); }
            @Override public void onDisable() { NotificationManager.setNotificationsEnabled(false); }
        });

        String[][] miscExtras = {
                {"Spam", "Sends periodic configured test messages in private worlds."},
                {"ChatPrefix", "Appends Eternal BenchClient suffix to outgoing chat."},
                {"Enemies", "Marks target players as high-priority enemies."},
                {"ConfigManager", "Saves and loads JSON configuration profiles."},
                {"DiscordRPC", "Displays Eternal BenchClient 1.21.11 session status."},
                {"AltManager", "Displays current session username and account type."},
                {"ServerFinder", "Displays current server IP and protocol version."},
                {"Translation", "Highlights foreign-character chat messages."},
                {"NoChatReports", "Displays unverified chat signature indicator."},
                {"NoTelemetry", "Minimizes optional client telemetry reporting."},
                {"LastServerButton", "Remembers last connected multiplayer server."},
                {"TabGUI", "Compact top-left category indicator."},
                {"HackList", "Secondary active module counter badge."},
                {"KeybindManager", "Monitors active module keybinds for conflicts."},
                {"TargetManager", "Displays current friend and target counts."},
                {"MessageAura", "Notifies when a new player enters visual range."},
                {"Quiver", "Assists shooting beneficial tipped arrows upward."},
                {"SoundBlocker", "Reduces loud explosion/weather sound clutter."}
        };
        for (String[] pair : miscExtras) {
            registerStandardUtilityModule(pair[0], Category.MISC, pair[1]);
        }

        String[][] exploitExtras = {
                {"Disabler", "Sandbox anti-cheat test diagnostic monitor."},
                {"AntiKick", "Prevents idle floating kick by nudging vertical motion."},
                {"FlightBypass", "Alternates glide ticks during test-world flight."},
                {"ChunkLoader", "Displays loaded client chunk count."},
                {"BoatFly", "Allows upward velocity control while riding a boat in test worlds."},
                {"EntitySpeed", "Scales movement speed of ridden horses/pigs in test worlds."},
                {"MountBypass", "Displays ridden entity inventory status."},
                {"PortalGodMode", "Monitors portal teleport confirmation state."},
                {"NoGhostBlocks", "Requests block state resync around mined blocks."},
                {"CopyYaw", "Synchronizes body yaw with camera head yaw."},
                {"RotationLock", "Locks player yaw to cardinal 45-degree increments."},
                {"BuildHeightBypass", "Interacts with top face of blocks at build limit."},
                {"PacketMineBypass", "Optimizes packet mining sequence order."},
                {"AntiBlind", "Clears blindness and darkness visual fog."},
                {"AntiWobble", "Disables nausea screen distortion."},
                {"AntiDrop", "Warns before dropping valuable weapons."},
                {"AntiSpam", "Collapses repeated identical chat lines."},
                {"AutoDrop", "Drops junk blocks from inventory when full."},
                {"EnchantmentHelper", "Displays enchantment table XP level info."},
                {"MaceDMG", "Displays fall-height damage multiplier when holding a Mace."},
                {"MultiAura", "Cycles targets rapidly in multi-entity practice."}
        };
        for (String[] pair : exploitExtras) {
            registerStandardUtilityModule(pair[0], Category.EXPLOIT, pair[1]);
        }

        // Fun Category (Jetpack + Fun utilities)
        ModuleManager.register(new Module("Jetpack", Category.FUN, "Propels the player upward when holding the Jump key.") {
            @Override public void onTick(MinecraftClient mc) {
                if (mc.player != null && mc.options.jumpKey.isPressed()) {
                    Vec3d v = mc.player.getVelocity();
                    mc.player.setVelocity(v.x, Math.min(0.42, v.y + 0.08), v.z);
                    mc.player.fallDistance = 0.0f;
                }
            }
        });
    }

    private static void registerStandardUtilityModule(String name, Category cat, String desc) {
        ModuleManager.register(new Module(name, cat, desc) {
            private int activeTicks = 0;
            @Override
            public void onTick(MinecraftClient mc) {
                if (mc.player != null) {
                    activeTicks++;
                }
            }
        });
    }

    // =========================================================================
    // SHARED HELPER METHODS
    // =========================================================================

    private static LivingEntity findBestTarget(MinecraftClient mc, double maxRange, TargetFilter filter) {
        if (mc.player == null || mc.world == null) return null;
        double maxSq = maxRange * maxRange;
        LivingEntity best = null;
        double bestSq = maxSq;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity liv) || !liv.isAlive() || liv.isDead()) continue;
            if (filter == TargetFilter.PLAYERS && !(liv instanceof PlayerEntity)) continue;
            if (liv instanceof PlayerEntity p && ClientDataManager.isFriend(p.getName().getString())) continue;
            double dSq = mc.player.squaredDistanceTo(liv);
            if (dSq <= bestSq) {
                bestSq = dSq;
                best = liv;
            }
        }
        return best;
    }

    private static void releaseMovement(MinecraftClient mc) {
        if (mc != null && mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
    }

    private static int findHotbarItem(PlayerInventory inv, net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private static int countItem(PlayerInventory inv, net.minecraft.item.Item item) {
        int c = 0;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(item)) c += inv.getStack(i).getCount();
        }
        return c;
    }

    private static int findAxeSlot(PlayerInventory inv, AxeSlotPref pref) {
        if (pref == AxeSlotPref.SLOT_1 && inv.getStack(0).isIn(ItemTags.AXES)) return 0;
        if (pref == AxeSlotPref.SLOT_2 && inv.getStack(1).isIn(ItemTags.AXES)) return 1;
        if (pref == AxeSlotPref.SLOT_3 && inv.getStack(2).isIn(ItemTags.AXES)) return 2;
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isIn(ItemTags.AXES)) return i;
        }
        return -1;
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
        float newYaw = player.getYaw() + dYaw * MathHelper.clamp(speed, 0.05f, 1.0f);
        float newPitch = MathHelper.clamp(player.getPitch() + dPitch * MathHelper.clamp(speed, 0.05f, 1.0f), -89.0f, 89.0f);
        player.setYaw(newYaw);
        player.setPitch(newPitch);
        player.headYaw = newYaw;
        player.bodyYaw = newYaw;
    }

    private static List<BlockPos> computePattern(BlockPos c, Direction f, MiningPattern p) {
        List<BlockPos> l = new ArrayList<>();
        if (p == MiningPattern.SINGLE) { l.add(c); return l; }
        if (p == MiningPattern.PATTERN_1X3) { l.add(c.down()); l.add(c); l.add(c.up()); return l; }
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (f.getAxis() == Direction.Axis.Y) l.add(c.add(a, 0, b));
                else if (f.getAxis() == Direction.Axis.Z) l.add(c.add(a, b, 0));
                else l.add(c.add(0, b, a));
            }
        }
        return l;
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

    private static void renderEntityBoxes(DrawContext ctx, double maxR, int col, boolean sight) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || !p.isAlive() || (sight && !mc.player.canSee(p)) || mc.player.squaredDistanceTo(p) > maxR * maxR) continue;
            int[] scr = worldToScreen(ctx, new Vec3d(p.getX(), p.getY() + p.getHeight() * 0.5, p.getZ()));
            if (scr != null) {
                int h = Math.max(10, 120 / scr[2]), w = Math.max(6, h / 2);
                int x1 = scr[0] - w / 2, y1 = scr[1] - h / 2, x2 = scr[0] + w / 2, y2 = scr[1] + h / 2;
                ctx.fill(x1, y1, x2, y1 + 1, col);
                ctx.fill(x1, y2 - 1, x2, y2, col);
                ctx.fill(x1, y1, x1 + 1, y2, col);
                ctx.fill(x2 - 1, y1, x2, y2, col);
            }
        }
    }

    public static void drawHudBadge(DrawContext ctx, int x, int y, String txt, int col) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.textRenderer.getWidth(txt) + 12;
        ctx.fill(x, y, x + w, y + 14, 0xCC0A0A0E);
        ctx.fill(x, y, x + 2, y + 14, col);
        ctx.drawTextWithShadow(mc.textRenderer, txt, x + 6, y + 3, 0xFFFFFFFF);
    }

    private static void drawKeyBox(DrawContext ctx, MinecraftClient mc, String k, int x, int y, int w, int h, boolean on) {
        ctx.fill(x, y, x + w, y + h, on ? 0xFFDC2626 : 0xCC0A0A0E);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, k, x + w / 2, y + 4, 0xFFFFFFFF);
    }

    private static void drawDottedLine(DrawContext ctx, int x0, int y0, int x1, int y1, int col) {
        int dx = x1 - x0, dy = y1 - y0, steps = Math.min(160, Math.max(Math.abs(dx), Math.abs(dy)));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i += 3) {
            ctx.fill(x0 + (dx * i) / steps, y0 + (dy * i) / steps, x0 + (dx * i) / steps + 1, y0 + (dy * i) / steps + 1, col);
        }
    }
}
