package com.tutien.uyap;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * UyApSystem - Hệ thống Uy Áp Cảnh Giới Rework.
 *
 * Tính năng:
 * 1. Chủ động (/uyap): Phóng thích kình lực uy áp cảnh giới trong bán kính 10m.
 *    - Hồi chiêu 20s, tiêu hao Linh Khí theo cảnh giới (tối đa 15% Linh Khí hiện tại / rank).
 *    - Hiệu ứng Nghe & Nhìn cực mạnh: ENTITY_WARDEN_SONIC_BOOM, ENTITY_LIGHTNING_BOLT_THUNDER,
 *      Particle.SONIC_BOOM, FLASH, DUST sóng xung kích 360 độ và SWEEP_ATTACK.
 *    - Tác động Vật lý & Khống chế: Hất tung (Y >= 0.75) và đẩy lùi mạnh mẽ ra xa tâm.
 *    - Áp hiệu ứng: Weakness II, Slowness II, Mining Fatigue I.
 *    - Nếu đối phương có cảnh giới thấp hơn tu sĩ: thêm Blindness và Nausea.
 * 2. Lệnh tiện lợi:
 *    - /uyap : Kích hoạt phóng thích uy áp.
 *    - /uyap toggle : Bật / tắt chế độ nhận áp chế uy áp thụ động.
 *    - /uyap on : Bật áp chế uy áp thụ động.
 *    - /uyap off : Tắt áp chế uy áp thụ động.
 *    - /uyap status : Kiểm tra trạng thái và hồi chiêu uy áp.
 * 3. Thụ động: Duy trì cơ chế áp chế cảnh giới khi đứng gần người cảnh giới cao hơn
 *    và giảm sát thương khi đánh vượt cấp.
 */
