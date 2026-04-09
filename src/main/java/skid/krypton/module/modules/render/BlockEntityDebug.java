package skid.krypton.module.modules.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Text;
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
    
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 10, 500, 200, 10);
    private final BooleanSetting showTracers = new BooleanSetting(EncryptedString.of("Tracers"), true);
    private final BooleanSetting showBoxes = new BooleanSetting(EncryptedString.of("Show Boxes"), true);
    private final BooleanSetting notifyBase = new BooleanSetting(EncryptedString.of("Notify on Sound"), true);
    
    private static final Color HIDDEN_BASE = new Color(255, 0, 0, 200);
    private static final Color NORMAL_BASE = new Color(255, 200, 50, 200);
    private static final Color PLAYER_SOUND = new Color(0, 255, 0, 200);
    
    private final Map<BlockPos, BaseNode> discoveredBases = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> soundCount = new ConcurrentHashMap<>();
    private final Set<BlockPos> notifiedBases = new HashSet<>();
    
    public BlockEntityDebug() {
        super(EncryptedString.of("Base Finder"), 
              EncryptedString.of("Finds hidden bases below Y20 via sound packets"), 
              0, Category.RENDER);
        this.addSettings(range, showTracers, showBoxes, notifyBase);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        discoveredBases.clear();
        soundCount.clear();
        notifiedBases.clear();
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§a[Base Finder] §fSound bypass active - detecting bases below Y20"), false);
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        discoveredBases.clear();
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§c[Base Finder] §fDisabled"), false);
        }
    }
    
    @EventListener
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.packet instanceof PlaySoundS2CPacket packet)) return;
        
        try {
            // FIXED: Different way to get sound path for your MC version
            String soundPath = packet.getSound().value().toString();
            BlockPos soundPos = new BlockPos((int)packet.getX(), (int)packet.getY(), (int)packet.getZ());
            
            if (mc.player == null) return;
            
            // Manual distance calculation
            double dx = mc.player.getX() - soundPos.getX();
            double dy = mc.player.getY() - soundPos.getY();
            double dz = mc.player.getZ() - soundPos.getZ();
            double distSq = dx*dx + dy*dy + dz*dz;
            double rangeSq = Math.pow(range.getValue(), 2);
            
            if (distSq > rangeSq) return;
            
            // Check if this is BELOW Y20 (the hidden zone!)
            boolean isHiddenBase = soundPos.getY() <= 20;
            boolean isDeepslate = soundPos.getY() < 0;
            
            Color color = null;
            String soundType = "";
            int priority = 0;
            
            // Categorize sounds using string contains
            if (soundPath.contains("chest") || soundPath.contains("shulker") || soundPath.contains("barrel")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Chest/Container";
                priority = 10;
            }
            else if (soundPath.contains("door") || soundPath.contains("piston") || soundPath.contains("gate")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Door/Piston";
                priority = 8;
            }
            else if (soundPath.contains("furnace") || soundPath.contains("blast_furnace")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Furnace";
                priority = 7;
            }
            else if (soundPath.contains("beacon")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Beacon";
                priority = 9;
            }
            else if (soundPath.contains("player") || soundPath.contains("step") || soundPath.contains("swim") || soundPath.contains("walk")) {
                color = PLAYER_SOUND;
                soundType = "Player Activity";
                priority = 10;
            }
            else if (soundPath.contains("anvil") || soundPath.contains("grindstone")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Anvil";
                priority = 6;
            }
            else if (soundPath.contains("enchant")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Enchanting";
                priority = 7;
            }
            else if (soundPath.contains("portal")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Portal";
                priority = 8;
            }
            else if (soundPath.contains("minecart") || soundPath.contains("rail")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Minecart";
                priority = 5;
            }
            else if (soundPath.contains("water") || soundPath.contains("lava")) {
                color = isHiddenBase ? HIDDEN_BASE : NORMAL_BASE;
                soundType = "Liquid";
                priority = 4;
            }
            
            if (color != null) {
                // Count multiple sounds at same location
                soundCount.put(soundPos, soundCount.getOrDefault(soundPos, 0) + 1);
                int count = soundCount.get(soundPos);
                
                // Increase visibility for multiple sounds
                if (count >= 3) {
                    color = HIDDEN_BASE;
                }
                
                // Add or update
                BaseNode existing = discoveredBases.get(soundPos);
                if (existing == null || existing.priority < priority) {
                    discoveredBases.put(soundPos, new BaseNode(soundPos, color, System.currentTimeMillis(), count, isHiddenBase, isDeepslate, soundType, priority));
                }
                
                // NOTIFY for hidden bases (below Y20)
                if (notifyBase.getValue() && isHiddenBase && !notifiedBases.contains(soundPos) && mc.player != null) {
                    notifiedBases.add(soundPos);
                    
                    mc.player.sendMessage(Text.literal(""), false);
                    mc.player.sendMessage(Text.literal("§c§l═══════════════════════════════════════"), false);
                    mc.player.sendMessage(Text.literal("§c§l🔴 HIDDEN BASE DETECTED BELOW Y20! 🔴"), false);
                    mc.player.sendMessage(Text.literal("§fLocation: §eX: " + soundPos.getX() + " Y: " + soundPos.getY() + " Z: " + soundPos.getZ()), false);
                    mc.player.sendMessage(Text.literal("§fLayer: " + (isDeepslate ? "§8Deepslate (Y < 0)" : "§7Stone Level (0-20)")), false);
                    mc.player.sendMessage(Text.literal("§fSound: §e" + soundType), false);
                    mc.player.sendMessage(Text.literal("§fSound Count: §e" + count), false);
                    mc.player.sendMessage(Text.literal("§c§l═══════════════════════════════════════"), false);
                    mc.player.sendMessage(Text.literal(""), false);
                    
                    // Play alert sound
                    if (mc.player != null) {
                        mc.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors silently
        }
    }
    
    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        
        // Cleanup old nodes (keep for 2 minutes)
        long cutoff = System.currentTimeMillis() - 120000;
        discoveredBases.entrySet().removeIf(entry -> {
            if (entry.getValue().timestamp < cutoff) {
                soundCount.remove(entry.getKey());
                notifiedBases.remove(entry.getKey());
                return true;
            }
            return false;
        });
        
        // Remove out of range
        double rangeSq = Math.pow(range.getValue(), 2);
        discoveredBases.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            double dx = mc.player.getX() - pos.getX();
            double dy = mc.player.getY() - pos.getY();
            double dz = mc.player.getZ() - pos.getZ();
            if ((dx*dx + dy*dy + dz*dz) > rangeSq) {
                soundCount.remove(pos);
                notifiedBases.remove(pos);
                return true;
            }
            return false;
        });
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
            long age = System.currentTimeMillis() - node.timestamp;
            
            // Pulse effect for recent sounds
            int alpha;
            if (age < 5000) {
                float pulse = 0.6f + 0.4f * (float)Math.sin(age / 200.0);
                alpha = 150 + (int)(100 * pulse);
            } else {
                alpha = Math.max(80, 150 - (int)(age / 2000));
            }
            
            // Make hidden bases more visible
            if (node.isHidden) {
                alpha = Math.min(255, alpha + 50);
            }
            
            Color renderColor = new Color(node.color.getRed(), node.color.getGreen(), node.color.getBlue(), Math.min(255, alpha));
            
            if (showBoxes.getValue()) {
                float x1 = pos.getX() + 0.1f;
                float y1 = pos.getY() + 0.1f;
                float z1 = pos.getZ() + 0.1f;
                float x2 = pos.getX() + 0.9f;
                float y2 = pos.getY() + 0.9f;
                float z2 = pos.getZ() + 0.9f;
                
                // Glow effect for hidden bases
                if (node.isHidden) {
                    RenderUtils.renderFilledBox(matrices, x1 - 0.15f, y1 - 0.15f, z1 - 0.15f, 
                                                x2 + 0.15f, y2 + 0.15f, z2 + 0.15f, 
                                                new Color(255, 0, 0, 60));
                }
                
                RenderUtils.renderFilledBox(matrices, x1, y1, z1, x2, y2, z2, renderColor);
            }
            
            if (showTracers.getValue() && mc.player != null) {
                Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                Color tracerColor = node.isHidden ? HIDDEN_BASE : renderColor;
                RenderUtils.renderLine(matrices, tracerColor, mc.player.getEyePos(), blockCenter);
            }
        }
        
        matrices.pop();
    }
    
    private static class BaseNode {
        BlockPos pos;
        Color color;
        long timestamp;
        int soundCount;
        boolean isHidden;
        boolean isDeepslate;
        String soundType;
        int priority;
        
        BaseNode(BlockPos pos, Color color, long timestamp, int soundCount, boolean isHidden, boolean isDeepslate, String soundType, int priority) {
            this.pos = pos;
            this.color = color;
            this.timestamp = timestamp;
            this.soundCount = soundCount;
            this.isHidden = isHidden;
            this.isDeepslate = isDeepslate;
            this.soundType = soundType;
            this.priority = priority;
        }
    }
}
