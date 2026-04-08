package skid.krypton.module.modules.render;

import net.minecraft.block.entity.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
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

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockEntityDebug extends Module {
    
    private final NumberSetting customRange = new NumberSetting(EncryptedString.of("Custom Range"), 10, 500, 0, 5);
    private final BooleanSetting useRenderDistance = new BooleanSetting(EncryptedString.of("Use Render Distance"), true);
    private final BooleanSetting highlightChests = new BooleanSetting(EncryptedString.of("Chests"), true);
    private final BooleanSetting highlightShulkers = new BooleanSetting(EncryptedString.of("Shulkers"), true);
    private final BooleanSetting highlightSpawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
    private final BooleanSetting highlightFurnaces = new BooleanSetting(EncryptedString.of("Furnaces"), true);
    private final BooleanSetting highlightBeacons = new BooleanSetting(EncryptedString.of("Beacons"), true);
    private final BooleanSetting showTracers = new BooleanSetting(EncryptedString.of("Show Tracers"), true);
    private final BooleanSetting aggressiveScan = new BooleanSetting(EncryptedString.of("Aggressive Scan"), true);
    
    // Colors
    private static final Color CHEST_COLOR = new Color(255, 200, 50, 180);
    private static final Color SHULKER_COLOR = new Color(200, 50, 255, 180);
    private static final Color SPAWNER_COLOR = new Color(255, 50, 50, 180);
    private static final Color FURNACE_COLOR = new Color(150, 150, 150, 180);
    private static final Color BEACON_COLOR = new Color(50, 200, 255, 180);
    
    // Cache
    private final Map<BlockPos, BlockEntityInfo> foundBlockEntities = new ConcurrentHashMap<>();
    private final Set<Long> scannedChunks = new HashSet<>();
    private int scanTimer = 0;
    private int forceLoadTimer = 0;
    
    public BlockEntityDebug() {
        super(EncryptedString.of("BlockEntity Debug"), 
              EncryptedString.of("Find active bases - Advanced Bypass"), 
              -1, Category.RENDER);
        this.addSettings(this.customRange, this.useRenderDistance, this.highlightChests, this.highlightShulkers, 
                        this.highlightSpawners, this.highlightFurnaces, this.highlightBeacons, 
                        this.showTracers, this.aggressiveScan);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        this.foundBlockEntities.clear();
        this.scannedChunks.clear();
        this.scanTimer = 0;
        this.forceLoadTimer = 0;
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
        this.forceLoadTimer++;
        
        // Force load chunks within render distance every 10 ticks
        if (aggressiveScan.getValue() && this.forceLoadTimer >= 10) {
            this.forceLoadNearbyChunks();
            this.forceLoadTimer = 0;
        }
        
        // Scan every 2 ticks for aggressive mode, 10 for passive
        int scanInterval = aggressiveScan.getValue() ? 2 : 10;
        if (this.scanTimer >= scanInterval) {
            this.scanForBlockEntities();
            this.scanTimer = 0;
        }
        
        this.updateDistances();
    }
    
    private void forceLoadNearbyChunks() {
        if (mc.player == null || mc.world == null) return;
        
        int renderDistance = mc.options.getViewDistance().getValue();
        int range = getRange();
        int chunkRadius = Math.max(renderDistance, range / 16) + 2;
        
        ClientPlayerEntity player = mc.player;
        int centerChunkX = player.getChunkPos().x;
        int centerChunkZ = player.getChunkPos().z;
        
        // Force chunks to load by accessing them
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                int chunkX = centerChunkX + x;
                int chunkZ = centerChunkZ + z;
                long chunkKey = (long) chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
                
                if (!scannedChunks.contains(chunkKey)) {
                    // Access chunk to force load
                    WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        scannedChunks.add(chunkKey);
                    }
                }
            }
        }
    }
    
    private int getRange() {
        if (useRenderDistance.getValue()) {
            return mc.options.getViewDistance().getValue() * 16;
        }
        return (int) this.customRange.getValue();
    }
    
    private void scanForBlockEntities() {
        if (mc.world == null || mc.player == null) return;
        
        int range = getRange();
        BlockPos playerPos = mc.player.getBlockPos();
        int chunkRadius = (range / 16) + 2;
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        
        // Scan all chunks within radius
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                WorldChunk chunk = mc.world.getChunk(playerChunkX + cx, playerChunkZ + cz);
                if (chunk == null) continue;
                
                scanChunk(chunk);
            }
        }
    }
    
    private void scanChunk(WorldChunk chunk) {
        if (mc.player == null) return;
        
        int range = getRange();
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Get all block entities in chunk
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
        
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity be = entry.getValue();
            
            if (be == null) continue;
            
            double distance = Math.sqrt(playerPos.getSquaredDistance(pos));
            if (distance > range) continue;
            
            Color color = getBlockEntityColor(be);
            if (color == null) continue;
            
            // Update or add
            foundBlockEntities.put(pos, new BlockEntityInfo(pos, be, color, distance, System.currentTimeMillis()));
        }
    }
    
    private void updateDistances() {
        if (mc.player == null) return;
        BlockPos playerPos = mc.player.getBlockPos();
        int range = getRange();
        
        Iterator<Map.Entry<BlockPos, BlockEntityInfo>> iterator = foundBlockEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockEntityInfo> entry = iterator.next();
            double newDistance = Math.sqrt(playerPos.getSquaredDistance(entry.getKey()));
            
            if (newDistance > range + 16) {
                iterator.remove();
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
        if (be instanceof TrappedChestBlockEntity && this.highlightChests.getValue()) return CHEST_COLOR;
        return null;
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (this.foundBlockEntities.isEmpty()) return;
        
        Camera camera = RenderUtils.getCamera();
        if (camera == null) return;
        
        MatrixStack matrices = event.matrixStack;
        Vec3d cameraPos = RenderUtils.getCameraPos();
        Vec3d playerPos = mc.player.getEyePos();
        
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (BlockEntityInfo info : this.foundBlockEntities.values()) {
            BlockPos pos = info.getPos();
            Color color = info.getColor();
            
            // Calculate alpha based on age (newer = brighter)
            long age = System.currentTimeMillis() - info.getFirstSeen();
            int alpha = aggressiveScan.getValue() ? 200 : Math.max(100, 200 - (int)(age / 1000));
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
            
            float x1 = pos.getX() + 0.05f;
            float y1 = pos.getY() + 0.05f;
            float z1 = pos.getZ() + 0.05f;
            float x2 = pos.getX() + 0.95f;
            float y2 = pos.getY() + 0.95f;
            float z2 = pos.getZ() + 0.95f;
            
            // Draw filled box
            RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, renderColor);
            
            // Draw outline by rendering a second box with wireframe (using same method but different color)
            Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
            RenderUtils.renderFilledBox(matrices, x1 - 0.02f, y1 - 0.02f, z1 - 0.02f, x1 + 0.02f, y2 + 0.02f, z2 + 0.02f, outlineColor);
            RenderUtils.renderFilledBox(matrices, x2 - 0.02f, y1 - 0.02f, z1 - 0.02f, x2 + 0.02f, y2 + 0.02f, z2 + 0.02f, outlineColor);
            RenderUtils.renderFilledBox(matrices, x1 - 0.02f, y1 - 0.02f, z1 - 0.02f, x2 + 0.02f, y1 + 0.02f, z2 + 0.02f, outlineColor);
            RenderUtils.renderFilledBox(matrices, x1 - 0.02f, y2 - 0.02f, z1 - 0.02f, x2 + 0.02f, y2 + 0.02f, z2 + 0.02f, outlineColor);
            
            // Draw tracer line to player
            if (this.showTracers.getValue()) {
                Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                RenderUtils.renderLine(matrices, outlineColor, blockCenter, playerPos);
            }
        }
        
        matrices.pop();
    }
    
    private static class BlockEntityInfo {
        private final BlockPos pos;
        private final BlockEntity entity;
        private final Color color;
        private final double distance;
        private final long firstSeen;
        
        public BlockEntityInfo(BlockPos pos, BlockEntity entity, Color color, double distance, long firstSeen) {
            this.pos = pos;
            this.entity = entity;
            this.color = color;
            this.distance = distance;
            this.firstSeen = firstSeen;
        }
        
        public BlockPos getPos() { return pos; }
        public BlockEntity getEntity() { return entity; }
        public Color getColor() { return color; }
        public double getDistance() { return distance; }
        public long getFirstSeen() { return firstSeen; }
    }
}
