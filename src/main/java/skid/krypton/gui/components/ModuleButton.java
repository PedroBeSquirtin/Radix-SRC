package skid.krypton.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import skid.krypton.Krypton;
import skid.krypton.gui.CategoryWindow;
import skid.krypton.gui.Component;
import skid.krypton.module.Module;
import skid.krypton.module.setting.*;
import skid.krypton.utils.*;
import skid.krypton.utils.TextRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ModuleButton {
    public List<Component> settings;
    public CategoryWindow parent;
    public Module module;
    public int offset;
    public boolean extended;
    public int settingOffset;
    public Color currentColor;
    public Color currentAlpha;
    public Animation animation;
    private final Color ACCENT_COLOR;
    private final Color HOVER_COLOR;
    private final Color ENABLED_COLOR;
    private final Color DISABLED_COLOR;
    private float hoverAnimation;
    private float enabledAnimation;

    public ModuleButton(final CategoryWindow parent, final Module module, final int offset) {
        this.settings = new ArrayList<>();
        this.animation = new Animation(0.0);
        this.ACCENT_COLOR = new Color(65, 150, 255);
        this.HOVER_COLOR = new Color(255, 255, 255, 15);
        this.ENABLED_COLOR = new Color(100, 200, 255);
        this.DISABLED_COLOR = new Color(180, 180, 190);
        this.hoverAnimation = 0.0f;
        this.enabledAnimation = 0.0f;
        this.parent = parent;
        this.module = module;
        this.offset = offset;
        this.extended = false;
        this.settingOffset = parent.getHeight();
        for (final Object next : module.getSettings()) {
            if (next instanceof BooleanSetting) {
                this.settings.add(new Checkbox(this, (Setting) next, this.settingOffset));
            } else if (next instanceof NumberSetting) {
                this.settings.add(new NumberBox(this, (Setting) next, this.settingOffset));
            } else if (next instanceof ModeSetting) {
                this.settings.add(new ModeBox(this, (Setting) next, this.settingOffset));
            } else if (next instanceof BindSetting) {
                this.settings.add(new Keybind(this, (Setting) next, this.settingOffset));
            } else if (next instanceof StringSetting) {
                this.settings.add(new TextBox(this, (Setting) next, this.settingOffset));
            } else if (next instanceof MinMaxSetting) {
                this.settings.add(new Slider(this, (Setting) next, this.settingOffset));
            } else if (next instanceof ItemSetting) {
                this.settings.add(new ItemBox(this, (Setting) next, this.settingOffset));
            }
            this.settingOffset += parent.getHeight();
        }
    }

    public void render(final DrawContext drawContext, final int n, final int n2, final float n3) {
        if (this.parent.getY() + this.offset > MinecraftClient.getInstance().getWindow().getHeight()) {
            return;
        }
        final Iterator<Component> iterator = this.settings.iterator();
        while (iterator.hasNext()) {
            iterator.next().onUpdate();
        }
        this.updateAnimations(n, n2, n3);
        final int x = this.parent.getX();
        final int n4 = this.parent.getY() + this.offset;
        final int width = this.parent.getWidth();
        final int height = this.parent.getHeight();
        
        this.renderButtonBackground(drawContext, x, n4, width, height);
        this.renderModuleInfo(drawContext, x, n4, width, height);
        
        if (this.extended) {
            this.renderSettings(drawContext, n, n2, n3);
        }
        if (this.isHovered(n, n2) && !this.parent.dragging) {
            Krypton.INSTANCE.GUI.setTooltip(this.module.getDescription(), n + 10, n2 + 10);
        }
    }

    private void updateAnimations(final int n, final int n2, final float n3) {
        final float n4 = n3 * 0.05f;
        float n5;
        if (this.isHovered(n, n2) && !this.parent.dragging) {
            n5 = 1.0f;
        } else {
            n5 = 0.0f;
        }
        this.hoverAnimation = (float) MathUtil.exponentialInterpolate(this.hoverAnimation, n5, 0.05, n4);
        float n6;
        if (this.module.isEnabled()) {
            n6 = 1.0f;
        } else {
            n6 = 0.0f;
        }
        this.enabledAnimation = (float) MathUtil.exponentialInterpolate(this.enabledAnimation, n6, 0.005, n4);
        this.enabledAnimation = (float) MathUtil.clampValue(this.enabledAnimation, 0.0, 1.0);
    }

    private void renderButtonBackground(final DrawContext drawContext, final int n, final int n2, final int n3, final int n4) {
        Color bgColor = new Color(22, 26, 32, 230);
        final Color a = ColorUtil.a(bgColor, this.HOVER_COLOR, this.hoverAnimation);
        drawContext.fill(n, n2, n + n3, n2 + n4, a.getRGB());
        
        // Separator line
        if (this.parent.moduleButtons.indexOf(this) > 0) {
            drawContext.fill(n + 8, n2, n + n3 - 8, n2 + 1, new Color(40, 44, 50, 150).getRGB());
        }
    }

    private void renderModuleInfo(final DrawContext drawContext, final int n, final int n2, final int n3, final int n4) {
        Color textColor = ColorUtil.a(this.DISABLED_COLOR, this.ENABLED_COLOR, this.enabledAnimation);
        TextRenderer.drawString(this.module.getName(), drawContext, n + 12, n2 + n4 / 2 - 4, textColor.getRGB());
        
        // Settings indicator if module has settings
        if (!this.module.getSettings().isEmpty()) {
            String indicator = this.extended ? "▼" : "▶";
            TextRenderer.drawString(indicator, drawContext, n + n3 - 20, n2 + n4 / 2 - 4, new Color(120, 125, 135).getRGB());
        }
        
        // Bind indicator
        if (this.module.getKeybind() != -1) {
            String bindText = KeyUtils.getKey(this.module.getKeybind()).toString();
            int bindWidth = TextRenderer.getWidth(bindText);
            RenderUtils.renderRoundedQuad(drawContext.getMatrices(), new Color(35, 40, 48, 200), 
                n + n3 - bindWidth - 35, n2 + n4 / 2 - 8, 
                n + n3 - 15, n2 + n4 / 2 + 6, 
                3.0, 3.0, 3.0, 3.0, 50.0);
            TextRenderer.drawString(bindText, drawContext, n + n3 - bindWidth - 30, n2 + n4 / 2 - 4, new Color(150, 155, 165).getRGB());
        }
    }

    private void renderSettings(final DrawContext drawContext, final int n, final int n2, final float n3) {
        final int n4 = this.parent.getY() + this.offset + this.parent.getHeight();
        final double animation = this.animation.getAnimation();
        RenderSystem.enableScissor(this.parent.getX(), Krypton.mc.getWindow().getHeight() - (n4 + (int) animation), this.parent.getWidth(), (int) animation);
        final Iterator<Component> iterator = this.settings.iterator();
        while (iterator.hasNext()) {
            iterator.next().render(drawContext, n, n2 - n4, n3);
        }
        this.renderSliderControls(drawContext);
        RenderSystem.disableScissor();
    }

    private void renderSliderControls(final DrawContext drawContext) {
        for (final Component next : this.settings) {
            if (next instanceof final NumberBox numberBox) {
                this.renderModernSliderKnob(drawContext, next.parentX() + Math.max(numberBox.lerpedOffsetX, 2.5), next.parentY() + numberBox.offset + next.parentOffset() + 27.5, numberBox.currentColor1);
            } else if (next instanceof Slider) {
                this.renderModernSliderKnob(drawContext, next.parentX() + Math.max(((Slider) next).lerpedOffsetMinX, 2.5), next.parentY() + next.offset + next.parentOffset() + 27.5, ((Slider) next).accentColor1);
                this.renderModernSliderKnob(drawContext, next.parentX() + Math.max(((Slider) next).lerpedOffsetMaxX, 2.5), next.parentY() + next.offset + next.parentOffset() + 27.5, ((Slider) next).accentColor1);
            }
        }
    }

    private void renderModernSliderKnob(final DrawContext drawContext, final double n, final double n2, final Color color) {
        RenderUtils.renderCircle(drawContext.getMatrices(), new Color(0, 0, 0, 80), n, n2, 6.0, 16);
        RenderUtils.renderCircle(drawContext.getMatrices(), color, n, n2, 5.0, 14);
        RenderUtils.renderCircle(drawContext.getMatrices(), new Color(255, 255, 255, 60), n, n2 - 1.0, 2.5, 10);
    }

    public void onExtend() {
        final Iterator<ModuleButton> iterator = this.parent.moduleButtons.iterator();
        while (iterator.hasNext()) {
            iterator.next().extended = false;
        }
    }

    public void keyPressed(final int n, final int n2, final int n3) {
        final Iterator<Component> iterator = this.settings.iterator();
        while (iterator.hasNext()) {
            iterator.next().keyPressed(n, n2, n3);
        }
    }

    public void mouseDragged(final double n, final double n2, final int n3, final double n4, final double n5) {
        if (this.extended) {
            final Iterator<Component> iterator = this.settings.iterator();
            while (iterator.hasNext()) {
                iterator.next().mouseDragged(n, n2, n3, n4, n5);
            }
        }
    }

    public void mouseClicked(final double n, final double n2, final int button) {
        if (this.isHovered(n, n2)) {
            if (button == 0) {
                if (!this.module.getSettings().isEmpty() && n > this.parent.getX() + this.parent.getWidth() - 25) {
                    if (!this.extended) {
                        this.onExtend();
                    }
                    this.extended = !this.extended;
                } else {
                    this.module.toggle();
                }
            } else if (button == 1) {
                if (!this.module.getSettings().isEmpty()) {
                    if (!this.extended) {
                        this.onExtend();
                    }
                    this.extended = !this.extended;
                }
            }
        }
        if (this.extended) {
            for (Component setting : this.settings) {
                setting.mouseClicked(n, n2, button);
            }
        }
    }

    public void onGuiClose() {
        this.currentAlpha = null;
        this.currentColor = null;
        this.hoverAnimation = 0.0f;
        float enabledAnimation;
        if (this.module.isEnabled()) {
            enabledAnimation = 1.0f;
        } else {
            enabledAnimation = 0.0f;
        }
        this.enabledAnimation = enabledAnimation;
        final Iterator<Component> iterator = this.settings.iterator();
        while (iterator.hasNext()) {
            iterator.next().onGuiClose();
        }
    }

    public void mouseReleased(final double n, final double n2, final int n3) {
        final Iterator<Component> iterator = this.settings.iterator();
        while (iterator.hasNext()) {
            iterator.next().mouseReleased(n, n2, n3);
        }
    }

    public boolean isHovered(final double n, final double n2) {
        return n > this.parent.getX() && n < this.parent.getX() + this.parent.getWidth() && n2 > this.parent.getY() + this.offset && n2 < this.parent.getY() + this.offset + this.parent.getHeight();
    }
}
