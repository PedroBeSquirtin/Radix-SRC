package skid.krypton.gui.components;
 
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import skid.krypton.gui.Component;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.MinMaxSetting;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public final class Slider extends Component {
 
    private static final Color TEXT_COLOR  = new Color(210, 215, 225);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 10);
    private static final Color TRACK_BG    = new Color(42, 48, 60);
    private static final Color THUMB_COLOR = new Color(240, 242, 248);
    private static final Color BADGE_BG    = new Color(32, 37, 48, 220);
 
    private boolean draggingMin, draggingMax;
    private double  offsetMinX, offsetMaxX;
    public double   lerpedOffsetMinX, lerpedOffsetMaxX;
    private float   hoverAnim;
    private final MinMaxSetting setting;
    public Color    accentColor1, accentColor2;
 
    public Slider(ModuleButton parent, Setting setting, int offset) {
        super(parent, setting, offset);
        this.setting          = (MinMaxSetting) setting;
        this.lerpedOffsetMinX = parentX();
        this.lerpedOffsetMaxX = parentX() + parentWidth();
    }
 
    @Override
    public void onUpdate() {
        final Color mc  = Utils.getMainColor(255, parent.settings.indexOf(this));
        final Color mc2 = Utils.getMainColor(255, parent.settings.indexOf(this) + 1);
        accentColor1 = animColor(accentColor1, mc);
        accentColor2 = animColor(accentColor2, mc2);
        super.onUpdate();
    }
 
    private Color animColor(Color cur, Color target) {
        if (cur == null) return new Color(target.getRed(), target.getGreen(), target.getBlue(), 0);
        cur = new Color(target.getRed(), target.getGreen(), target.getBlue(), cur.getAlpha());
        if (cur.getAlpha() != 255) cur = ColorUtil.a(0.05f, 255, cur);
        return cur;
    }
 
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        final MatrixStack ms = ctx.getMatrices();
        updateAnimations(mx, my, delta);
 
        offsetMinX = (setting.getCurrentMin() - setting.getMinValue()) / (setting.getMaxValue() - setting.getMinValue()) * (parentWidth() - 10) + 5.0;
        offsetMaxX = (setting.getCurrentMax() - setting.getMinValue()) / (setting.getMaxValue() - setting.getMinValue()) * (parentWidth() - 10) + 5.0;
        lerpedOffsetMinX = MathUtil.approachValue((float)(0.5 * delta), lerpedOffsetMinX, offsetMinX);
        lerpedOffsetMaxX = MathUtil.approachValue((float)(0.5 * delta), lerpedOffsetMaxX, offsetMaxX);
 
        if (!parent.parent.dragging && hoverAnim > 0.01f)
            ctx.fill(parentX(), parentY() + parentOffset() + offset,
                    parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(),
                    new Color(255, 255, 255, (int)(10 * hoverAnim)).getRGB());
 
        final int trackY = parentY() + offset + parentOffset() + 24;
        final int trackX = parentX() + 5;
        final int trackW = parentWidth() - 10;
 
        // track background
        RenderUtils.renderRoundedQuad(ms, TRACK_BG,
                trackX, trackY, trackX + trackW, trackY + 4,
                2.0, 2.0, 2.0, 2.0, 50.0);
 
        // filled range
        if (lerpedOffsetMaxX > lerpedOffsetMinX && accentColor1 != null) {
            RenderUtils.renderRoundedQuad(ms, accentColor1,
                    trackX + lerpedOffsetMinX - 5.0, trackY,
                    trackX + lerpedOffsetMaxX - 5.0, trackY + 4,
                    2.0, 2.0, 2.0, 2.0, 50.0);
        }
 
        // name label
        TextRenderer.drawString(setting.getName(), ctx,
                parentX() + 6, parentY() + parentOffset() + offset + 9,
                TEXT_COLOR.getRGB());
 
        // value badge
        final String val = getDisplayText();
        final int bw = TextRenderer.getWidth(val) + 10;
        final int bx = parentX() + parentWidth() - bw - 4;
        final int by = parentY() + parentOffset() + offset + 6;
        RenderUtils.renderRoundedQuad(ms, BADGE_BG, bx, by, bx + bw, by + 14, 3.0, 3.0, 3.0, 3.0, 50.0);
        TextRenderer.drawString(val, ctx, bx + 5, by + 2, accentColor1 != null ? accentColor1.getRGB() : TEXT_COLOR.getRGB());
 
        // thumbs (rendered by ModuleButton's renderSliderKnobs, so just the flat thumbs here as fallback)
        final float thumbMid = trackY + 2f - 4f;
        RenderUtils.renderRoundedQuad(ms, THUMB_COLOR,
                (float)(trackX + lerpedOffsetMinX - 5.0 - 4.0), thumbMid,
                (float)(trackX + lerpedOffsetMinX - 5.0 + 4.0), thumbMid + 8f,
                4.0, 4.0, 4.0, 4.0, 50.0);
        RenderUtils.renderRoundedQuad(ms, THUMB_COLOR,
                (float)(trackX + lerpedOffsetMaxX - 5.0 - 4.0), thumbMid,
                (float)(trackX + lerpedOffsetMaxX - 5.0 + 4.0), thumbMid + 8f,
                4.0, 4.0, 4.0, 4.0, 50.0);
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        hoverAnim = (float) MathUtil.exponentialInterpolate(hoverAnim,
                (isHovered(mx, my) && !parent.parent.dragging) ? 1f : 0f,
                0.25, delta * 0.05f);
    }
 
    private String getDisplayText() {
        if (setting.getCurrentMin() == setting.getCurrentMax())
            return fmt(setting.getCurrentMin());
        return fmt(setting.getCurrentMin()) + " - " + fmt(setting.getCurrentMax());
    }
 
    private String fmt(double v) {
        final double s = setting.getStep();
        if (s == 0.1)  return String.format("%.1f", v);
        if (s == 0.01) return String.format("%.2f", v);
        if (s == 0.001)return String.format("%.3f", v);
        if (s >= 1.0)  return String.format("%.0f", v);
        return String.valueOf(v);
    }
 
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && isHovered(mx, my)) {
            if      (isHoveredMin(mx, my)) { draggingMin = true; slideMin(mx); }
            else if (isHoveredMax(mx, my)) { draggingMax = true; slideMax(mx); }
            else if (mx < parentX() + offsetMinX) { draggingMin = true; slideMin(mx); }
            else if (mx > parentX() + offsetMaxX) { draggingMax = true; slideMax(mx); }
            else if (mx - (parentX() + offsetMinX) < (parentX() + offsetMaxX) - mx) { draggingMin = true; slideMin(mx); }
            else { draggingMax = true; slideMax(mx); }
        }
        super.mouseClicked(mx, my, btn);
    }
 
    @Override
    public void keyPressed(int key, int scan, int mods) {
        if (mouseOver && key == 259) {
            setting.setCurrentMax(setting.getDefaultMax());
            setting.setCurrentMin(setting.getDefaultMin());
        }
        super.keyPressed(key, scan, mods);
    }
 
    public boolean isHoveredMin(double mx, double my) {
        return isHovered(mx, my) && mx > parentX() + offsetMinX - 8 && mx < parentX() + offsetMinX + 8;
    }
 
    public boolean isHoveredMax(double mx, double my) {
        return isHovered(mx, my) && mx > parentX() + offsetMaxX - 8 && mx < parentX() + offsetMaxX + 8;
    }
 
    @Override
    public void mouseReleased(double mx, double my, int btn) {
        if (btn == 0) { draggingMin = false; draggingMax = false; }
        super.mouseReleased(mx, my, btn);
    }
 
    @Override
    public void mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingMin) slideMin(mx);
        if (draggingMax) slideMax(mx);
        super.mouseDragged(mx, my, btn, dx, dy);
    }
 
    @Override
    public void onGuiClose() {
        accentColor1 = null;
        accentColor2 = null;
        hoverAnim    = 0f;
        super.onGuiClose();
    }
 
    private void slideMin(double mx) {
        setting.setCurrentMin(Math.min(
                MathUtil.roundToNearest(
                        MathHelper.clamp((mx - (parentX() + 5)) / (parentWidth() - 10), 0.0, 1.0)
                                * (setting.getMaxValue() - setting.getMinValue()) + setting.getMinValue(),
                        setting.getStep()),
                setting.getCurrentMax()));
    }
 
    private void slideMax(double mx) {
        setting.setCurrentMax(Math.max(
                MathUtil.roundToNearest(
                        MathHelper.clamp((mx - (parentX() + 5)) / (parentWidth() - 10), 0.0, 1.0)
                                * (setting.getMaxValue() - setting.getMinValue()) + setting.getMinValue(),
                        setting.getStep()),
                setting.getCurrentMin()));
    }
}
 
