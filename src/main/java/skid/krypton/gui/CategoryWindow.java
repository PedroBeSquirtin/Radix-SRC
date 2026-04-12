package skid.krypton.gui;
 
import net.minecraft.client.gui.DrawContext;
import skid.krypton.Krypton;
import skid.krypton.gui.components.ModuleButton;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
 
public final class CategoryWindow {
 
    // ── colours ──────────────────────────────────────────────────────────────
    /** Panel body – matches screenshot dark-navy look */
    private static final Color PANEL_BG    = new Color(22, 26, 33, 252);
    /** Header is slightly lighter */
    private static final Color HEADER_BG   = new Color(28, 33, 42, 255);
    /** Thin separator under the header */
    private static final Color SEP_COLOR   = new Color(45, 50, 62, 255);
    private static final Color TEXT_MAIN   = new Color(225, 228, 235);
    private static final Color TEXT_DIM    = new Color(110, 118, 135);
    private static final Color DOT_COLOR   = new Color(120, 128, 145);
 
    // ── layout ────────────────────────────────────────────────────────────────
    public List<ModuleButton> moduleButtons;
    public int x, y;
    private final int width;
    private final int height;   // row / header height
    public Color currentColor;
    private final Category category;
    public boolean dragging;
    public boolean extended;
    private int dragX, dragY;
    private int prevX, prevY;
    public ClickGUI parent;
    private float hoverAnim;
    // total animated height of the expanded module list
    private float expandAnim;
 
    public CategoryWindow(int x, int y, int width, int height, Category category, ClickGUI parent) {
        this.moduleButtons = new ArrayList<>();
        this.x = this.prevX = x;
        this.y = this.prevY = y;
        this.width    = width;
        this.height   = height;   // == ClickGUI.HEADER_HEIGHT
        this.category = category;
        this.parent   = parent;
        this.extended = true;
        this.expandAnim = 1.0f;
 
        final List<Module> modules = new ArrayList<>(Krypton.INSTANCE.getModuleManager().a(category));
        int offset = height;   // first button starts right below the header
        for (Module m : modules) {
            this.moduleButtons.add(new ModuleButton(this, m, offset));
            offset += ClickGUI.ROW_HEIGHT;
        }
    }
 
    // ── rendering ─────────────────────────────────────────────────────────────
 
