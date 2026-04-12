package skid.krypton.gui.components;
 
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import skid.krypton.gui.Component;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public final class NumberBox extends Component {
 
    private static final Color TEXT_COLOR  = new Color(210, 215, 225);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 10);
    private static final Color TRACK_BG    = new Color(42, 48, 60);
    private static final Color BADGE_BG    = new Color(32, 37, 48, 220);
 
    public boolean dragging;
    public double  offsetX;
    public double  lerpedOffsetX;
    private float  hoverAnim;
    private final NumberSetting setting;
    public Color   currentColor1;
 
    public NumberBox(ModuleButton parent, Setting setting, int offset) {
        super(parent, setting, offset);
        this.lerpedOffsetX = 0.0;
        this.hoverAnim     = 0f;
        this.setting       = (NumberSetting) setting;
    }
 
    @Override
    public void onUpdate() {
        final Color mc = Utils.getMainColor(255, parent.settings.indexOf(this));
        if (currentColor1 == null)
            currentColor1 = new Color(mc.getRed(), mc.getGreen(), mc.getBlue(), 0);
        else
            currentColor1 = new Color(mc.getRed(), mc.getGreen(), mc.getBlue(), currentColor1.getAlpha());
        if (currentColor1.getAlpha() != 255)
            currentColor1 = ColorUtil.a(0.05f, 255, currentColor1);
        super.onUpdate();
    }
 
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        updateAnimations(mx, my, delta);
 
        offsetX       = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()) * parentWidth();
        lerpedOffsetX = MathUtil.approachValue((float)(0.5 * delta), lerpedOffsetX, offsetX);
 
        // hover highlight
        if (!parent.parent.dragging && hoverAnim > 0.01f)
            ctx.fill(parentX(), parentY() + parentOffset() + offset,
                    parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(),
                    new Color(255, 255, 255, (int)(10 * hoverAnim)).getRGB());
 
        final int trackY = parentY() + offset + parentOffset() + 24;
        final int trackX = parentX() + 6;
        final int trackW = parentWidth() - 12;
 
        // track background
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), TRACK_BG,
                trackX, trackY, trackX + trackW, trackY + 4,
                2.0, 2.0, 2.0, 2.0, 50.0);
        // track fill
        if (lerpedOffsetX > 2.5) {
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), currentColor1,
                    trackX, trackY,
                    trackX + Math.max(lerpedOffsetX - 6.0, 0.0), trackY + 4,
                    2.0, 2.0, 2.0, 2.0, 50.0);
        }
 
        // setting name
        TextRenderer.drawString(setting.getName(), ctx,
                parentX() + 6,
                parentY() + parentOffset() + offset + 9,
                TEXT_COLOR.getRGB());
 
        // value badge (right side, above track)
        final String val = getDisplayValue();
        final int bw = TextRenderer.getWidth(val) + 10;
        final int bx = parentX() + parentWidth() - bw - 4;
        final int nameY = parentY() + parentOffset() + offset + 6;
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), BADGE_BG,
                bx, nameY, bx + bw, nameY + 14,
                3.0, 3.0, 3.0, 3.0, 50.0);
        TextRenderer.drawString(val, ctx, bx + 5, nameY + 2, currentColor1.getRGB());
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        hoverAnim = (float) MathUtil.exponentialInterpolate(hoverAnim,
                (isHovered(mx, my) && !parent.parent.dragging) ? 1f : 0f,
                0.25, delta * 0.05f);
    }
 
    private String getDisplayValue() {
        final double v = setting.getValue();
        final double f = setting.getFormat();
        if (f == 0.1)    return String.format("%.1f", v);
        if (f == 0.01)   return String.format("%.2f", v);
        if (f == 0.001)  return String.format("%.3f", v);
        if (f == 1e-4)   return String.format("%.4f", v);
        if (f >= 1.0)    return String.format("%.0f", v);
        return String.valueOf(v);
    }
 
    @Override
    public void onGuiClose() {
        currentColor1 = null;
        hoverAnim     = 0f;
        super.onGuiClose();
    }
 
    private void slide(double mx) {
        setting.getValue(MathUtil.roundToNearest(
                MathHelper.clamp((mx - (parentX() + 6)) / (parentWidth() - 12), 0.0, 1.0)
                        * (setting.getMax() - setting.getMin()) + setting.getMin(),
                setting.getFormat()));
    }
 
    @Override
    public void keyPressed(int key, int scan, int mods) {
        if (mouseOver && parent.extended && key == 259)
            setting.getValue(setting.getDefaultValue());
        super.keyPressed(key, scan, mods);
    }
 
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (isHovered(mx, my) && btn == 0) { dragging = true; slide(mx); }
        super.mouseClicked(mx, my, btn);
    }
 
    @Override
    public void mouseReleased(double mx, double my, int btn) {
        if (btn == 0) dragging = false;
        super.mouseReleased(mx, my, btn);
    }
 
    @Override
    public void mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) slide(mx);
        super.mouseDragged(mx, my, btn, dx, dy);
    }
}
 
