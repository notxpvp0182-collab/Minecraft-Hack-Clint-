package bench.client.modules;

import bench.client.core.Module;
import bench.client.core.ModuleManager;
import bench.client.modules.combat.*;
import bench.client.modules.movement.*;
import bench.client.modules.render.*;
import bench.client.modules.player.*;
import bench.client.modules.world.*;
import bench.client.modules.misc.*;
import bench.client.modules.exploit.*;
import bench.client.modules.fun.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Set;

public final class AllModulesRegistry {
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

    private static Reach reachMod;
    private static Velocity velocityMod;
    private static Step stepMod;
    private static FastPlace fastPlaceMod;
    private static FastBreak fastBreakMod;
    private static Timer timerMod;
    private static XRay xrayMod;

    private AllModulesRegistry() {
    }

    public static boolean shouldHideBlockForXray(Block block) {
        return xrayMod != null && xrayMod.isEnabled() && !XRAY_KEEP.contains(block);
    }

    public static double getCustomEntityReach() {
        return (reachMod != null && reachMod.isEnabled()) ? reachMod.range.getValue() : -1.0;
    }

    public static boolean isVelocityEnabled() {
        return velocityMod != null && velocityMod.isEnabled();
    }

    public static double getVelocityHorizontal() {
        return 0.0;
    }

    public static double getVelocityVertical() {
        return 0.0;
    }

    public static float getCustomStepHeight() {
        return (stepMod != null && stepMod.isEnabled()) ? stepMod.range.getValue().floatValue() : -1.0f;
    }

    public static int getFastPlaceMaxCooldown() {
        return (fastPlaceMod != null && fastPlaceMod.isEnabled()) ? 0 : -1;
    }

    public static double getFastBreakMultiplier() {
        return (fastBreakMod != null && fastBreakMod.isEnabled()) ? fastBreakMod.range.getValue() : -1.0;
    }

    public static double getTimerSpeedMultiplier() {
        return (timerMod != null && timerMod.isEnabled()) ? Math.max(0.5, Math.min(2.0, timerMod.range.getValue() / 3.0)) : 1.0;
    }

