package skid.krypton.module.modules.render;

import net.minecraft.block.entity.*;
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
    
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 10, 500, 500, 10);
    private final BooleanSetting soundBypass = new BooleanSetting(EncryptedString.of("Sound Bypass (Ultimate)"), true);
    private final BooleanSetting showTracers = new BooleanSetting(EncryptedString.of("Tracers"), true);
    private final BooleanSetting showBoxes = new BooleanSetting(EncryptedString.of("Show Boxes"), true);
    private final BooleanSetting markActiveBases = new BooleanSetting(EncryptedString.of("Mark Active Bases"), true);
    
    // Colors - Brighter for active bases
    private static final Color ACTIVE_BASE = new Color(255, 0, 0, 255);      // Bright Red - Active right now
    private static final Color CHEST_SOUND = new Color(255, 200, 50, 200);    // Orange - Chest activity
    private static final Color DOOR_SOUND = new Color(255, 100, 0, 200);       // Dark Orange - Door/Piston
    private static final Color FURNACE_SOUND = new Color(150, 150, 150, 200);  // Gray - Furnace
    private static final Color BEACON_SOUND = new Color(50, 200, 255, 200);    // Cyan - Beacon
    private static final Color PLAYER_SOUND = new Color(0, 255, 0, 200);       // Green - Player activity
    
    // Cache with priority (sound discoveries are HIGH priority)
    private final Map<BlockPos, BaseNode> discoveredBases = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> soundCount = new ConcurrentHashMap<>();
    private int scanTimer = 0;
    
    public BlockEntityDebug() {
        super(EncryptedString.of("Base Finder"), 
              EncryptedString.of("ULTIMATE DONUTSMP BYPASS - Sound packets cannot be blocked"), 
              -1, Category.RENDER);
        this.addSettings(range, soundBypass, showTracers, showBoxes, markActiveBases);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        discoveredBases.clear();
        soundCount.clear();
        scanTimer = 0;
    }
    
    // THE ULTIMATE BYPASS: Sound packets
    // Servers CANNOT block these without breaking the game
    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!soundBypass.getValue()) return;
        if (!(event.getPacket() instanceof PlaySoundS2CPacket packet)) return;
        
        String soundPath = packet.getSound().value().id().getPath();
        BlockPos soundPos = new BlockPos((int)packet.getX(), (int)packet.getY(), (int)packet.getZ());
        
        // Check if within our range (can extend beyond render distance!)
        if (mc.player != null && mc.player.getSquaredDistance(soundPos.toCenterPos()) > Math.pow(range.getValue(), 2)) {
            return; // Out of our configured range
        }
        
        Color color = null;
        String type = null;
        int priority = 0;
        
        // Categorize sounds (all indicate base activity)
        if (soundPath.contains("chest.open") || soundPath.contains("chest.close")) {
            color = CHEST_SOUND;
            type = "§6Chest Activity";  // Gold color in chat
            priority = 5;
        }
        else if (soundPath.contains("shulker_box.open") || soundPath.contains("shulker_box.close")) {
            color = CHEST_SOUND;
            type = "§5Shulker Activity";
            priority = 5;
        }
        else if (soundPath.contains("piston") || soundPath.contains("door") || soundPath.contains("gate")) {
            color = DOOR_SOUND;
            type = "§6Redstone/Piston";
            priority = 4;
        }
        else if (soundPath.contains("furnace") || soundPath.contains("blast_furnace") || soundPath.contains("smoker")) {
            color = FURNACE_SOUND;
            type = "§7Furnace Activity";
            priority = 3;
        }
        else if (soundPath.contains("beacon") || soundPath.contains("conduit")) {
            color = BEACON_SOUND;
            type = "§bBeacon/Conduit";
            priority = 4;
        }
        else if (soundPath.contains("entity.player") || soundPath.contains("entity.experience")) {
            color = PLAYER_SOUND;
            type = "§aPlayer Activity";
            priority = 5;
        }
        else if (soundPath.contains("block.anvil") || soundPath.contains("block.grindstone")) {
            color = DOOR_SOUND;
            type = "§6Anvil/Repair";
            priority = 3;
        }
        else if (soundPath.contains("block.barrel.open") || soundPath.contains("block.barrel.close")) {
            color = CHEST_SOUND;
            type = "§6Barrel Activity";
            priority = 4;
        }
        
        if (color != null) {
            // Count multiple sounds at same location
            soundCount.put(soundPos, soundCount.getOrDefault(soundPos, 0) + 1);
            int soundCountValue = soundCount.get(soundPos);
            
            // Higher priority for locations with multiple sounds
            if (soundCountValue > 3) {
                color = ACTIVE_BASE;
                type = "§c§lHIGH ACTIVITY BASE!";
                priority = 10;
            }
            
            // Add or update the discovered base
            BaseNode existing = discoveredBases.get(soundPos);
            if (existing == null || existing.priority() < priority) {
                discoveredBases.put(soundPos, new BaseNode(soundPos, color, type, System.currentTimeMillis(), priority, soundCountValue));
            }
            
            // Optional: Send chat notification for high-activity bases
            if (markActiveBases.getValue() && soundCountValue >= 3) {
                sendMessage("§c[Base Finder] §fHigh activity detected at §c" + soundPos.getX() + " " + soundPos.getY() + " " + soundPos.getZ() + " §f(" + soundCountValue + " sounds)");
            }
        }
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        
        scanTimer++;
        
        // Backup scan for tile entities (only works for loaded chunks)
        if (scanTimer >= 20) {
            scanTileEntities();
            scanTimer = 0;
        }
        
        // Cleanup old nodes (keep for 2 minutes)
        long cutoff = System.currentTimeMillis() - 120000;
        discoveredBases.entrySet().removeIf(entry -> {
            if (entry.getValue().timestamp() < cutoff) {
                soundCount.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        // Remove out of range
        double rangeSq = Math.pow(range.getValue(), 2);
        discoveredBases.entrySet().removeIf(entry -> 
            mc.player.getSquaredDistance(entry.getKey().toCenterPos()) > rangeSq);
    }
    
    private void scanTileEntities() {
        if (mc.world == null) return;
        
        try {
            Iterable<WorldChunk> chunks = mc.world.getChunkManager().getLoadedChunks();
            for (WorldChunk chunk : chunks) {
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    if (be instanceof ChestBlockEntity || be instanceof ShulkerBoxBlockEntity || be instanceof MobSpawnerBlockEntity) {
                        // Only add if we haven't found it via sound yet (sounds are more reliable)
                        discoveredBases.putIfAbsent(entry.getKey(), 
                            new BaseNode(entry.getKey(), CHEST_SOUND, "Static Chest", System.currentTimeMillis(), 1, 0));
                    }
                }
            }
        } catch (Exception e) {
            // Chunk access might be blocked - that's fine, sounds still work!
        }
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (discoveredBases.isEmpty()) return;
        
        MatrixStack matrices = event.matrixStack;
        Vec3d cameraPos = RenderUtils.getCameraPos();
        
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (BaseNode node : discoveredBases.values()) {
            BlockPos pos = node.pos();
            Color color = node.color();
            long age = System.currentTimeMillis() - node.timestamp();
            
            // Pulse effect for recent activity (last 10 seconds)
            int alpha;
            if (age < 10000) {
                // Pulsing animation for active bases
                float pulse = 0.5f + 0.5f * (float)Math.sin(age / 200.0);
                alpha = 150 + (int)(100 * pulse);
            } else {
                alpha = Math.max(80, 150 - (int)(age / 2000));
            }
            
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
            
            if (showBoxes.getValue()) {
                float x1 = pos.getX() + 0.1f;
                float y1 = pos.getY() + 0.1f;
                float z1 = pos.getZ() + 0.1f;
                float x2 = pos.getX() + 0.9f;
                float y2 = pos.getY() + 0.9f;
                float z2 = pos.getZ() + 0.9f;
                
                // Thicker outline for high-activity bases
                if (node.priority() >= 5) {
                    // Draw outer glow
                    RenderUtils.renderFilledBox(matrices, x1 - 0.1f, y1 - 0.1f, z1 - 0.1f, x2 + 0.1f, y2 + 0.1f, z2 + 0.1f, new Color(255, 0, 0, 100));
                }
                
                RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, renderColor);
            }
            
            if (showTracers.getValue()) {
                Vec3d blockCenter = pos.toCenterPos();
                Color tracerColor = node.priority() >= 5 ? ACTIVE_BASE : renderColor;
                RenderUtils.renderLine(matrices, tracerColor, mc.player.getEyePos(), blockCenter);
            }
        }
        
        matrices.pop();
    }
    
    private record BaseNode(BlockPos pos, Color color, String type, long timestamp, int priority, int soundCount) {}
}