public class UyApSystem implements Listener, CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;
    private static final int CHECK_RADIUS = 10;
    private static final String KEY_UYAP_ENABLED = "uyap_enabled";
    private static final long COOLDOWN_MILLIS = 20_000L; // 20s hồi chiêu

    // Lưu cooldown phóng thích uy áp của từng người chơi
    private final Map<UUID, Long> activeCooldowns = new HashMap<>();

    public UyApSystem(TuTienPlugin plugin) {
        this.plugin = plugin;
        startPassiveLoop();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_fd17b10807"));
            return true;
        }

        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return true;

        if (!data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cBạn chưa chọn class tu tiên! Dùng §e/tutien chon");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "on", "bat", "bật" -> {
                    setPassiveEnabled(player, data, true);
                    return true;
                }
                case "off", "tat", "tắt" -> {
                    setPassiveEnabled(player, data, false);
                    return true;
                }
                case "toggle" -> {
                    boolean curr = isPassiveEnabled(data);
                    setPassiveEnabled(player, data, !curr);
                    return true;
                }
                case "status", "trangthai", "trạngthái" -> {
                    showStatus(player, data);
                    return true;
                }
                case "help", "?" -> {
                    showHelp(player);
                    return true;
                }
                default -> {
                    // Nếu gõ tham số lạ, hiển thị hướng dẫn
                    player.sendMessage(plugin.prefix() + "§cDùng: §e/uyap [toggle|on|off|status]");
                    return true;
                }
            }
        }

        // Gõ /uyap không có tham số: Phóng thích kỹ năng Uy Áp!
        releaseActiveUyAp(player, data);
        return true;
    }

    /**
     * Kỹ năng phóng thích Uy Áp cảnh giới bộc phát ra xung quanh.
     */
    public void releaseActiveUyAp(Player player, PlayerData data) {
        UUID uid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastUse = activeCooldowns.getOrDefault(uid, 0L);
        long diff = now - lastUse;

        if (diff < COOLDOWN_MILLIS) {
            double remainSec = (COOLDOWN_MILLIS - diff) / 1000.0;
            player.sendMessage(plugin.prefix() + String.format("§eKỹ năng Uy Áp đang hồi chiêu! Còn §c%.1fs", remainSec));
            return;
        }

        // Tính tiêu hao linh khí: 5% max LK của rank, tối thiểu 200 LK, tối đa 15% LK hiện có
        long baseCost = Math.max(200L, (long)(data.getMaxLinhKhi() * 0.05));
        long cost = Math.min(baseCost, Math.max(200L, (long)(data.getLinhKhi() * 0.15)));

        if (data.getLinhKhi() < cost) {
            player.sendMessage(plugin.prefix() + "§cKhông đủ Linh Lực để phóng thích Uy Áp! (Cần tối thiểu §e" + cost + " LK§c)");
            return;
        }

        data.spendLinhKhi(cost);
        if (player.isOnline()) {
            plugin.getSkillItemManager().updateInfoItem(player, data);
        }
        activeCooldowns.put(uid, now);

        Location center = player.getLocation();
        World world = player.getWorld();

        // 1. Hiệu ứng Âm thanh Nghe cực mạnh
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 1.2f);

        // 2. Hiệu ứng Hình ảnh Visual: FLASH, SONIC_BOOM, SWEEP_ATTACK, Vòng bụi DUST 360 độ
        world.spawnParticle(Particle.FLASH, center.clone().add(0, 1.2, 0), 3, 0.2, 0.2, 0.2, 0);
        world.spawnParticle(Particle.SONIC_BOOM, center.clone().add(0, 1.0, 0), 1);
        world.spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 0.8, 0), 10, 1.5, 0.3, 1.5, 0.1);

        // Sóng xung kích DUST 360 độ bán kính tỏa ra 10m
        Particle.DustOptions goldDust = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.8f);
        Particle.DustOptions crimsonDust = new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.5f);

        for (int r = 2; r <= 10; r += 2) {
            int points = r * 8;
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                Location particleLoc = new Location(world, x, center.getY() + 0.3, z);
                world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, (r % 4 == 0) ? goldDust : crimsonDust);
            }
        }

        // 3. Tác động Vật lý & Khống chế lên kẻ địch xung quanh trong bán kính 10m
        int myStageOrd = data.getStage().ordinal();
        int affectedCount = 0;

        for (Entity nearby : player.getNearbyEntities(10.0, 6.0, 10.0)) {
            if (!(nearby instanceof LivingEntity target)) continue;
            if (target.equals(player)) continue;

            // Không đánh trúng pet thuần hóa của chính mình
            if (target instanceof Tameable tameable && tameable.getOwner() != null
                    && player.getUniqueId().equals(tameable.getOwner().getUniqueId())) {
                continue;
            }

            // Không áp chế đồng đội cùng Party
            if (target instanceof Player targetPlayer) {
                if (plugin.getPartyManager() != null && plugin.getPartyManager().isInParty(player)) {
                    // Nếu cùng party thì bỏ qua
                    var partyOfPlayer = getPartyOf(player);
                    if (partyOfPlayer != null && partyOfPlayer.members.contains(targetPlayer.getUniqueId())) {
                        continue;
                    }
                }
            }

            affectedCount++;

            // Tính toán Vector đẩy lùi & Hất tung Y >= 0.75
            Vector knockback = target.getLocation().toVector().subtract(center.toVector());
            if (knockback.lengthSquared() < 0.001) {
                knockback = player.getLocation().getDirection().setY(0);
            }
            knockback.setY(0).normalize().multiply(1.35); // Đẩy lùi xa
            knockback.setY(Math.max(0.78, 0.78 + (myStageOrd * 0.015))); // Lực nâng Y >= 0.75
            target.setVelocity(knockback);

            // Debuff chuẩn: Suy Yếu II (Weakness II), Làm Chậm II (Slowness II), Mệt Mỏi Đào Mỏ I (Mining Fatigue I)
            // Thời lượng: 160 ticks = 8 giây
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 1, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1, false, true, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 160, 0, false, true, true));

            // Kiểm tra cảnh giới mục tiêu
            boolean lowerStage = false;
            if (target instanceof Player targetPlayer) {
                PlayerData targetData = plugin.getDataManager().get(targetPlayer);
                if (targetData != null && targetData.hasClass()) {
                    if (targetData.getStage().ordinal() < myStageOrd) {
                        lowerStage = true;
                    }
                } else {
                    lowerStage = true; // Người chơi chưa tu luyện
                }
            } else {
                // Quái vật thường coi như có cảnh giới thấp hơn tu sĩ
                lowerStage = true;
            }

            // Nếu mục tiêu có cảnh giới thấp hơn tu sĩ: nhận thêm Mù Mắt (Blindness) và Choáng Váng Say Sẩm (Nausea)
            if (lowerStage) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0, false, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 0, false, true, true));
            }

            // Hiệu ứng hạt bụi đỏ lên kẻ trúng đòn
            target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.0, 0), 15, 0.4, 0.6, 0.4, 0,
                    new Particle.DustOptions(Color.fromRGB(220, 20, 20), 1.2f));

            if (target instanceof Player targetPlayer) {
                targetPlayer.sendMessage(plugin.prefix() + "§c§lUY ÁP BỘC PHÁT! §e" + player.getName()
                        + " §7đã phóng thích uy áp cảnh giới đè bẹp bạn!");
            }
        }

        player.sendMessage(plugin.prefix() + "§6§l✦ UY ÁP PHÓNG THÍCH! §eĐã trấn áp §a"
                + affectedCount + " §emục tiêu trong phạm vi 10m! §7(-" + cost + " LK)");
    }

    private com.tutien.party.PartyManager.Party getPartyOf(Player p) {
        // Trích xuất party an toàn qua reflection hoặc duyệt nếu cần
        try {
            var field = com.tutien.party.PartyManager.class.getDeclaredField("partyOf");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, com.tutien.party.PartyManager.Party> map =
                    (Map<UUID, com.tutien.party.PartyManager.Party>) field.get(plugin.getPartyManager());
            return map != null ? map.get(p.getUniqueId()) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void setPassiveEnabled(Player player, PlayerData data, boolean enabled) {
        data.setExtraBool(KEY_UYAP_ENABLED, enabled);
        if (!enabled) {
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.removePotionEffect(PotionEffectType.NAUSEA);
            player.sendMessage(plugin.prefix() + "§7Uy Áp thụ động §c§lĐÃ TẮT§7 — bạn sẽ không còn bị người cảnh giới cao hơn áp chế.");
        } else {
            player.sendMessage(plugin.prefix() + "§7Uy Áp thụ động §a§lĐÃ BẬT§7 — trở lại cơ chế áp chế cảnh giới bình thường.");
        }
        plugin.getDataManager().save(data);
    }

    private boolean isPassiveEnabled(PlayerData data) {
        return data.getExtraBool(KEY_UYAP_ENABLED, true);
    }

    private void showStatus(Player player, PlayerData data) {
        boolean passive = isPassiveEnabled(data);
        long lastUse = activeCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long diff = System.currentTimeMillis() - lastUse;
        String cdStatus;
        if (diff >= COOLDOWN_MILLIS) {
            cdStatus = "§aSẵn sàng phóng thích (/uyap)";
        } else {
            double remainSec = (COOLDOWN_MILLIS - diff) / 1000.0;
            cdStatus = String.format("§eĐang hồi chiêu (còn %.1fs)", remainSec);
        }

        player.sendMessage("§6§l╔════════════ TRẠNG THÁI UY ÁP ════════════╗");
        player.sendMessage("  §7• Kỹ năng Uy Áp: " + cdStatus);
        player.sendMessage("  §7• Áp chế thụ động: " + (passive ? "§a§lBẬT §7(Đang nhận hiệu ứng áp chế)" : "§c§lTẮT §7(Miễn nhiễm áp chế)"));
        player.sendMessage("  §7• Cảnh giới hiện tại: " + data.getStage().getColor() + data.getStage().getDisplayName() + " " + data.getStage().getRankName(data.getRank()));
        player.sendMessage("  §7• Lệnh nhanh: §e/uyap §7(Phóng thích) | §e/uyap toggle §7(Bật/tắt thụ động)");
        player.sendMessage("§6§l╚════════════════════════════════════════╝");
    }

    private void showHelp(Player player) {
        player.sendMessage("§6§l╔══════════ HỆ THỐNG UY ÁP CẢNH GIỚI ══════════╗");
        player.sendMessage("  §e/uyap              §8- §fPhóng thích Uy Áp cảnh giới bộc phát (10m)");
        player.sendMessage("  §e/uyap toggle       §8- §fBật/Tắt chế độ nhận áp chế thụ động");
        player.sendMessage("  §e/uyap on           §8- §fBật chế độ nhận áp chế thụ động");
        player.sendMessage("  §e/uyap off          §8- §fTắt chế độ nhận áp chế thụ động");
        player.sendMessage("  §e/uyap status       §8- §fXem trạng thái hồi chiêu và thiết lập");
        player.sendMessage("§6§l╚════════════════════════════════════════╝");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("toggle", "on", "off", "status", "help");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(prefix)) result.add(s);
            }
            return result;
        }
        return Collections.emptyList();
    }

    // ══════════════════════════════════════════════
    //  ÁP CHẾ THỤ ĐỘNG THEO CHU KỲ (PASSIVE LOOP)
    // ══════════════════════════════════════════════
    private void startPassiveLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(player);
                    if (data == null || !data.hasClass()) continue;

                    // Nếu player đã tắt uy áp thụ động: xóa hết potion và bỏ qua hoàn toàn
                    if (!isPassiveEnabled(data)) {
                        removeUyAp(player);
                        continue;
                    }

                    // Tìm người chơi cao cấp xung quanh
                    int myOrd = data.getStage().ordinal();
                    int maxGap = 0;
                    String oppressor = null;

                    for (Entity nearby : player.getNearbyEntities(CHECK_RADIUS, CHECK_RADIUS, CHECK_RADIUS)) {
                        if (!(nearby instanceof Player other)) continue;
                        if (other == player) continue;
                        PlayerData od = plugin.getDataManager().get(other);
                        if (od == null || !od.hasClass()) continue;
                        if (!isPassiveEnabled(od)) continue;

                        int theirOrd = od.getStage().ordinal();
                        int gap = theirOrd - myOrd;
                        if (gap > maxGap) {
                            maxGap = gap;
                            oppressor = other.getName();
                        }
                    }

                    if (maxGap >= 2) applyUyAp(player, data, maxGap, oppressor);
                    else removeUyAp(player);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Mỗi 5 giây
    }

    private void applyUyAp(Player player, PlayerData data, int gap, String oppressorName) {
        int dur = 120; // 6 giây

        if (gap >= 10) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, 2, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, dur, 0, false, false));
            notifyUyAp(player, gap, oppressorName, "§c§lUY ÁP CỰC MẠNH");
        } else if (gap >= 5) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, dur, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, dur, 0, false, false));
            notifyUyAp(player, gap, oppressorName, "§4Uy Áp Mạnh");
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, dur, 0, false, false));
            notifyUyAp(player, gap, oppressorName, "§7Uy Áp Nhẹ");
        }
    }

    private void notifyUyAp(Player player, int gap, String name, String level) {
        String msg = "§8[§c⚠ " + level + "§8] §7" + name + " (hơn " + gap + " cấp) đã áp chế bạn!";
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(msg));
    }

    private void removeUyAp(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        player.removePotionEffect(PotionEffectType.NAUSEA);
    }

    // Giảm sát thương người thấp cấp đánh vào người cao cấp
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        PlayerData atkData = plugin.getDataManager().get(attacker);
        PlayerData vicData = plugin.getDataManager().get(victim);
        if (atkData == null || vicData == null) return;
        if (!atkData.hasClass() || !vicData.hasClass()) return;
        if (!isPassiveEnabled(atkData)) return;
        if (!isPassiveEnabled(vicData)) return;

        int gap = vicData.getStage().ordinal() - atkData.getStage().ordinal();
        if (gap >= 5) {
            double mult = gap >= 10 ? 0.10 : gap >= 7 ? 0.25 : 0.40;
            event.setDamage(event.getDamage() * mult);
            attacker.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection().deserialize("§c⚠ Uy Áp cảnh giới! Sát thương giảm " + (int)((1 - mult) * 100) + "%"));
        }
    }
}
