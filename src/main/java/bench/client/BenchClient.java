package bench.client;

import bench.client.core.ClientDataManager;
import bench.client.core.Module;
import bench.client.core.ModuleManager;
import bench.client.core.NotificationManager;
import bench.client.core.SettingManager;
import bench.client.gui.ClickGui;
import bench.client.modules.AllModulesRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class BenchClient implements ClientModInitializer {
    public static final KeyBinding.Category KEY_CATEGORY = KeyBinding.Category.create(
            Identifier.of("benchclient", "general")
    );

    private static KeyBinding openGuiKey;
    private static KeyBinding cycleModuleKey;
    private static boolean wasDeadLastTick = false;

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        AllModulesRegistry.registerAll();
        SettingManager.load();

        Runtime.getRuntime().addShutdownHook(new Thread(SettingManager::save, "EternalBenchClient-Save"));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SettingManager.save());

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.benchclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KEY_CATEGORY
        ));

        cycleModuleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.benchclient.cycle_module",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_CONTROL,
                KEY_CATEGORY
        ));

        ClientSendMessageEvents.MODIFY_CHAT.register(msg -> {
            Module chatPrefix = ModuleManager.byName("ChatPrefix");
            if (chatPrefix != null && chatPrefix.isEnabled() && !msg.startsWith("/")) {
                return msg + " | Eternal";
            }
            return msg;
        });

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            if (MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud()) {
                return;
            }
            ModuleManager.renderAllHud(drawContext);
            NotificationManager.render(drawContext);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen instanceof ClickGui) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new ClickGui());
                }
            }
            while (cycleModuleKey.wasPressed()) {
                ModuleManager.cycle();
            }
            if (client.player != null && client.world != null) {
                if (client.player.isDead() && !wasDeadLastTick) {
                    wasDeadLastTick = true;
                    ClientDataManager.incrementDeaths();
                } else if (!client.player.isDead()) {
                    wasDeadLastTick = false;
                }

                for (Module m : ModuleManager.all()) {
                    int key = m.getKeyBind();
                    if (key != GLFW.GLFW_KEY_UNKNOWN && key > 0 && client.currentScreen == null) {
                        if (InputUtil.isKeyPressed(client.getWindow(), key)) {
                            if (!m.wasKeyPressedLastTick()) {
                                m.setKeyPressedLastTick(true);
                                m.toggle();
                            }
                        } else {
                            m.setKeyPressedLastTick(false);
                        }
                    } else {
                        m.setKeyPressedLastTick(false);
                    }
                }
                ModuleManager.tickAll(client);
            }
        });
    }
}