    public static void registerAll() {
        // COMBAT (36)
        ModuleManager.register(new KillAura());
        ModuleManager.register(new MSST());
        ModuleManager.register(new AutoFightBot());
        ModuleManager.register(new AimAssist());
        ModuleManager.register(new TriggerBot());
        ModuleManager.register(new Criticals());
        reachMod = new Reach();
        ModuleManager.register(reachMod);
        velocityMod = new Velocity();
        ModuleManager.register(velocityMod);
        ModuleManager.register(new AutoCrystal());
        ModuleManager.register(new AnchorAura());
        ModuleManager.register(new BedAura());
        ModuleManager.register(new Surround());
        ModuleManager.register(new AutoTotem());
        ModuleManager.register(new AutoArmor());
        ModuleManager.register(new AutoGapple());
        ModuleManager.register(new AutoPot());
        ModuleManager.register(new Backtrack());
        ModuleManager.register(new HitboxExpander());
        ModuleManager.register(new AntiBot());
        ModuleManager.register(new Teams());
        ModuleManager.register(new Friends());
        ModuleManager.register(new AutoTrap());
        ModuleManager.register(new HoleFiller());
        ModuleManager.register(new BowAimbot());
        ModuleManager.register(new AutoBowRelease());
        ModuleManager.register(new CrossbowSpam());
        ModuleManager.register(new AttributeSwap());
        ModuleManager.register(new AutoWeapon());
        ModuleManager.register(new AutoRod());
        ModuleManager.register(new SpearAssist());
        ModuleManager.register(new InvTotemBind());
        ModuleManager.register(new MidClickPearl());
        ModuleManager.register(new CartBind());
        ModuleManager.register(new AutoShieldBreaker());
        ModuleManager.register(new TargetHUD());
        ModuleManager.register(new AttackCooldown());

        // MOVEMENT (27)
        ModuleManager.register(new Fly());
        ModuleManager.register(new Speed());
        ModuleManager.register(new Sprint());
        ModuleManager.register(new NoFall());
        ModuleManager.register(new Jesus());
        ModuleManager.register(new Spider());
        stepMod = new Step();
        ModuleManager.register(stepMod);
        ModuleManager.register(new ReverseStep());
        ModuleManager.register(new LongJump());
        ModuleManager.register(new HighJump());
        ModuleManager.register(new Glide());
        ModuleManager.register(new ElytraFly());
        ModuleManager.register(new ElytraNav());
        ModuleManager.register(new ElytraBoost());
        ModuleManager.register(new TridentBoost());
        ModuleManager.register(new SafeWalk());
        ModuleManager.register(new Freecam());
        ModuleManager.register(new AntiVoid());
        ModuleManager.register(new NoSlow());
        ModuleManager.register(new InventoryMove());
        ModuleManager.register(new Blink());
        ModuleManager.register(new NoClip());
        ModuleManager.register(new FastClimb());
        ModuleManager.register(new Parkour());
        ModuleManager.register(new AntiAFK());
        ModuleManager.register(new AutoWalk());
        ModuleManager.register(new FlightStatusHUD());

        // RENDER (47)
        ModuleManager.register(new ActiveModulesHUD());
        ModuleManager.register(new HUD());
        ModuleManager.register(new PlayerESP());
        ModuleManager.register(new ChestESP());
        ModuleManager.register(new MobESP());
        ModuleManager.register(new ItemESP());
        ModuleManager.register(new BlockESP());
        ModuleManager.register(new StorageESP());
        ModuleManager.register(new HoleESP());
        ModuleManager.register(new TunnelESP());
        ModuleManager.register(new VoidESP());
        ModuleManager.register(new CityESP());
        ModuleManager.register(new Fullbright());
        ModuleManager.register(new Nametags());
        ModuleManager.register(new HealthTags());
        ModuleManager.register(new Tracers());
        ModuleManager.register(new ChunkBorders());
        ModuleManager.register(new Waypoints());
        ModuleManager.register(new NewerChunks());
        ModuleManager.register(new ShulkerPreview());
        xrayMod = new XRay();
        ModuleManager.register(xrayMod);
        ModuleManager.register(new NoRender());
        ModuleManager.register(new Trajectories());
        ModuleManager.register(new Breadcrumbs());
        ModuleManager.register(new Trail());
        ModuleManager.register(new ItemPhysics());
        ModuleManager.register(new Search());
        ModuleManager.register(new LightOverlay());
        ModuleManager.register(new BlockSelection());
        ModuleManager.register(new Chams());
        ModuleManager.register(new PopChams());
        ModuleManager.register(new NameProtect());
        ModuleManager.register(new CameraTweaks());
        ModuleManager.register(new Zoom());
        ModuleManager.register(new CustomFont());
        ModuleManager.register(new Blur());
        ModuleManager.register(new HandView());
        ModuleManager.register(new HiddenEffects());
        ModuleManager.register(new BossStack());
        ModuleManager.register(new BetterTab());
        ModuleManager.register(new BetterTooltips());
        ModuleManager.register(new BetterChat());
        ModuleManager.register(new BetterBeacons());
        ModuleManager.register(new Ambience());
        ModuleManager.register(new TimeChanger());
        ModuleManager.register(new Marker());
        ModuleManager.register(new LogoutSpots());

        // PLAYER (45)
        ModuleManager.register(new AutoEat());
        ModuleManager.register(new AutoTool());
        fastPlaceMod = new FastPlace();
        ModuleManager.register(fastPlaceMod);
        ModuleManager.register(new AirPlace());
        ModuleManager.register(new Scaffold());
        ModuleManager.register(new AutoDisconnect());
        ModuleManager.register(new DeathCoords());
        ModuleManager.register(new InventoryTweaks());
        ModuleManager.register(new ChestSwap());
        ModuleManager.register(new AutoReplenish());
        ModuleManager.register(new PotionSaver());
        ModuleManager.register(new AutoWither());
        ModuleManager.register(new AutoSign());
        ModuleManager.register(new MiddleClick());
        ModuleManager.register(new FastXP());
        ModuleManager.register(new AutoMend());
        ModuleManager.register(new AutoRespawn());
        ModuleManager.register(new AutoReconnect());
        ModuleManager.register(new AutoMount());
        ModuleManager.register(new AutoBreed());
        ModuleManager.register(new AutoShearer());
        ModuleManager.register(new AutoNametag());
        ModuleManager.register(new AutoSmelter());
        ModuleManager.register(new AutoBrewer());
        ModuleManager.register(new AutoFish());
        ModuleManager.register(new FastUse());
        ModuleManager.register(new Multitask());
        ModuleManager.register(new NoInteract());
        ModuleManager.register(new GhostHand());
        ModuleManager.register(new BuildHeight());
        ModuleManager.register(new AntiHunger());
        ModuleManager.register(new AntiAnvil());
        ModuleManager.register(new AntiBed());
        ModuleManager.register(new AntiAnchor());
        ModuleManager.register(new SelfTrap());
        ModuleManager.register(new SelfWeb());
        ModuleManager.register(new Burrow());
        ModuleManager.register(new Offhand());
        ModuleManager.register(new NoRotate());
        ModuleManager.register(new ServerSpoof());
        ModuleManager.register(new PingSpoof());
        ModuleManager.register(new AntiPacketKick());
        ModuleManager.register(new PacketCanceller());
        ModuleManager.register(new PacketLogger());
        ModuleManager.register(new Debugger());

        // WORLD (30)
        ModuleManager.register(new Nuker());
        ModuleManager.register(new AutoMine());
        ModuleManager.register(new VeinMiner());
        ModuleManager.register(new SpeedMine());
        ModuleManager.register(new InstantRebreak());
        ModuleManager.register(new BreakDelay());
        ModuleManager.register(new PacketMine());
        ModuleManager.register(new NoMiningTrace());
        ModuleManager.register(new AutoBuild());
        ModuleManager.register(new AutoFarm());
        ModuleManager.register(new SpawnProofer());
        ModuleManager.register(new StashFinder());
        ModuleManager.register(new HighwayBuilder());
        ModuleManager.register(new AutoTunnel());
        ModuleManager.register(new Terraform());
        ModuleManager.register(new Excavator());
        ModuleManager.register(new LiquidFiller());
        ModuleManager.register(new LiquidInteract());
        ModuleManager.register(new AutoCity());
        ModuleManager.register(new AutoAnvil());
        ModuleManager.register(new AutoWeb());
        ModuleManager.register(new Collisions());
        ModuleManager.register(new Slippy());
        ModuleManager.register(new EchestFarmer());
        ModuleManager.register(new InfiniteMiner());
        ModuleManager.register(new AutoWasp());
        ModuleManager.register(new ClickTP());
        ModuleManager.register(new Swarm());
        ModuleManager.register(new Flamethrower());
        ModuleManager.register(new Mining3x3());

        // MISC (23)
        timerMod = new Timer();
        ModuleManager.register(timerMod);
        fastBreakMod = new FastBreak();
        ModuleManager.register(fastBreakMod);
        ModuleManager.register(new Sneak());
        ModuleManager.register(new Spam());
        ModuleManager.register(new ChatPrefix());
        ModuleManager.register(new Enemies());
        ModuleManager.register(new ConfigManager());
        ModuleManager.register(new DiscordRPC());
        ModuleManager.register(new AltManager());
        ModuleManager.register(new ServerFinder());
        ModuleManager.register(new Translation());
        ModuleManager.register(new NoChatReports());
        ModuleManager.register(new NoTelemetry());
        ModuleManager.register(new LastServerButton());
        ModuleManager.register(new TabGUI());
        ModuleManager.register(new HackList());
        ModuleManager.register(new KeybindManager());
        ModuleManager.register(new TargetManager());
        ModuleManager.register(new MessageAura());
        ModuleManager.register(new Quiver());
        ModuleManager.register(new SoundBlocker());
        ModuleManager.register(new KeystrokesHUD());
        ModuleManager.register(new Notifications());

        // EXPLOIT (21)
        ModuleManager.register(new Disabler());
        ModuleManager.register(new AntiKick());
        ModuleManager.register(new FlightBypass());
        ModuleManager.register(new ChunkLoader());
        ModuleManager.register(new BoatFly());
        ModuleManager.register(new EntitySpeed());
        ModuleManager.register(new MountBypass());
        ModuleManager.register(new PortalGodMode());
        ModuleManager.register(new NoGhostBlocks());
        ModuleManager.register(new CopyYaw());
        ModuleManager.register(new RotationLock());
        ModuleManager.register(new BuildHeightBypass());
        ModuleManager.register(new PacketMineBypass());
        ModuleManager.register(new AntiBlind());
        ModuleManager.register(new AntiWobble());
        ModuleManager.register(new AntiDrop());
        ModuleManager.register(new AntiSpam());
        ModuleManager.register(new AutoDrop());
        ModuleManager.register(new EnchantmentHelper());
        ModuleManager.register(new MaceDMG());
        ModuleManager.register(new MultiAura());

        // FUN (1)
        ModuleManager.register(new Jetpack());
    }
}
