package bench.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static int cycleIndex = 0;

    private ModuleManager() {
    }

    public static void register(Module module) {
        if (byName(module.getName()) == null) {
            module.setHudLayerSilent(MODULES.size());
            MODULES.add(module);
        }
    }

    public static Module byName(String name) {
        for (Module m : MODULES) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }

    public static <T extends Module> T get(Class<T> clazz) {
        for (Module m : MODULES) {
            if (clazz.isInstance(m)) {
                return clazz.cast(m);
            }
        }
        return null;
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static List<Module> byCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.getCategory() == category) {
                list.add(m);
            }
        }
        return list;
    }

    public static List<Module> favorites() {
        List<Module> list = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.isFavorite()) {
                list.add(m);
            }
        }
        return list;
    }

    public static List<Module> search(String query) {
        if (query == null || query.isBlank()) {
            return all();
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<Module> list = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.getName().toLowerCase(Locale.ROOT).contains(q)
                    || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                list.add(m);
            }
        }
        return list;
    }

    public static List<Module> findKeybindConflicts(int keyCode, Module exclude) {
        if (keyCode <= 0) {
            return Collections.emptyList();
        }
        List<Module> conflicts = new ArrayList<>();
        for (Module m : MODULES) {
            if (m != exclude && m.getKeyBind() == keyCode) {
                conflicts.add(m);
            }
        }
        return conflicts;
    }

    public static void cycle() {
        if (MODULES.isEmpty()) {
            return;
        }
        MODULES.get(cycleIndex % MODULES.size()).toggle();
        cycleIndex = (cycleIndex + 1) % MODULES.size();
    }

    public static void tickAll(MinecraftClient client) {
        for (Module m : MODULES) {
            if (m.isEnabled()) {
                try {
                    m.onTick(client);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void renderAllHud(DrawContext context) {
        List<Module> active = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.isEnabled()) {
                active.add(m);
            }
        }
        active.sort(Comparator.comparingInt(Module::getHudLayer));
        for (Module m : active) {
            try {
                m.onRenderHud(context);
            } catch (Exception ignored) {
            }
        }
    }
}
