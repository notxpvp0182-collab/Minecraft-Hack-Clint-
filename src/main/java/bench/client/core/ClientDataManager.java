package bench.client.core;

import net.minecraft.util.math.BlockPos;
import java.util.*;

public final class ClientDataManager {
    public record Waypoint(String name, int x, int y, int z, String dimension, int color) {}
    private static final List<Long> LEFT_CLICKS = new LinkedList<>(), RIGHT_CLICKS = new LinkedList<>();
    private static final Set<String> FRIENDS = new HashSet<>();
    private static final List<Waypoint> WAYPOINTS = new ArrayList<>();
    private static final long SESSION_START_MS = System.currentTimeMillis();
    private static int sessionDeaths = 0, comboHits = 0;
    private static long lastAttackTimeMs = 0L;

    static { WAYPOINTS.add(new Waypoint("Spawn", 0, 64, 0, "minecraft:overworld", 0xFFDC2626)); }

    public static void recordLeftClick() {
        long now = System.currentTimeMillis();
        synchronized (LEFT_CLICKS) { LEFT_CLICKS.add(now); }
        comboHits = (now - lastAttackTimeMs > 2000L) ? 1 : (comboHits + 1);
        lastAttackTimeMs = now;
    }
    public static void recordRightClick() { synchronized (RIGHT_CLICKS) { RIGHT_CLICKS.add(System.currentTimeMillis()); } }
    public static int getComboHits() {
        if (System.currentTimeMillis() - lastAttackTimeMs > 2000L) comboHits = 0;
        return comboHits;
    }
    public static int getLeftCps(int windowMs) {
        long cutoff = System.currentTimeMillis() - Math.max(250, windowMs);
        synchronized (LEFT_CLICKS) {
            LEFT_CLICKS.removeIf(t -> t < cutoff);
            return (int) Math.round(LEFT_CLICKS.size() * (1000.0 / Math.max(250, windowMs)));
        }
    }
    public static int getRightCps(int windowMs) {
        long cutoff = System.currentTimeMillis() - Math.max(250, windowMs);
        synchronized (RIGHT_CLICKS) {
            RIGHT_CLICKS.removeIf(t -> t < cutoff);
            return (int) Math.round(RIGHT_CLICKS.size() * (1000.0 / Math.max(250, windowMs)));
        }
    }
    public static boolean isFriend(String name) { return name != null && FRIENDS.contains(name.toLowerCase(Locale.ROOT)); }
    public static void toggleFriend(String name) {
        if (name == null || name.isBlank()) return;
        String key = name.toLowerCase(Locale.ROOT);
        if (FRIENDS.contains(key)) {
            FRIENDS.remove(key);
            NotificationManager.push("Friends", "Removed " + name, false);
        } else {
            FRIENDS.add(key);
            NotificationManager.push("Friends", "Added " + name, true);
        }
        SettingManager.save();
    }
    public static Set<String> getFriends() { return Collections.unmodifiableSet(FRIENDS); }
    public static void setFriendsFromConfig(List<String> list) {
        FRIENDS.clear();
        for (String s : list) if (s != null && !s.isBlank()) FRIENDS.add(s.toLowerCase(Locale.ROOT));
    }
    public static List<Waypoint> getWaypoints() { return Collections.unmodifiableList(WAYPOINTS); }
    public static void addWaypoint(String name, BlockPos pos, String dim, int col) {
        WAYPOINTS.add(new Waypoint(name, pos.getX(), pos.getY(), pos.getZ(), dim, col));
        NotificationManager.push("Waypoints", "Saved " + name, true);
        SettingManager.save();
    }
    public static void setWaypointsFromConfig(List<Waypoint> loaded) { WAYPOINTS.clear(); WAYPOINTS.addAll(loaded); }
    public static void incrementDeaths() { sessionDeaths++; }
    public static int getSessionDeaths() { return sessionDeaths; }
    public static String getSessionDurationFormatted() {
        long s = Math.max(0L, (System.currentTimeMillis() - SESSION_START_MS) / 1000L);
        return String.format(Locale.ROOT, "%02dm %02ds", s / 60L, s % 60L);
    }
}
