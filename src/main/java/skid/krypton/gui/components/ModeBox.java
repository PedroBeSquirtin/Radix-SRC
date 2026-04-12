package skid.krypton.gui.components;
 
import net.minecraft.client.gui.DrawContext;
import skid.krypton.gui.Component;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.ModeSetting;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public final class ModeBox extends Component {
 
    private static final Color TEXT_COLOR  = new Color(210, 215, 225);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 10);
    private static final Color PILL_BG     = new Color(32, 37, 48, 230);
    private static final Color PILL_BORDER = new Color(55, 62, 78);
    private static final Color ARROW_COLOR = new Color(130, 138, 155);
 
    private final ModeSetting<?> setting;
    private float hoverAnim;
    private float selectAnim, prevSelectAnim;
    private boolean wasClicked;
    public Color currentColor;
 
    public ModeBox(ModuleButton parent, Setting setting, int offset) {
        super(parent, setting, offset);
        this.setting = (ModeSetting<?>)setting;
    }
 
    @Override
    public void onUpdate() {
        final Color mc = Utils.getMainColor(255, parent.settings.indexOf(this));
        if (currentColor == null)
            currentColor = new Color(mc.getRed(), mc.getGreen(), mc.getBlue(), 0);
        else
            currentColor = new Color(mc.getRed(), mc.getGreen(), mc.getBlue(), currentColor.getAlpha());
        if (currentColor.getAlpha() != 255)
            currentColor = ColorUtil.a(0.05f, 255, currentColor);
        super.onUpdate();
    }
 
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        updateAnimations(mx, my, delta);
 
        // hover highlight
        if (!parent.parent.dragging && hoverAnim > 0.01f)
            ctx.fill(parentX(), parentY() + parentOffset() + offset,
                    parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(),
                    new Color(255, 255, 255, (int)(10 * hoverAnim)).getRGB());
 
        final int labelY = parentY() + parentOffset() + offset + parentHeight() / 2 - 5;
 
        // setting name
        TextRenderer.drawString(setting.getName(), ctx, parentX() + 8, labelY, TEXT_COLOR.getRGB());
 
        // mode value pill (right-aligned) - using plain ASCII only (font safety)
        final String modeName = setting.getValue().name();
        final int pillW = TextRenderer.getWidth(modeName) + TextRenderer.getWidth("< ") + TextRenderer.getWidth(" >") + 14;
        final int pillX = parentX() + parentWidth() - pillW - 6;
        final int pillY = labelY - 3;
 
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), PILL_BG,
                pillX, pillY, pillX + pillW, pillY + 16,
                4.0, 4.0, 4.0, 4.0, 50.0);
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), PILL_BORDER,
                pillX, pillY, pillX + pillW, pillY + 1,
                0.0, 0.0, 0.0, 0.0, 50.0);
        // left arrow
        TextRenderer.drawString("<", ctx, pillX + 4, pillY + 2, ARROW_COLOR.getRGB());
        // mode name (accent colour)
        final int nameX = pillX + TextRenderer.getWidth("< ") + 6;
        TextRenderer.drawString(modeName, ctx, nameX, pillY + 2, currentColor != null ? currentColor.getRGB() : TEXT_COLOR.getRGB());
        // right arrow
        TextRenderer.drawString(">", ctx, pillX + pillW - TextRenderer.getWidth(">") - 4, pillY + 2, ARROW_COLOR.getRGB());
 
        // animated underline selector
        renderSelector(ctx, delta);
 
        if (wasClicked) {
            wasClicked    = false;
            prevSelectAnim = 0f;
            selectAnim    = 0.01f;
        }
    }
 
    private void renderSelector(DrawContext ctx, float delta) {
        final int size = setting.getPossibleValues().size();
        if (size < 2) return;
 
        final int index  = setting.getPossibleValues().indexOf(setting.getValue());
        final int trackX = parentX() + 6;
        final int trackW = parentWidth() - 12;
        final int trackY = parentY() + parentOffset() + offset + parentHeight() - 5;
        final int segW   = trackW / size;
 
        // track bg
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), new Color(42, 48, 60),
                trackX, trackY, trackX + trackW, trackY + 3,
                1.5, 1.5, 1.5, 1.5, 50.0);
 
        // animated segment
        final int prevIdx  = (index - 1 + size) % size;
        final int nextIdx  = (index + 1) % size;
        float segX;
        if (prevSelectAnim > 0.01f)
            segX = (float) MathUtil.linearInterpolate(trackX + prevIdx * segW, trackX + index * segW, 1f - prevSelectAnim);
        else if (selectAnim > 0.01f)
            segX = (float) MathUtil.linearInterpolate(trackX + index * segW, trackX + nextIdx * segW, selectAnim);
        else
            segX = trackX + index * segW;
 
        if (currentColor != null) {
            RenderUtils.renderRoundedQuad(ctx.getMatrices(), currentColor,
                    segX, trackY, segX + segW, trackY + 3,
                    1.5, 1.5, 1.5, 1.5, 50.0);
        }
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        final float dt = delta * 0.05f;
        hoverAnim   = (float) MathUtil.exponentialInterpolate(hoverAnim,
                (isHovered(mx, my) && !parent.parent.dragging) ? 1f : 0f, 0.25, dt);
        if (selectAnim > 0.01f) {
            selectAnim = (float) MathUtil.exponentialInterpolate(selectAnim, 0.0, 0.15, dt);
            if (selectAnim < 0.01f) prevSelectAnim = 0.99f;
        }
        if (prevSelectAnim > 0.01f)
            prevSelectAnim = (float) MathUtil.exponentialInterpolate(prevSelectAnim, 0.0, 0.15, dt);
    }
 
    @Override
    public void keyPressed(int key, int scan, int mods) {
        if (mouseOver && parent.extended) {
            if      (key == 259) setting.setModeIndex(setting.getOriginalValue());
            else if (key == 262) { setting.cycleUp();   wasClicked = true; }
            else if (key == 263) { setting.cycleDown(); wasClicked = true; }
        }
        super.keyPressed(key, scan, mods);
    }
 
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (isHovered(mx, my)) {
            if      (btn == 0) { setting.cycleUp();                              wasClicked = true; }
            else if (btn == 1) { setting.cycleDown();                            wasClicked = true; }
            else if (btn == 2)   setting.setModeIndex(setting.getOriginalValue());
        }
        super.mouseClicked(mx, my, btn);
    }
 
    @Override
    public void onGuiClose() {
        currentColor    = null;
        hoverAnim       = 0f;
        selectAnim      = 0f;
        prevSelectAnim  = 0f;
        super.onGuiClose();
    }
}
