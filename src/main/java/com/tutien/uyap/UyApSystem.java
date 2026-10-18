package com.tutien.uyap;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * UyApSystem - Uy ap canh gioi.
 *
 * Khi nguoi choi cao cap (Tu Linh Gioi tro len) dung gan nguoi choi thap hon:
 * - Gap 2+ canh gioi: giam ATK/DEF, them slowness nhe
 * - Gap 5+ canh gioi: them nausea, manh hon
 * - Gap 10+: giam manh nhat, co the co weakness
 *
 * Pham vi: 10m
 * Check moi 5 giay
 */
public class UyApSystem implements Listener, org.bukkit.command.CommandExecutor {

    private final TuTienPlugin plugin;
    // Khớp với UyApManager (RADIUS=10, MIN_DIFF=2) — xem ghi chú trong UyApManager.java
    private static final int CHECK_RADIUS = 10;
    private static final String KEY_UYAP_ENABLED = "uyap_enabled";

    public UyApSystem(TuTienPlugin plugin) {
        this.plugin = plugin;
        startLoop();
    }

    // ══════════════════════════════════════════════
    //  /uyap [on|off] — bat/tat hieu ung Uy Ap len BAN THAN
    // ══════════════════════════════════════════════
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command cmd,
                              String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_fd17b10807"));
            return true;
        }
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return true;

        boolean current = data.getExtraBool(KEY_UYAP_ENABLED, true);
        boolean next;
        if (args.length > 0) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            if (arg.equals("on") || arg.equals("bat") || arg.equals("bật")) next = true;
            else if (arg.equals("off") || arg.equals("tat") || arg.equals("tắt")) next = false;
            else if (arg.equals("status") || arg.equals("trangthai") || arg.equals("trạngthái")) {
                player.sendMessage(plugin.prefix() + "§7Uy Áp hiện đang: " + (current ? "§a§lBẬT" : "§c§lTẮT"));
                return true;
            } else {
                player.sendMessage(plugin.prefix() + "§cDùng: §e/uyap <on|off|bat|tat|status>");
                return true;
            }
        } else {
            next = !current; // toggle neu khong co tham so
        }

        data.setExtraBool(KEY_UYAP_ENABLED, next);
        if (!next) {
            // Tat ngay hieu ung dang co — lam TRUOC khi ghi dia, de nguoi choi
            // thay hieu ung mat ngay lap tuc thay vi phai cho saveAll() xong.
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.WEAKNESS);
            player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
            player.removePotionEffect(PotionEffectType.NAUSEA);
            player.sendMessage(plugin.prefix() + "§7Uy Áp §c§lĐÃ TẮT§7 — bạn sẽ không còn bị người cảnh giới cao hơn áp chế.");
        } else {
            player.sendMessage(plugin.prefix() + "§7Uy Áp §a§lĐÃ BẬT§7 — trở lại cơ chế cảnh giới bình thường.");
        }
        // Chi luu rieng du lieu cua nguoi choi nay, KHONG dung saveAll() — saveAll()
        // ghi dia dong bo cho TAT CA nguoi choi dang online, gay giat/lag va lam
        // tre viec go bo hieu ung o tren (vi saveAll chay truoc removePotionEffect
        // truoc day). Ghi chu ngay tren main thread nhung chi 1 file nen khong dang ke.
        plugin.getDataManager().save(data);
        return true;
    }

    private boolean isUyApEnabled(PlayerData data) {
        return data.getExtraBool(KEY_UYAP_ENABLED, true);
    }

    private void startLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(player);
                    if (data == null || !data.hasClass()) continue;

                    // Nếu player đã tắt uy áp: xóa hết potion và bỏ qua hoàn toàn
                    if (!isUyApEnabled(data)) {
                        removeUyAp(player);
                        continue;
                    }

                    // Tim nguoi choi cao cap xung quanh
                    int myOrd = data.getStage().ordinal();
                    int maxGap = 0;
                    String oppressor = null;

                    for (Entity nearby : player.getNearbyEntities(CHECK_RADIUS, CHECK_RADIUS, CHECK_RADIUS)) {
                        if (!(nearby instanceof Player other)) continue;
                        if (other == player) continue;
                        PlayerData od = plugin.getDataManager().get(other);
                        if (od == null || !od.hasClass()) continue;
                        int theirOrd = od.getStage().ordinal();
                        int gap = theirOrd - myOrd;
                        if (gap > maxGap) { maxGap = gap; oppressor = other.getName(); }
                    }

                    if (maxGap >= 2) applyUyAp(player, data, maxGap, oppressor);
                    else removeUyAp(player);
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // moi 5 giay
    }

    private void applyUyAp(Player player, PlayerData data, int gap, String oppressorName) {
        // Gap  2-4: Slowness I, nho
        // Gap  5-9: Slowness II + Weakness I + Mining Fatigue I
        // Gap 10+:  Slowness III + Weakness II + Mining Fatigue II + Nausea

        int dur = 120; // 6 giay (re-apply truoc khi het)

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
        // Chi hien action bar, khong spam chat
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

    // Giam sát thương nguoi thap cap nhan vao nguoi cao cap
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        PlayerData atkData = plugin.getDataManager().get(attacker);
        PlayerData vicData = plugin.getDataManager().get(victim);
        if (atkData == null || vicData == null) return;
        if (!atkData.hasClass() || !vicData.hasClass()) return;
        // Cả attacker lẫn victim đều phải bật Uy Áp thì mới tính giảm sát thương
        if (!isUyApEnabled(atkData)) return;
        if (!isUyApEnabled(vicData)) return;

        int gap = vicData.getStage().ordinal() - atkData.getStage().ordinal();
        if (gap >= 5) {
            // Kẻ tấn công thấp hơn nhiều -> giam sát thương 60-90%
            double mult = gap >= 10 ? 0.10 : gap >= 7 ? 0.25 : 0.40;
            event.setDamage(event.getDamage() * mult);
            attacker.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize("§c⚠ Uy Áp cảnh giới! Sát thương giảm " + (int)((1-mult)*100) + "%"));
        }
    }
}
