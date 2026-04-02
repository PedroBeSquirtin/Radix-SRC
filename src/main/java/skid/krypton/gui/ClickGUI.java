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
    // ===== RADIX-INSPIRED COLORS =====
    private static final Color DARK_NAVY = new Color(18, 22, 32, 255);
    private static final Color CHARCOAL = new Color(28, 32, 42, 255);
    private static final Color SURFACE = new Color(38, 42, 55, 255);
    private static final Color ELECTRIC_BLUE = new Color(0, 150, 255, 255);
    private static final Color VIOLET = new Color(138, 43, 226, 255);
    private static final Color TEXT_PRIMARY = new Color(245, 245, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(170, 180, 200, 255);
    private static final Color TEXT_MUTED = new Color(100, 110, 130, 255);
    private static final Color HOVER = new Color(55, 65, 85, 255);
    private static final Color BORDER = new Color(50, 55, 70, 255);
    private static final Color SUCCESS = new Color(0, 200, 100, 255);
    private static final Color TOGGLE_ON = new Color(0, 160, 255, 255);
    private static final Color TOGGLE_OFF = new Color(70, 75, 90, 255);
    private static final Color GLOW = new Color(0, 150, 255, 40);

    // ===== LAYOUT =====
    private static final int CATEGORY_WIDTH = 150;
    private static final int MODULE_WIDTH = 200;
    private static final int SETTINGS_WIDTH = 280;
    private static final int PANEL_HEIGHT = 460;
    private static final int HEADER_HEIGHT = 50;
    private static final int ITEM_HEIGHT = 34;
    private static final int PADDING = 15;
    private static final int CORNER_RADIUS = 12;

    // ===== STATE =====
    private Category selectedCategory;
    private Module selectedModule;
    private TextFieldWidget searchBox;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private String tooltipText = null;
    private int tooltipX, tooltipY;
    public Color currentColor;
    
    // Slider dragging state
    private boolean draggingSlider = false;
    private Setting draggedSetting = null;

    public ClickGUI() {
        super(Text.empty());
        this.selectedCategory = Category.COMBAT;
    }

    // ===== COMPATIBILITY METHODS =====
    public boolean isDraggingAlready() { return draggingSlider; }
    
    public void setTooltip(CharSequence text, int x, int y) {
        this.tooltipText = text.toString();
        this.tooltipX = x;
        this.tooltipY = y;
    }
    
    public void onGuiClose() {
        Krypton.mc.setScreenAndRender(Krypton.INSTANCE.screen);
        this.currentColor = null;
        draggingSlider = false;
        draggedSetting = null;
    }

    private static int toMCColor(Color c) {
        return net.minecraft.util.math.ColorHelper.Argb.getArgb(c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    protected void init() {
        super.init();
        
        // Search box
        int panelX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + 20)) / 2;
        int searchX = panelX + CATEGORY_WIDTH + 10 + MODULE_WIDTH - 140;
        int searchY = (height - PANEL_HEIGHT) / 2 + 12;
        
        searchBox = new TextFieldWidget(textRenderer, searchX, searchY, 130, 22, Text.literal("Search"));
        searchBox.setMaxLength(50);
        searchBox.setDrawsBackground(true);
        addDrawableChild(searchBox);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (Krypton.mc.currentScreen != this) return;
        
        // Background
        int bgAlpha = skid.krypton.module.modules.client.Krypton.renderBackground.getValue() ? 180 : 0;
        context.fill(0, 0, width, height, new Color(0, 0, 0, bgAlpha).getRGB());
        
        // Calculate panel positions
        int startX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + 20)) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;
        
        // Draw all three panels
        renderCategoryPanel(context, startX, startY, mouseX, mouseY);
        renderModulePanel(context, startX + CATEGORY_WIDTH + 10, startY, mouseX, mouseY);
        renderSettingsPanel(context, startX + CATEGORY_WIDTH + MODULE_WIDTH + 20, startY, mouseX, mouseY);
        
        // Draw tooltip if active
        if (tooltipText != null && !tooltipText.isEmpty()) {
            renderTooltip(context, tooltipText, tooltipX, tooltipY);
            tooltipText = null;
        }
    }
    
    private void renderCategoryPanel(DrawContext context, int x, int y, int mouseX, int mouseY) {
        // Panel background
        RenderUtils.renderRoundedQuad(context.getMatrices(), CHARCOAL, x, y, x + CATEGORY_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        RenderUtils.renderRoundedOutline(context, BORDER, x, y, x + CATEGORY_WIDTH, y + PANEL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5f, 20);
        
        // Header
        RenderUtils.renderRoundedQuad(context.getMatrices(), SURFACE, x, y, x + CATEGORY_WIDTH, y + HEADER_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, 0, 0, 50);
        context.drawText(textRenderer, "CATEGORIES", x + PADDING, y + HEADER_HEIGHT / 2 - 4, toMCColor(ELECTRIC_BLUE), true);
        context.fill(x + PADDING, y + HEADER_HEIGHT - 2, x + CATEGORY_WIDTH - PADDING, y + HEADER_HEIGHT - 1, toMCColor(ELECTRIC_BLUE));
        
        // Category list
        int itemY = y + HEADER_HEIGHT + 5;
        for (Category category : Category.values()) {
            boolean isSelected = category == selectedCategory;
            boolean isHovered = isHovered(mouseX, mouseY, x, itemY, CATEGORY_WIDTH, ITEM_HEIGHT);
            
            if (isSelected) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(0, 150, 255, 30), x + 5, itemY, x + CATEGORY_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
                context.fill(x + 3, itemY + 5, x + 5, itemY + ITEM_HEIGHT - 5, toMCColor(ELECTRIC_BLUE));
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), HOVER, x + 5, itemY, x + CATEGORY_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
            }
            
            context.drawText(textRenderer, category.name.toString(), x + PADDING + 8, itemY + ITEM_HEIGHT / 2 - 4, toMCColor(isSelected ? ELECTRIC_BLUE : TEXT_SECONDARY), true);
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
        
        // Module list
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(selectedCategory);
        List<Module> filtered = new ArrayList<>();
        String search = searchBox.getText().toLowerCase();
        
        for (Module module : modules) {
            if (search.isEmpty() || module.getName().toString().toLowerCase().contains(search)) {
                filtered.add(module);
            }
        }
        
        maxScroll = Math.max(0, filtered.size() - 11);
        int itemY = y + HEADER_HEIGHT + 5;
        int visible = 0;
        
        for (int i = scrollOffset; i < filtered.size() && visible < 11; i++) {
            Module module = filtered.get(i);
            boolean isSelected = module == selectedModule;
            boolean isHovered = isHovered(mouseX, mouseY, x, itemY, MODULE_WIDTH, ITEM_HEIGHT);
            boolean isEnabled = module.isEnabled();
            
            if (isSelected) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(0, 150, 255, 25), x + 5, itemY, x + MODULE_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), HOVER, x + 5, itemY, x + MODULE_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
            }
            
            // Module name
            context.drawText(textRenderer, module.getName().toString(), x + PADDING, itemY + ITEM_HEIGHT / 2 - 4, toMCColor(isEnabled ? ELECTRIC_BLUE : TEXT_PRIMARY), true);
            
            // Status indicator
            int indicatorX = x + MODULE_WIDTH - 22;
            int indicatorY = itemY + ITEM_HEIGHT / 2 - 4;
            RenderUtils.renderRoundedQuad(context.getMatrices(), isEnabled ? SUCCESS : TOGGLE_OFF, indicatorX, indicatorY, indicatorX + 8, indicatorY + 8, 4, 4, 4, 4, 30);
            
            if (isEnabled) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(0, 200, 100, 60), indicatorX - 3, indicatorY - 2, indicatorX + 11, indicatorY + 10, 7, 7, 7, 7, 20);
            }
            
            itemY += ITEM_HEIGHT + 2;
            visible++;
        }
        
        // Scrollbar
        if (maxScroll > 0) {
            int scrollBarHeight = (int)((PANEL_HEIGHT - HEADER_HEIGHT - 10) * (11.0f / filtered.size()));
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
            context.fill(x + PADDING, y + HEADER_HEIGHT - 2, x + SETTINGS_WIDTH - PADDING, y + HEADER_HEIGHT - 1, toMCColor(ELECTRIC_BLUE));
            
            int itemY = y + HEADER_HEIGHT + 5;
            for (Object settingObj : selectedModule.getSettings()) {
                if (settingObj instanceof Setting) {
                    Setting setting = (Setting) settingObj;
                    boolean isHovered = isHovered(mouseX, mouseY, x, itemY, SETTINGS_WIDTH, ITEM_HEIGHT);
                    
                    if (isHovered && !draggingSlider) {
                        RenderUtils.renderRoundedQuad(context.getMatrices(), HOVER, x + 5, itemY, x + SETTINGS_WIDTH - 5, itemY + ITEM_HEIGHT, 6, 6, 6, 6, 30);
                    }
                    
                    context.drawText(textRenderer, setting.getName().toString(), x + PADDING, itemY + ITEM_HEIGHT / 2 - 4, toMCColor(TEXT_PRIMARY), true);
                    renderSettingControl(context, setting, x, itemY, mouseX, mouseY);
                    
                    itemY += ITEM_HEIGHT + 2;
                }
            }
        } else {
            context.drawText(textRenderer, "SELECT A MODULE", x + SETTINGS_WIDTH / 2 - 60, y + PANEL_HEIGHT / 2 - 4, toMCColor(TEXT_MUTED), true);
        }
    }
    
    private void renderSettingControl(DrawContext context, Setting setting, int panelX, int y, int mouseX, int mouseY) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting) setting;
            int toggleX = panelX + SETTINGS_WIDTH - 45;
            int toggleY = y + ITEM_HEIGHT / 2 - 10;
            
            // Toggle background
            RenderUtils.renderRoundedQuad(context.getMatrices(), bool.getValue() ? TOGGLE_ON : TOGGLE_OFF, toggleX, toggleY, toggleX + 35, toggleY + 20, 10, 10, 10, 10, 30);
            
            // Handle
            int handleX = bool.getValue() ? toggleX + 19 : toggleX + 2;
            RenderUtils.renderRoundedQuad(context.getMatrices(), Color.WHITE, handleX, toggleY + 3, handleX + 12, toggleY + 17, 8, 8, 8, 8, 30);
            
        } else if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting) setting;
            String value = String.format("%.2f", num.getValue());
            int valueWidth = textRenderer.getWidth(value);
            context.drawText(textRenderer, value, panelX + SETTINGS_WIDTH - PADDING - valueWidth, y + ITEM_HEIGHT / 2 - 4, toMCColor(ELECTRIC_BLUE), true);
            
            // Slider
            int sliderX = panelX + PADDING + 60;
            int sliderY = y + ITEM_HEIGHT - 10;
            int sliderWidth = SETTINGS_WIDTH - PADDING - 60 - valueWidth - 15;
            
            RenderUtils.renderRoundedQuad(context.getMatrices(), TOGGLE_OFF, sliderX, sliderY, sliderX + sliderWidth, sliderY + 3, 2, 2, 2, 2, 20);
            
            double progress = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
            int progressWidth = (int) (sliderWidth * progress);
            if (progressWidth > 0) {
                RenderUtils.renderRoundedQuad(context.getMatrices(), ELECTRIC_BLUE, sliderX, sliderY, sliderX + progressWidth, sliderY + 3, 2, 2, 2, 2, 20);
            }
            
            // Handle
            int handleX = sliderX + progressWidth - 4;
            RenderUtils.renderRoundedQuad(context.getMatrices(), Color.WHITE, handleX, sliderY - 4, handleX + 8, sliderY + 7, 6, 6, 6, 6, 30);
            
            // Handle slider dragging
            if (draggingSlider && draggedSetting == setting) {
                updateSliderValue(num, mouseX, sliderX, sliderX + sliderWidth);
            }
            
        } else if (setting instanceof ModeSetting) {
            ModeSetting<?> mode = (ModeSetting<?>) setting;
            String value = mode.getValue().toString();
            int valueWidth = textRenderer.getWidth(value);
            
            // Arrow buttons
            context.drawText(textRenderer, "<", panelX + SETTINGS_WIDTH - valueWidth - 30, y + ITEM_HEIGHT / 2 - 4, toMCColor(ELECTRIC_BLUE), true);
            context.drawText(textRenderer, value, panelX + SETTINGS_WIDTH - valueWidth - 15, y + ITEM_HEIGHT / 2 - 4, toMCColor(TEXT_PRIMARY), true);
            context.drawText(textRenderer, ">", panelX + SETTINGS_WIDTH - 15, y + ITEM_HEIGHT / 2 - 4, toMCColor(ELECTRIC_BLUE), true);
            
        } else if (setting instanceof BindSetting) {
            BindSetting bind = (BindSetting) setting;
            String value = bind.getValue() == -1 ? "NONE" : KeyUtils.getKey(bind.getValue()).toString();
            int valueWidth = textRenderer.getWidth(value);
            
            RenderUtils.renderRoundedQuad(context.getMatrices(), SURFACE, panelX + SETTINGS_WIDTH - valueWidth - 20, y + ITEM_HEIGHT / 2 - 10, panelX + SETTINGS_WIDTH - 10, y + ITEM_HEIGHT / 2 + 8, 6, 6, 6, 6, 30);
            context.drawText(textRenderer, value, panelX + SETTINGS_WIDTH - valueWidth - 15, y + ITEM_HEIGHT / 2 - 4, toMCColor(VIOLET), true);
        }
    }
    
    private void updateSliderValue(NumberSetting setting, int mouseX, int sliderStart, int sliderEnd) {
        double progress = Math.max(0, Math.min(1, (double)(mouseX - sliderStart) / (sliderEnd - sliderStart)));
        double newValue = setting.getMin() + progress * (setting.getMax() - setting.getMin());
        setting.getValue(MathUtil.roundToNearest(newValue, setting.getFormat()));
    }
    
    private void renderTooltip(DrawContext context, String text, int x, int y) {
        int width = textRenderer.getWidth(text);
        int bgX = x + 10;
        int bgY = y + 15;
        
        if (bgX + width + 10 > this.width) {
            bgX = this.width - width - 15;
        }
        
        RenderUtils.renderRoundedQuad(context.getMatrices(), new Color(25, 28, 38, 220), bgX - 5, bgY - 3, bgX + width + 5, bgY + 15, 6, 6, 6, 6, 50);
        context.drawText(textRenderer, text, bgX, bgY, 0xFFFFFF, true);
    }
    
    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + 20)) / 2;
        int startY = (height - PANEL_HEIGHT) / 2;
        
        // Category clicks
        int categoryY = startY + HEADER_HEIGHT + 5;
        for (Category category : Category.values()) {
            if (isHovered((int)mouseX, (int)mouseY, startX, categoryY, CATEGORY_WIDTH, ITEM_HEIGHT)) {
                selectedCategory = category;
                selectedModule = null;
                scrollOffset = 0;
                return true;
            }
            categoryY += ITEM_HEIGHT + 2;
        }
        
        // Module clicks
        int moduleX = startX + CATEGORY_WIDTH + 10;
        int moduleY = startY + HEADER_HEIGHT + 5;
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(selectedCategory);
        List<Module> filtered = new ArrayList<>();
        String search = searchBox.getText().toLowerCase();
        
        for (Module module : modules) {
            if (search.isEmpty() || module.getName().toString().toLowerCase().contains(search)) {
                filtered.add(module);
            }
        }
        
        int visible = 0;
        for (int i = scrollOffset; i < filtered.size() && visible < 11; i++) {
            Module module = filtered.get(i);
            if (isHovered((int)mouseX, (int)mouseY, moduleX, moduleY, MODULE_WIDTH, ITEM_HEIGHT)) {
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
        
        // Settings panel clicks (for sliders and toggles)
        if (selectedModule != null) {
            int settingsX = startX + CATEGORY_WIDTH + MODULE_WIDTH + 20;
            int settingsY = startY + HEADER_HEIGHT + 5;
            int index = 0;
            
            for (Object settingObj : selectedModule.getSettings()) {
                if (settingObj instanceof Setting) {
                    int itemY = settingsY + index * (ITEM_HEIGHT + 2);
                    
                    if (isHovered((int)mouseX, (int)mouseY, settingsX, itemY, SETTINGS_WIDTH, ITEM_HEIGHT)) {
                        if (settingObj instanceof BooleanSetting) {
                            ((BooleanSetting) settingObj).toggle();
                            return true;
                        } else if (settingObj instanceof ModeSetting) {
                            ModeSetting<?> mode = (ModeSetting<?>) settingObj;
                            int valueWidth = textRenderer.getWidth(mode.getValue().toString());
                            if (mouseX > settingsX + SETTINGS_WIDTH - 15) {
                                mode.cycleUp();
                            } else if (mouseX < settingsX + SETTINGS_WIDTH - valueWidth - 30) {
                                mode.cycleDown();
                            }
                            return true;
                        } else if (settingObj instanceof NumberSetting) {
                            draggingSlider = true;
                            draggedSetting = (Setting) settingObj;
                            return true;
                        } else if (settingObj instanceof BindSetting) {
                            BindSetting bind = (BindSetting) settingObj;
                            if (bind.isListening()) {
                                bind.setListening(false);
                            } else {
                                bind.setListening(true);
                            }
                            return true;
                        }
                    }
                    index++;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSlider && draggedSetting instanceof NumberSetting) {
            int startX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + 20)) / 2;
            int settingsX = startX + CATEGORY_WIDTH + MODULE_WIDTH + 20;
            
            // Find the slider position
            int index = 0;
            for (Object settingObj : selectedModule.getSettings()) {
                if (settingObj == draggedSetting) {
                    int y = (height - PANEL_HEIGHT) / 2 + HEADER_HEIGHT + 5 + index * (ITEM_HEIGHT + 2);
                    String value = String.format("%.2f", ((NumberSetting) draggedSetting).getValue());
                    int valueWidth = textRenderer.getWidth(value);
                    int sliderX = settingsX + PADDING + 60;
                    int sliderWidth = SETTINGS_WIDTH - PADDING - 60 - valueWidth - 15;
                    
                    updateSliderValue((NumberSetting) draggedSetting, (int)mouseX, sliderX, sliderX + sliderWidth);
                    return true;
                }
                index++;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = false;
        draggedSetting = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int moduleX = (width - (CATEGORY_WIDTH + MODULE_WIDTH + SETTINGS_WIDTH + 20)) / 2 + CATEGORY_WIDTH + 10;
        int moduleY = (height - PANEL_HEIGHT) / 2 + HEADER_HEIGHT + 5;
        
        if (mouseX >= moduleX && mouseX <= moduleX + MODULE_WIDTH && mouseY >= moduleY && mouseY <= moduleY + PANEL_HEIGHT - HEADER_HEIGHT) {
            scrollOffset -= (int) vertical;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == 256) {
                searchBox.setFocused(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        if (keyCode == 256) {
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
        onGuiClose();
    }
}
