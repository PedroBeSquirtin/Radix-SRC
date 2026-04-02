package skid.krypton.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
    private Category selectedCategory;
    private Module selectedModule;
    private String searchQuery;
    public Color currentColor;
    private boolean searchFocused;
    private boolean draggingSlider;
    private Setting draggingSliderSetting;
    private float animationProgress;
    private long lastFrame;

    // Radix-inspired color scheme - Dark Navy / Charcoal with Electric Blue & Violet accents
    private static final Color DARK_NAVY = new Color(15, 20, 30, 255);
    private static final Color CHARCOAL = new Color(25, 28, 35, 255);
    private static final Color SURFACE = new Color(30, 35, 45, 255);
    private static final Color ELEVATED = new Color(38, 42, 55, 255);
    private static final Color ELECTRIC_BLUE = new Color(0, 150, 255, 255);
    private static final Color VIOLET = new Color(138, 43, 226, 255);
    private static final Color GLOW_BLUE = new Color(0, 150, 255, 40);
    private static final Color TEXT_PRIMARY = new Color(245, 245, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(160, 170, 190, 255);
    private static final Color TEXT_MUTED = new Color(100, 110, 130, 255);
    private static final Color BORDER = new Color(45, 52, 68, 255);
    private static final Color HOVER = new Color(45, 55, 75, 255);
    private static final Color SUCCESS = new Color(0, 200, 100, 255);
    
    // Layout constants
    private static final int SETTINGS_PANEL_WIDTH = 340;
    private static final int CATEGORY_PANEL_WIDTH = 160;
    private static final int MODULE_PANEL_WIDTH = 280;
    private static final int HEADER_HEIGHT = 60;
    private static final int ITEM_HEIGHT = 38;
    private static final int PADDING = 18;
    private static final int PANEL_SPACING = 3;
    private static final int CORNER_RADIUS = 12;
    private static final int TOTAL_WIDTH = SETTINGS_PANEL_WIDTH + CATEGORY_PANEL_WIDTH + MODULE_PANEL_WIDTH + (PANEL_SPACING * 2);
    private static final int TOTAL_HEIGHT = 540;

    public ClickGUI() {
        super(Text.empty());
        this.selectedCategory = Category.COMBAT;
        this.searchQuery = "";
        this.searchFocused = false;
        this.draggingSlider = false;
        this.draggingSliderSetting = null;
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

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (Krypton.mc.currentScreen == this) {
            updateAnimation();
            
            if (Krypton.INSTANCE.screen != null) {
                Krypton.INSTANCE.screen.render(drawContext, 0, 0, delta);
            }
            
            // Background with fade
            int bgAlpha = (int) (180 * animationProgress);
            drawContext.fill(0, 0, Krypton.mc.getWindow().getWidth(), Krypton.mc.getWindow().getHeight(), 
                new Color(0, 0, 0, bgAlpha).getRGB());
            
            RenderUtils.unscaledProjection();
            int scaledMouseX = mouseX * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
            int scaledMouseY = mouseY * (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
            
            // Scale animation
            float scale = 0.92f + (animationProgress * 0.08f);
            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(Krypton.mc.getWindow().getWidth() / 2f, Krypton.mc.getWindow().getHeight() / 2f, 0);
            drawContext.getMatrices().scale(scale, scale, 1);
            drawContext.getMatrices().translate(-Krypton.mc.getWindow().getWidth() / 2f, -Krypton.mc.getWindow().getHeight() / 2f, 0);
            
            super.render(drawContext, scaledMouseX, scaledMouseY, delta);
            
            // Draw panels
            this.renderModernBackground(drawContext);
            this.renderSettingsPanel(drawContext, scaledMouseX, scaledMouseY);
            this.renderCategoryPanel(drawContext, scaledMouseX, scaledMouseY);
            this.renderModulePanel(drawContext, scaledMouseX, scaledMouseY);
            
            drawContext.getMatrices().pop();
            RenderUtils.scaledProjection();
        }
    }
    
    private void renderModernBackground(DrawContext drawContext) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2 - 10;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2 - 10;
        int endX = startX + TOTAL_WIDTH + 20;
        int endY = startY + TOTAL_HEIGHT + 20;
        
        // Outer glow
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), GLOW_BLUE, startX, startY, endX, endY, CORNER_RADIUS + 4, CORNER_RADIUS + 4, CORNER_RADIUS + 4, CORNER_RADIUS + 4, 50);
        
        // Main background
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), DARK_NAVY, 
            (screenWidth - TOTAL_WIDTH) / 2, (screenHeight - TOTAL_HEIGHT) / 2,
            (screenWidth + TOTAL_WIDTH) / 2, (screenHeight + TOTAL_HEIGHT) / 2,
            CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
            
        // Subtle border
        RenderUtils.renderRoundedOutline(drawContext, BORDER,
            (screenWidth - TOTAL_WIDTH) / 2, (screenHeight - TOTAL_HEIGHT) / 2,
            (screenWidth + TOTAL_WIDTH) / 2, (screenHeight + TOTAL_HEIGHT) / 2,
            CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5, 1.5);
    }

    private void renderSettingsPanel(DrawContext drawContext, int mouseX, int mouseY) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        int endX = startX + SETTINGS_PANEL_WIDTH;
        int endY = startY + TOTAL_HEIGHT;
        
        // Panel
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), CHARCOAL, startX, startY, endX, endY, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        RenderUtils.renderRoundedOutline(drawContext, BORDER, startX, startY, endX, endY, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5, 1.5);
        
        // Header
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(20, 23, 33, 255), startX, startY, endX, startY + HEADER_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, 0, 0, 50);
        
        if (this.selectedModule != null) {
            String headerText = this.selectedModule.getName().toString().toUpperCase();
            TextRenderer.drawString(headerText, drawContext, startX + PADDING, startY + 24, toMCColor(ELECTRIC_BLUE));
            
            // Accent line
            drawContext.fill(startX + PADDING, startY + HEADER_HEIGHT - 2, endX - PADDING, startY + HEADER_HEIGHT - 1, toMCColor(ELECTRIC_BLUE));
            
            int yOffset = startY + HEADER_HEIGHT + PADDING;
            
            for (Object setting : this.selectedModule.getSettings()) {
                if (setting instanceof Setting) {
                    Setting s = (Setting) setting;
                    boolean isHovered = isHoveredInRect(mouseX, mouseY, startX, yOffset, SETTINGS_PANEL_WIDTH, ITEM_HEIGHT);
                    
                    if (isHovered && !this.draggingSlider) {
                        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), HOVER, startX + 8, yOffset, endX - 8, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
                    }
                    
                    TextRenderer.drawString(s.getName().toString(), drawContext, startX + PADDING, yOffset + 12, toMCColor(TEXT_PRIMARY));
                    this.renderModernSetting(drawContext, setting, startX, endX, yOffset, mouseX, mouseY);
                    
                    yOffset += ITEM_HEIGHT + 6;
                }
            }
        } else {
            TextRenderer.drawCenteredString("Select a module", drawContext, startX + SETTINGS_PANEL_WIDTH / 2, startY + 150, toMCColor(TEXT_MUTED));
        }
    }
    
    private void renderModernSetting(DrawContext drawContext, Object setting, int startX, int endX, int yOffset, int mouseX, int mouseY) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            boolean value = boolSetting.getValue();
            
            int toggleX = endX - 52;
            int toggleY = yOffset + 10;
            int toggleWidth = 40;
            int toggleHeight = 18;
            
            // Background
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), value ? ELECTRIC_BLUE : new Color(50, 55, 70, 255), 
                toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, 9, 9, 9, 9, 30);
            
            // Handle
            int handleX = value ? toggleX + toggleWidth - 16 : toggleX + 2;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), Color.WHITE, 
                handleX, toggleY + 2, handleX + 12, toggleY + toggleHeight - 2, 8, 8, 8, 8, 30);
                
        } else if (setting instanceof NumberSetting) {
            NumberSetting numSetting = (NumberSetting) setting;
            String valueText = String.format("%.2f", numSetting.getValue());
            
            TextRenderer.drawString(valueText, drawContext, endX - PADDING - TextRenderer.getWidth(valueText), yOffset + 12, toMCColor(ELECTRIC_BLUE));
            
            int sliderY = yOffset + 28;
            int sliderStartX = startX + PADDING;
            int sliderEndX = endX - PADDING;
            int sliderWidth = sliderEndX - sliderStartX;
            
            // Track
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(45, 50, 65, 255), sliderStartX, sliderY, sliderEndX, sliderY + 3, 2, 2, 2, 2, 20);
            
            // Progress
            double progress = (numSetting.getValue() - numSetting.getMin()) / (numSetting.getMax() - numSetting.getMin());
            int progressWidth = (int) (sliderWidth * progress);
            if (progressWidth > 0) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), ELECTRIC_BLUE, sliderStartX, sliderY, sliderStartX + progressWidth, sliderY + 3, 2, 2, 2, 2, 20);
            }
            
            // Handle
            int handleX = sliderStartX + progressWidth - 4;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), Color.WHITE, handleX, sliderY - 3, handleX + 7, sliderY + 6, 4, 4, 4, 4, 30);
            
        } else if (setting instanceof ModeSetting) {
            ModeSetting<?> modeSetting = (ModeSetting<?>) setting;
            String valueText = modeSetting.getValue().toString();
            
            int pillWidth = Math.max(80, TextRenderer.getWidth(valueText) + 24);
            int pillX = endX - pillWidth - PADDING;
            int pillY = yOffset + 8;
            
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(45, 50, 70, 255), pillX, pillY, pillX + pillWidth, pillY + 22, 11, 11, 11, 11, 30);
            TextRenderer.drawString(valueText, drawContext, pillX + 12, pillY + 7, toMCColor(TEXT_PRIMARY));
            
        } else if (setting instanceof BindSetting) {
            BindSetting bindSetting = (BindSetting) setting;
            String valueText = bindSetting.getValue() == -1 ? "None" : KeyUtils.getKey(bindSetting.getValue()).toString();
            
            int pillWidth = Math.max(70, TextRenderer.getWidth(valueText) + 24);
            int pillX = endX - pillWidth - PADDING;
            int pillY = yOffset + 8;
            
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(45, 50, 70, 255), pillX, pillY, pillX + pillWidth, pillY + 22, 11, 11, 11, 11, 30);
            TextRenderer.drawString(valueText, drawContext, pillX + 12, pillY + 7, toMCColor(VIOLET));
        }
    }

    private void renderCategoryPanel(DrawContext drawContext, int mouseX, int mouseY) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        int endX = startX + CATEGORY_PANEL_WIDTH;
        int endY = startY + TOTAL_HEIGHT;
        
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), CHARCOAL, startX, startY, endX, endY, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        RenderUtils.renderRoundedOutline(drawContext, BORDER, startX, startY, endX, endY, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5, 1.5);
        
        // Logo
        TextRenderer.drawString("KRYPTON+", drawContext, startX + PADDING, startY + 24, toMCColor(ELECTRIC_BLUE));
        drawContext.fill(startX + PADDING, startY + 44, endX - PADDING, startY + 45, toMCColor(ELECTRIC_BLUE));
        
        int yOffset = startY + HEADER_HEIGHT + PADDING;
        
        for (Category category : Category.values()) {
            boolean isSelected = category == this.selectedCategory;
            boolean isHovered = isHoveredInRect(mouseX, mouseY, startX, yOffset, CATEGORY_PANEL_WIDTH, ITEM_HEIGHT);
            
            if (isSelected) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(0, 150, 255, 30), startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
                drawContext.fill(startX + 5, yOffset, startX + 7, yOffset + ITEM_HEIGHT, toMCColor(ELECTRIC_BLUE));
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), HOVER, startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
            }
            
            Color textColor = isSelected ? ELECTRIC_BLUE : TEXT_SECONDARY;
            TextRenderer.drawString(category.name.toString(), drawContext, startX + PADDING + 8, yOffset + 12, toMCColor(textColor));
            
            yOffset += ITEM_HEIGHT + 4;
        }
    }

    private void renderModulePanel(DrawContext drawContext, int mouseX, int mouseY) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING + CATEGORY_PANEL_WIDTH + PANEL_SPACING;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        int endX = startX + MODULE_PANEL_WIDTH;
        int endY = startY + TOTAL_HEIGHT;
        
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), CHARCOAL, startX, startY, endX, endY, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        RenderUtils.renderRoundedOutline(drawContext, BORDER, startX, startY, endX, endY, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 1.5, 1.5);
        
        // Category title
        TextRenderer.drawString(this.selectedCategory.name.toString(), drawContext, startX + PADDING, startY + 24, toMCColor(TEXT_PRIMARY));
        
        // Search bar
        int searchY = startY + HEADER_HEIGHT - 18;
        int searchStartX = startX + PADDING;
        int searchEndX = endX - PADDING;
        
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), SURFACE, searchStartX, searchY, searchEndX, searchY + 28, 8, 8, 8, 8, 30);
        
        // Search icon
        TextRenderer.drawString("🔍", drawContext, searchStartX + 8, searchY + 8, toMCColor(TEXT_MUTED));
        
        String searchText = this.searchQuery.isEmpty() ? "Search modules..." : this.searchQuery;
        Color searchTextColor = this.searchQuery.isEmpty() ? TEXT_MUTED : TEXT_PRIMARY;
        TextRenderer.drawString(searchText, drawContext, searchStartX + 28, searchY + 8, toMCColor(searchTextColor));
        
        // Cursor
        if (this.searchFocused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = searchStartX + 28 + TextRenderer.getWidth(this.searchQuery);
            drawContext.fill(cursorX, searchY + 6, cursorX + 1, searchY + 22, toMCColor(ELECTRIC_BLUE));
        }
        
        // Module list
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(this.selectedCategory);
        int yOffset = startY + HEADER_HEIGHT + 30;
        
        for (Module module : modules) {
            if (!this.searchQuery.isEmpty() && !module.getName().toString().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                continue;
            }
            
            boolean isSelected = module == this.selectedModule;
            boolean isHovered = isHoveredInRect(mouseX, mouseY, startX, yOffset, MODULE_PANEL_WIDTH, ITEM_HEIGHT);
            boolean isEnabled = module.isEnabled();
            
            if (isSelected) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(0, 150, 255, 20), startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), HOVER, startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
            }
            
            Color nameColor = isEnabled ? ELECTRIC_BLUE : TEXT_SECONDARY;
            TextRenderer.drawString(module.getName().toString(), drawContext, startX + PADDING, yOffset + 12, toMCColor(nameColor));
            
            // Status indicator
            int indicatorX = endX - 24;
            int indicatorY = yOffset + 12;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), isEnabled ? SUCCESS : new Color(60, 65, 80, 255), 
                indicatorX, indicatorY, indicatorX + 8, indicatorY + 8, 4, 4, 4, 4, 30);
            
            if (isEnabled) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(0, 200, 100, 80), 
                    indicatorX - 4, indicatorY - 2, indicatorX + 12, indicatorY + 10, 8, 8, 8, 8, 20);
            }
            
            yOffset += ITEM_HEIGHT + 4;
        }
    }
    
    private boolean isHoveredInRect(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    private boolean isSearchBarHovered(int mouseX, int mouseY) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING + CATEGORY_PANEL_WIDTH + PANEL_SPACING;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        int searchY = startY + HEADER_HEIGHT - 18;
        int searchStartX = startX + PADDING;
        int searchEndX = startX + MODULE_PANEL_WIDTH - PADDING;
        
        return isHoveredInRect(mouseX, mouseY, searchStartX, searchY, searchEndX - searchStartX, 28);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchFocused) {
            if (keyCode == 259 && !this.searchQuery.isEmpty()) {
                this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                return true;
            } else if (keyCode == 256) {
                this.searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.searchFocused && (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '_' || chr == '-')) {
            this.searchQuery += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scaledMouseX = mouseX * MinecraftClient.getInstance().getWindow().getScaleFactor();
        double scaledMouseY = mouseY * MinecraftClient.getInstance().getWindow().getScaleFactor();
        
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        
        // Search bar
        if (isSearchBarHovered((int) scaledMouseX, (int) scaledMouseY)) {
            this.searchFocused = true;
            return true;
        } else {
            this.searchFocused = false;
        }
        
        // Category panel
        int categoryStartX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING;
        int categoryStartY = (screenHeight - TOTAL_HEIGHT) / 2 + HEADER_HEIGHT + PADDING;
        int categoryY = categoryStartY;
        
        for (Category category : Category.values()) {
            if (isHoveredInRect((int) scaledMouseX, (int) scaledMouseY, categoryStartX, categoryY, CATEGORY_PANEL_WIDTH, ITEM_HEIGHT)) {
                this.selectedCategory = category;
                this.selectedModule = null;
                return true;
            }
            categoryY += ITEM_HEIGHT + 4;
        }
        
        // Module panel
        int moduleStartX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING + CATEGORY_PANEL_WIDTH + PANEL_SPACING;
        int moduleStartY = (screenHeight - TOTAL_HEIGHT) / 2 + HEADER_HEIGHT + 30;
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(this.selectedCategory);
        int moduleY = moduleStartY;
        
        for (Module module : modules) {
            if (!this.searchQuery.isEmpty() && !module.getName().toString().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                continue;
            }
            
            if (isHoveredInRect((int) scaledMouseX, (int) scaledMouseY, moduleStartX, moduleY, MODULE_PANEL_WIDTH, ITEM_HEIGHT)) {
                if (button == 0) {
                    module.toggle();
                } else if (button == 1) {
                    this.selectedModule = module;
                }
                return true;
            }
            moduleY += ITEM_HEIGHT + 4;
        }
        
        // Settings panel - Number slider dragging
        if (this.selectedModule != null) {
            int settingsStartX = (screenWidth - TOTAL_WIDTH) / 2;
            int settingsStartY = (screenHeight - TOTAL_HEIGHT) / 2 + HEADER_HEIGHT + PADDING;
            int settingY = settingsStartY;
            
            for (Object setting : this.selectedModule.getSettings()) {
                if (setting instanceof NumberSetting) {
                    NumberSetting numSetting = (NumberSetting) setting;
                    int sliderY = settingY + 28;
                    int sliderStartX = settingsStartX + PADDING;
                    int sliderEndX = settingsStartX + SETTINGS_PANEL_WIDTH - PADDING;
                    
                    if (isHoveredInRect((int) scaledMouseX, (int) scaledMouseY, sliderStartX, sliderY - 5, sliderEndX - sliderStartX, 14)) {
                        this.draggingSlider = true;
                        this.draggingSliderSetting = (Setting) setting;
                        this.updateSliderValue(numSetting, (int) scaledMouseX, sliderStartX, sliderEndX);
                        return true;
                    }
                } else if (setting instanceof BooleanSetting && isHoveredInRect((int) scaledMouseX, (int) scaledMouseY, settingsStartX, settingY, SETTINGS_PANEL_WIDTH, ITEM_HEIGHT)) {
                    ((BooleanSetting) setting).toggle();
                    return true;
                } else if (setting instanceof ModeSetting && isHoveredInRect((int) scaledMouseX, (int) scaledMouseY, settingsStartX, settingY, SETTINGS_PANEL_WIDTH, ITEM_HEIGHT)) {
                    ModeSetting<?> modeSetting = (ModeSetting<?>) setting;
                    if (button == 0) {
                        modeSetting.cycleUp();
                    } else if (button == 1) {
                        modeSetting.cycleDown();
                    }
                    return true;
                }
                settingY += ITEM_HEIGHT + 6;
            }
        }
        
        return super.mouseClicked(scaledMouseX, scaledMouseY, button);
    }
    
    private void updateSliderValue(NumberSetting setting, int mouseX, int sliderStartX, int sliderEndX) {
        double progress = Math.max(0.0, Math.min(1.0, (double) (mouseX - sliderStartX) / (sliderEndX - sliderStartX)));
        double newValue = setting.getMin() + progress * (setting.getMax() - setting.getMin());
        setting.getValue(MathUtil.roundToNearest(newValue, setting.getFormat()));
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingSlider && this.draggingSliderSetting instanceof NumberSetting) {
            double scaledMouseX = mouseX * MinecraftClient.getInstance().getWindow().getScaleFactor();
            int screenWidth = Krypton.mc.getWindow().getWidth();
            int settingsStartX = (screenWidth - TOTAL_WIDTH) / 2;
            int sliderStartX = settingsStartX + PADDING;
            int sliderEndX = settingsStartX + SETTINGS_PANEL_WIDTH - PADDING;
            
            this.updateSliderValue((NumberSetting) this.draggingSliderSetting, (int) scaledMouseX, sliderStartX, sliderEndX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingSlider) {
            this.draggingSlider = false;
            this.draggingSliderSetting = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean shouldPause() {
        return false;
    }

    public void close() {
        Krypton.INSTANCE.getModuleManager().getModuleByClass(skid.krypton.module.modules.client.Krypton.class).setEnabled(false);
        this.onGuiClose();
    }

    public void onGuiClose() {
        Krypton.mc.setScreenAndRender(Krypton.INSTANCE.screen);
        this.currentColor = null;
        this.searchFocused = false;
        this.draggingSlider = false;
        this.draggingSliderSetting = null;
    }
}
