package skid.krypton.gui;
 
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import skid.krypton.Krypton;
import skid.krypton.module.Category;
import skid.krypton.utils.ColorUtil;
import skid.krypton.utils.RenderUtils;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
public final class ClickGUI extends Screen {
    public List<CategoryWindow> windows;
    public Color currentColor;
    private CharSequence tooltipText;
    private int tooltipX;
    private int tooltipY;
    private final Color DESCRIPTION_BG;
 
    // Panel dimensions matching the screenshot style
    public static final int PANEL_WIDTH  = 160;
    public static final int HEADER_HEIGHT = 36;
    public static final int ROW_HEIGHT   = 32;
 
    public ClickGUI() {
        super(Text.empty());
        this.windows = new ArrayList<>();
        this.tooltipText = null;
        this.DESCRIPTION_BG = new Color(20, 24, 30, 245);
 
        int x = 18;
        final Category[] values = Category.values();
        for (Category value : values) {
            this.windows.add(new CategoryWindow(x, 18, PANEL_WIDTH, HEADER_HEIGHT, value, this));
            x += PANEL_WIDTH + 10;
        }
    }
 
    public boolean isDraggingAlready() {
        for (CategoryWindow window : this.windows) {
            if (window.dragging) return true;
        }
        return false;
    }
 
    public void setTooltip(final CharSequence tooltipText, final int tooltipX, final int tooltipY) {
        this.tooltipText = tooltipText;
        this.tooltipX    = tooltipX;
        this.tooltipY    = tooltipY;
    }
 
    public void setInitialFocus() {
        if (this.client == null) return;
        super.setInitialFocus();
    }
 
    @Override
    public void render(final DrawContext drawContext, final int n, final int n2, final float n3) {
        if (Krypton.mc.currentScreen != this) return;
 
        if (Krypton.INSTANCE.screen != null)
            Krypton.INSTANCE.screen.render(drawContext, 0, 0, n3);
 
        if (this.currentColor == null) {
            this.currentColor = new Color(0, 0, 0, 0);
        } else {
            this.currentColor = new Color(0, 0, 0, this.currentColor.getAlpha());
        }
 
        final int targetAlpha = skid.krypton.module.modules.client.Krypton.renderBackground.getValue() ? 150 : 0;
        if (this.currentColor.getAlpha() != targetAlpha)
            this.currentColor = ColorUtil.a(0.05f, targetAlpha, this.currentColor);
 
        if (Krypton.mc.currentScreen instanceof ClickGUI)
            drawContext.fill(0, 0, Krypton.mc.getWindow().getWidth(), Krypton.mc.getWindow().getHeight(), this.currentColor.getRGB());
 
        RenderUtils.unscaledProjection();
        final int scaledX = n  * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final int scaledY = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
 
        super.render(drawContext, scaledX, scaledY, n3);
 
        for (final CategoryWindow next : this.windows) {
            next.render(drawContext, scaledX, scaledY, n3);
            next.updatePosition(scaledX, scaledY, n3);
        }
 
        if (this.tooltipText != null) {
            this.renderTooltip(drawContext, this.tooltipText, this.tooltipX, this.tooltipY);
            this.tooltipText = null;
        }
 
        RenderUtils.scaledProjection();
    }
 
    @Override
    public boolean keyPressed(final int n, final int n2, final int n3) {
        for (CategoryWindow w : this.windows) w.keyPressed(n, n2, n3);
        return super.keyPressed(n, n2, n3);
    }
 
    @Override
    public boolean mouseClicked(final double n, final double n2, final int n3) {
        final double sx = n  * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final double sy = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        for (CategoryWindow w : this.windows) w.mouseClicked(sx, sy, n3);
        return super.mouseClicked(sx, sy, n3);
    }
 
    @Override
    public boolean mouseDragged(final double n, final double n2, final int n3, final double n4, final double n5) {
        final double sx = n  * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final double sy = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        for (CategoryWindow w : this.windows) w.mouseDragged(sx, sy, n3, n4, n5);
        return super.mouseDragged(sx, sy, n3, n4, n5);
    }
 
    @Override
    public boolean mouseScrolled(final double n, final double n2, final double n3, final double n4) {
        final double sy = n2 * MinecraftClient.getInstance().getWindow().getScaleFactor();
        for (CategoryWindow w : this.windows) w.mouseScrolled(n, sy, n3, n4);
        return super.mouseScrolled(n, sy, n3, n4);
    }
 
    @Override
    public boolean shouldPause() { return false; }
 
    @Override
    public void close() {
        Krypton.INSTANCE.getModuleManager().getModuleByClass(skid.krypton.module.modules.client.Krypton.class).setEnabled(false);
        this.onGuiClose();
    }
 
    public void onGuiClose() {
        Krypton.mc.setScreenAndRender(Krypton.INSTANCE.screen);
        this.currentColor = null;
        for (CategoryWindow w : this.windows) w.onGuiClose();
    }
 
    @Override
    public boolean mouseReleased(final double n, final double n2, final int n3) {
        final double sx = n  * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        final double sy = n2 * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
        for (CategoryWindow w : this.windows) w.mouseReleased(sx, sy, n3);
        return super.mouseReleased(sx, sy, n3);
    }
 
    private void renderTooltip(final DrawContext drawContext, final CharSequence text, int x, final int y) {
        if (text == null || text.length() == 0) return;
        final int tw = TextRenderer.getWidth(text);
        final int fw = Krypton.mc.getWindow().getFramebufferWidth();
        if (x + tw + 10 > fw) x = fw - tw - 10;
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.DESCRIPTION_BG,
                x - 6, y - 5, x + tw + 6, y + 16,
                4.0, 4.0, 4.0, 4.0, 50.0);
        TextRenderer.drawString(text, drawContext, x, y, new Color(220, 225, 235).getRGB());
    }
}
