package bench.client.gui;

import bench.client.core.*;
import bench.client.core.Module;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.Locale;

public class ClickGui extends Screen {
    public enum SidebarTab {
        MODULES("Modules"), FAVORITES("Favorites"), SEARCH("Search"), PROFILES("Profiles"), HUD_EDITOR("HUD Layout");
        private final String label;
        SidebarTab(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static SidebarTab activeSidebar = SidebarTab.MODULES;
    private static Category activeCategory = Category.COMBAT;
    private static Module selectedModule = null;
    private static String searchQuery = "";
    private static boolean searchFocused = false;
    private static int scrollOffset = 0, settingsScrollOffset = 0;

    private int winX, winY, winW, winH, dragOffsetX, dragOffsetY;
    private boolean draggingWindow = false, resizingWindow = false;
    private Setting<?> draggingSlider = null;
    private Module draggingHudModule = null, selectedHudModule = null, listeningModuleBind = null;
    private KeybindSetting listeningSettingBind = null;
    private ColorSetting activeColorPicker = null;

    public ClickGui() { super(Text.literal("Eternal BenchClient Black-Red GUI")); }

    @Override
    protected void init() {
        winW = Math.min(this.width - 16, Math.max(500, SettingManager.getGuiWinW()));
        winH = Math.min(this.height - 16, Math.max(280, SettingManager.getGuiWinH()));
        winX = SettingManager.getGuiWinX(); winY = SettingManager.getGuiWinY();
        if (winX < 0 || winX + 80 > this.width) winX = Math.max(8, (this.width - winW) / 2);
        if (winY < 0 || winY + 60 > this.height) winY = Math.max(8, (this.height - winH) / 2);
        if (selectedModule == null) selectedModule = ModuleManager.byName("AutoFightBot");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (activeSidebar == SidebarTab.HUD_EDITOR) { renderHudEditorMode(ctx); return; }
        ctx.fill(winX - 2, winY - 2, winX + winW + 2, winY + winH + 2, 0x77000000);
        ctx.fill(winX, winY, winX + winW, winY + winH, 0xF50A0A0E);
        drawRectBorder(ctx, winX, winY, winW, winH, 0xFF991B1B);
        int sidebarW = 98, topBarH = 28;
        ctx.fill(winX, winY, winX + sidebarW, winY + winH, 0xFF0E0E14);
        ctx.fill(winX + sidebarW - 1, winY, winX + sidebarW, winY + winH, 0xFF27272A);
        ctx.fill(winX + 8, winY + 7, winX + 22, winY + 21, 0xFFDC2626);
        ctx.drawTextWithShadow(this.textRenderer, "E", winX + 12, winY + 10, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "ETERNAL", winX + 26, winY + 7, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "RED v3.0", winX + 26, winY + 16, 0xFFEF4444);

        int navY = winY + 34;
        for (SidebarTab tab : SidebarTab.values()) {
            boolean active = (tab == activeSidebar);
            if (active) ctx.fill(winX + 5, navY, winX + sidebarW - 5, navY + 19, 0xFFDC2626);
            ctx.drawTextWithShadow(this.textRenderer, tab.getLabel(), winX + 12, navY + 5, active ? 0xFFFFFFFF : 0xFFA1A1AA);
            navY += 23;
        }
        ctx.drawTextWithShadow(this.textRenderer, "Profile: " + SettingManager.getCurrentProfile(), winX + 8, winY + winH - 26, 0xFFEF4444);
        ctx.drawTextWithShadow(this.textRenderer, "Credit: Ahmad", winX + 8, winY + winH - 14, 0xFF71717A);

        ctx.fill(winX + sidebarW, winY, winX + winW, winY + topBarH, 0xFF0E0E14);
        ctx.fill(winX + sidebarW, winY + topBarH - 1, winX + winW, winY + topBarH, 0xFF27272A);
        int catX = winX + sidebarW + 6;
        int catTabW = Math.max(44, (winW - sidebarW - 28) / Category.values().length);
        for (Category cat : Category.values()) {
            boolean sel = (activeSidebar == SidebarTab.MODULES && cat == activeCategory);
            ctx.drawTextWithShadow(this.textRenderer, cat.getTitle(), catX + 3, winY + 10, sel ? 0xFFEF4444 : 0xFFA1A1AA);
            if (sel) ctx.fill(catX, winY + topBarH - 2, catX + catTabW - 4, winY + topBarH, 0xFFDC2626);
            catX += catTabW;
        }
        ctx.drawTextWithShadow(this.textRenderer, "X", winX + winW - 12, winY + 9, 0xFFEF4444);

        int cx = winX + sidebarW + 6, cy = winY + topBarH + 6, cw = winW - sidebarW - 12, ch = winH - topBarH - 12;
        if (activeSidebar == SidebarTab.PROFILES) renderProfilesPanel(ctx, cx, cy, cw, ch);
        else {
            int listW = Math.max(155, (int) (cw * 0.44));
            renderModuleListColumn(ctx, cx, cy, listW, ch);
            renderSettingsInspectorColumn(ctx, cx + listW + 6, cy, cw - listW - 6, ch);
        }
        ctx.fill(winX + winW - 8, winY + winH - 2, winX + winW - 2, winY + winH - 1, 0xFFDC2626);
        ctx.fill(winX + winW - 2, winY + winH - 8, winX + winW - 1, winY + winH - 2, 0xFFDC2626);
        if (activeColorPicker != null) renderColorPickerModal(ctx);
    }

    private void renderModuleListColumn(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0xFF0E0E14);
        drawRectBorder(ctx, x, y, w, h, 0xFF27272A);
        List<Module> modules = getVisibleModules();
        ctx.drawTextWithShadow(this.textRenderer, "Modules (" + modules.size() + ")", x + 8, y + 6, 0xFFFFFFFF);
        ctx.fill(x + 6, y + 18, x + w - 6, y + 33, 0xFF09090C);
        drawRectBorder(ctx, x + 6, y + 18, w - 12, 15, searchFocused ? 0xFFDC2626 : 0xFF27272A);
        String qText = searchQuery.isEmpty() && !searchFocused ? "Search modules..." : searchQuery + (searchFocused ? "_" : "");
        ctx.drawTextWithShadow(this.textRenderer, qText, x + 10, y + 22, searchQuery.isEmpty() && !searchFocused ? 0xFF71717A : 0xFFFFFFFF);

        int rowY = y + 38, maxRows = Math.max(1, (h - 42) / 24);
        int start = Math.max(0, Math.min(scrollOffset, Math.max(0, modules.size() - maxRows)));
        for (int i = start; i < Math.min(modules.size(), start + maxRows); i++) {
            Module m = modules.get(i);
            boolean sel = (m == selectedModule);
            ctx.fill(x + 5, rowY, x + w - 5, rowY + 22, sel ? 0xFF281114 : 0xFF14141C);
            if (sel) drawRectBorder(ctx, x + 5, rowY, w - 10, 22, 0xFFDC2626);
            ctx.drawTextWithShadow(this.textRenderer, m.isFavorite() ? "★" : "☆", x + 9, rowY + 4, m.isFavorite() ? 0xFFEF4444 : 0xFF71717A);
            ctx.drawTextWithShadow(this.textRenderer, m.getName(), x + 20, rowY + 3, 0xFFFFFFFF);
            ctx.drawTextWithShadow(this.textRenderer, m.getCategory().getTitle(), x + 20, rowY + 12, 0xFFEF4444);
            drawPillSwitch(ctx, x + w - 30, rowY + 6, m.isEnabled());
            rowY += 24;
        }
    }

