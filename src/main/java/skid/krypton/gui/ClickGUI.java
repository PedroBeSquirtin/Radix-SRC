package skid.krypton.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import skid.krypton.module.Category;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {

    private final List<CategoryWindow> windows = new ArrayList<>();

    public ClickGUI() {
        super(Text.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        windows.clear();

        int x = 20;

        for (Category c : Category.values()) {
            windows.add(new CategoryWindow(x, 40, 130, 20, c, this));
            x += 140; // spacing like screenshot
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);

        for (CategoryWindow w : windows) {
            w.updatePosition(mouseX, mouseY, delta);
            w.render(ctx, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CategoryWindow w : windows) {
            w.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (CategoryWindow w : windows) {
            w.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean isDraggingAlready() {
        return false;
    }
}
