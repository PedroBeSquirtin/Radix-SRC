package skid.krypton.module.modules.render;

import net.minecraft.block.entity.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.PacketEvent;
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
    
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 10, 500, 200, 10);
    private final BooleanSetting soundBypass = new BooleanSetting(EncryptedString.of("Sound Bypass"), true);
    private final BooleanSetting entityDetect = new BooleanSetting(EncryptedString.of("Entity Detect"), true);
    private final BooleanSetting showTracers = new BooleanSetting(EncryptedString.of("Tracers"), true);
    private final BooleanSetting showBoxes = new BooleanSetting(EncryptedString.of("Show Boxes"), true);
    
    // Colors for different detection methods
    private static final Color SOUND_COLOR = new Color(255, 50, 50, 200);      // Red - Sound detected (high confidence)
    private static final Color CHEST_COLOR = new Color(255, 200, 50, 180);      // Orange - Chest
    private static final Color SHULKER_COLOR = new Color(200, 50, 255, 180);     // Purple - Shulker
    private static final Color SPAWNER_COLOR = new Color(255, 50, 50, 180);      // Red - Spawner
    private static final Color ITEM_FRAME_COLOR = new Color(50, 200, 255, 180);  // Blue - Item Frame
    private static final Color UNKNOWN_COLOR = new Color(255, 255, 255, 150);     // White - Unknown
    
    // Cache
    private final Map<BlockPos, BaseNode> discoveredBases = new ConcurrentHashMap<>();
    private int scanTimer = 0;
    
    public BlockEntityDebug() {
        super(EncryptedString.of("Base Finder"), 
              EncryptedString.of("Ultimate DonutSMP Bypass - Uses Sound Packets"), 
              -1, Category.RENDER);
        this.addSettings(range, soundBypass, entityDetect, showTracers, showBoxes);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        discoveredBases.clear();
        scanTimer = 0;
    }
    
    // ULTIMATE BYPASS: Sound packets (server CANNOT hide these)
    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!soundBypass.getValue()) return;
        if (!(event.getPacket() instanceof PlaySoundS2CPacket packet)) return;
        
        String soundPath = packet.getSound().value().id().getPath();
        BlockPos soundPos = new BlockPos((int)packet.getX(), (int)packet.getY(), (int)packet.getZ());
        
        // Base activity sounds
        boolean isBaseSound = false;
        Color color = SOUND_COLOR;
        String type = "Unknown";
        
        if (soundPath.contains("chest.open") || soundPath.contains("chest.close")) {
            isBaseSound = true;
            type = "Chest Action";
            color = new Color(255, 200, 50, 255);
        }
        else if (soundPath.contains("shulker_box.open") || soundPath.contains("shulker_box.close")) {
            isBaseSound = true;
            type = "Shulker Action";
            color = new Color(200, 50, 255, 255);
        }
        else if (soundPath.contains("piston") || soundPath.contains("door")) {
            isBaseSound = true;
            type = "Redstone Activity";
            color = new Color(255, 100, 0, 255);
        }
        else if (soundPath.contains("furnace") || soundPath.contains("blast")) {
            isBaseSound = true;
            type = "Furnace Activity";
            color = new Color(150, 150, 150, 255);
        }
        else if (soundPath.contains("block.beacon")) {
            isBaseSound = true;
            type = "Beacon Active";
            color = new Color(50, 200, 255, 255);
        }
        else if (soundPath.contains("entity.experience") || soundPath.contains("entity.player")) {
            isBaseSound = true;
            type = "Player Activity";
            color = new Color(0, 255, 0, 255);
        }
        
        if (isBaseSound) {
            discoveredBases.put(soundPos, new BaseNode(soundPos, color, type, System.currentTimeMillis(), "SOUND"));
        }
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        scanTimer++;
        
        if (scanTimer >= 10) {
            // Entity-based detection (backup method)
            if (entityDetect.getValue()) {
                scanEntities();
                scanItemFrames();
                scanTileEntities();
            }
            scanTimer = 0;
        }
        
        // Cleanup out-of-range nodes
        double rangeSq = Math.pow(range.getValue(), 2);
        discoveredBases.entrySet().removeIf(entry -> 
            mc.player.getSquaredDistance(entry.getKey().toCenterPos()) > rangeSq);
    }
    
    private void scanEntities() {
        if (mc.world == null) return;
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemEntity item) {
                String name = item.getStack().getName().getString().toLowerCase();
                if (name.contains("diamond") || name.contains("netherite") || name.contains("elytra")) {
                    BlockPos pos = entity.getBlockPos();
                    discoveredBases.putIfAbsent(pos, new BaseNode(pos, CHEST_COLOR, "Valuable Drop", System.currentTimeMillis(), "ENTITY"));
                }
            }
        }
    }
    
    private void scanItemFrames() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity frame && !frame.getStack().isEmpty()) {
                BlockPos pos = frame.getBlockPos();
                discoveredBases.putIfAbsent(pos, new BaseNode(pos, ITEM_FRAME_COLOR, "Item Frame", System.currentTimeMillis(), "ENTITY"));
            }
        }
    }
    
    private void scanTileEntities() {
        Iterable<WorldChunk> chunks = mc.world.getChunkManager().getLoadedChunks();
        for (WorldChunk chunk : chunks) {
            if (chunk == null) continue;
            for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                BlockEntity be = entry.getValue();
                Color color = null;
                String type = null;
                
                if (be instanceof ChestBlockEntity) {
                    color = CHEST_COLOR;
                    type = "Chest";
                } else if (be instanceof ShulkerBoxBlockEntity) {
                    color = SHULKER_COLOR;
                    type = "Shulker";
                } else if (be instanceof MobSpawnerBlockEntity) {
                    color = SPAWNER_COLOR;
                    type = "Spawner";
                } else {
                    continue;
                }
                
                discoveredBases.putIfAbsent(entry.getKey(), 
                    new BaseNode(entry.getKey(), color, type, System.currentTimeMillis(), "TILE"));
            }
        }
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (discoveredBases.isEmpty()) return;
        
        Camera camera = RenderUtils.getCamera();
        if (camera == null) return;
        
        MatrixStack matrices = event.matrixStack;
        Vec3d cameraPos = RenderUtils.getCameraPos();
        
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (BaseNode node : discoveredBases.values()) {
            BlockPos pos = node.pos();
            Color color = node.color();
            
            // Pulse animation for sound-detected bases (they're active right now!)
            long age = System.currentTimeMillis() - node.timestamp();
            boolean isSound = node.source().equals("SOUND");
            
            int alpha;
            if (isSound && age < 5000) {
                // Pulsing effect for recent sounds
                alpha = 180 + (int)(75 * Math.sin(age / 200.0));
            } else {
                alpha = Math.max(80, 200 - (int)(age / 5000));
            }
            
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
            
            if (showBoxes.getValue()) {
                float x1 = pos.getX() + 0.1f;
                float y1 = pos.getY() + 0.1f;
                float z1 = pos.getZ() + 0.1f;
                float x2 = pos.getX() + 0.9f;
                float y2 = pos.getY() + 0.9f;
                float z2 = pos.getZ() + 0.9f;
                
                RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, renderColor);
            }
            
            if (showTracers.getValue()) {
                Vec3d blockCenter = pos.toCenterPos();
                RenderUtils.renderLine(matrices, renderColor, mc.player.getEyePos(), blockCenter);
            }
        }
        
        matrices.pop();
    }
    
    private record BaseNode(BlockPos pos, Color color, String type, long timestamp, String source) {}
}