    private void renderSettingsInspectorColumn(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0xFF0E0E14);
        drawRectBorder(ctx, x, y, w, h, 0xFF27272A);
        if (selectedModule == null) return;
        ctx.drawTextWithShadow(this.textRenderer, selectedModule.getName(), x + 8, y + 7, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, selectedModule.getCategory().getTitle(), x + 8, y + 17, 0xFFEF4444);
        drawPillSwitch(ctx, x + w - 30, y + 9, selectedModule.isEnabled());

        String desc = selectedModule.getDescription();
        ctx.drawTextWithShadow(this.textRenderer, desc.length() > 42 ? desc.substring(0, 42) : desc, x + 8, y + 29, 0xFFA1A1AA);
        if (desc.length() > 42) ctx.drawTextWithShadow(this.textRenderer, desc.substring(42, Math.min(desc.length(), 84)), x + 8, y + 38, 0xFFA1A1AA);
        ctx.fill(x + 6, y + 49, x + w - 6, y + 50, 0xFF27272A);

        int sy = y + 54;
        boolean listen = (listeningModuleBind == selectedModule);
        ctx.fill(x + 6, sy, x + w - 6, sy + 19, 0xFF14141C);
        ctx.drawTextWithShadow(this.textRenderer, "Keybind", x + 10, sy + 5, 0xFFFFFFFF);
        String kbTxt = listen ? "Press Key..." : "[" + selectedModule.getKeyBindName() + "]";
        ctx.fill(x + w - 72, sy + 2, x + w - 10, sy + 17, listen ? 0xFFDC2626 : 0xFF09090C);
        drawRectBorder(ctx, x + w - 72, sy + 2, 62, 15, 0xFF27272A);
        ctx.drawCenteredTextWithShadow(this.textRenderer, kbTxt, x + w - 41, sy + 5, 0xFFFFFFFF);
        sy += 22;

