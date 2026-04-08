package skid.krypton.module.modules.render;

import net.minecraft.block.entity.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.module.Category;
import skid.krypton.module.Module;
import skid.krypton.module.setting.BooleanSetting;
import skid.krypton.module.setting.NumberSetting;
import skid.krypton.utils.EncryptedString;
import skid.krypton.utils.RenderUtils;
import skid.krypton.utils.TextRenderer;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockEntityDebug extends Module {
    
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 10, 80, 40, 5);
    private final BooleanSetting highlightChests = new BooleanSetting(EncryptedString.of("Chests"), true);
    private final BooleanSetting highlightShulkers = new BooleanSetting(EncryptedString.of("Shulkers"), true);
    private final BooleanSetting highlightSpawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
    private final BooleanSetting highlightFurnaces = new BooleanSetting(EncryptedString.of("Furnaces"), true);
    private final BooleanSetting highlightBeacons = new BooleanSetting(EncryptedString.of("Beacons"), true);
    private final BooleanSetting showDistance = new BooleanSetting(EncryptedString.of("Show Distance"), true);
    
    // Colors
    private static final Color CHEST_COLOR = new Color(255, 200, 50, 150);
    private static final Color SHULKER_COLOR = new Color(200, 50, 255, 150);
    private static final Color SPAWNER_COLOR = new Color(255, 50, 50, 150);
    private static final Color FURNACE_COLOR = new Color(150, 150, 150, 150);
    private static final Color BEACON_COLOR = new Color(50, 200, 255, 150);
    
    // Cache
    private final Map<BlockPos, BlockEntityInfo> foundBlockEntities = new ConcurrentHashMap<>();
    private int scanTimer = 0;
    
    public BlockEntityDebug() {
        super(EncryptedString.of("BlockEntity Debug"), 
              EncryptedString.of("Find active bases by highlighting block entities"), 
              -1, Category.RENDER);
        this.addSettings(this.range, this.highlightChests, this.highlightShulkers, 
                        this.highlightSpawners, this.highlightFurnaces, this.highlightBeacons, 
                        this.showDistance);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        this.foundBlockEntities.clear();
        this.scanTimer = 0;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        this.foundBlockEntities.clear();
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        this.scanTimer++;
        
        // Scan every 5 ticks to reduce lag
        if (this.scanTimer >= 5) {
            this.scanForBlockEntities();
            this.scanTimer = 0;
        }
    }
    
    private void scanForBlockEntities() {
        if (mc.world == null) return;
        
        int radius = (int) this.range.getValue();
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Clear old entries that are out of range
        this.foundBlockEntities.entrySet().removeIf(entry -> {
            double dist = Math.sqrt(playerPos.getSquaredDistance(entry.getKey()));
            return dist > radius + 10;
        });
        
        // Scan nearby chunks
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
                    
                    Color color = getBlockEntityColor(be);
                    if (color == null) continue;
                    
                    this.foundBlockEntities.put(pos, new BlockEntityInfo(pos, be, color, distance));
                }
            }
        }
    }
    
    private Color getBlockEntityColor(BlockEntity be) {
        if (be instanceof ChestBlockEntity && this.highlightChests.getValue()) return CHEST_COLOR;
        if (be instanceof ShulkerBoxBlockEntity && this.highlightShulkers.getValue()) return SHULKER_COLOR;
        if (be instanceof MobSpawnerBlockEntity && this.highlightSpawners.getValue()) return SPAWNER_COLOR;
        if (be instanceof FurnaceBlockEntity && this.highlightFurnaces.getValue()) return FURNACE_COLOR;
        if (be instanceof BeaconBlockEntity && this.highlightBeacons.getValue()) return BEACON_COLOR;
        if (be instanceof BarrelBlockEntity && this.highlightChests.getValue()) return CHEST_COLOR;
        return null;
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (this.foundBlockEntities.isEmpty()) return;
        
        Camera camera = RenderUtils.getCamera();
        if (camera == null) return;
        
        MatrixStack matrices = event.matrixStack;
        Vec3d cameraPos = RenderUtils.getCameraPos();
        
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (BlockEntityInfo info : this.foundBlockEntities.values()) {
            BlockPos pos = info.getPos();
            Color color = info.getColor();
            
            float x1 = pos.getX() + 0.1f;
            float y1 = pos.getY() + 0.1f;
            float z1 = pos.getZ() + 0.1f;
            float x2 = pos.getX() + 0.9f;
            float y2 = pos.getY() + 0.9f;
            float z2 = pos.getZ() + 0.9f;
            
            // Filled box
            RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, color);
            
            // Draw distance text
            if (this.showDistance.getValue()) {
                String distText = (int)info.getDistance() + "m";
                matrices.push();
                matrices.translate(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.scale(0.025f, 0.025f, 0.025f);
                TextRenderer.drawString(distText, matrices, -TextRenderer.getWidth(distText) / 2, 0, Color.WHITE.getRGB());
                matrices.pop();
            }
        }
        
        matrices.pop();
    }
    
    private static class BlockEntityInfo {
        private final BlockPos pos;
        private final BlockEntity entity;
        private final Color color;
        private final double distance;
        
        public BlockEntityInfo(BlockPos pos, BlockEntity entity, Color color, double distance) {
            this.pos = pos;
            this.entity = entity;
            this.color = color;
            this.distance = distance;
        }
        
        public BlockPos getPos() { return pos; }
        public BlockEntity getEntity() { return entity; }
        public Color getColor() { return color; }
        public double getDistance() { return distance; }
    }
}
