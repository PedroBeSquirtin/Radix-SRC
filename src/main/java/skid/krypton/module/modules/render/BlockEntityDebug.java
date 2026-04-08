package skid.krypton.module.modules.render;

import net.minecraft.block.entity.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.Render3DEvent;
import skid.krypton.event.events.TickEvent;
import skid.krypton.event.events.ChunkEvent;
import skid.krypton.event.events.PacketEvent;
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
    
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 10, 80, 50, 5);
    private final BooleanSetting highlightChests = new BooleanSetting(EncryptedString.of("Chests"), true);
    private final BooleanSetting highlightShulkers = new BooleanSetting(EncryptedString.of("Shulkers"), true);
    private final BooleanSetting highlightSpawners = new BooleanSetting(EncryptedString.of("Spawners"), true);
    private final BooleanSetting highlightFurnaces = new BooleanSetting(EncryptedString.of("Furnaces"), true);
    private final BooleanSetting highlightBeacons = new BooleanSetting(EncryptedString.of("Beacons"), true);
    private final BooleanSetting showDistance = new BooleanSetting(EncryptedString.of("Show Distance"), true);
    private final BooleanSetting silentMode = new BooleanSetting(EncryptedString.of("Silent Mode (Anti-Detect)"), true);
    
    // Colors
    private static final Color CHEST_COLOR = new Color(255, 200, 50, 150);
    private static final Color SHULKER_COLOR = new Color(200, 50, 255, 150);
    private static final Color SPAWNER_COLOR = new Color(255, 50, 50, 150);
    private static final Color FURNACE_COLOR = new Color(150, 150, 150, 150);
    private static final Color BEACON_COLOR = new Color(50, 200, 255, 150);
    
    // Cache - stores block entities we've naturally discovered
    private final Map<BlockPos, BlockEntityInfo> foundBlockEntities = new ConcurrentHashMap<>();
    private final Set<Long> scannedChunks = new HashSet<>();
    private int passiveScanTimer = 0;
    private int renderTimer = 0;
    
    public BlockEntityDebug() {
        super(EncryptedString.of("BlockEntity Debug"), 
              EncryptedString.of("Find active bases - DonutSMP Bypass"), 
              -1, Category.RENDER);
        this.addSettings(this.range, this.highlightChests, this.highlightShulkers, 
                        this.highlightSpawners, this.highlightFurnaces, this.highlightBeacons, 
                        this.showDistance, this.silentMode);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        this.foundBlockEntities.clear();
        this.scannedChunks.clear();
        this.passiveScanTimer = 0;
        this.renderTimer = 0;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        this.foundBlockEntities.clear();
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        this.passiveScanTimer++;
        this.renderTimer++;
        
        // ONLY scan when chunks load naturally (NO forced refreshes)
        if (this.passiveScanTimer >= 20 && silentMode.getValue()) { // Slower = less detection
            this.passiveScanFromLoadedChunks();
            this.passiveScanTimer = 0;
        }
        
        // Update distances for all found block entities every render
        if (this.renderTimer >= 5) {
            this.updateDistances();
            this.renderTimer = 0;
        }
    }
    
    @EventListener
    public void onChunkLoad(ChunkEvent.Load event) {
        if (mc.world == null || silentMode.getValue()) return;
        
        // Scan chunks as they load naturally - THIS BYPASSES DETECTION
        WorldChunk chunk = event.getChunk();
        if (chunk != null && !scannedChunks.contains(chunk.getPos().toLong())) {
            scannedChunks.add(chunk.getPos().toLong());
            this.scanChunkPassive(chunk);
        }
    }
    
    private void passiveScanFromLoadedChunks() {
        if (mc.world == null || mc.player == null) return;
        
        int radius = (int) this.range.getValue();
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Get currently loaded chunks (these are legitimately loaded by the game)
        Iterable<WorldChunk> loadedChunks = mc.world.getChunkManager().getLoadedChunks();
        
        for (WorldChunk chunk : loadedChunks) {
            if (chunk == null) continue;
            
            // Check if chunk is within range
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            int playerChunkX = playerPos.getX() >> 4;
            int playerChunkZ = playerPos.getZ() >> 4;
            
            int dx = Math.abs(chunkX - playerChunkX);
            int dz = Math.abs(chunkZ - playerChunkZ);
            int chunkDist = dx * dx + dz * dz;
            int maxChunkDist = (radius / 16) + 1;
            
            if (chunkDist <= maxChunkDist * maxChunkDist) {
                scanChunkPassive(chunk);
            }
        }
    }
    
    private void scanChunkPassive(WorldChunk chunk) {
        if (mc.player == null) return;
        
        int radius = (int) this.range.getValue();
        BlockPos playerPos = mc.player.getBlockPos();
        
        // Use chunk's block entity map directly (no extra packets sent)
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
        
        for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity be = entry.getValue();
            
            if (be == null) continue;
            
            double distance = Math.sqrt(playerPos.getSquaredDistance(pos));
            if (distance > radius) continue;
            
            Color color = getBlockEntityColor(be);
            if (color == null) continue;
            
            // Only update if position changed or new
            if (!foundBlockEntities.containsKey(pos)) {
                this.foundBlockEntities.put(pos, new BlockEntityInfo(pos, be, color, distance, System.currentTimeMillis()));
            }
        }
    }
    
    private void updateDistances() {
        if (mc.player == null) return;
        
        BlockPos playerPos = mc.player.getBlockPos();
        
        for (Map.Entry<BlockPos, BlockEntityInfo> entry : foundBlockEntities.entrySet()) {
            BlockEntityInfo info = entry.getValue();
            double newDistance = Math.sqrt(playerPos.getSquaredDistance(info.getPos()));
            
            // Remove if out of range
            if (newDistance > this.range.getValue() + 10) {
                foundBlockEntities.remove(entry.getKey());
            } else {
                // Update distance in the info object (need to recreate or add setter)
                BlockEntityInfo updated = new BlockEntityInfo(info.getPos(), info.getEntity(), info.getColor(), newDistance, info.getFirstSeen());
                foundBlockEntities.put(entry.getKey(), updated);
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
        if (be instanceof EndPortalBlockEntity && this.highlightSpawners.getValue()) return SPAWNER_COLOR;
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
            
            // Fade older entries (older than 30 seconds)
            if (silentMode.getValue() && System.currentTimeMillis() - info.getFirstSeen() > 30000) {
                int alpha = Math.max(50, color.getAlpha() - 50);
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            }
            
            float x1 = pos.getX() + 0.1f;
            float y1 = pos.getY() + 0.1f;
            float z1 = pos.getZ() + 0.1f;
            float x2 = pos.getX() + 0.9f;
            float y2 = pos.getY() + 0.9f;
            float z2 = pos.getZ() + 0.9f;
            
            // Filled box
            RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, color);
            
            // Draw outline for better visibility
            RenderUtils.renderOutlineBox(matrices, x1, y1, z1, x2, y2, z2, 2.0f, Color.WHITE);
            
            // Draw distance text - FIXED for DonutSMP
            if (this.showDistance.getValue()) {
                String distText = (int)info.getDistance() + "m";
                
                matrices.push();
                matrices.translate(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.scale(0.025f, 0.025f, 0.025f);
                
                // Use the client's TextRenderer properly
                net.minecraft.client.font.TextRenderer textRenderer = mc.textRenderer;
                int textWidth = textRenderer.getWidth(distText);
                int x = -textWidth / 2;
                int y = 0;
                
                // Draw background
                matrices.push();
                matrices.translate(0, 0, 0);
                matrices.scale(1.0f, 1.0f, 1.0f);
                matrices.pop();
                
                // Draw text using the proper method for your version
                textRenderer.draw(distText, x, y, Color.WHITE.getRGB(), false, matrices.peek().getPositionMatrix(), matrices.peek().getNormalMatrix(), false, 0, 15728880);
                
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
