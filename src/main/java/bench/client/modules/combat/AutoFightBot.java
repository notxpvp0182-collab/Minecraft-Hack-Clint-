package bench.client.modules.combat;

import bench.client.core.BoolSetting;
import bench.client.core.Category;
import bench.client.core.ClientDataManager;
import bench.client.core.DoubleSetting;
import bench.client.core.EnumSetting;
import bench.client.core.IntSetting;
import bench.client.core.Module;
import bench.client.modules.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Random;

public class AutoFightBot extends Module {
    public enum State { IDLE, ENGAGE, COMBAT, HEAL, KITING, DISENGAGE }
    public enum GameModeType { AUTO, SURVIVAL, KITPVP }

    public final DoubleSetting detectRange = add(new DoubleSetting("Detect Range", "Target detection range.", 8.0, 4.0, 16.0));
    public final DoubleSetting attackRange = add(new DoubleSetting("Attack Range", "Melee strike reach.", 3.0, 2.0, 4.5));
    public final DoubleSetting hpThreshold = add(new DoubleSetting("HP Threshold", "Health to trigger HEAL/KITING.", 8.0, 2.0, 18.0));
    public final IntSetting reactionMin = add(new IntSetting("Reaction Delay Min", "Minimum attack reaction delay (ms).", 50, 0, 300));
    public final IntSetting reactionMax = add(new IntSetting("Reaction Delay Max", "Maximum attack reaction delay (ms).", 150, 50, 500));
    public final BoolSetting criticals = add(new BoolSetting("Criticals", "Perform jump-timed critical attacks.", true));
    public final BoolSetting wTap = add(new BoolSetting("W-Tap", "Release and re-engage sprint on hit.", true));
    public final BoolSetting shieldBreak = add(new BoolSetting("Shield Break", "Switch to axe when target blocks.", true));
    public final BoolSetting autoHeal = add(new BoolSetting("Auto Heal", "Eat Gapple or throw healing potion on low HP.", true));
    public final BoolSetting kiteOnLowHp = add(new BoolSetting("Kite On Low HP", "Backpedal and strafe when low HP.", true));
    public final EnumSetting<GameModeType> gameMode = add(new EnumSetting<>("Game Mode", "Combat profile awareness.", GameModeType.AUTO));
    public final BoolSetting friendsCheck = add(new BoolSetting("Friends", "Do not fight Friends.", true));
    public final BoolSetting teamsCheck = add(new BoolSetting("Teams", "Do not fight Teammates.", false));
    public final BoolSetting yieldManual = add(new BoolSetting("Yield Manual Move", "Pause bot movement if user presses keys.", false));

    private State state = State.IDLE;
    private LivingEntity currentTarget = null;
    private final Random random = new Random();
    private long nextAttackAllowedMs = 0L;
    private int wTapTicks = 0;
    private int strafeDir = 1;
    private int strafeTicks = 0;
    private int previousWeaponSlot = -1;

    public AutoFightBot() {
        super("AutoFightBot", Category.COMBAT, "Autonomous 6-state combat state machine (IDLE/ENGAGE/COMBAT/HEAL/KITING/DISENGAGE).");
    }

    @Override
    public void onDisable() {
        state = State.IDLE;
        currentTarget = null;
        MSST.unlockTarget();
        releaseKeys(MinecraftClient.getInstance());
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
            state = State.IDLE;
            return;
        }
        if (mc.currentScreen != null) {
            releaseKeys(mc);
            return;
        }
        if (yieldManual.getValue() && isUserPressingMovement(mc)) {
            return;
        }

        // Validate current target
        double maxDetectSq = detectRange.getValue() * detectRange.getValue();
        if (currentTarget != null) {
            if (!currentTarget.isAlive() || currentTarget.isDead() || currentTarget.isRemoved()
                    || mc.player.squaredDistanceTo(currentTarget) > maxDetectSq) {
                state = State.DISENGAGE;
            }
        }

