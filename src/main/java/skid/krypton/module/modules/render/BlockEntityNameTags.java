package skid.krypton.module.modules.render;

import net.minecraft.block.entity.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.RenderUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class BlockEntityNameTags extends Module {
    
    // Display settings
    private final NumberSetting maxDistance = new NumberSetting(EncryptedString.of("Max Distance"), 10, 100, 50, 5);
    private final NumberSetting textScale = new NumberSetting(EncryptedString.of("Text Scale"), 0.5, 2.0, 1.0, 0.1);
    private final NumberSetting backgroundAlpha = new NumberSetting(EncryptedString.of("Background Alpha"), 0, 255, 100, 5);
    private final NumberSetting textRed = new NumberSetting(EncryptedString.of("Text Red"), 0, 255, 255, 1);
    private final NumberSetting textGreen = new NumberSetting(EncryptedString.of("Text Green"), 0, 255, 255, 1);
    private final NumberSetting textBlue = new NumberSetting(EncryptedString.of("Text Blue"), 0, 255, 255, 1);
    
    // ESP settings
    private final BooleanSetting chestESP = new BooleanSetting(EncryptedString.of("Chest ESP"), true);
    private final BooleanSetting shulkerESP = new BooleanSetting(EncryptedString.of("Shulker ESP"), true);
    private final BooleanSetting furnaceESP = new BooleanSetting(EncryptedString.of("Furnace ESP"), true);
    private final BooleanSetting hopperESP = new BooleanSetting(EncryptedString.of("Hopper ESP"), true);
    private final BooleanSetting dropperESP = new BooleanSetting(EncryptedString.of("Dropper ESP"), true);
    private final BooleanSetting dispenserESP = new BooleanSetting(EncryptedString.of("Dispenser ESP"), true);
    private final BooleanSetting barrelESP = new BooleanSetting(EncryptedString.of("Barrel ESP"), true);
    private final BooleanSetting spawnerESP = new BooleanSetting(EncryptedString.of("Spawner ESP"), true);
    private final BooleanSetting beaconESP = new BooleanSetting(EncryptedString.of("Beacon ESP"), true);
    private final BooleanSetting brewingStandESP = new BooleanSetting(EncryptedString.of("Brewing Stand ESP"), true);
    private final BooleanSetting enderChestESP = new BooleanSetting(EncryptedString.of("Ender Chest ESP"), true);
    
    // ESP colors
    private final NumberSetting chestRed = new NumberSetting(EncryptedString.of("Chest Red"), 0, 255, 255, 1);
    private final NumberSetting chestGreen = new NumberSetting(EncryptedString.of("Chest Green"), 0, 255, 200, 1);
    private final NumberSetting chestBlue = new NumberSetting(EncryptedString.of("Chest Blue"), 0, 255, 100, 1);
    private final NumberSetting spawnerRed = new NumberSetting(EncryptedString.of("Spawner Red"), 0, 255, 255, 1);
    private final NumberSetting spawnerGreen = new NumberSetting(EncryptedString.of("Spawner Green"), 0, 255, 0, 1);
    private final NumberSetting spawnerBlue = new NumberSetting(EncryptedString.of("Spawner Blue"), 0, 255, 0, 1);
    
    private final Map<BlockPos, Long> renderTimes = new HashMap<>();
    
    public BlockEntityNameTags() {
        super(EncryptedString.of("BlockEntity NameTags"), EncryptedString.of("Shows nametags and ESP for block entities"), -1, Category.RENDER);
        addSettings(maxDistance, textScale, backgroundAlpha, textRed, textGreen, textBlue,
                    chestESP, shulkerESP, furnaceESP, hopperESP, dropperESP, dispenserESP,
                    barrelESP, spawnerESP, beaconESP, brewingStandESP, enderChestESP,
                    chestRed, chestGreen, chestBlue, spawnerRed, spawnerGreen, spawnerBlue);
    }
    
    private Color getTextColor() {
        return new Color((int) textRed.getValue(), (int) textGreen.getValue(), (int) textBlue.getValue(), 255);
    }
    
    private Color getESPColor(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof TrappedChestBlockEntity) {
            return new Color((int) chestRed.getValue(), (int) chestGreen.getValue(), (int) chestBlue.getValue(), 100);
        } else if (blockEntity instanceof SpawnerBlockEntity) {
            return new Color((int) spawnerRed.getValue(), (int) spawnerGreen.getValue(), (int) spawnerBlue.getValue(), 100);
        } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return new Color(200, 100, 255, 100);
        } else if (blockEntity instanceof FurnaceBlockEntity) {
            return new Color(128, 128, 128, 100);
        } else if (blockEntity instanceof HopperBlockEntity) {
            return new Color(100, 100, 100, 100);
        } else if (blockEntity instanceof BarrelBlockEntity) {
            return new Color(139, 69, 19, 100);
        } else if (blockEntity instanceof BeaconBlockEntity) {
            return new Color(0, 255, 255, 100);
        } else if (blockEntity instanceof BrewingStandBlockEntity) {
            return new Color(0, 150, 0, 100);
        } else if (blockEntity instanceof EnderChestBlockEntity) {
            return new Color(100, 0, 100, 100);
        }
        return new Color(255, 255, 255, 100);
    }
    
    private String getBlockEntityName(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity) {
            return "Chest";
        } else if (blockEntity instanceof TrappedChestBlockEntity) {
            return "Trapped Chest";
        } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return "Shulker Box";
        } else if (blockEntity instanceof FurnaceBlockEntity) {
            return "Furnace";
        } else if (blockEntity instanceof HopperBlockEntity) {
            return "Hopper";
        } else if (blockEntity instanceof DropperBlockEntity) {
            return "Dropper";
        } else if (blockEntity instanceof DispenserBlockEntity) {
            return "Dispenser";
        } else if (blockEntity instanceof BarrelBlockEntity) {
            return "Barrel";
        } else if (blockEntity instanceof SpawnerBlockEntity) {
            SpawnerBlockEntity spawner = (SpawnerBlockEntity) blockEntity;
            try {
                String entityType = spawner.getLogic().getRenderedEntity().getDefaultName().getString();
                return "Spawner: " + entityType;
            } catch (Exception e) {
                return "Spawner";
            }
        } else if (blockEntity instanceof BeaconBlockEntity) {
            return "Beacon";
        } else if (blockEntity instanceof BrewingStandBlockEntity) {
            return "Brewing Stand";
        } else if (blockEntity instanceof EnderChestBlockEntity) {
            return "Ender Chest";
        }
        return blockEntity.getCachedState().getBlock().getName().getString();
    }
    
    private boolean shouldRender(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof TrappedChestBlockEntity) {
            return chestESP.getValue();
        } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return shulkerESP.getValue();
        } else if (blockEntity instanceof FurnaceBlockEntity) {
            return furnaceESP.getValue();
        } else if (blockEntity instanceof HopperBlockEntity) {
            return hopperESP.getValue();
        } else if (blockEntity instanceof DropperBlockEntity) {
            return dropperESP.getValue();
        } else if (blockEntity instanceof DispenserBlockEntity) {
            return dispenserESP.getValue();
        } else if (blockEntity instanceof BarrelBlockEntity) {
            return barrelESP.getValue();
        } else if (blockEntity instanceof SpawnerBlockEntity) {
            return spawnerESP.getValue();
        } else if (blockEntity instanceof BeaconBlockEntity) {
            return beaconESP.getValue();
        } else if (blockEntity instanceof BrewingStandBlockEntity) {
            return brewingStandESP.getValue();
        } else if (blockEntity instanceof EnderChestBlockEntity) {
            return enderChestESP.getValue();
        }
        return false;
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();
        
        // Remove old render times
        long currentTime = System.currentTimeMillis();
        renderTimes.entrySet().removeIf(entry -> currentTime - entry.getValue() > 1000);
        
        for (BlockEntity blockEntity : mc.world.blockEntities) {
            BlockPos pos = blockEntity.getPos();
            Vec3d blockPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            double distanceSq = cameraPos.squaredDistanceTo(blockPos);
            
            if (distanceSq > maxDistSq) continue;
            if (!shouldRender(blockEntity)) continue;
            
            // Render ESP outline
            renderESPOutline(event.matrixStack, blockEntity, blockPos, cameraPos, distanceSq);
            
            // Render nametag
            renderNameTag(event.matrixStack, blockEntity, blockPos, cameraPos, distanceSq);
        }
    }
    
    private void renderESPOutline(MatrixStack matrixStack, BlockEntity blockEntity, Vec3d blockPos, Vec3d cameraPos, double distanceSq) {
        matrixStack.push();
        
        double x = blockPos.x - cameraPos.x;
        double y = blockPos.y - cameraPos.y;
        double z = blockPos.z - cameraPos.z;
        
        matrixStack.translate(x, y, z);
        
        Color espColor = getESPColor(blockEntity);
        float alpha = (float) (espColor.getAlpha() / 255.0);
        float r = espColor.getRed() / 255f;
        float g = espColor.getGreen() / 255f;
        float b = espColor.getBlue() / 255f;
        
        // Render box around block entity
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        float size = 0.5f;
        
        // Draw outline box
        // Bottom square
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, -size, -size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, -size, size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, -size, size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, -size, -size).color(r, g, b, alpha);
        
        // Top square
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, size, -size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, size, -size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, size, -size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, size, size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, size, size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, size, size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, size, size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, size, -size).color(r, g, b, alpha);
        
        // Vertical lines
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, size, -size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, size, -size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), size, size, size).color(r, g, b, alpha);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -size, size, size).color(r, g, b, alpha);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        matrixStack.pop();
    }
    
    private void renderNameTag(MatrixStack matrixStack, BlockEntity blockEntity, Vec3d blockPos, Vec3d cameraPos, double distanceSq) {
        matrixStack.push();
        
        double x = blockPos.x - cameraPos.x;
        double y = blockPos.y - cameraPos.y + 0.8;
        double z = blockPos.z - cameraPos.z;
        
        matrixStack.translate(x, y, z);
        
        // Always face the camera
        float yaw = mc.gameRenderer.getCamera().getYaw();
        float pitch = mc.gameRenderer.getCamera().getPitch();
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        
        float scale = (float) (textScale.getValue() * (1.0 - Math.min(0.5, distanceSq / (maxDistance.getValue() * maxDistance.getValue()) * 0.5)));
        matrixStack.scale(scale, scale, scale);
        
        String name = getBlockEntityName(blockEntity);
        TextRenderer textRenderer = mc.textRenderer;
        int textWidth = textRenderer.getWidth(name);
        int textHeight = textRenderer.fontHeight;
        
        // Draw background
        int alpha = backgroundAlpha.getValue().intValue();
        Color bgColor = new Color(0, 0, 0, alpha);
        Color textColor = getTextColor();
        
        DrawContext drawContext = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
        
        // Use immediate rendering for nametag
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        
        // Background
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -textWidth / 2f - 2, -textHeight - 1, 0)
              .color(bgColor.getRed() / 255f, bgColor.getGreen() / 255f, bgColor.getBlue() / 255f, bgColor.getAlpha() / 255f);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), textWidth / 2f + 2, -textHeight - 1, 0)
              .color(bgColor.getRed() / 255f, bgColor.getGreen() / 255f, bgColor.getBlue() / 255f, bgColor.getAlpha() / 255f);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), textWidth / 2f + 2, 1, 0)
              .color(bgColor.getRed() / 255f, bgColor.getGreen() / 255f, bgColor.getBlue() / 255f, bgColor.getAlpha() / 255f);
        buffer.vertex(matrixStack.peek().getPositionMatrix(), -textWidth / 2f - 2, 1, 0)
              .color(bgColor.getRed() / 255f, bgColor.getGreen() / 255f, bgColor.getBlue() / 255f, bgColor.getAlpha() / 255f);
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        // Draw text
        drawContext.drawText(textRenderer, name, (int)(-textWidth / 2f), -textHeight, textColor.getRGB(), true);
        
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        
        matrixStack.pop();
    }
}
