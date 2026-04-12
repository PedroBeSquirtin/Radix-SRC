package skid.krypton.gui.components;
 
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import skid.krypton.gui.Component;
import skid.krypton.module.setting.Setting;
import skid.krypton.module.setting.ItemSetting;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;
 
import java.awt.*;
 
public final class ItemBox extends Component {
 
    private static final Color TEXT_COLOR  = new Color(210, 215, 225);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 10);
    private static final Color ITEM_BG     = new Color(28, 33, 43);
    private static final Color ITEM_BORDER = new Color(52, 58, 74);
 
    private final ItemSetting setting;
    private float hoverAnim;
    private Color currentColor;
 
    public ItemBox(ModuleButton parent, Setting setting, int offset) {
        super(parent, setting, offset);
        this.setting = (ItemSetting) setting;
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
 
        final int labelX = parentX() + 6;
        final int labelY = parentY() + parentOffset() + offset + parentHeight() / 2 - 5;
        TextRenderer.drawString(setting.getName(), ctx, labelX, labelY, TEXT_COLOR.getRGB());
 
        // item box (right side)
        final int boxX = labelX + TextRenderer.getWidth(setting.getName() + ": ") + 6;
        final int boxY = labelY - 8;
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), ITEM_BORDER,
                boxX, boxY, boxX + 22, boxY + 22, 4.0, 4.0, 4.0, 4.0, 50.0);
        RenderUtils.renderRoundedQuad(ctx.getMatrices(), ITEM_BG,
                boxX + 1, boxY + 1, boxX + 21, boxY + 21, 3.5, 3.5, 3.5, 3.5, 50.0);
 
        final Item item = setting.getItem();
        if (item != null && item != Items.AIR)
            ctx.drawItem(new ItemStack(item), boxX + 3, boxY + 3);
        else
            TextRenderer.drawCenteredString("?", ctx, boxX + 11, boxY + 4,
                    new Color(140, 148, 165, 200).getRGB());
    }
 
    private void updateAnimations(int mx, int my, float delta) {
        hoverAnim = (float) MathUtil.exponentialInterpolate(hoverAnim,
                (isHovered(mx, my) && !parent.parent.dragging) ? 1f : 0f,
                0.25, delta * 0.05f);
    }
 
    @Override
    public void mouseClicked(double mx, double my, int btn) {
        if (isHovered(mx, my) && btn == 0)
            mc.setScreen(new ItemFilter(this, setting));
        super.mouseClicked(mx, my, btn);
    }
 
    @Override
    public void onGuiClose() {
        currentColor = null;
        hoverAnim    = 0f;
        super.onGuiClose();
    }
}
