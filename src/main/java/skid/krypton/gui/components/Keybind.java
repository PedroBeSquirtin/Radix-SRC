package skid.krypton.gui.components;
 
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import skid.krypton.gui.Component;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.BindSetting;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public final class Keybind extends Component {
 
    private static final Color TEXT_COLOR        = new Color(210, 215, 225);
    private static final Color LISTEN_TEXT_COLOR = new Color(255, 255, 255);
    private static final Color HOVER_COLOR       = new Color(255, 255, 255, 10);
    private static final Color BTN_BG            = new Color(38, 43, 55);
    private static final Color BTN_ACTIVE_BG     = new Color(55, 62, 78);
 
    private final BindSetting keybind;
    private Color accentColor;
    private float hoverAnim;
    private float listenAnim;
 
    public Keybind(ModuleButton parent, Setting setting, int offset) {
        super(parent, setting, offset);
        this.keybind = (BindSetting) setting;
    }
 
    @Override
    public void onUpdate() {
        final Color mc = Utils.getMainColor(255, parent.settings.indexOf(this));
        if (accentColor == null)
            accentColor = new Color(mc.getRed(), mc.getGreen(), mc.getBlue(), 0);
        else
            accentColor = new Color(mc.getRed(), mc.getGreen(), mc.getBlue(), accentColor.getAlpha());
        if (accentColor.getAlpha() != 255)
            accentColor = ColorUtil.a(0.05f, 255, accentColor);
        super.onUpdate();
    }
 
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        final MatrixStack ms = ctx.getMatrices();
        updateAnimations(mx, my, delta);
 
        if (!parent.parent.dragging && hoverAnim > 0.01f)
            ctx.fill(parentX(), parentY() + parentOffset() + offset,
                    parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(),
                    new Color(255, 255, 255, (int)(10 * hoverAnim)).getRGB());
 
        // label
        TextRenderer.drawString(setting.getName(), ctx,
                parentX() + 8,
                parentY() + parentOffset() + offset + parentHeight() / 2 - 5,
                TEXT_COLOR.getRGB());
 
        // key button
        final String label = keybind.isListening() ? "Listening..." : KeyUtils.getKey(keybind.getValue()).toString();
        final int lw   = TextRenderer.getWidth(label);
        final int btnW = Math.max(60, lw + 14);
        final int btnX = parentX() + parentWidth() - btnW - 6;
        final int btnY = parentY() + parentOffset() + offset + parentHeight() / 2 - 9;
 
        final Color trackColor = ColorUtil.a(BTN_BG, BTN_ACTIVE_BG, listenAnim);
        RenderUtils.renderRoundedQuad(ms, trackColor, btnX, btnY, btnX + btnW, btnY + 18, 4.0, 4.0, 4.0, 4.0, 50.0);
 
        // accent overlay when listening or hovering button
        final float overlayAlpha = listenAnim * 0.65f + (isButtonHovered(mx, my, btnX, btnY, btnW, 18) ? 0.2f : 0f);
        if (overlayAlpha > 0.01f && accentColor != null) {
            RenderUtils.renderRoundedQuad(ms,
                    new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                            (int)(accentColor.getAlpha() * overlayAlpha)),
                    btnX, btnY, btnX + btnW, btnY + 18, 4.0, 4.0, 4.0, 4.0, 50.0);
        }
        // pulse when listening
        if (keybind.isListening()) {
            final float pulse = (float)(Math.abs(Math.sin(System.currentTimeMillis() / 500.0)) * 0.25f);
            if (accentColor != null)
                RenderUtils.renderRoundedQuad(ms,
                        new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                                (int)(accentColor.getAlpha() * pulse)),
                        btnX, btnY, btnX + btnW, btnY + 18, 4.0, 4.0, 4.0, 4.0, 50.0);
        }
 
        final Color labelColor = ColorUtil.a(TEXT_COLOR, LISTEN_TEXT_COLOR, listenAnim);
        TextRenderer.drawString(label, ctx, btnX + (btnW - lw) / 2, btnY + 4, labelColor.getRGB());
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        final float dt = delta * 0.05f;
        hoverAnim  = (float) MathUtil.exponentialInterpolate(hoverAnim,
                (isHovered(mx, my) && !parent.parent.dragging) ? 1f : 0f, 0.25, dt);
        listenAnim = (float) MathUtil.exponentialInterpolate(listenAnim,
                keybind.isListening() ? 1f : 0f, 0.35, dt);
    }
 
    private boolean isButtonHovered(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }
 
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        final String lbl = keybind.isListening() ? "Listening..." : KeyUtils.getKey(keybind.getValue()).toString();
        final int bw  = Math.max(60, TextRenderer.getWidth(lbl) + 14);
        final int bx  = parentX() + parentWidth() - bw - 6;
        final int by  = parentY() + parentOffset() + offset + parentHeight() / 2 - 9;
        if (isButtonHovered(mx, my, bx, by, bw, 18)) {
            if (!keybind.isListening()) {
                if (btn == 0) { keybind.toggleListening(); keybind.setListening(true); }
            } else {
                if (keybind.isModuleKey()) parent.module.setKeybind(btn);
                keybind.setValue(btn);
                keybind.setListening(false);
            }
        }
        super.mouseClicked(mx, my, btn);
    }
 
    @Override
    public void keyPressed(int key, int scan, int mods) {
        if (keybind.isListening()) {
            if (key == 256) {
                keybind.setListening(false);
            } else if (key == 259) {
                if (keybind.isModuleKey()) parent.module.setKeybind(-1);
                keybind.setValue(-1);
                keybind.setListening(false);
            } else {
                if (keybind.isModuleKey()) parent.module.setKeybind(key);
                keybind.setValue(key);
                keybind.setListening(false);
            }
        }
        super.keyPressed(key, scan, mods);
    }
 
    @Override
    public void onGuiClose() {
        accentColor = null;
        hoverAnim   = 0f;
        listenAnim  = 0f;
        super.onGuiClose();
    }
}
 