        List<Setting<?>> settings = selectedModule.getSettings();
        for (int idx = settingsScrollOffset; idx < settings.size(); idx++) {
            if (sy + 21 > y + h - 4) break;
            Setting<?> s = settings.get(idx);
            ctx.fill(x + 6, sy, x + w - 6, sy + 21, 0xFF14141C);
            ctx.drawTextWithShadow(this.textRenderer, s.getName(), x + 10, sy + 4, 0xFFFFFFFF);
            int cx = x + w - 74, cw = 64;
            if (s instanceof BoolSetting b) drawPillSwitch(ctx, x + w - 30, sy + 5, b.getValue());
            else if (s instanceof ColorSetting c) {
                ctx.fill(cx + 20, sy + 4, cx + cw, sy + 17, c.getValue());
                drawRectBorder(ctx, cx + 20, sy + 4, cw - 20, 13, 0xFFFFFFFF);
            } else if (s instanceof EnumSetting<?> e) {
                ctx.fill(cx, sy + 3, cx + cw, sy + 18, 0xFF09090C);
                ctx.drawCenteredTextWithShadow(this.textRenderer, e.getValue().name(), cx + cw / 2, sy + 6, 0xFFEF4444);
            } else if (s instanceof KeybindSetting k) {
                boolean kl = (listeningSettingBind == k);
                ctx.fill(cx, sy + 3, cx + cw, sy + 18, kl ? 0xFFDC2626 : 0xFF09090C);
                ctx.drawCenteredTextWithShadow(this.textRenderer, kl ? "Press..." : k.getKeyName(), cx + cw / 2, sy + 6, 0xFFFFFFFF);
            } else if (s instanceof DoubleSetting d) {
                ctx.fill(cx, sy + 3, cx + cw, sy + 18, 0xFF09090C);
                int fill = (int) (cw * ((d.getValue() - d.getMin()) / (d.getMax() - d.getMin())));
                ctx.fill(cx, sy + 16, cx + fill, sy + 18, 0xFFDC2626);
                ctx.drawTextWithShadow(this.textRenderer, String.format(Locale.ROOT, "%.1f", d.getValue()), cx + 4, sy + 6, 0xFFFFFFFF);
            } else if (s instanceof IntSetting i) {
                ctx.fill(cx, sy + 3, cx + cw, sy + 18, 0xFF09090C);
                int fill = (int) (cw * ((double) (i.getValue() - i.getMin()) / Math.max(1, i.getMax() - i.getMin())));
                ctx.fill(cx, sy + 16, cx + fill, sy + 18, 0xFFDC2626);
                ctx.drawTextWithShadow(this.textRenderer, Integer.toString(i.getValue()), cx + 4, sy + 6, 0xFFFFFFFF);
            }
            sy += 23;
        }
    }

    private void renderColorPickerModal(DrawContext ctx) {
        int px = Math.max(10, Math.min(this.width - 150, winX + winW / 2 - 70));
        int py = Math.max(10, Math.min(this.height - 110, winY + winH / 2 - 50));
        ctx.fill(px, py, px + 140, py + 96, 0xF80A0A0E);
        drawRectBorder(ctx, px, py, 140, 96, 0xFFDC2626);
        ctx.drawTextWithShadow(this.textRenderer, activeColorPicker.getName() + " " + activeColorPicker.getHexString(), px + 6, py + 6, 0xFFFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, "Red:   " + activeColorPicker.getRed() + "  [+/-]", px + 8, py + 22, 0xFFEF4444);
        ctx.drawTextWithShadow(this.textRenderer, "Green: " + activeColorPicker.getGreen() + "  [+/-]", px + 8, py + 36, 0xFF10B981);
        ctx.drawTextWithShadow(this.textRenderer, "Blue:  " + activeColorPicker.getBlue() + "  [+/-]", px + 8, py + 50, 0xFF3B82F6);
        ctx.fill(px + 108, py + 22, px + 132, py + 60, activeColorPicker.getValue());
        ctx.fill(px + 76, py + 72, px + 132, py + 88, 0xFFDC2626);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Done", px + 104, py + 76, 0xFFFFFFFF);
    }

