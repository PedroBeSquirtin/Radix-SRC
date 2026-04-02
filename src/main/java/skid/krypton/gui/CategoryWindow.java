package skid.krypton.gui;

import net.minecraft.client.gui.DrawContext;
import skid.krypton.gui.components.ModuleButton;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.utils.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryWindow {

    public List<ModuleButton> moduleButtons = new ArrayList<>();

    public int x, y;
    private final int width, height;

    private final Category category;
    private final ClickGUI parent;

    public boolean dragging, extended = true;

    private int dragX, dragY;
    private float hoverAnim = 0f;

    public CategoryWindow(int x, int y, int width, int height, Category category, ClickGUI parent) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.category = category;
        this.parent = parent;

        int offset = height;

        for (Module m : parent.getModules(category)) {
            moduleButtons.add(new ModuleButton(this, m, offset));
            offset += 20;
        }
    }

    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnim = (float) MathUtil.approachValue(0.15f * delta, hoverAnim, hovered ? 1f : 0f);

        Color bg = ColorUtil.a(
                new Color(20, 22, 28, 220),
                new Color(35, 40, 50, 255),
                hoverAnim
        );

        RenderUtils.renderRoundedQuad(
                ctx.getMatrices(),
                bg,
                x, y,
                x + width, y + height,
                8, 8, extended ? 0 : 8, extended ? 0 : 8,
                50
        );

        TextRenderer.drawString(
                category.name(),
                ctx,
                x + 8,
                y + 6,
                Color.WHITE.getRGB()
        );

        if (extended) {
            for (ModuleButton m : moduleButtons) {
                m.render(ctx, mouseX, mouseY, delta);
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY)) {
            if (button == 0) {
                dragging = true;
                dragX = (int) (mouseX - x);
                dragY = (int) (mouseY - y);
            }
            if (button == 1) {
                extended = !extended;
            }
        }

        if (extended) {
            moduleButtons.forEach(m -> m.mouseClicked(mouseX, mouseY, button));
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
    }

    public void update(double mouseX, double mouseY) {
        if (dragging) {
            x = (int) mouseX - dragX;
            y = (int) mouseY - dragY;
        }
    }

    public boolean isHovered(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public int getWidth() { return width; }
}
