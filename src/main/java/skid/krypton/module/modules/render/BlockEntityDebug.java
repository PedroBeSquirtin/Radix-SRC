package skid.krypton.module.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import skid.krypton.event.EventListener;
import skid.krypton.event.events.PacketReceiveEvent;
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
    private final BooleanSetting showTracers = new BooleanSetting(EncryptedString.of("Tracers"), true);
    private final BooleanSetting showBoxes = new BooleanSetting(EncryptedString.of("Show Boxes"), true);
    private final BooleanSetting notifyNewBase = new BooleanSetting(EncryptedString.of("Notify New Base"), true);
    
    // Colors based on Y-level (BELOW 20 = hidden base!)
    private static final Color HIDDEN_BASE = new Color(255, 0, 0, 255);      // Bright Red - Below Y20
    private static final Color NORMAL_BASE = new Color(255, 200, 50, 200);    // Orange - Above Y20
    private static final Color PLAYER_ACTIVITY = new Color(0, 255, 0, 200);   // Green - Player sounds
    
    // Cache
    private final Map<BlockPos, BaseNode> discoveredBases = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> soundCount = new ConcurrentHashMap<>();
    private final Set<BlockPos> notifiedBases = new HashSet<>();
    
    public BlockEntityDebug() {
        super(EncryptedString.of("Base Finder"), 
              EncryptedString.of("FINDS BASES BELOW Y20 - Sound packets bypass chunk limits"), 
              -1, Category.RENDER);
        this.addSettings(range, showTracers, showBoxes, notifyNewBase);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        discoveredBases.clear();
        soundCount.clear();
        notifiedBases.clear();
        sendMessage("§a[Base Finder] §fSound bypass active - Will detect hidden bases below Y20!");
    }
    
    // THE BYPASS: Sound packets work at ANY Y-level!
    @EventListener
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.packet instanceof PlaySoundS2CPacket packet)) return;
        
        String soundPath = packet.getSound().value().id().getPath();
        BlockPos soundPos = new BlockPos((int)packet.getX(), (int)packet.getY(), (int)packet.getZ());
        
        // Check range
        if (mc.player != null && mc.player.getSquaredDistance(soundPos.toCenterPos()) > Math.pow(range.getValue(), 2)) {
            return;
        }
        
        // Check if this is BELOW Y20 (the hidden zone where bases are!)
        boolean isHiddenBase = soundPos.getY() <= 20;
        boolean isDeepslate = soundPos.getY() < 0;
        
        Color color = null;
        String type = null;
        int priority = 0;
        
        // Categorize sounds
        if (soundPath.contains("chest.open") || soundPath.contains("chest.close") ||
            soundPath.contains("shulker_box.open") || soundPath.contains("barrel.open")) {
            color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
            type = isHiddenBase ? "§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f(Chest)" : "Chest Activity";
            priority = 10;
        }
        else if (soundPath.contains("piston") || soundPath.contains("door") || soundPath.contains("gate")) {
            color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
            type = isHiddenBase ? "§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f(Redstone)" : "Redstone Activity";
            priority = 8;
        }
        else if (soundPath.contains("furnace") || soundPath.contains("blast_furnace")) {
            color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
            type = isHiddenBase ? "§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f(Furnace)" : "Furnace Activity";
            priority = 7;
        }
        else if (soundPath.contains("beacon") || soundPath.contains("conduit")) {
            color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
            type = isHiddenBase ? "§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f(Beacon)" : "Beacon Activity";
            priority = 9;
        }
        else if (soundPath.contains("entity.player") || soundPath.contains("entity.experience")) {
            color = PLAYER_ACTIVITY;
            type = isHiddenBase ? "§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f(Player Activity)" : "§aPlayer Activity nearby";
            priority = 10;
        }
        else if (soundPath.contains("block.anvil") || soundPath.contains("block.grindstone")) {
            color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
            type = isHiddenBase ? "§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f(Anvil)" : "Anvil Use";
            priority = 6;
        }
        else if (soundPath.contains("block.enchantment_table")) {
            color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
            type = isHiddenBase ? "§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f(Enchanting)" : "Enchanting";
            priority = 7;
        }
        
        if (color != null) {
            // Count multiple sounds
            soundCount.put(soundPos, soundCount.getOrDefault(soundPos, 0) + 1);
            int count = soundCount.get(soundPos);
            
            // Higher priority for multiple sounds
            if (count >= 3) {
                color = HIDDEN_BASE;
                type = "§c§l⚠⚠ HIGH ACTIVITY HIDDEN BASE! ⚠⚠";
                priority = 10;
            }
            
            // Add or update
            BaseNode existing = discoveredBases.get(soundPos);
            if (existing == null || existing.priority < priority) {
                discoveredBases.put(soundPos, new BaseNode(soundPos, color, type, System.currentTimeMillis(), priority, count, isHiddenBase, isDeepslate));
                
                // NOTIFY for hidden bases below Y20!
                if (notifyNewBase.getValue() && isHiddenBase && !notifiedBases.contains(soundPos)) {
                    notifiedBases.add(soundPos);
                    sendMessage("");
                    sendMessage("§c§l═══════════════════════════════════════");
                    sendMessage("§c§l🔴 HIDDEN BASE DETECTED BELOW Y20! 🔴");
                    sendMessage("§fLocation: §eX: " + soundPos.getX() + " Y: " + soundPos.getY() + " Z: " + soundPos.getZ());
                    sendMessage("§fLayer: " + (isDeepslate ? "§8Deepslate (Y < 0)" : "§7Stone Level (0-20)"));
                    sendMessage("§fReason: §e" + type.replace("§c§l⚠ HIDDEN BASE BELOW Y20! ⚠ §f", ""));
                    sendMessage("§fSound Count: §e" + count);
                    sendMessage("§c§l═══════════════════════════════════════");
                    sendMessage("");
                    
                    // Play sound alert
                    if (mc.player != null) {
                        mc.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                }
            }
        }
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        
        // Cleanup old nodes (keep for 5 minutes)
        long cutoff = System.currentTimeMillis() - 300000;
        discoveredBases.entrySet().removeIf(entry -> {
            if (entry.getValue().timestamp < cutoff) {
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
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (discoveredBases.isEmpty()) return;
        
        MatrixStack matrices = event.matrixStack;
        Vec3d cameraPos = RenderUtils.getCameraPos();
        
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        
        for (BaseNode node : discoveredBases.values()) {
            BlockPos pos = node.pos;
            Color color = node.color;
            long age = System.currentTimeMillis() - node.timestamp;
            
            // Calculate alpha (pulse for recent activity)
            int alpha;
            if (age < 10000) {
                float pulse = 0.5f + 0.5f * (float)Math.sin(age / 150.0);
                alpha = 150 + (int)(105 * pulse);
            } else {
                alpha = Math.max(100, 200 - (int)(age / 3000));
            }
            
            // Make hidden bases (below Y20) more visible
            if (node.isHiddenBase) {
                alpha = Math.min(255, alpha + 50);
            }
            
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
            
            if (showBoxes.getValue()) {
                float x1 = pos.getX() + 0.1f;
                float y1 = pos.getY() + 0.1f;
                float z1 = pos.getZ() + 0.1f;
                float x2 = pos.getX() + 0.9f;
                float y2 = pos.getY() + 0.9f;
                float z2 = pos.getZ() + 0.9f;
                
                // Glow effect for hidden bases (below Y20)
                if (node.isHiddenBase) {
                    // Outer glow
                    RenderUtils.renderFilledBox(matrices, x1 - 0.2f, y1 - 0.2f, z1 - 0.2f, 
                                                x2 + 0.2f, y2 + 0.2f, z2 + 0.2f, 
                                                new Color(255, 0, 0, 60));
                    // Inner glow
                    RenderUtils.renderFilledBox(matrices, x1 - 0.1f, y1 - 0.1f, z1 - 0.1f, 
                                                x2 + 0.1f, y2 + 0.1f, z2 + 0.1f, 
                                                new Color(255, 0, 0, 120));
                }
                
                RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, renderColor);
            }
            
            if (showTracers.getValue()) {
                Vec3d blockCenter = pos.toCenterPos();
                Color tracerColor = node.isHiddenBase ? HIDDEN_BASE : renderColor;
                RenderUtils.renderLine(matrices, tracerColor, mc.player.getEyePos(), blockCenter);
            }
        }
        
        matrices.pop();
    }
    
    private static class BaseNode {
        public final BlockPos pos;
        public final Color color;
        public final String type;
        public final long timestamp;
        public final int priority;
        public final int soundCount;
        public final boolean isHiddenBase;
        public final boolean isDeepslate;
        
        public BaseNode(BlockPos pos, Color color, String type, long timestamp, int priority, int soundCount, boolean isHiddenBase, boolean isDeepslate) {
            this.pos = pos;
            this.color = color;
            this.type = type;
            this.timestamp = timestamp;
            this.priority = priority;
            this.soundCount = soundCount;
            this.isHiddenBase = isHiddenBase;
            this.isDeepslate = isDeepslate;
        }
    }
}