        switch (state) {
            case IDLE -> {
                LivingEntity found = findEnemy(mc);
                if (found != null) {
                    currentTarget = found;
                    state = State.ENGAGE;
                } else {
                    releaseKeys(mc);
                }
            }
            case ENGAGE -> {
                if (currentTarget == null) {
                    state = State.IDLE;
                    return;
                }
                MSST.lockTarget(currentTarget);
                ModuleUtils.smoothAimAt(mc.player, currentTarget, 0.35f);
                state = State.COMBAT;
            }
            case COMBAT -> {
                if (currentTarget == null) {
                    state = State.DISENGAGE;
                    return;
                }
                MSST.lockTarget(currentTarget);
                ModuleUtils.smoothAimAt(mc.player, currentTarget, 0.38f);

                if (mc.player.getHealth() <= hpThreshold.getValue()) {
                    state = autoHeal.getValue() ? State.HEAL : (kiteOnLowHp.getValue() ? State.KITING : State.COMBAT);
                    if (state != State.COMBAT) return;
                }

                double dist = Math.sqrt(mc.player.squaredDistanceTo(currentTarget));
                double reach = attackRange.getValue();

                // Movement & W-Tap rhythm
                if (wTapTicks > 0) {
                    wTapTicks--;
                    mc.options.forwardKey.setPressed(false);
                    mc.player.setSprinting(false);
                } else {
                    mc.options.forwardKey.setPressed(dist > 1.35);
                    if (dist > 3.0) {
                        mc.player.setSprinting(true);
                    }
                }
                mc.options.backKey.setPressed(false);

                if (++strafeTicks > 16 || mc.player.horizontalCollision) {
                    strafeDir = -strafeDir;
                    strafeTicks = 0;
                }
                mc.options.leftKey.setPressed(strafeDir < 0 && dist <= reach + 1.2);
                mc.options.rightKey.setPressed(strafeDir > 0 && dist <= reach + 1.2);

                // If enemy drinks potion, raise offhand shield if present
                if (currentTarget.isUsingItem() && mc.player.getOffHandStack().isOf(Items.SHIELD)) {
                    mc.options.useKey.setPressed(true);
                } else {
                    mc.options.useKey.setPressed(false);
                }

                if (dist <= reach) {
                    if (criticals.getValue() && mc.player.isOnGround() && !mc.player.isSubmergedInWater()) {
                        mc.player.jump();
                    }
                    long now = System.currentTimeMillis();
                    if (now >= nextAttackAllowedMs && mc.player.getAttackCooldownProgress(0.5f) >= 0.92f) {
                        PlayerInventory inv = mc.player.getInventory();
                        if (shieldBreak.getValue() && currentTarget.isBlocking()) {
                            int axe = findAxe(inv);
                            if (axe != -1) {
                                previousWeaponSlot = inv.getSelectedSlot();
                                inv.setSelectedSlot(axe);
                            }
                        } else if (previousWeaponSlot != -1) {
                            inv.setSelectedSlot(previousWeaponSlot);
                            previousWeaponSlot = -1;
                        }

                        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(currentTarget, mc.player.isSprinting()));
                        mc.interactionManager.attackEntity(mc.player, currentTarget);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        ClientDataManager.recordLeftClick();

                        int minD = reactionMin.getValue();
                        int maxD = Math.max(minD + 1, reactionMax.getValue());
                        nextAttackAllowedMs = now + minD + random.nextInt(maxD - minD);
                        if (wTap.getValue()) {
                            wTapTicks = 2;
                        }
                    }
                }
            }
            case HEAL -> {
                if (mc.player.getHealth() > hpThreshold.getValue() + 4.0f) {
                    mc.options.useKey.setPressed(false);
                    state = State.COMBAT;
                    return;
                }
                PlayerInventory inv = mc.player.getInventory();
                int healSlot = ModuleUtils.findHotbarItem(inv, Items.ENCHANTED_GOLDEN_APPLE);
                if (healSlot == -1) healSlot = ModuleUtils.findHotbarItem(inv, Items.GOLDEN_APPLE);
                if (healSlot == -1) healSlot = ModuleUtils.findHotbarItem(inv, Items.SPLASH_POTION);

                if (healSlot == -1) {
                    state = kiteOnLowHp.getValue() ? State.KITING : State.COMBAT;
                    return;
                }
                inv.setSelectedSlot(healSlot);
                mc.options.useKey.setPressed(true);
            }
            case KITING -> {
                if (currentTarget == null) {
                    state = State.DISENGAGE;
                    return;
                }
                MSST.lockTarget(currentTarget);
                ModuleUtils.smoothAimAt(mc.player, currentTarget, 0.35f);
                mc.options.forwardKey.setPressed(false);

                // Cliff safety check before backpedaling
                Vec3d v = mc.player.getVelocity();
                BlockPos behind = BlockPos.ofFloored(mc.player.getX() + v.x, mc.player.getY() - 0.5, mc.player.getZ() + v.z);
                if (!mc.world.getBlockState(behind).isAir()) {
                    mc.options.backKey.setPressed(true);
                } else {
                    mc.options.backKey.setPressed(false);
                }

                if (++strafeTicks > 14) {
                    strafeDir = -strafeDir;
                    strafeTicks = 0;
                }
                mc.options.leftKey.setPressed(strafeDir < 0);
                mc.options.rightKey.setPressed(strafeDir > 0);

                if (mc.player.getHealth() > hpThreshold.getValue() + 3.0f) {
                    state = State.COMBAT;
                }
            }
            case DISENGAGE -> {
                MSST.unlockTarget();
                currentTarget = null;
                releaseKeys(mc);
                state = State.IDLE;
            }
        }
    }

    private LivingEntity findEnemy(MinecraftClient mc) {
        double maxSq = detectRange.getValue() * detectRange.getValue();
        LivingEntity best = null;
        double bestSq = maxSq;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity liv) || !liv.isAlive() || liv.isDead()) continue;
            if (liv instanceof PlayerEntity p) {
                if (friendsCheck.getValue() && ClientDataManager.isFriend(p.getName().getString())) continue;
                if (teamsCheck.getValue() && mc.player.isTeammate(p)) continue;
            } else if (gameMode.getValue() == GameModeType.KITPVP) {
                continue;
            }
            double dSq = mc.player.squaredDistanceTo(liv);
            if (dSq < bestSq) {
                bestSq = dSq;
                best = liv;
            }
        }
        return best;
    }

    private int findAxe(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isIn(ItemTags.AXES)) return i;
        }
        return -1;
    }

    private boolean isUserPressingMovement(MinecraftClient mc) {
        long win = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(win, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
    }

    private void releaseKeys(MinecraftClient mc) {
        if (mc != null && mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.useKey.setPressed(false);
        }
    }

    @Override
    public void onRenderHud(DrawContext ctx) {
        String targetName = (currentTarget != null) ? currentTarget.getName().getString() : "None";
        String txt = String.format(Locale.ROOT, "AutoFightBot [%s] Target: %s", state.name(), targetName);
        ModuleUtils.drawHudBadge(ctx, 8, ctx.getScaledWindowHeight() - 48, txt, 0xFFDC2626);
    }
}
