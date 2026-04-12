package skid.krypton.gui.components;
 
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import skid.krypton.Krypton;
import skid.krypton.gui.CategoryWindow;
import skid.krypton.gui.ClickGUI;
import skid.krypton.gui.Component;
import skid.krypton.module.Module;
import skid.krypton.module.setting.*;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
public final class ModuleButton {
 
    // ── colours ──────────────────────────────────────────────────────────────
    private static final Color ROW_BG          = new Color(22, 26, 33, 0);      // transparent – panel bg shows
    private static final Color ROW_HOVER       = new Color(255, 255, 255, 10);
    private static final Color ROW_SEP         = new Color(38, 43, 54, 200);
    private static final Color TEXT_ON         = new Color(220, 225, 235);
    private static final Color TEXT_OFF        = new Color(140, 146, 160);
    private static final Color TOGGLE_TRACK_OFF= new Color(48, 54, 66);
    private static final Color TOGGLE_THUMB    = new Color(255, 255, 255);
 
    // ── toggle switch geometry ────────────────────────────────────────────────
    private static final int   TOGGLE_W = 30;
    private static final int   TOGGLE_H = 16;
    private static final float TOGGLE_R = 8f;   // track corner radius = half height
    private static final int   THUMB_R  = 6;    // thumb radius
 
    // ── state ─────────────────────────────────────────────────────────────────
    public List<Component> settings;
    public CategoryWindow  parent;
    public Module          module;
    public int             offset;
    public boolean         extended;
    public int             settingOffset;
    public Color           currentColor;
    public Color           currentAlpha;
    public Animation       animation;
 
    private float hoverAnim;
    private float toggleAnim;   // 0 = off, 1 = on (drives thumb position + track colour)
 
    public ModuleButton(CategoryWindow parent, Module module, int offset) {
        this.settings      = new ArrayList<>();
        this.animation     = new Animation(0.0);
        this.parent        = parent;
        this.module        = module;
        this.offset        = offset;
        this.extended      = false;
        this.settingOffset = ClickGUI.ROW_HEIGHT;
 
        // initialise toggle animation to current state
        this.toggleAnim = module.isEnabled() ? 1f : 0f;
 
        for (Object s : module.getSettings()) {
            if      (s instanceof BooleanSetting) settings.add(new Checkbox(this, (Setting)s, settingOffset));
            else if (s instanceof NumberSetting)  settings.add(new NumberBox(this, (Setting)s, settingOffset));
            else if (s instanceof ModeSetting)    settings.add(new ModeBox(this, (Setting)s, settingOffset));
            else if (s instanceof BindSetting)    settings.add(new Keybind(this, (Setting)s, settingOffset));
            else if (s instanceof StringSetting)  settings.add(new TextBox(this, (Setting)s, settingOffset));
            else if (s instanceof MinMaxSetting)  settings.add(new Slider(this, (Setting)s, settingOffset));
            else if (s instanceof ItemSetting)    settings.add(new ItemBox(this, (Setting)s, settingOffset));
            settingOffset += ClickGUI.ROW_HEIGHT;
        }
    }
 
    // ── rendering ─────────────────────────────────────────────────────────────
 
