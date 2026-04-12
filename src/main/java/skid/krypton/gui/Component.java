package skid.krypton.gui;
 
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import skid.krypton.gui.components.ModuleButton;
import skid.krypton.module.setting.Setting;
import skid.krypton.utils.ColorUtil;
import skid.krypton.utils.RenderUtils;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public abstract class Component {
    public MinecraftClient mc;
    public ModuleButton    parent;
    public Setting         setting;
    public int             offset;
    public Color           currentColor;
    public boolean         mouseOver;
    int x, y, width, height;
 
    public Component(ModuleButton parent, Setting setting, int offset) {
        this.mc      = MinecraftClient.getInstance();
        this.parent  = parent;
        this.setting = setting;
        this.offset  = offset;
        this.x       = parentX();
        this.y       = parentY() + parentOffset() + offset;
        this.width   = parentX() + parentWidth();
        this.height  = parentY() + parentOffset() + offset + parentHeight();
    }
 
    public int parentX()      { return parent.parent.getX(); }
    public int parentY()      { return parent.parent.getY(); }
    public int parentWidth()  { return parent.parent.getWidth(); }
    public int parentHeight() { return parent.parent.getHeight(); }
    public int parentOffset() { return parent.offset; }
 
    public void render(DrawContext ctx, int mx, int my, float delta) {
        mouseOver = isHovered(mx, my);
        x      = parentX();
        y      = parentY() + parentOffset() + offset;
        width  = parentX() + parentWidth();
        height = parentY() + parentOffset() + offset + parentHeight();
        // draw base row background (dark, slightly lighter than panel)
        ctx.fill(x, y, width, height,
                (currentColor != null ? currentColor : new Color(25, 29, 37, 200)).getRGB());
    }
 
    public void renderDescription(DrawContext ctx, int mx, int my, float delta) {
        if (isHovered(mx, my) && setting.getDescription() != null && !parent.parent.dragging) {
            final CharSequence desc = setting.getDescription();
            final int dw = TextRenderer.getWidth(desc);
            final int dx = mc.getWindow().getWidth() / 2 - dw / 2;
            final int dy = mc.getWindow().getHeight() / 2 + 294;
            RenderUtils.renderRoundedQuad(ctx.getMatrices(),
                    new Color(20, 24, 32, 210),
                    dx - 6, dy, dx + dw + 6, dy + 24,
                    3.0, 10.0);
            TextRenderer.drawString(desc, ctx, dx, dy + 6, Color.WHITE.getRGB());
        }
    }
 
    public void onGuiClose()                                      { currentColor = null; }
    public void keyPressed(int key, int scan, int mods)           {}
    public void mouseClicked(double mx, double my, int btn)       {}
    public void mouseReleased(double mx, double my, int btn)      {}
    public void mouseDragged(double mx, double my, int btn, double dx, double dy) {}
 
    public boolean isHovered(double mx, double my) {
        return mx > parentX()
                && mx < parentX() + parentWidth()
                && my > offset + parentOffset() + parentY()
                && my < offset + parentOffset() + parentY() + parentHeight();
    }
 
    public void onUpdate() {
        // Row background: near-transparent dark
        if (currentColor == null)
            currentColor = new Color(24, 28, 37, 0);
        else
            currentColor = new Color(24, 28, 37, currentColor.getAlpha());
        if (currentColor.getAlpha() != 115)
            currentColor = ColorUtil.a(0.05f, 115, currentColor);
    }
}
