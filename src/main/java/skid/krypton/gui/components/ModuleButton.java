package skid.krypton.gui.components;

import net.minecraft.client.gui.DrawContext;
import skid.krypton.gui.CategoryWindow;
import skid.krypton.module.Module;
import skid.krypton.utils.*;

import java.awt.*;

public class ModuleButton {

    public final CategoryWindow parent;
    public final Module module;

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
                && mouseY >= y && mouseY <= y + 18;

        hover = (float) MathUtil.approachValue(0.2f * delta, hover, hovered ? 1f : 0f);

        Color bg = module.isEnabled()
                ? new Color(220, 50, 50)
                : ColorUtil.a(new Color(30,30,30), new Color(50,50,50), hover);

        RenderUtils.renderRoundedQuad(
                ctx.getMatrices(),
                bg,
                x + 4, y,
                x + parent.getWidth() - 4, y + 18,
                6,6,6,6,
                30
        );

        TextRenderer.drawString(
                module.getName(),
                ctx,
                x + 8,
                y + 5,
                Color.WHITE.getRGB()
        );
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        int x = parent.x;
        int y = parent.y + offset;

        if (mouseX >= x && mouseX <= x + parent.getWidth()
                && mouseY >= y && mouseY <= y + 18) {

            if (button == 0) {
                module.toggle();
            }
        }
    }
}
