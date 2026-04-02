package skid.krypton.gui.components;

import net.minecraft.client.gui.DrawContext;
import skid.krypton.module.Module;
import skid.krypton.gui.CategoryWindow;
import skid.krypton.utils.*;

import java.awt.*;

public class ModuleButton {

    private final CategoryWindow parent;
    private final Module module;

    public int offset;

    private float hover = 0f;

    public ModuleButton(CategoryWindow parent, Module module, int offset) {
        this.parent = parent;
        this.module = module;
        this.offset = offset;
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        int x = parent.x;
        int y = parent.y + offset;

        boolean hovered = mouseX >= x && mouseX <= x + parent.getWidth()
                && mouseY >= y && mouseY <= y + 20;

        hover = MathUtil.approachValue(0.2f * delta, hover, hovered ? 1 : 0);

        boolean enabled = module.isEnabled();

        // RED highlight like screenshot
        Color bg = enabled
                ? new Color(200, 40, 40, 255)
                : ColorUtil.a(new Color(30, 30, 35), new Color(50, 50, 60), hover);

        RenderUtils.renderRoundedQuad(
                ctx.getMatrices(),
                bg,
                x + 4, y,
                x + parent.getWidth() - 4, y + 18,
                6, 6, 6, 6,
                30
        );

        TextRenderer.drawString(
                module.getName(),
                ctx,
                x + 10,
                y + 6,
                enabled ? Color.WHITE.getRGB() : new Color(180, 180, 180).getRGB()
        );
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        int x = parent.x;
        int y = parent.y + offset;

        if (mouseX >= x && mouseX <= x + parent.getWidth()
                && mouseY >= y && mouseY <= y + 20) {

            if (button == 0) {
                module.toggle();
            }
        }
    }

    public void mouseReleased(double x, double y, int button) {}
}
