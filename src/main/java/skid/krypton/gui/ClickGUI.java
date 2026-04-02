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
    public List<CategoryWindow> windows;
    public Color currentColor;
    private CharSequence tooltipText;
    private int tooltipX;
    private int tooltipY;
    private final Color DESCRIPTION_BG = new Color(40, 40, 40, 200);
    
    // Modern Radix-style variables
    private Category selectedCategory;
    private Module selectedModule;
    private String searchQuery;
    private boolean searchFocused;
    private boolean draggingSlider;
    private Setting draggingSliderSetting;
    private float animationProgress;
    private long lastFrame;

    // Radix-inspired color scheme
    private static final Color DARK_NAVY = new Color(15, 20, 30, 255);
    private static final Color CHARCOAL = new Color(25, 28, 35, 255);
    private static final Color ELECTRIC_BLUE = new Color(0, 150, 255, 255);
    private static final Color VIOLET = new Color(138, 43, 226, 255);
    private static final Color TEXT_PRIMARY = new Color(245, 245, 255, 255);
    private static final Color TEXT_SECONDARY = new Color(160, 170, 190, 255);
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
        this.windows = new ArrayList<>();
        this.selectedCategory = Category.COMBAT;
        this.searchQuery = "";
        this.searchFocused = false;
        this.draggingSlider = false;
        this.draggingSliderSetting = null;
        this.animationProgress = 0;
        this.lastFrame = System.currentTimeMillis();
        
        // Initialize category windows (for compatibility with old system)
        int n = 50;
        for (Category value : Category.values()) {
            this.windows.add(new CategoryWindow(n, 50, 230, 30, value, this));
            n += 250;
        }
    }

    // ===== Required methods for compatibility =====
    
    public boolean isDraggingAlready() {
        for (CategoryWindow window : this.windows) {
            if (window.dragging) {
                return true;
            }
        }
        return false;
    }

    public void setTooltip(CharSequence tooltipText, int tooltipX, int tooltipY) {
        this.tooltipText = tooltipText;
        this.tooltipX = tooltipX;
        this.tooltipY = tooltipY;
    }

    private void renderTooltip(DrawContext drawContext, CharSequence charSequence, int n, int n2) {
        if (charSequence == null || charSequence.length() == 0) {
            return;
        }
        int a = TextRenderer.getWidth(charSequence);
        int framebufferWidth = Krypton.mc.getWindow().getFramebufferWidth();
        if (n + a + 10 > framebufferWidth) {
            n = framebufferWidth - a - 10;
        }
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), this.DESCRIPTION_BG, 
            n - 5, n2 - 5, n + a + 5, n2 + 15, 6.0, 6.0, 6.0, 6.0, 50.0);
        TextRenderer.drawString(charSequence, drawContext, n, n2, Color.WHITE.getRGB());
    }

    // ===== Modern rendering methods =====
    
    private void updateAnimation() {
        long now = System.currentTimeMillis();
        float delta = Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float target = (Krypton.mc.currentScreen == this) ? 1.0f : 0.0f;
        animationProgress += (target - animationProgress) * delta * 8;
        animationProgress = Math.max(0, Math.min(1, animationProgress));
    }

    private static int toMCColor(Color c) {
        return net.minecraft.util.math.ColorHelper.Argb.getArgb(c.getAlpha(), c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (Krypton.mc.currentScreen == this) {
            updateAnimation();
            
            if (Krypton.INSTANCE.screen != null) {
                Krypton.INSTANCE.screen.render(drawContext, 0, 0, delta);
            }
            
            // Background handling (from your original code)
            if (this.currentColor == null) {
                this.currentColor = new Color(0, 0, 0, 0);
            }
            int targetAlpha = skid.krypton.module.modules.client.Krypton.renderBackground.getValue() ? 200 : 0;
            if (this.currentColor.getAlpha() != targetAlpha) {
                this.currentColor = ColorUtil.a(0.05f, targetAlpha, this.currentColor);
            }
            drawContext.fill(0, 0, Krypton.mc.getWindow().getWidth(), Krypton.mc.getWindow().getHeight(), this.currentColor.getRGB());
            
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
            
            // Render modern panels
            this.renderModernBackground(drawContext);
            this.renderSettingsPanel(drawContext, scaledMouseX, scaledMouseY);
            this.renderCategoryPanel(drawContext, scaledMouseX, scaledMouseY);
            this.renderModulePanel(drawContext, scaledMouseX, scaledMouseY);
            
            // Tooltip rendering
            if (this.tooltipText != null) {
                this.renderTooltip(drawContext, this.tooltipText, this.tooltipX, this.tooltipY);
                this.tooltipText = null;
            }
            
            drawContext.getMatrices().pop();
            RenderUtils.scaledProjection();
        }
    }
    
    private void renderModernBackground(DrawContext drawContext) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), DARK_NAVY, 
            startX, startY, startX + TOTAL_WIDTH, startY + TOTAL_HEIGHT,
            CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
    }

    private void renderSettingsPanel(DrawContext drawContext, int mouseX, int mouseY) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        int endX = startX + SETTINGS_PANEL_WIDTH;
        
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), CHARCOAL, startX, startY, endX, startY + TOTAL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        
        if (this.selectedModule != null) {
            TextRenderer.drawString(this.selectedModule.getName().toString().toUpperCase(), drawContext, startX + PADDING, startY + 24, toMCColor(ELECTRIC_BLUE));
            drawContext.fill(startX + PADDING, startY + HEADER_HEIGHT - 2, endX - PADDING, startY + HEADER_HEIGHT - 1, toMCColor(ELECTRIC_BLUE));
            
            int yOffset = startY + HEADER_HEIGHT + PADDING;
            for (Object setting : this.selectedModule.getSettings()) {
                if (setting instanceof Setting) {
                    Setting s = (Setting) setting;
                    boolean isHovered = mouseX >= startX && mouseX <= endX && mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT;
                    
                    if (isHovered && !this.draggingSlider) {
                        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), HOVER, startX + 8, yOffset, endX - 8, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
                    }
                    
                    TextRenderer.drawString(s.getName().toString(), drawContext, startX + PADDING, yOffset + 12, toMCColor(TEXT_PRIMARY));
                    this.renderSettingControl(drawContext, setting, startX, endX, yOffset);
                    yOffset += ITEM_HEIGHT + 6;
                }
            }
        } else {
            TextRenderer.drawCenteredString("Select a module", drawContext, startX + SETTINGS_PANEL_WIDTH / 2, startY + 150, toMCColor(TEXT_SECONDARY));
        }
    }
    
    private void renderSettingControl(DrawContext drawContext, Object setting, int startX, int endX, int yOffset) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting) setting;
            int toggleX = endX - 52;
            int toggleY = yOffset + 10;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), bool.getValue() ? ELECTRIC_BLUE : new Color(50, 55, 70, 255), toggleX, toggleY, toggleX + 40, toggleY + 18, 9, 9, 9, 9, 30);
            int handleX = bool.getValue() ? toggleX + 24 : toggleX + 2;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), Color.WHITE, handleX, toggleY + 2, handleX + 12, toggleY + 16, 8, 8, 8, 8, 30);
        } else if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting) setting;
            String valueText = String.format("%.2f", num.getValue());
            TextRenderer.drawString(valueText, drawContext, endX - PADDING - TextRenderer.getWidth(valueText), yOffset + 12, toMCColor(ELECTRIC_BLUE));
            
            int sliderY = yOffset + 28;
            int sliderStartX = startX + PADDING;
            int sliderEndX = endX - PADDING;
            double progress = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
            int progressWidth = (int) ((sliderEndX - sliderStartX) * progress);
            
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(45, 50, 65, 255), sliderStartX, sliderY, sliderEndX, sliderY + 3, 2, 2, 2, 2, 20);
            if (progressWidth > 0) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), ELECTRIC_BLUE, sliderStartX, sliderY, sliderStartX + progressWidth, sliderY + 3, 2, 2, 2, 2, 20);
            }
        }
    }

    private void renderCategoryPanel(DrawContext drawContext, int mouseX, int mouseY) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        int endX = startX + CATEGORY_PANEL_WIDTH;
        
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), CHARCOAL, startX, startY, endX, startY + TOTAL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        TextRenderer.drawString("KRYPTON+", drawContext, startX + PADDING, startY + 24, toMCColor(ELECTRIC_BLUE));
        drawContext.fill(startX + PADDING, startY + 44, endX - PADDING, startY + 45, toMCColor(ELECTRIC_BLUE));
        
        int yOffset = startY + HEADER_HEIGHT + PADDING;
        for (Category category : Category.values()) {
            boolean isSelected = category == this.selectedCategory;
            boolean isHovered = mouseX >= startX && mouseX <= endX && mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT;
            
            if (isSelected) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(0, 150, 255, 30), startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
                drawContext.fill(startX + 5, yOffset, startX + 7, yOffset + ITEM_HEIGHT, toMCColor(ELECTRIC_BLUE));
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), HOVER, startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
            }
            
            TextRenderer.drawString(category.name.toString(), drawContext, startX + PADDING + 8, yOffset + 12, toMCColor(isSelected ? ELECTRIC_BLUE : TEXT_SECONDARY));
            yOffset += ITEM_HEIGHT + 4;
        }
    }

    private void renderModulePanel(DrawContext drawContext, int mouseX, int mouseY) {
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        int startX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING + CATEGORY_PANEL_WIDTH + PANEL_SPACING;
        int startY = (screenHeight - TOTAL_HEIGHT) / 2;
        int endX = startX + MODULE_PANEL_WIDTH;
        
        RenderUtils.renderRoundedQuad(drawContext.getMatrices(), CHARCOAL, startX, startY, endX, startY + TOTAL_HEIGHT, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, 50);
        TextRenderer.drawString(this.selectedCategory.name.toString(), drawContext, startX + PADDING, startY + 24, toMCColor(TEXT_PRIMARY));
        
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(this.selectedCategory);
        int yOffset = startY + HEADER_HEIGHT + 30;
        
        for (Module module : modules) {
            if (!this.searchQuery.isEmpty() && !module.getName().toString().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                continue;
            }
            
            boolean isSelected = module == this.selectedModule;
            boolean isHovered = mouseX >= startX && mouseX <= endX && mouseY >= yOffset && mouseY <= yOffset + ITEM_HEIGHT;
            boolean isEnabled = module.isEnabled();
            
            if (isSelected) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(0, 150, 255, 20), startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
            } else if (isHovered) {
                RenderUtils.renderRoundedQuad(drawContext.getMatrices(), HOVER, startX + 5, yOffset, endX - 5, yOffset + ITEM_HEIGHT, 8, 8, 8, 8, 30);
            }
            
            TextRenderer.drawString(module.getName().toString(), drawContext, startX + PADDING, yOffset + 12, toMCColor(isEnabled ? ELECTRIC_BLUE : TEXT_SECONDARY));
            
            int indicatorX = endX - 24;
            int indicatorY = yOffset + 12;
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), isEnabled ? SUCCESS : new Color(60, 65, 80, 255), indicatorX, indicatorY, indicatorX + 8, indicatorY + 8, 4, 4, 4, 4, 30);
            
            yOffset += ITEM_HEIGHT + 4;
        }
    }

    // ===== Input handling =====
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scaledMouseX = mouseX * MinecraftClient.getInstance().getWindow().getScaleFactor();
        double scaledMouseY = mouseY * MinecraftClient.getInstance().getWindow().getScaleFactor();
        
        int screenWidth = Krypton.mc.getWindow().getWidth();
        int screenHeight = Krypton.mc.getWindow().getHeight();
        
        // Category clicks
        int categoryStartX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING;
        int categoryStartY = (screenHeight - TOTAL_HEIGHT) / 2 + HEADER_HEIGHT + PADDING;
        int categoryY = categoryStartY;
        
        for (Category category : Category.values()) {
            if (scaledMouseX >= categoryStartX && scaledMouseX <= categoryStartX + CATEGORY_PANEL_WIDTH &&
                scaledMouseY >= categoryY && scaledMouseY <= categoryY + ITEM_HEIGHT) {
                this.selectedCategory = category;
                this.selectedModule = null;
                return true;
            }
            categoryY += ITEM_HEIGHT + 4;
        }
        
        // Module clicks
        int moduleStartX = (screenWidth - TOTAL_WIDTH) / 2 + SETTINGS_PANEL_WIDTH + PANEL_SPACING + CATEGORY_PANEL_WIDTH + PANEL_SPACING;
        int moduleStartY = (screenHeight - TOTAL_HEIGHT) / 2 + HEADER_HEIGHT + 30;
        List<Module> modules = Krypton.INSTANCE.getModuleManager().a(this.selectedCategory);
        int moduleY = moduleStartY;
        
        for (Module module : modules) {
            if (!this.searchQuery.isEmpty() && !module.getName().toString().toLowerCase().contains(this.searchQuery.toLowerCase())) {
                continue;
            }
            
            if (scaledMouseX >= moduleStartX && scaledMouseX <= moduleStartX + MODULE_PANEL_WIDTH &&
                scaledMouseY >= moduleY && scaledMouseY <= moduleY + ITEM_HEIGHT) {
                if (button == 0) {
                    module.toggle();
                } else if (button == 1) {
                    this.selectedModule = module;
                }
                return true;
            }
            moduleY += ITEM_HEIGHT + 4;
        }
        
        return super.mouseClicked(scaledMouseX, scaledMouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (CategoryWindow window : this.windows) {
            window.keyPressed(keyCode, scanCode, modifiers);
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
        this.onGuiClose();
    }

    public void onGuiClose() {
        Krypton.mc.setScreenAndRender(Krypton.INSTANCE.screen);
        this.currentColor = null;
        for (CategoryWindow window : this.windows) {
            window.onGuiClose();
        }
    }
}