    public void render(final DrawContext ctx, final int mx, final int my, final float delta) {
 
        // lazy-init current color
        if (this.currentColor == null) this.currentColor = PANEL_BG;
 
        // hover glow
        final float hoverTarget = (isHovered(mx, my) && !dragging) ? 1f : 0f;
        hoverAnim += (hoverTarget - hoverAnim) * delta * 0.08f;
 
        // expand animation
        final float expandTarget = extended ? 1f : 0f;
        expandAnim += (expandTarget - expandAnim) * delta * 0.18f;
 
        updateButtons(delta);
 
        // -- total content height ---
        int totalModuleHeight = 0;
        for (ModuleButton mb : moduleButtons) totalModuleHeight += (int) mb.animation.getAnimation();
 
        final int panelBottom = prevY + height + (int) (totalModuleHeight * expandAnim);
 
        // ---- drop shadow --------------------------------------------------------
        RenderUtils.renderRoundedQuad(ctx.getMatrices(),
                new Color(0, 0, 0, 60),
                prevX + 3, prevY + 4, prevX + width + 3, panelBottom + 4,
                7.0, 7.0, 7.0, 7.0, 50.0);
 
        // ---- panel background ---------------------------------------------------
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), PANEL_BG,
                prevX, prevY, prevX + width, panelBottom,
                7.0, 7.0, 7.0, 7.0, 50.0);
 
        // ---- header background --------------------------------------------------
        // Only top corners are rounded; bottom corners are square
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), HEADER_BG,
                prevX, prevY, prevX + width, prevY + height,
                7.0, 7.0, 0.0, 0.0, 50.0);
 
        // ---- thin separator line under header -----------------------------------
        ctx.fill(prevX, prevY + height - 1, prevX + width, prevY + height, SEP_COLOR.getRGB());
 
        // ---- accent dot on the left edge of header ------------------------------
        final Color accent = Utils.getMainColor(255, category.ordinal());
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), accent,
                prevX + 6, prevY + height / 2 - 4,
                prevX + 9, prevY + height / 2 + 4,
                2.0, 2.0, 2.0, 2.0, 50.0);
 
        // ---- category name ------------------------------------------------------
        TextRenderer.drawString(category.name, ctx,
                prevX + 14,
                prevY + height / 2 - 6,
                TEXT_MAIN.getRGB());
 
        // ---- three dots on the right (plain ASCII, safe for any font) -----------
        TextRenderer.drawString(". . .", ctx,
                prevX + width - TextRenderer.getWidth(". . .") - 8,
                prevY + height / 2 - 6,
                DOT_COLOR.getRGB());
 
        // ---- module buttons (clipped to expanded area) --------------------------
        if (expandAnim > 0.01f) {
            renderModuleButtons(ctx, mx, my, delta);
        }
    }
 
    private void renderModuleButtons(DrawContext ctx, int mx, int my, float delta) {
        for (ModuleButton mb : moduleButtons) {
            mb.render(ctx, mx, my, delta);
        }
    }
 
    // ── input ─────────────────────────────────────────────────────────────────
 
    public void keyPressed(int key, int scan, int mods) {
        for (ModuleButton mb : moduleButtons) mb.keyPressed(key, scan, mods);
    }
 
    public void onGuiClose() {
        this.currentColor = null;
        this.dragging = false;
        for (ModuleButton mb : moduleButtons) mb.onGuiClose();
    }
 
    public void mouseClicked(double mx, double my, int button) {
        if (isHovered(mx, my)) {
            switch (button) {
                case 0:
                    if (!parent.isDraggingAlready()) {
                        dragging = true;
                        dragX = (int)(mx - x);
                        dragY = (int)(my - y);
                    }
                    break;
                case 1:
                    extended = !extended;
                    break;
            }
        }
        // three-dots area: right-click anywhere on header toggles too (handled above)
        // clicks on module buttons
        if (extended || expandAnim > 0.05f) {
            for (ModuleButton mb : moduleButtons) mb.mouseClicked(mx, my, button);
        }
    }
 
    public void mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (extended) {
            for (ModuleButton mb : moduleButtons) mb.mouseDragged(mx, my, button, dx, dy);
        }
    }
 
    public void mouseReleased(double mx, double my, int button) {
        if (button == 0 && dragging) dragging = false;
        for (ModuleButton mb : moduleButtons) mb.mouseReleased(mx, my, button);
    }
 
    public void mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        prevX = x;
        prevY = y;
        prevY += (int)(vScroll * 20.0);
        y     += (int)(vScroll * 20.0);
    }
 
    // ── animation helpers ─────────────────────────────────────────────────────
 
    public void updateButtons(float delta) {
        int runningHeight = height;
        for (ModuleButton mb : moduleButtons) {
            final double targetH = mb.extended
                    ? height * (mb.settings.size() + 1)
                    : ClickGUI.ROW_HEIGHT;
            mb.animation.animate(0.5 * delta, targetH);
            mb.offset = runningHeight;
            runningHeight += (int) mb.animation.getAnimation();
        }
    }
 
    public void updatePosition(double mx, double my, float delta) {
        prevX = x;
        prevY = y;
        if (dragging) {
            final double targetX = isHovered(mx, my) ? x : prevX;
            final double targetY = isHovered(mx, my) ? y : prevY;
            x = (int) MathUtil.approachValue(0.3f * delta, targetX, mx - dragX);
            y = (int) MathUtil.approachValue(0.3f * delta, targetY, my - dragY);
        }
    }
 
    // ── accessors ─────────────────────────────────────────────────────────────
 
    public int getX()      { return prevX; }
    public int getY()      { return prevY; }
    public void setX(int x){ this.x = x; }
    public void setY(int y){ this.y = y; }
    public int getWidth()  { return width; }
    public int getHeight() { return height; }
 
    public boolean isHovered(double mx, double my) {
        return mx > x && mx < x + width && my > y && my < y + height;
    }
 
    public boolean isPrevHovered(double mx, double my) {
        return mx > prevX && mx < prevX + width && my > prevY && my < prevY + height;
    }
}
