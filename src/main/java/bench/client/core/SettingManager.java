package bench.client.core;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class SettingManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String[] PROFILES = {"default", "pvp", "mining", "render"};
    private static String currentProfile = "default";
    private static boolean loading = false;
    private static int guiWinX = -1, guiWinY = -1, guiWinW = 620, guiWinH = 360;

    public static String getCurrentProfile() { return currentProfile; }
    public static void switchProfile(String profileName) {
        if (profileName == null || profileName.isBlank()) return;
        save();
        currentProfile = profileName.toLowerCase();
        loadProfileFile(getProfilePath(currentProfile));
        saveMasterState();
        NotificationManager.push("Profile", "Loaded: " + currentProfile.toUpperCase(), true);
    }
    public static int getGuiWinX() { return guiWinX; }
    public static int getGuiWinY() { return guiWinY; }
    public static int getGuiWinW() { return guiWinW; }
    public static int getGuiWinH() { return guiWinH; }
    public static void setGuiBounds(int x, int y, int w, int h) {
        guiWinX = x; guiWinY = y; guiWinW = Math.max(480, w); guiWinH = Math.max(270, h);
    }
    private static Path getMasterPath() { return FabricLoader.getInstance().getConfigDir().resolve("benchclient_state.json"); }
    private static Path getProfilePath(String profile) {
        return FabricLoader.getInstance().getConfigDir().resolve("default".equalsIgnoreCase(profile) ? "benchclient.json" : "benchclient_" + profile + ".json");
    }
    public static void load() {
        loading = true;
        try {
            Path master = getMasterPath();
            if (Files.exists(master)) {
                try (Reader r = Files.newBufferedReader(master)) {
                    JsonElement el = JsonParser.parseReader(r);
                    if (el != null && el.isJsonObject()) {
                        JsonObject o = el.getAsJsonObject();
                        if (o.has("activeProfile")) currentProfile = o.get("activeProfile").getAsString();
                        if (o.has("winX")) guiWinX = o.get("winX").getAsInt();
                        if (o.has("winY")) guiWinY = o.get("winY").getAsInt();
                        if (o.has("winW")) guiWinW = Math.max(480, o.get("winW").getAsInt());
                        if (o.has("winH")) guiWinH = Math.max(270, o.get("winH").getAsInt());
                    }
                } catch (Exception ignored) {}
            }
            loadProfileFile(getProfilePath(currentProfile));
        } finally { loading = false; }
        save();
    }
    private static void loadProfileFile(Path path) {
        if (!Files.exists(path)) return;
        boolean prev = loading; loading = true;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (rootElement == null || !rootElement.isJsonObject()) return;
            JsonObject root = rootElement.getAsJsonObject();
            for (Module module : ModuleManager.all()) {
                try {
                    if (!root.has(module.getName()) || !root.get(module.getName()).isJsonObject()) continue;
                    JsonObject mo = root.getAsJsonObject(module.getName());
                    if (mo.has("enabled")) module.loadStateFromConfig(mo.get("enabled").getAsBoolean());
                    if (mo.has("favorite")) module.setFavoriteSilent(mo.get("favorite").getAsBoolean());
                    if (mo.has("keyBind")) module.setKeyBindSilent(mo.get("keyBind").getAsInt());
                    if (mo.has("hudX") && mo.has("hudY")) module.setHudPosSilent(mo.get("hudX").getAsInt(), mo.get("hudY").getAsInt());
                    if (mo.has("hudLayer")) module.setHudLayerSilent(mo.get("hudLayer").getAsInt());
                    if (mo.has("settings") && mo.get("settings").isJsonObject()) {
                        JsonObject so = mo.getAsJsonObject("settings");
                        for (Setting<?> s : module.getSettings()) {
                            try {
                                if (!so.has(s.getName())) continue;
                                JsonElement v = so.get(s.getName());
                                if (s instanceof BoolSetting b) b.setValueSilent(v.getAsBoolean());
                                else if (s instanceof DoubleSetting d) d.setValueSilent(v.getAsDouble());
                                else if (s instanceof IntSetting i) i.setValueSilent(v.getAsInt());
                                else if (s instanceof ColorSetting c) c.setValueSilent(v.getAsInt());
                                else if (s instanceof KeybindSetting k) k.setValueSilent(v.getAsInt());
                                else if (s instanceof EnumSetting<?> e) e.setByName(v.getAsString());
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {} finally { loading = prev; }
    }
    public static void save() {
        if (loading) return;
        saveMasterState();
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            mo.addProperty("favorite", m.isFavorite());
            mo.addProperty("keyBind", m.getKeyBind());
            mo.addProperty("hudX", m.getHudX());
            mo.addProperty("hudY", m.getHudY());
            mo.addProperty("hudLayer", m.getHudLayer());
            JsonObject so = new JsonObject();
            for (Setting<?> s : m.getSettings()) {
                if (s instanceof BoolSetting b) so.addProperty(b.getName(), b.getValue());
                else if (s instanceof DoubleSetting d) so.addProperty(d.getName(), d.getValue());
                else if (s instanceof IntSetting i) so.addProperty(i.getName(), i.getValue());
                else if (s instanceof ColorSetting c) so.addProperty(c.getName(), c.getValue());
                else if (s instanceof KeybindSetting k) so.addProperty(k.getName(), k.getValue());
                else if (s instanceof EnumSetting<?> e) so.addProperty(e.getName(), e.getValue().name());
            }
            mo.add("settings", so);
            root.add(m.getName(), mo);
        }
        writeJsonAtomically(getProfilePath(currentProfile), root);
    }
    private static void saveMasterState() {
        JsonObject master = new JsonObject();
        master.addProperty("activeProfile", currentProfile);
        master.addProperty("winX", guiWinX);
        master.addProperty("winY", guiWinY);
        master.addProperty("winW", guiWinW);
        master.addProperty("winH", guiWinH);
        writeJsonAtomically(getMasterPath(), master);
    }
    private static void writeJsonAtomically(Path target, JsonObject json) {
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) { GSON.toJson(json, writer); }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }
}
