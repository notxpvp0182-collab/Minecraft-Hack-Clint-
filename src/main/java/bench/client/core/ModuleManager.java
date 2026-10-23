package bench.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static int cycleIndex = 0;

    public static void register(Module m) {
        if (byName(m.getName()) == null) {
            m.setHudLayerSilent(MODULES.size());
            MODULES.add(m);
        }
    }
    public static Module byName(String name) {
        for (Module m : MODULES) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }
    public static List<Module> byCategory(Category c) {
        List<Module> l = new ArrayList<>();
        for (Module m : MODULES) if (m.getCategory() == c) l.add(m);
        return l;
    }
    public static List<Module> favorites() {
        List<Module> l = new ArrayList<>();
        for (Module m : MODULES) if (m.isFavorite()) l.add(m);
        return l;
    }
    public static List<Module> search(String q) {
        if (q == null || q.isBlank()) return all();
        String s = q.toLowerCase(Locale.ROOT).trim();
        List<Module> l = new ArrayList<>();
        for (Module m : MODULES) {
            if (m.getName().toLowerCase(Locale.ROOT).contains(s) || m.getDescription().toLowerCase(Locale.ROOT).contains(s)) l.add(m);
        }
        return l;
    }
    public static List<Module> findKeybindConflicts(int keyCode, Module exclude) {
        if (keyCode <= 0) return Collections.emptyList();
        List<Module> l = new ArrayList<>();
        for (Module m : MODULES) if (m != exclude && m.getKeyBind() == keyCode) l.add(m);
        return l;
    }
    public static void cycle() {
        if (MODULES.isEmpty()) return;
        MODULES.get(cycleIndex % MODULES.size()).toggle();
        cycleIndex = (cycleIndex + 1) % MODULES.size();
    }
    public static void tickAll(MinecraftClient client) {
        for (Module m : MODULES) if (m.isEnabled()) { try { m.onTick(client); } catch (Exception ignored) {} }
    }
    public static void renderAllHud(DrawContext context) {
        List<Module> active = new ArrayList<>();
        for (Module m : MODULES) if (m.isEnabled()) active.add(m);
        active.sort(Comparator.comparingInt(Module::getHudLayer));
        for (Module m : active) { try { m.onRenderHud(context); } catch (Exception ignored) {} }
    }
}
