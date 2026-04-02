package skid.krypton.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import skid.krypton.module.Category;
import skid.krypton.module.Module;

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
            windows.add(new CategoryWindow(x, 40, 120, 20, c, this));
            x += 130;
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        for (CategoryWindow w : windows) {
            w.update(mouseX, mouseY);
            w.render(ctx, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        windows.forEach(w -> w.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        windows.forEach(w -> w.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // 🔥 TEMP: replace your broken module manager call
    public List<Module> getModules(Category category) {
        return new ArrayList<>(); // TODO hook your real modules here
    }
}
