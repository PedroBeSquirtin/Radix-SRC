package skid.krypton.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import skid.krypton.Krypton;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.*;
import skid.krypton.utils.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public final class ClickGUI extends Screen {
    // Color scheme - Radix inspired
    private static final Color DARK_NAVY = new Color(15, 20, 30, 255);
    private static final Color CHARCOAL = new Color(25, 28, 35, 255);
    private static final Color SURFACE = new Color(30, 35, 45, 255);
    private static final Color ELECTRIC_BLUE = new Color(0, 150, 255, 255);
    private static final Color VIOLET = new Color(138, 43, 226, 255);
    private static final Color TEXT_PRIMARY = new Color(245, 245, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(160, 170, 190, 255);
    private static final Color TEXT_MUTED = new Color(100, 110, 130, 255);
    private static final Color HOVER = new Color(45, 55, 75, 255);
    private static final Color SUCCESS = new Color(0, 200, 100, 255);
    private static final Color BORDER = new Color(45, 52, 68, 255);

    // Layout
    private static final int CATEGORY_WIDTH = 140;
    private static final int MODULE_WIDTH = 200;
    private static final int SETTINGS_WIDTH = 280;
    private static final int PANEL_HEIGHT = 450;
    private static final int HEADER_HEIGHT = 50;
    private static final int ITEM_HEIGHT = 32;
    private static final int PADDING = 15;
    private static final int SPACING = 2;
    private static final int CORNER_RADIUS = 10;

    // State
    private Category selectedCategory;
    private Module selectedModule;
    private String searchQuery;
    private TextFieldWidget searchBox;
    private int scrollOffset;
    private int maxScroll;
    private float animationProgress;
    private long lastFrame;
    public Color currentColor;

    public ClickGUI() {
        super(Text.empty());
        this.selectedCategory = Category.COMBAT;
        this.searchQuery = "";
        this.scrollOffset = 0;
        this.animationProgress = 0;
        this.lastFrame = System.currentTimeMillis();
    }

    private static int toMCColor(Color c) {
        return net.minecraft.util.math.ColorHelper.Argb.getArgb(c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
    }

    private void updateAnimation() {
        long now = System.currentTimeMillis();
        float delta = Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float target = (Krypton.mc.currentScreen == this) ? 1.0f : 0.0f;
        animationProgress += (target - animationProgress) * delta * 8;
        animationProgress = Math.max(0, Math.min(1, animationProgress));
    }

    @Override
    protected void init() {
        int panelX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + SPACING * 2)) / 2;
        int searchX = panelX + CATEGORY_WIDTH + SPACING + MODULE_WIDTH - 150;
        
        searchBox = new TextFieldWidget(textRenderer, searchX, (height - PANEL_HEIGHT) / 2 + 15, 140, 25, Text.literal("Search"));
        searchBox.setMaxLength(50);
        searchBox.setDrawsBackground(true);
        addDrawableChild(searchBox);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (Krypton.mc.currentScreen != this) return;
        
        updateAnimation();
        
        // Background
        int bgAlpha = (int) (180 * animationProgress);
        context.fill(0, 0, width, height, new Color(0, 0, 0, bgAlpha).getRGB());
        
        // Scale animation
        float scale = 0.92f + (animationProgress * 0.08f);
        context.getMatrices().push();
        context.getMatrices().translate(width / 2f, height / 2f, 0);
        context.getMatrices().scale(scale, scale, 1);
        context.getMatrices().translate(-width / 2f, -height / 2f, 0);
        
        int panelX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + SPACING * 2)) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        
        // Draw panels
        renderCategoryPanel(context, panelX, panelY, mouseX, mouseY);
        renderModulePanel(context, panelX + CATEGORY_WIDTH + SPACING, panelY, mouseX, mouseY);
        renderSettingsPanel(context, panelX + CATEGORY_WIDTH + SPACING + MODULE_WIDTH + SPACING, panelY, mouseX, mouseY);
        
        // Title
        String title = "KRYPTON+ v2.0";
        int titleWidth = textRenderer.getWidth(title);
        context.drawText(textRenderer, title, panelX + CATEGORY_WIDTH / 2 - titleWidth / 2, panelY - 25, toMCColor(ELECTRIC_BLUE), true);
        
        context.getMatrices().pop();
    }
    
    private void renderCategoryPanel(DrawContext context, int x, int y, int mouseX, int mouseY) {
        // Panel background
        RenderUtils.renderRoundedQuad(context.getMatrices(), CHARCOAL, x, y, x + CATEGORY_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        RenderUtils.renderRoundedOutline(context, BORDER, x, y, x + CATEGORY_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5f, 20);
        
        // Header
        RenderUtils.renderRoundedQuad(context.getMatrices(), SURFACE, x, y, x + CATEGORY_WIDTH, y + HEADER_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, 0, 0, 50);
        context.drawText(textRenderer, "CATEGORIES", x + PADDING, y + HEADER_HEIGHT / 2 - 4, toMCColor(TEXT_SECONDARY), true);
        
        int itemY = y + HEADER_HEIGHT + 5;
        for (Category category : Category.values()) {
            boolean isSelected = category == selectedCategory;
            boolean isHovered = mouseX >= x && mouseX <= x + CATEGORY_WIDTH && 
                               mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            
            // Background
            if (isSelected) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(0, 150, 255, 40), x + 5, itemY, x + CATEGORY_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
                context.fill(x + 3, itemY, x + 5, itemY + ITEM_HEIGHT, toMCColor(ELECTRIC_BLUE));
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), HOVER, x + 5, itemY, x + CATEGORY_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
            }
            
            // Text
            Color textColor = isSelected ? ELECTRIC_BLUE : TEXT_SECONDARY;
            context.drawText(textRenderer, category.name.toString(), x + PADDING + 5, itemY + ITEM_HEIGHT / 2 - 4, toMCColor(textColor), true);
            
            itemY += ITEM_HEIGHT + 2;
        }
    }
    
    private void renderModulePanel(DrawContext context, int x, int y, int mouseX, int mouseY) {
        // Panel background
        RenderUtils.renderRoundedQuad(context.getMatrices(), CHARCOAL, x, y, x + MODULE_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        RenderUtils.renderRoundedOutline(context, BORDER, x, y, x + MODULE_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5f, 20);
        
        // Header
        RenderUtils.renderRoundedQuad(context.getMatrices(), SURFACE, x, y, x + MODULE_WIDTH, y + HEADER_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, 0, 0, 50);
        context.drawText(textRenderer, selectedCategory.name.toString().toUpperCase(), x + PADDING, y + HEADER_HEIGHT / 2 - 4, toMCColor(ELECTRIC_BLUE), true);
        
        // Search results / modules
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(selectedCategory);
        List<Module> filtered = new ArrayList<>();
        String searchText = searchBox.getText().toLowerCase();
        
        for (Module module : modules) {
            if (searchText.isEmpty() || module.getName().toString().toLowerCase().contains(searchText)) {
                filtered.add(module);
            }
        }
        
        maxScroll = Math.max(0, filtered.size() - 12);
        int itemY = y + HEADER_HEIGHT + 5;
        int visible = 0;
        
        for (int i = scrollOffset; i < filtered.size() && visible < 12; i++) {
            Module module = filtered.get(i);
            boolean isSelected = module == selectedModule;
            boolean isHovered = mouseX >= x && mouseX <= x + MODULE_WIDTH && 
                               mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            boolean isEnabled = module.isEnabled();
            
            // Background
            if (isSelected) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(0, 150, 255, 30), x + 5, itemY, x + MODULE_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), HOVER, x + 5, itemY, x + MODULE_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
            }
            
            // Module name
            Color nameColor = isEnabled ? ELECTRIC_BLUE : TEXT_PRIMARY;
            context.drawText(textRenderer, module.getName().toString(), x + PADDING, itemY + ITEM_HEIGHT / 2 - 4, toMCColor(nameColor), true);
            
            // Status indicator
            int indicatorX = x + MODULE_WIDTH - 25;
            int indicatorY = itemY + ITEM_HEIGHT / 2 - 4;
            RenderUtils.renderRoundedQuad(context.getMatrices(), isEnabled ? SUCCESS : new Color(60, 65, 80, 255), indicatorX, indicatorY, indicatorX + 8, indicatorY + 8, 4, 4, 4, 4, 30);
            
            if (isEnabled) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(0, 200, 100, 60), indicatorX - 3, indicatorY - 2, indicatorX + 11, indicatorY + 10, 7, 7, 7, 7, 20);
            }
            
            itemY += ITEM_HEIGHT + 2;
            visible++;
        }
        
        // Scrollbar
        if (maxScroll > 0) {
            int scrollBarHeight = (int)((PANEL_HEIGHT - HEADER_HEIGHT - 10) * (12.0f / filtered.size()));
            int scrollBarY = y + HEADER_HEIGHT + 5 + (int)((scrollOffset / (float)maxScroll) * (PANEL_HEIGHT - HEADER_HEIGHT - 10 - scrollBarHeight));
            RenderUtils.renderRoundedQuad(context.getMatrices(), ELECTRIC_BLUE, x + MODULE_WIDTH - 6, scrollBarY, x + MODULE_WIDTH - 3, scrollBarY + scrollBarHeight, 3, 3, 3, 3, 30);
        }
    }
    
    private void renderSettingsPanel(DrawContext context, int x, int y, int mouseX, int mouseY) {
        // Panel background
        RenderUtils.renderRoundedQuad(context.getMatrices(), CHARCOAL, x, y, x + SETTINGS_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        RenderUtils.renderRoundedOutline(context, BORDER, x, y, x + SETTINGS_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5f, 20);
        
        // Header
        RenderUtils.renderRoundedQuad(context.getMatrices(), SURFACE, x, y, x + SETTINGS_WIDTH, y + HEADER_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, 0, 0, 50);
        
        if (selectedModule != null) {
            context.drawText(textRenderer, "SETTINGS", x + PADDING, y + HEADER_HEIGHT / 2 - 4, toMCColor(TEXT_SECONDARY), true);
            context.drawText(textRenderer, selectedModule.getName().toString(), x + SETTINGS_WIDTH - PADDING - textRenderer.getWidth(selectedModule.getName().toString()), y + HEADER_HEIGHT / 2 - 4, toMCColor(ELECTRIC_BLUE), true);
            
            // Accent line
            context.fill(x + PADDING, y + HEADER_HEIGHT - 2, x + SETTINGS_WIDTH - PADDING, y + HEADER_HEIGHT - 1, toMCColor(ELECTRIC_BLUE));
            
            int itemY = y + HEADER_HEIGHT + 5;
            for (Object settingObj : selectedModule.getSettings()) {
                if (settingObj instanceof Setting) {
                    Setting setting = (Setting) settingObj;
                    boolean isHovered = mouseX >= x && mouseX <= x + SETTINGS_WIDTH && 
                                       mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
                    
                    if (isHovered) {
                        RenderUtils.renderRoundedQuad(context.getMatrices(), HOVER, x + 5, itemY, x + SETTINGS_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
                    }
                    
                    context.drawText(textRenderer, setting.getName().toString(), x + PADDING, itemY + ITEM_HEIGHT / 2 - 4, toMCColor(TEXT_PRIMARY), true);
                    renderSettingControl(context, setting, x, itemY);
                    
                    itemY += ITEM_HEIGHT + 2;
                }
            }
        } else {
            context.drawText(textRenderer, "SELECT A MODULE", x + SETTINGS_WIDTH / 2 - 60, y + PANEL_HEIGHT / 2 - 4, toMCColor(TEXT_MUTED), true);
        }
    }
    
    private void renderSettingControl(DrawContext context, Setting setting, int panelX, int y) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting) setting;
            int toggleX = panelX + SETTINGS_WIDTH - 45;
            int toggleY = y + ITEM_HEIGHT / 2 - 9;
            
            // Toggle background
            RenderUtils.renderRoundedQuad(context.getMatrices(), bool.getValue() ? ELECTRIC_BLUE : new Color(50, 55, 70, 255), 
                toggleX, toggleY, toggleX + 35, toggleY + 18, 9, 9, 9, 9, 30);
            
            // Handle
            int handleX = bool.getValue() ? toggleX + 19 : toggleX + 2;
            RenderUtils.renderRoundedQuad(context.getMatrices(), Color.WHITE, 
                handleX, toggleY + 2, handleX + 12, toggleY + 16, 7, 7, 7, 7, 30);
                
        } else if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting) setting;
            String value = String.format("%.2f", num.getValue());
            int valueWidth = textRenderer.getWidth(value);
            context.drawText(textRenderer, value, panelX + SETTINGS_WIDTH - PADDING - valueWidth, y + ITEM_HEIGHT / 2 - 4, toMCColor(ELECTRIC_BLUE), true);
            
            // Slider track
            int sliderX = panelX + PADDING;
            int sliderY = y + ITEM_HEIGHT - 8;
            int sliderWidth = SETTINGS_WIDTH - PADDING * 2 - valueWidth - 10;
            
            RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(45, 50, 65, 255), 
                sliderX, sliderY, sliderX + sliderWidth, sliderY + 3, 2, 2, 2, 2, 20);
            
            // Progress
            double progress = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
            int progressWidth = (int) (sliderWidth * progress);
            if (progressWidth > 0) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), ELECTRIC_BLUE, 
                    sliderX, sliderY, sliderX + progressWidth, sliderY + 3, 2, 2, 2, 2, 20);
            }
            
        } else if (setting instanceof ModeSetting) {
            ModeSetting<?> mode = (ModeSetting<?>) setting;
            String value = mode.getValue().toString();
            int valueWidth = textRenderer.getWidth(value);
            
            // Pill background
            int pillX = panelX + SETTINGS_WIDTH - valueWidth - PADDING - 10;
            int pillY = y + ITEM_HEIGHT / 2 - 10;
            RenderUtils.renderRoundedQuad(context.getMatrices(), SURFACE, pillX, pillY, pillX + valueWidth + 15, pillY + 20, 10, 10, 10, 10, 30);
            context.drawText(textRenderer, value, pillX + 8, pillY + 6, toMCColor(ELECTRIC_BLUE), true);
            
        } else if (setting instanceof BindSetting) {
            BindSetting bind = (BindSetting) setting;
            String value = bind.getValue() == -1 ? "NONE" : KeyUtils.getKey(bind.getValue()).toString();
            int valueWidth = textRenderer.getWidth(value);
            
            int pillX = panelX + SETTINGS_WIDTH - valueWidth - PADDING - 10;
            int pillY = y + ITEM_HEIGHT / 2 - 10;
            RenderUtils.renderRoundedQuad(context.getMatrices(), SURFACE, pillX, pillY, pillX + valueWidth + 15, pillY + 20, 10, 10, 10, 10, 30);
            context.drawText(textRenderer, value, pillX + 8, pillY + 6, toMCColor(VIOLET), true);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int panelX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + SPACING * 2)) / 2;
        int panelY = (height - PANEL_HEIGHT) / 2;
        
        // Category clicks
        int categoryY = panelY + HEADER_HEIGHT + 5;
        for (Category category : Category.values()) {
            if (mouseX >= panelX && mouseX <= panelX + CATEGORY_WIDTH &&
                mouseY >= categoryY && mouseY <= categoryY + ITEM_HEIGHT) {
                selectedCategory = category;
                selectedModule = null;
                scrollOffset = 0;
                return true;
            }
            categoryY += ITEM_HEIGHT + 2;
        }
        
        // Module clicks
        int moduleX = panelX + CATEGORY_WIDTH + SPACING;
        int moduleY = panelY + HEADER_HEIGHT + 5;
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(selectedCategory);
        List<Module> filtered = new ArrayList<>();
        String searchText = searchBox.getText().toLowerCase();
        
        for (Module module : modules) {
            if (searchText.isEmpty() || module.getName().toString().toLowerCase().contains(searchText)) {
                filtered.add(module);
            }
        }
        
        int visible = 0;
        for (int i = scrollOffset; i < filtered.size() && visible < 12; i++) {
            Module module = filtered.get(i);
            if (mouseX >= moduleX && mouseX <= moduleX + MODULE_WIDTH &&
                mouseY >= moduleY && mouseY <= moduleY + ITEM_HEIGHT) {
                if (button == 0) {
                    module.toggle();
                } else if (button == 1) {
                    selectedModule = module;
                }
                return true;
            }
            moduleY += ITEM_HEIGHT + 2;
            visible++;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int moduleX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + SPACING * 2)) / 2 + CATEGORY_WIDTH + SPACING;
        int moduleY = (height - PANEL_HEIGHT) / 2 + HEADER_HEIGHT + 5;
        
        if (mouseX >= moduleX && mouseX <= moduleX + MODULE_WIDTH &&
            mouseY >= moduleY && mouseY <= moduleY + PANEL_HEIGHT - HEADER_HEIGHT) {
            scrollOffset -= (int) vertical;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == 256) { // Escape
                searchBox.setFocused(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        if (keyCode == 256) { // Escape
            close();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    @Override
    public void close() {
        Krypton.INSTANCE.getModuleManager().getModuleByClass(skid.krypton.module.modules.client.Krypton.class).setEnabled(false);
        Krypton.mc.setScreenAndRender(Krypton.INSTANCE.screen);
    }
}
