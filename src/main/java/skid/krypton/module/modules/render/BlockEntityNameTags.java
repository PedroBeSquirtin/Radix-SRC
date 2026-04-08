package skid.krypton.module.modules.render;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.RenderUtils;
import skid.krypton.utils.TextRenderer;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockEntityNameTags extends Module {
    
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 10, 80, 40, 5);
    private final BooleanSetting showChests = new BooleanSetting(EncryptedString.of("Show Chests"), true);
    private final BooleanSetting showShulkers = new BooleanSetting(EncryptedString.of("Show Shulkers"), true);
    
    private final Map<BlockPos, String> nameTags = new ConcurrentHashMap<>();
    private int scanTimer = 0;
    
    public BlockEntityNameTags() {
        super(EncryptedString.of("BlockEntity NameTags"), 
              EncryptedString.of("Shows name tags on block entities"), 
              -1, Category.RENDER);
        this.addSettings(this.range, this.showChests, this.showShulkers);
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        this.scanTimer++;
        
        if (this.scanTimer >= 10) {
            this.scanForBlockEntities();
            this.scanTimer = 0;
        }
    }
    
    private void scanForBlockEntities() {
        if (mc.world == null) return;
        
        this.nameTags.clear();
        int radius = (int) this.range.getValue();
        BlockPos playerPos = mc.player.getBlockPos();
        
        int chunkRadius = (radius / 16) + 1;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                WorldChunk chunk = mc.world.getChunk(playerChunkX + cx, playerChunkZ + cz);
                if (chunk == null) continue;
                
                for (BlockPos pos : chunk.getBlockEntityPositions()) {
                    BlockEntity be = mc.world.getBlockEntity(pos);
                    if (be == null) continue;
                    
                    double distance = Math.sqrt(playerPos.getSquaredDistance(pos));
                    if (distance > radius) continue;
                    
                    String name = getBlockEntityName(be);
                    if (name != null) {
                        this.nameTags.put(pos, name);
                    }
                }
            }
        }
    }
    
    private String getBlockEntityName(BlockEntity be) {
        if (be instanceof ChestBlockEntity && this.showChests.getValue()) return "Chest";
        if (be instanceof ShulkerBoxBlockEntity && this.showShulkers.getValue()) return "Shulker Box";
        return null;
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (this.nameTags.isEmpty()) return;
        
        Camera camera = RenderUtils.getCamera();
        if (camera == null) return;
        
        MatrixStack matrices = event.matrixStack;
        Vec3d cameraPos = RenderUtils.getCameraPos();
        
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (Map.Entry<BlockPos, String> entry : this.nameTags.entrySet()) {
            BlockPos pos = entry.getKey();
            String name = entry.getValue();
            
            matrices.push();
            matrices.translate(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.scale(0.025f, 0.025f, 0.025f);
            TextRenderer.drawString(name, matrices, -TextRenderer.getWidth(name) / 2, 0, Color.WHITE.getRGB());
            matrices.pop();
        }
        
        matrices.pop();
    }
}