    public void render(DrawContext ctx, int mx, int my, float delta) {
        final int rowY = parent.getY() + offset;
        if (rowY > MinecraftClient.getInstance().getWindow().getHeight()) return;
 
        for (Component c : settings) c.onUpdate();
 
        updateAnimations(mx, my, delta);
 
        final int x = parent.getX();
        final int w = parent.getWidth();
        final int h = ClickGUI.ROW_HEIGHT;
 
        // ---- row hover highlight ------------------------------------------------
        if (hoverAnim > 0.01f) {
            ctx.fill(x, rowY, x + w, rowY + h,
                    new Color(255, 255, 255, (int)(12 * hoverAnim)).getRGB());
        }
 
        // ---- thin separator at the top of each row ------------------------------
        ctx.fill(x + 10, rowY, x + w - 10, rowY + 1, ROW_SEP.getRGB());
 
        // ---- module name --------------------------------------------------------
        final Color nameColor = interpolateColor(TEXT_OFF, TEXT_ON, toggleAnim);
        TextRenderer.drawString(module.getName(), ctx,
                x + 12, rowY + h / 2 - 5,
                nameColor.getRGB());
 
        // ---- toggle switch on the right -----------------------------------------
        renderToggle(ctx, x + w - TOGGLE_W - 10, rowY + h / 2 - TOGGLE_H / 2);
 
        // ---- settings arrow (if module has settings) ----------------------------
        if (!module.getSettings().isEmpty()) {
            final String arrow = extended ? "▾" : "▸";
            TextRenderer.drawString(arrow, ctx,
                    x + w - TOGGLE_W - 24, rowY + h / 2 - 5,
                    new Color(90, 98, 115).getRGB());
        }
 
        // ---- keybind badge ------------------------------------------------------
        if (module.getKeybind() != -1) {
            final String bindTxt = KeyUtils.getKey(module.getKeybind()).toString();
            final int bw = TextRenderer.getWidth(bindTxt) + 8;
            final int bx = x + w - TOGGLE_W - (module.getSettings().isEmpty() ? 20 : 36) - bw;
            final int by = rowY + h / 2 - 7;
            RenderUtils.renderRoundedQuad(ctx.getMatrices(),
                    new Color(32, 37, 47, 200),
                    bx, by, bx + bw, by + 14,
                    3.0, 3.0, 3.0, 3.0, 50.0);
            TextRenderer.drawString(bindTxt, ctx, bx + 4, by + 2,
                    new Color(140, 148, 165).getRGB());
        }
 
        // ---- tooltip on hover ---------------------------------------------------
        if (isHovered(mx, my) && !parent.dragging && module.getDescription() != null) {
            Krypton.INSTANCE.GUI.setTooltip(module.getDescription(), mx + 10, my + 10);
        }
 
        // ---- settings rows (scissor-clipped) ------------------------------------
        if (extended) renderSettings(ctx, mx, my, delta);
    }
 
