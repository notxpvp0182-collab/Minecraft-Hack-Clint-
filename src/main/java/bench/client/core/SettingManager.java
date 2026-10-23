package bench.client.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class SettingManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String[] PROFILES = {"default", "pvp", "mining", "render"};

    private static String currentProfile = "default";
    private static boolean loading = false;

    private static int guiWinX = -1;
    private static int guiWinY = -1;
    private static int guiWinW = 620;
    private static int guiWinH = 360;

    private SettingManager() {
    }

    public static String getCurrentProfile() {
        return currentProfile;
    }

    public static void switchProfile(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return;
        }
        save();
        currentProfile = profileName.toLowerCase();
        loadProfileFile(getProfilePath(currentProfile));
        saveMasterState();
        NotificationManager.push("Profile", "Loaded: " + currentProfile.toUpperCase(), true);
    }

    public static int getGuiWinX() {
        return guiWinX;
    }

    public static int getGuiWinY() {
        return guiWinY;
    }

    public static int getGuiWinW() {
        return guiWinW;
    }

    public static int getGuiWinH() {
        return guiWinH;
    }

    public static void setGuiBounds(int x, int y, int w, int h) {
        guiWinX = x;
        guiWinY = y;
        guiWinW = Math.max(480, w);
        guiWinH = Math.max(270, h);
    }

    private static Path getMasterPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("benchclient_state.json");
    }

    private static Path getProfilePath(String profile) {
        if ("default".equalsIgnoreCase(profile)) {
            return FabricLoader.getInstance().getConfigDir().resolve("benchclient.json");
        }
        return FabricLoader.getInstance().getConfigDir().resolve("benchclient_" + profile + ".json");
    }

    public static void load() {
        loading = true;
        try {
            Path master = getMasterPath();
            if (Files.exists(master)) {
                try (Reader r = Files.newBufferedReader(master)) {
                    JsonElement el = JsonParser.parseReader(r);
                    if (el != null && el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.has("activeProfile")) {
                            currentProfile = obj.get("activeProfile").getAsString();
                        }
                        if (obj.has("winX")) guiWinX = obj.get("winX").getAsInt();
                        if (obj.has("winY")) guiWinY = obj.get("winY").getAsInt();
                        if (obj.has("winW")) guiWinW = Math.max(480, obj.get("winW").getAsInt());
                        if (obj.has("winH")) guiWinH = Math.max(270, obj.get("winH").getAsInt());
                    }
                } catch (Exception ignored) {
                }
            }
            loadProfileFile(getProfilePath(currentProfile));
        } finally {
            loading = false;
        }
        save();
    }

    private static void loadProfileFile(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        boolean prev = loading;
        loading = true;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (rootElement == null || !rootElement.isJsonObject()) {
                return;
            }
            JsonObject root = rootElement.getAsJsonObject();

            if (root.has("_gui") && root.get("_gui").isJsonObject()) {
                JsonObject g = root.getAsJsonObject("_gui");
                if (g.has("winX")) guiWinX = g.get("winX").getAsInt();
                if (g.has("winY")) guiWinY = g.get("winY").getAsInt();
                if (g.has("winW")) guiWinW = Math.max(480, g.get("winW").getAsInt());
                if (g.has("winH")) guiWinH = Math.max(270, g.get("winH").getAsInt());
            }

            if (root.has("_friends") && root.get("_friends").isJsonArray()) {
                List<String> friends = new ArrayList<>();
                for (JsonElement f : root.getAsJsonArray("_friends")) {
                    friends.add(f.getAsString());
                }
                ClientDataManager.setFriendsFromConfig(friends);
            }

            if (root.has("_waypoints") && root.get("_waypoints").isJsonArray()) {
                List<ClientDataManager.Waypoint> wps = new ArrayList<>();
                for (JsonElement we : root.getAsJsonArray("_waypoints")) {
                    if (we.isJsonObject()) {
                        JsonObject wo = we.getAsJsonObject();
                        wps.add(new ClientDataManager.Waypoint(
                                wo.has("name") ? wo.get("name").getAsString() : "WP",
                                wo.has("x") ? wo.get("x").getAsInt() : 0,
                                wo.has("y") ? wo.get("y").getAsInt() : 64,
                                wo.has("z") ? wo.get("z").getAsInt() : 0,
                                wo.has("dim") ? wo.get("dim").getAsString() : "minecraft:overworld",
                                wo.has("color") ? wo.get("color").getAsInt() : 0xFFDC2626
                        ));
                    }
                }
                if (!wps.isEmpty()) {
                    ClientDataManager.setWaypointsFromConfig(wps);
                }
            }

            for (Module module : ModuleManager.all()) {
                try {
                    if (!root.has(module.getName()) || !root.get(module.getName()).isJsonObject()) {
                        continue;
                    }
                    JsonObject modObj = root.getAsJsonObject(module.getName());
                    if (modObj.has("enabled")) {
                        module.loadStateFromConfig(modObj.get("enabled").getAsBoolean());
                    }
                    if (modObj.has("favorite")) {
                        module.setFavoriteSilent(modObj.get("favorite").getAsBoolean());
                    }
                    if (modObj.has("keyBind")) {
                        module.setKeyBindSilent(modObj.get("keyBind").getAsInt());
                    }
                    if (modObj.has("hudX") && modObj.has("hudY")) {
                        module.setHudPosSilent(modObj.get("hudX").getAsInt(), modObj.get("hudY").getAsInt());
                    }
                    if (modObj.has("hudLayer")) {
                        module.setHudLayerSilent(modObj.get("hudLayer").getAsInt());
                    }
                    if (modObj.has("settings") && modObj.get("settings").isJsonObject()) {
                        JsonObject settingsObj = modObj.getAsJsonObject("settings");
                        for (Setting<?> setting : module.getSettings()) {
                            try {
                                if (!settingsObj.has(setting.getName())) {
                                    continue;
                                }
                                JsonElement val = settingsObj.get(setting.getName());
                                if (setting instanceof BoolSetting b) {
                                    b.setValueSilent(val.getAsBoolean());
                                } else if (setting instanceof DoubleSetting d) {
                                    d.setValueSilent(val.getAsDouble());
                                } else if (setting instanceof IntSetting i) {
                                    i.setValueSilent(val.getAsInt());
                                } else if (setting instanceof ColorSetting c) {
                                    c.setValueSilent(val.getAsInt());
                                } else if (setting instanceof KeybindSetting k) {
                                    k.setValueSilent(val.getAsInt());
                                } else if (setting instanceof EnumSetting<?> e) {
                                    e.setByName(val.getAsString());
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        } finally {
            loading = prev;
        }
    }

    public static void save() {
        if (loading) {
            return;
        }
        saveMasterState();
        Path path = getProfilePath(currentProfile);
        JsonObject root = new JsonObject();

        JsonObject guiObj = new JsonObject();
        guiObj.addProperty("winX", guiWinX);
        guiObj.addProperty("winY", guiWinY);
        guiObj.addProperty("winW", guiWinW);
        guiObj.addProperty("winH", guiWinH);
        root.add("_gui", guiObj);

        JsonArray friendsArr = new JsonArray();
        for (String f : ClientDataManager.getFriends()) {
            friendsArr.add(f);
        }
        root.add("_friends", friendsArr);

        JsonArray wpArr = new JsonArray();
        for (ClientDataManager.Waypoint wp : ClientDataManager.getWaypoints()) {
            JsonObject wo = new JsonObject();
            wo.addProperty("name", wp.name());
            wo.addProperty("x", wp.x());
            wo.addProperty("y", wp.y());
            wo.addProperty("z", wp.z());
            wo.addProperty("dim", wp.dimension());
            wo.addProperty("color", wp.color());
            wpArr.add(wo);
        }
        root.add("_waypoints", wpArr);

        for (Module module : ModuleManager.all()) {
            JsonObject modObj = new JsonObject();
            modObj.addProperty("enabled", module.isEnabled());
            modObj.addProperty("favorite", module.isFavorite());
            modObj.addProperty("keyBind", module.getKeyBind());
            modObj.addProperty("hudX", module.getHudX());
            modObj.addProperty("hudY", module.getHudY());
            modObj.addProperty("hudLayer", module.getHudLayer());

            JsonObject settingsObj = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BoolSetting b) {
                    settingsObj.addProperty(b.getName(), b.getValue());
                } else if (setting instanceof DoubleSetting d) {
                    settingsObj.addProperty(d.getName(), d.getValue());
                } else if (setting instanceof IntSetting i) {
                    settingsObj.addProperty(i.getName(), i.getValue());
                } else if (setting instanceof ColorSetting c) {
                    settingsObj.addProperty(c.getName(), c.getValue());
                } else if (setting instanceof KeybindSetting k) {
                    settingsObj.addProperty(k.getName(), k.getValue());
                } else if (setting instanceof EnumSetting<?> e) {
                    settingsObj.addProperty(e.getName(), e.getValue().name());
                }
            }
            modObj.add("settings", settingsObj);
            root.add(module.getName(), modObj);
        }

        writeJsonAtomically(path, root);
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
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(json, writer);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }
}
