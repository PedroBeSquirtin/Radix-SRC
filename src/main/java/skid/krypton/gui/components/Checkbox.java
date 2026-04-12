package skid.krypton.gui.components;
 
import net.minecraft.client.gui.DrawContext;
import skid.krypton.gui.Component;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public final class Checkbox extends Component {
 
    private static final Color TEXT_COLOR  = new Color(210, 215, 225);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 10);
    private static final Color BOX_BORDER  = new Color(55, 62, 78);
    private static final Color BOX_BG      = new Color(28, 33, 42);
    private static final int   BOX_SIZE    = 13;
 
    private final BooleanSetting setting;
    private float hoverAnim;
    private float enabledAnim;
 
    public Checkbox(ModuleButton parent, Setting setting, int offset) {
        super(parent, setting, offset);
        this.setting     = (BooleanSetting) setting;
        this.hoverAnim   = 0f;
        this.enabledAnim = this.setting.getValue() ? 1f : 0f;
    }
 
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        updateAnimations(mx, my, delta);
 
        // hover row highlight
        if (!parent.parent.dragging && hoverAnim > 0.01f)
            ctx.fill(parentX(), parentY() + parentOffset() + offset,
                    parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(),
                    new Color(255, 255, 255, (int)(10 * hoverAnim)).getRGB());
 
        // label
        TextRenderer.drawString(setting.getName(), ctx,
                parentX() + 27,
                parentY() + parentOffset() + offset + parentHeight() / 2 - 5,
                TEXT_COLOR.getRGB());
 
        renderBox(ctx);
    }
 
    private void renderBox(DrawContext ctx) {
        final int bx = parentX() + 8;
        final int by = parentY() + parentOffset() + offset + parentHeight() / 2 - BOX_SIZE / 2;
        final Color accent = Utils.getMainColor(255, parent.settings.indexOf(this));
 
        // border
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), BOX_BORDER,
                bx, by, bx + BOX_SIZE, by + BOX_SIZE,
                3.0, 3.0, 3.0, 3.0, 50.0);
        // background
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), BOX_BG,
                bx + 1, by + 1, bx + BOX_SIZE - 1, by + BOX_SIZE - 1,
                2.5, 2.5, 2.5, 2.5, 50.0);
 
        // fill (animated)
        if (enabledAnim > 0.01f) {
            final Color fill = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                    (int)(255f * enabledAnim));
            final float pad = 2f + 5f * (1f - enabledAnim) * 0.5f;
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), fill,
                    bx + pad, by + pad,
                    bx + BOX_SIZE - pad, by + BOX_SIZE - pad,
                    1.5, 1.5, 1.5, 1.5, 50.0);
            // glow
            if (enabledAnim > 0.7f) {
                final float gf = (enabledAnim - 0.7f) * 3.33f;
                RenderUtils.renderRoundedQuad(ctx.getMatrices(),
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(35f * gf)),
                        bx - 1, by - 1, bx + BOX_SIZE + 1, by + BOX_SIZE + 1,
                        3.5, 3.5, 3.5, 3.5, 50.0);
            }
        }
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        final float dt = delta * 0.05f;
        hoverAnim   = (float) MathUtil.exponentialInterpolate(hoverAnim,
                (isHovered(mx, my) && !parent.parent.dragging) ? 1f : 0f, 0.25, dt);
        enabledAnim = (float) MathUtil.exponentialInterpolate(enabledAnim,
                setting.getValue() ? 1f : 0f, 0.008, dt);
        enabledAnim = (float) MathUtil.clampValue(enabledAnim, 0.0, 1.0);
    }
 
    @Override
    public void keyPressed(int key, int scan, int mods) {
        if (mouseOver && parent.extended && key == 259)
            setting.setValue(setting.getDefaultValue());
        super.keyPressed(key, scan, mods);
    }
 
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (isHovered(mx, my) && btn == 0) setting.toggle();
        super.mouseClicked(mx, my, btn);
    }
 
    @Override
    public void onGuiClose() {
        super.onGuiClose();
        hoverAnim   = 0f;
        enabledAnim = setting.getValue() ? 1f : 0f;
    }
}
 