    /** Renders a rounded toggle switch track + sliding thumb */
    private void renderToggle(DrawContext ctx, int tx, int ty) {
        final Color accent = Utils.getMainColor(255, parent.moduleButtons.indexOf(this));
 
        // track
        final Color trackColor = interpolateColor(TOGGLE_TRACK_OFF, accent, toggleAnim);
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), trackColor,
                tx, ty, tx + TOGGLE_W, ty + TOGGLE_H,
                TOGGLE_R, TOGGLE_R, TOGGLE_R, TOGGLE_R, 50.0);
 
        // thumb – slides from left (off) to right (on)
        final float thumbTravel = TOGGLE_W - TOGGLE_H;   // how far the thumb moves
        final float thumbX = tx + 2 + thumbTravel * toggleAnim;
        final float thumbY = ty + 2;
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), TOGGLE_THUMB,
                thumbX, thumbY,
                thumbX + (TOGGLE_H - 4), thumbY + (TOGGLE_H - 4),
                TOGGLE_R - 2, TOGGLE_R - 2, TOGGLE_R - 2, TOGGLE_R - 2, 50.0);
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        final float dt = delta * 0.05f;
        final float hoverTarget  = (isHovered(mx, my) && !parent.dragging) ? 1f : 0f;
        hoverAnim  = (float) MathUtil.exponentialInterpolate(hoverAnim,  hoverTarget,  0.2,  dt);
 
        final float toggleTarget = module.isEnabled() ? 1f : 0f;
        toggleAnim = (float) MathUtil.exponentialInterpolate(toggleAnim, toggleTarget, 0.01, dt);
        toggleAnim = (float) MathUtil.clampValue(toggleAnim, 0.0, 1.0);
    }
 
    private void renderSettings(DrawContext ctx, int mx, int my, float delta) {
        final int clipTop    = parent.getY() + offset + ClickGUI.ROW_HEIGHT;
        final int clipHeight = (int) animation.getAnimation();
        if (clipHeight <= 0) return;
 
        RenderSystem.enableScissor(
                parent.getX(),
                Krypton.mc.getWindow().getHeight() - (clipTop + clipHeight),
                parent.getWidth(),
                clipHeight);
 
        for (Component c : settings) c.render(ctx, mx, my - clipTop, delta);
        renderSliderKnobs(ctx);
 
        RenderSystem.disableScissor();
    }
 
    private void renderSliderKnobs(DrawContext ctx) {
        for (Component c : settings) {
            if (c instanceof NumberBox nb) {
                renderKnob(ctx, c.parentX() + Math.max(nb.lerpedOffsetX, 2.5),
                        c.parentY() + nb.offset + c.parentOffset() + 27.5, nb.currentColor1);
            } else if (c instanceof Slider sl) {
                renderKnob(ctx, c.parentX() + Math.max(sl.lerpedOffsetMinX, 2.5),
                        c.parentY() + c.offset + c.parentOffset() + 27.5, sl.accentColor1);
                renderKnob(ctx, c.parentX() + Math.max(sl.lerpedOffsetMaxX, 2.5),
                        c.parentY() + c.offset + c.parentOffset() + 27.5, sl.accentColor1);
            }
        }
    }
 
    private void renderKnob(DrawContext ctx, double kx, double ky, Color color) {
        RenderUtils.renderCircle(ctx.getMatrices(), new Color(0, 0, 0, 80),  kx, ky, 6.0, 16);
        RenderUtils.renderCircle(ctx.getMatrices(), color,                    kx, ky, 5.0, 14);
        RenderUtils.renderCircle(ctx.getMatrices(), new Color(255,255,255,55), kx, ky - 1.0, 2.5, 10);
    }
 
    // ── helpers ───────────────────────────────────────────────────────────────
 
    /** Simple linear colour interpolation */
    private static Color interpolateColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
                (int)(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }
 
    // ── input ─────────────────────────────────────────────────────────────────
 
    public void onExtend() {
        for (ModuleButton mb : parent.moduleButtons) mb.extended = false;
    }
 
    public void keyPressed(int key, int scan, int mods) {
        for (Component c : settings) c.keyPressed(key, scan, mods);
    }
 
    public void mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (extended) for (Component c : settings) c.mouseDragged(mx, my, btn, dx, dy);
    }
 
    public void mouseClicked(double mx, double my, int button) {
        if (isHovered(mx, my)) {
            if (button == 0) {
                // clicking the toggle area
                final int tx = parent.getX() + parent.getWidth() - TOGGLE_W - 10;
                final int ty = parent.getY() + offset + ClickGUI.ROW_HEIGHT / 2 - TOGGLE_H / 2;
                if (mx >= tx && mx <= tx + TOGGLE_W && my >= ty && my <= ty + TOGGLE_H) {
                    module.toggle();
                } else if (!module.getSettings().isEmpty()) {
                    // clicking the arrow area (right side but not the toggle)
                    final int arrowX = parent.getX() + parent.getWidth() - TOGGLE_W - 24;
                    if (mx >= arrowX - 6 && mx <= arrowX + 12) {
                        if (!extended) onExtend();
                        extended = !extended;
                    } else {
                        // clicking the name area = toggle module
                        module.toggle();
                    }
                } else {
                    module.toggle();
                }
            } else if (button == 1) {
                if (!module.getSettings().isEmpty()) {
                    if (!extended) onExtend();
                    extended = !extended;
                }
            }
        }
        if (extended) for (Component c : settings) c.mouseClicked(mx, my, button);
    }
 
    public void onGuiClose() {
        this.currentAlpha = null;
        this.currentColor = null;
        this.hoverAnim    = 0f;
        this.toggleAnim   = module.isEnabled() ? 1f : 0f;
        for (Component c : settings) c.onGuiClose();
    }
 
    public void mouseReleased(double mx, double my, int btn) {
        for (Component c : settings) c.mouseReleased(mx, my, btn);
    }
 
    public boolean isHovered(double mx, double my) {
        return mx > parent.getX()
                && mx < parent.getX() + parent.getWidth()
                && my > parent.getY() + offset
                && my < parent.getY() + offset + ClickGUI.ROW_HEIGHT;
    }
}
 