    private void renderProfilesPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0xFF0E0E14);
        drawRectBorder(ctx, x, y, w, h, 0xFF27272A);
        ctx.drawTextWithShadow(this.textRenderer, "Configuration Profiles", x + 10, y + 8, 0xFFFFFFFF);
        int py = y + 26;
        for (String p : SettingManager.PROFILES) {
            boolean act = p.equalsIgnoreCase(SettingManager.getCurrentProfile());
            ctx.fill(x + 10, py, x + w - 10, py + 24, act ? 0xFF3F1116 : 0xFF14141C);
            drawRectBorder(ctx, x + 10, py, w - 20, 24, act ? 0xFFDC2626 : 0xFF27272A);
            ctx.drawTextWithShadow(this.textRenderer, "Profile: " + p.toUpperCase(Locale.ROOT) + (act ? " [ACTIVE]" : ""), x + 18, py + 8, act ? 0xFFEF4444 : 0xFFFFFFFF);
            py += 30;
        }
    }

    private void renderHudEditorMode(DrawContext ctx) {
        ctx.fill(0, 0, this.width, this.height, 0x55000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "HUD Layout Editor — Drag HUD elements | Right-Click to Return", this.width / 2, 10, 0xFFEF4444);
        for (Module m : ModuleManager.all()) {
            if (m.isEnabled()) {
                m.onRenderHud(ctx);
                drawRectBorder(ctx, m.getHudX() - 2, m.getHudY() - 2, m.getHudW(), m.getHudH(), 0xFFEF4444);
            }
        }
    }

    private List<Module> getVisibleModules() {
        if (!searchQuery.isBlank() || activeSidebar == SidebarTab.SEARCH) return ModuleManager.search(searchQuery);
        if (activeSidebar == SidebarTab.FAVORITES) return ModuleManager.favorites();
        return ModuleManager.byCategory(activeCategory);
    }

    private void drawPillSwitch(DrawContext ctx, int x, int y, boolean on) {
        ctx.fill(x, y, x + 22, y + 11, on ? 0xFFDC2626 : 0xFF27272A);
        int kx = on ? (x + 13) : (x + 2);
        ctx.fill(kx, y + 2, kx + 7, y + 9, 0xFFFFFFFF);
    }

    private void drawRectBorder(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x + w, y + 1, col); ctx.fill(x, y + h - 1, x + w, y + h, col);
        ctx.fill(x, y, x + 1, y + h, col); ctx.fill(x + w - 1, y, x + w, y + h, col);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mx = click.x(), my = click.y(); int btn = click.button();
        if (activeColorPicker != null) {
            int px = Math.max(10, Math.min(this.width - 150, winX + winW / 2 - 70));
            int py = Math.max(10, Math.min(this.height - 110, winY + winH / 2 - 50));
            int step = (btn == 0) ? 16 : -16;
            if (mx >= px + 6 && mx <= px + 104) {
                if (my >= py + 20 && my <= py + 32) activeColorPicker.setRgb((activeColorPicker.getRed() + step + 256) % 256, activeColorPicker.getGreen(), activeColorPicker.getBlue());
                if (my >= py + 34 && my <= py + 46) activeColorPicker.setRgb(activeColorPicker.getRed(), (activeColorPicker.getGreen() + step + 256) % 256, activeColorPicker.getBlue());
                if (my >= py + 48 && my <= py + 60) activeColorPicker.setRgb(activeColorPicker.getRed(), activeColorPicker.getGreen(), (activeColorPicker.getBlue() + step + 256) % 256);
            }
            if (mx >= px + 76 && mx <= px + 132 && my >= py + 72 && my <= py + 88) activeColorPicker = null;
            return true;
        }
        if (activeSidebar == SidebarTab.HUD_EDITOR) {
            if (btn == 1) { activeSidebar = SidebarTab.MODULES; SettingManager.save(); return true; }
            for (Module m : ModuleManager.all()) {
                if (m.isEnabled() && mx >= m.getHudX() - 4 && mx <= m.getHudX() + m.getHudW() && my >= m.getHudY() - 4 && my <= m.getHudY() + m.getHudH() + 4) {
                    draggingHudModule = m; selectedHudModule = m; dragOffsetX = (int) mx - m.getHudX(); dragOffsetY = (int) my - m.getHudY(); return true;
                }
            }
            return true;
        }
        if (mx >= winX + winW - 14 && mx <= winX + winW && my >= winY + winH - 14 && my <= winY + winH) {
            resizingWindow = true; dragOffsetX = (int) mx - winW; dragOffsetY = (int) my - winH; return true;
        }
        if (mx >= winX + winW - 16 && mx <= winX + winW && my >= winY && my <= winY + 22) { close(); return true; }

        int sidebarW = 98, topBarH = 28, navY = winY + 34;
        for (SidebarTab tab : SidebarTab.values()) {
            if (mx >= winX + 4 && mx <= winX + sidebarW - 4 && my >= navY && my <= navY + 19) {
                activeSidebar = tab; searchFocused = (tab == SidebarTab.SEARCH); scrollOffset = 0; return true;
            }
            navY += 23;
        }
        if (mx >= winX && mx <= winX + winW && my >= winY && my <= winY + topBarH) {
            int catX = winX + sidebarW + 6;
            int catTabW = Math.max(44, (winW - sidebarW - 28) / Category.values().length);
            for (Category cat : Category.values()) {
                if (mx >= catX && mx <= catX + catTabW - 2) {
                    activeCategory = cat; activeSidebar = SidebarTab.MODULES; scrollOffset = 0; settingsScrollOffset = 0;
                    List<Module> l = ModuleManager.byCategory(cat); if (!l.isEmpty()) selectedModule = l.get(0);
                    return true;
                }
                catX += catTabW;
            }
            draggingWindow = true; dragOffsetX = (int) mx - winX; dragOffsetY = (int) my - winY; return true;
        }

        int cx = winX + sidebarW + 6, cy = winY + topBarH + 6, cw = winW - sidebarW - 12, ch = winH - topBarH - 12;
        if (activeSidebar == SidebarTab.PROFILES) {
            int py = cy + 26;
            for (String p : SettingManager.PROFILES) {
                if (mx >= cx + 10 && mx <= cx + cw - 10 && my >= py && my <= py + 24) { SettingManager.switchProfile(p); return true; }
                py += 30;
            }
            return true;
        }
        int listW = Math.max(155, (int) (cw * 0.44)), ix = cx + listW + 6, iw = cw - listW - 6;
        if (mx >= cx + 6 && mx <= cx + listW - 6 && my >= cy + 18 && my <= cy + 33) {
            searchFocused = true; if (btn == 1) searchQuery = ""; return true;
        } else searchFocused = false;

        List<Module> modules = getVisibleModules();
        int rowY = cy + 38, maxRows = Math.max(1, (ch - 42) / 24);
        int start = Math.max(0, Math.min(scrollOffset, Math.max(0, modules.size() - maxRows)));
        for (int i = start; i < Math.min(modules.size(), start + maxRows); i++) {
            Module m = modules.get(i);
            if (mx >= cx + 5 && mx <= cx + listW - 5 && my >= rowY && my <= rowY + 22) {
                if (mx <= cx + 18) m.toggleFavorite();
                else if (btn == 1) { selectedModule = m; settingsScrollOffset = 0; }
                else if (btn == 0) { m.toggle(); selectedModule = m; settingsScrollOffset = 0; }
                return true;
            }
            rowY += 24;
        }
        if (selectedModule != null && mx >= ix && mx <= ix + iw) {
            if (mx >= ix + iw - 34 && my >= cy + 5 && my <= cy + 22) { selectedModule.toggle(); return true; }
            int sy = cy + 54;
            if (my >= sy && my <= sy + 19) {
                if (btn == 1) { selectedModule.setKeyBind(GLFW.GLFW_KEY_UNKNOWN); listeningModuleBind = null; }
                else { listeningModuleBind = (listeningModuleBind == selectedModule) ? null : selectedModule; listeningSettingBind = null; }
                return true;
            }
            sy += 22;
            int ctrlX = ix + iw - 74, ctrlW = 64;
            List<Setting<?>> settings = selectedModule.getSettings();
            for (int idx = settingsScrollOffset; idx < settings.size(); idx++) {
                if (sy + 21 > cy + ch - 4) break;
                Setting<?> s = settings.get(idx);
                if (my >= sy && my <= sy + 21) {
                    if (s instanceof BoolSetting b) b.toggle();
                    else if (s instanceof ColorSetting c) activeColorPicker = c;
                    else if (s instanceof EnumSetting<?> e) e.cycle();
                    else if (s instanceof KeybindSetting k) {
                        if (btn == 1) { k.setValue(GLFW.GLFW_KEY_UNKNOWN); listeningSettingBind = null; }
                        else listeningSettingBind = (listeningSettingBind == k) ? null : k;
                    } else if (s instanceof DoubleSetting d) {
                        draggingSlider = d; updateSliderValue(d, mx, ctrlX, ctrlW);
                    } else if (s instanceof IntSetting i) {
                        draggingSlider = i; updateSliderValue(i, mx, ctrlX, ctrlW);
                    }
                    return true;
                }
                sy += 23;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double dX, double dY) {
        double mx = click.x(), my = click.y();
        if (draggingHudModule != null) {
            draggingHudModule.setHudPos(Math.max(2, Math.min(this.width - 40, (int) mx - dragOffsetX)), Math.max(2, Math.min(this.height - 16, (int) my - dragOffsetY)));
            return true;
        }
        if (draggingWindow) {
            winX = Math.max(0, Math.min(this.width - 100, (int) mx - dragOffsetX));
            winY = Math.max(0, Math.min(this.height - 60, (int) my - dragOffsetY));
            SettingManager.setGuiBounds(winX, winY, winW, winH); return true;
        }
        if (resizingWindow) {
            winW = Math.max(500, Math.min(this.width - winX - 4, (int) mx - dragOffsetX));
            winH = Math.max(280, Math.min(this.height - winY - 4, (int) my - dragOffsetY));
            SettingManager.setGuiBounds(winX, winY, winW, winH); return true;
        }
        if (draggingSlider != null) {
            int cw = winW - 110, listW = Math.max(155, (int) (cw * 0.44)), ix = winX + 104 + listW + 6, iw = cw - listW - 6;
            updateSliderValue(draggingSlider, mx, ix + iw - 74, 64); return true;
        }
        return super.mouseDragged(click, dX, dY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingWindow = false; resizingWindow = false; draggingSlider = null; draggingHudModule = null;
        SettingManager.save(); return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int cw = winW - 110, listW = Math.max(155, (int) (cw * 0.44));
        if (mx > winX + 104 + listW) {
            if (vAmt < 0) settingsScrollOffset++; else if (vAmt > 0 && settingsScrollOffset > 0) settingsScrollOffset--;
        } else {
            if (vAmt < 0) scrollOffset++; else if (vAmt > 0 && scrollOffset > 0) scrollOffset--;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int k = keyInput.key();
        if (listeningModuleBind != null) {
            listeningModuleBind.setKeyBind((k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_DELETE) ? GLFW.GLFW_KEY_UNKNOWN : k);
            listeningModuleBind = null; SettingManager.save(); return true;
        }
        if (listeningSettingBind != null) {
            listeningSettingBind.setValue((k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_DELETE) ? GLFW.GLFW_KEY_UNKNOWN : k);
            listeningSettingBind = null; SettingManager.save(); return true;
        }
        if (searchFocused) {
            if (k == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); return true; }
            if (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (searchFocused) {
            String ch = charInput.asString();
            if (ch != null && !ch.isEmpty() && searchQuery.length() < 24) { searchQuery += ch; scrollOffset = 0; return true; }
        }
        return super.charTyped(charInput);
    }

    private void updateSliderValue(Setting<?> s, double mx, int x, int w) {
        double pct = Math.max(0.0, Math.min(1.0, (mx - x) / (double) Math.max(1, w)));
        if (s instanceof DoubleSetting d) d.setValue(Math.round((d.getMin() + pct * (d.getMax() - d.getMin())) * 10.0) / 10.0);
        else if (s instanceof IntSetting i) i.setValue((int) Math.round(i.getMin() + pct * (i.getMax() - i.getMin())));
    }

    @Override
    public void close() {
        SettingManager.setGuiBounds(winX, winY, winW, winH); SettingManager.save(); super.close();
    }
    @Override public boolean shouldPause() { return false; }
}
