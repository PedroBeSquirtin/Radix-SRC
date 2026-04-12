package skid.krypton.gui.components;
 
import net.minecraft.client.gui.DrawContext;
import skid.krypton.gui.Component;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.StringSetting;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public final class TextBox extends Component {
 
    private static final Color TEXT_COLOR  = new Color(210, 215, 225);
    private static final Color VALUE_COLOR = new Color(100, 190, 255);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 10);
    private static final Color INPUT_BG    = new Color(28, 33, 43);
    private static final Color INPUT_BORDER= new Color(52, 58, 74);
 
    private final StringSetting setting;
    private float hoverAnim;
    private Color currentColor;
 
    public TextBox(ModuleButton parent, Setting setting, int offset) {
        super(parent, setting, offset);
        this.setting = (StringSetting) setting;
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
 
        if (!parent.parent.dragging && hoverAnim > 0.01f)
            ctx.fill(parentX(), parentY() + parentOffset() + offset,
                    parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(),
                    new Color(255, 255, 255, (int)(10 * hoverAnim)).getRGB());
 
        final int labelY = parentY() + parentOffset() + offset + parentHeight() / 2 - 5;
        TextRenderer.drawString(setting.getName(), ctx, parentX() + 8, labelY, TEXT_COLOR.getRGB());
 
        // input field
        final int fieldX = parentX() + TextRenderer.getWidth(setting.getName() + ": ") + 14;
        final int fieldW = parentWidth() - fieldX + parentX() - 6;
        final int fieldY = labelY - 2;
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), INPUT_BORDER,
                fieldX, fieldY, fieldX + fieldW, fieldY + 16,
                3.5, 3.5, 3.5, 3.5, 50.0);
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), INPUT_BG,
                fieldX + 1, fieldY + 1, fieldX + fieldW - 1, fieldY + 15,
                3.0, 3.0, 3.0, 3.0, 50.0);
        TextRenderer.drawString(fmtDisplay(setting.getValue()), ctx,
                fieldX + 5, fieldY + 3, VALUE_COLOR.getRGB());
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        hoverAnim = (float) MathUtil.exponentialInterpolate(hoverAnim,
                (isHovered(mx, my) && !parent.parent.dragging) ? 1f : 0f,
                0.25, delta * 0.05f);
    }
 
    private String fmtDisplay(String s) {
        if (s.isEmpty()) return "...";
        return s.length() <= 7 ? s : s.substring(0, 4) + "...";
    }
 
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (isHovered(mx, my) && btn == 0)
            mc.setScreen(new StringBox(this, setting));
        super.mouseClicked(mx, my, btn);
    }
 
    @Override
    public void onGuiClose() {
        currentColor = null;
        hoverAnim    = 0f;
        super.onGuiClose();
    }
}
