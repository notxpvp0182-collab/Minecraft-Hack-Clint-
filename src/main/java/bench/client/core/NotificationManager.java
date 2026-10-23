package bench.client.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class NotificationManager {
    public record Toast(String title, String message, boolean positive, long expiresAtMs) {}

    private static final List<Toast> TOASTS = new LinkedList<>();
    private static boolean notificationsEnabled = true;

    private NotificationManager() {
    }

    public static void setNotificationsEnabled(boolean enabled) {
        notificationsEnabled = enabled;
    }

    public static void push(String title, String message, boolean positive) {
        if (!notificationsEnabled) {
            return;
        }
        synchronized (TOASTS) {
            if (TOASTS.size() >= 5) {
                TOASTS.remove(0);
            }
            TOASTS.add(new Toast(title, message, positive, System.currentTimeMillis() + 2200L));
        }
    }

    public static void render(DrawContext context) {
        if (!notificationsEnabled) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int y = sh - 34;

        synchronized (TOASTS) {
            Iterator<Toast> it = TOASTS.iterator();
            while (it.hasNext()) {
                Toast t = it.next();
                if (now >= t.expiresAtMs()) {
                    it.remove();
                    continue;
                }
                String text = t.title() + ": " + t.message();
                int w = client.textRenderer.getWidth(text) + 16;
                int x = sw - w - 8;
                context.fill(x, y, x + w, y + 18, 0xE60A0A0E);
                int accent = t.positive() ? 0xFFDC2626 : 0xFF52525B;
                context.fill(x, y, x + 3, y + 18, accent);
                context.drawTextWithShadow(client.textRenderer, text, x + 7, y + 5, 0xFFFFFFFF);
                y -= 22;
            }
        }
    }
}
