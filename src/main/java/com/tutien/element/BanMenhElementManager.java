package com.tutien.element;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Quản lý Dị Hỏa / Thần Lôi Bản Mệnh:
 * - Hấp thu, thuần hóa & thôn phệ.
 * - Hiệu ứng chiến đấu Visual Particle & Âm thanh.
 * - Sát thương đốt cháy chuẩn xuyên giáp (Dị Hỏa) hoặc giật sét lan AOE (Thần Lôi).
 * - Tăng tỉ lệ thành công khi Luyện Đan và Rèn Đúc.
 */
public class BanMenhElementManager implements Listener, CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;
    private final NamespacedKey itemKey;

    public static final String KEY_ACTIVE_DIHOA   = "banmenh_dihoa";
    public static final String KEY_ACTIVE_THANLOI = "banmenh_thanloi";

    // Track đang thuần hóa để hiển thị tĩnh tọa
    private final Set<UUID> tamingPlayers = new HashSet<>();

    public BanMenhElementManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "seed_element");
        startPassiveAmbientTask();
    }

    /**
     * Tạo Item Hạt Giống Chủng Tử Dị Hỏa / Thần Lôi.
     */
    public ItemStack createSeedItem(BanMenhElement el, int amount) {
        Material mat = el.getType() == BanMenhElement.ElementType.DI_HOA ? Material.FIRE_CHARGE : Material.NETHER_STAR;
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(el.getColor() + "✦ " + el.getType().getName() + ": " + el.getDisplayName() + " ✦");
            List<String> lore = new ArrayList<>();
            lore.add("§7Chủng tử bản nguyên sinh ra từ thiên địa linh khí.");
            lore.add("§8" + el.getDescription());
            lore.add("");
            lore.add("§eThuộc tính bộc phát:");
            if (el.getType() == BanMenhElement.ElementType.DI_HOA) {
                lore.add("  §7• Thiêu đốt xuyên giáp: §a+" + (int)(el.getDamageMultiplier() * 100) + "%");
                lore.add("  §7• Tăng tốc & phẩm chất Đan Dược: §6+" + (int)(el.getAlchemyBonus() * 100) + "%");
                lore.add("  §7• Tăng tỉ lệ Rèn Pháp Bảo: §b+" + (int)(el.getForgeBonus() * 100) + "%");
            } else {
                lore.add("  §7• Sấm chớp giật lan kẻ thù: §a+" + (int)(el.getDamageMultiplier() * 100) + "%");
                lore.add("  §7• Tỉ lệ chí mạng Rèn Đúc: §b+" + (int)(el.getForgeBonus() * 100) + "%");
                lore.add("  §7• Tăng tốc độ Luyện Đan: §6+" + (int)(el.getAlchemyBonus() * 100) + "%");
            }
            lore.add("");
            lore.add("§a► Cầm trên tay và gõ: §e/dihoa dung | /thanloi dung §ađể thuần hóa!");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, el.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    public BanMenhElement getElementFromSeed(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        return BanMenhElement.fromId(id);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_fd17b10807"));
            return true;
        }

        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return true;

        boolean isDiHoa = label.equalsIgnoreCase("dihoa") || label.equalsIgnoreCase("hoa");

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "dung", "hapthu", "thonphe" -> {
                    startTaming(player, data, isDiHoa);
                    return true;
                }
                case "thao", "huy", "remove" -> {
                    removeElement(player, data, isDiHoa);
                    return true;
                }
                case "status", "info" -> {
                    showStatus(player, data);
                    return true;
                }
                case "list" -> {
                    showList(player, isDiHoa);
                    return true;
                }
                default -> {
                    showHelp(player, isDiHoa);
                    return true;
                }
            }
        }

        showStatus(player, data);
        return true;
    }

    /**
     * Bắt đầu quá trình thuần hóa Dị Hỏa / Thần Lôi.
     */
    public void startTaming(Player player, PlayerData data, boolean isDiHoa) {
        UUID uid = player.getUniqueId();
        if (tamingPlayers.contains(uid)) {
            player.sendMessage(plugin.prefix() + "§eĐang trong quá trình tĩnh tọa hấp thu linh vật thiên địa, chớ manh động!");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        BanMenhElement el = getElementFromSeed(hand);
        if (el == null) {
            player.sendMessage(plugin.prefix() + "§cBạn cần cầm Chủng Tử §e"
                    + (isDiHoa ? "Dị Hỏa" : "Thần Lôi") + " §ctrên tay chính!");
            return;
        }

        if (isDiHoa && el.getType() != BanMenhElement.ElementType.DI_HOA) {
            player.sendMessage(plugin.prefix() + "§cĐây là Thần Lôi! Hãy dùng lệnh §e/thanloi dung§c.");
            return;
        }
        if (!isDiHoa && el.getType() != BanMenhElement.ElementType.THAN_LOI) {
            player.sendMessage(plugin.prefix() + "§cĐây là Dị Hỏa! Hãy dùng lệnh §e/dihoa dung§c.");
            return;
        }

        int myStage = data.getStage().ordinal();
        if (myStage < el.getMinStageRequired()) {
            player.sendMessage(plugin.prefix() + "§cTu vi chưa đủ để trấn áp " + el.getColor() + el.getDisplayName()
                    + "§c! Cần tối thiểu cảnh giới cấp " + el.getMinStageRequired() + ".");
            return;
        }

        long lkCost = Math.max(5000L, (long)(data.getMaxLinhKhi() * 0.15));
        if (data.getLinhKhi() < lkCost) {
            player.sendMessage(plugin.prefix() + "§cKinh mạch cạn kiệt, cần ít nhất §e" + lkCost + " Linh Khí §cđể bảo vệ thần hồn!");
            return;
        }

        // Tiêu hao item và linh khí
        hand.setAmount(hand.getAmount() - 1);
        data.spendLinhKhi(lkCost);
        tamingPlayers.add(uid);

        player.sendMessage(plugin.prefix() + "§6§l✦ BẮT ĐẦU THÔN PHỆ " + el.getColor() + el.getDisplayName().toUpperCase() + "§6§l!");
        player.sendMessage("§7Hãy tĩnh tọa vận công trong 5 giây để rèn luyện kinh mạch...");
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 3, false, false));

        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    tamingPlayers.remove(uid);
                    cancel();
                    return;
                }

                step++;
                Location loc = player.getLocation().add(0, 1.0, 0);
                World w = player.getWorld();

                if (el.getType() == BanMenhElement.ElementType.DI_HOA) {
                    if (el.getDustOptions() != null) {
                        w.spawnParticle(Particle.DUST, loc, 25, 0.5, 0.8, 0.5, 0, el.getDustOptions());
                    } else {
                        w.spawnParticle(el.getParticle(), loc, 20, 0.4, 0.7, 0.4, 0.05);
                    }
                    w.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.8f + step * 0.1f);
                } else {
                    if (el.getDustOptions() != null) {
                        w.spawnParticle(Particle.DUST, loc, 30, 0.6, 1.0, 0.6, 0, el.getDustOptions());
                    } else {
                        w.spawnParticle(el.getParticle(), loc, 15, 0.5, 0.8, 0.5, 0.1);
                    }
                    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.2f + step * 0.1f);
                }

                if (step >= 5) {
                    tamingPlayers.remove(uid);
                    if (isDiHoa) {
                        data.setExtraStr(KEY_ACTIVE_DIHOA, el.getId());
                    } else {
                        data.setExtraStr(KEY_ACTIVE_THANLOI, el.getId());
                    }
                    plugin.getDataManager().save(data);

                    // Hiệu ứng hoàn tất
                    w.spawnParticle(Particle.FLASH, loc, 5, 0.3, 0.3, 0.3, 0);
                    w.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 1.0f);
                    player.sendMessage(plugin.prefix() + "§a§l✓ THUẦN HÓA THÀNH CÔNG! "
                            + el.getColor() + el.getDisplayName() + " §ađã dung nhập vào bản mệnh chân nguyên!");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void removeElement(Player player, PlayerData data, boolean isDiHoa) {
        String key = isDiHoa ? KEY_ACTIVE_DIHOA : KEY_ACTIVE_THANLOI;
        String curId = data.getExtraStr(key, null);
        if (curId == null) {
            player.sendMessage(plugin.prefix() + "§cBạn chưa dung hợp " + (isDiHoa ? "Dị Hỏa" : "Thần Lôi") + " nào!");
            return;
        }

        BanMenhElement el = BanMenhElement.fromId(curId);
        data.setExtraStr(key, null);
        plugin.getDataManager().save(data);

        // Trả lại chủng tử cho người chơi
        if (el != null) {
            ItemStack seed = createSeedItem(el, 1);
            player.getInventory().addItem(seed).values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
        }

        player.sendMessage(plugin.prefix() + "§eĐã phong ấn và trục xuất " + (el != null ? el.getDisplayName() : "") + " khỏi bản mệnh.");
    }

    private void showStatus(Player player, PlayerData data) {
        String dhId = data.getExtraStr(KEY_ACTIVE_DIHOA, null);
        String tlId = data.getExtraStr(KEY_ACTIVE_THANLOI, null);
        BanMenhElement diHoa = BanMenhElement.fromId(dhId);
        BanMenhElement thanLoi = BanMenhElement.fromId(tlId);

        player.sendMessage("§6§l╔══════════ THIÊN ĐỊA LINH VẬT BẢN MỆNH ══════════╗");
        player.sendMessage("  §e🔥 Dị Hỏa: " + (diHoa != null
                ? diHoa.getColor() + "§l" + diHoa.getDisplayName() + " §7(+" + (int)(diHoa.getDamageMultiplier() * 100) + "% sát thương chuẩn)"
                : "§7Chưa thuần hóa (Dùng §e/dihoa list§7)"));
        player.sendMessage("  §d⚡ Thần Lôi: " + (thanLoi != null
                ? thanLoi.getColor() + "§l" + thanLoi.getDisplayName() + " §7(+" + (int)(thanLoi.getDamageMultiplier() * 100) + "% sát thương lôi)"
                : "§7Chưa thuần hóa (Dùng §e/thanloi list§7)"));
        player.sendMessage("  §7• Hiệu quả đan đạo: " + (diHoa != null ? "§a+" + (int)(diHoa.getAlchemyBonus() * 100) + "%" : "§70%"));
        player.sendMessage("  §7• Hiệu quả rèn đúc: " + (thanLoi != null ? "§a+" + (int)(thanLoi.getForgeBonus() * 100) + "%" : (diHoa != null ? "§a+" + (int)(diHoa.getForgeBonus() * 100) + "%" : "§70%")));
        player.sendMessage("§6§l╚══════════════════════════════════════════════╝");
    }

    private void showList(Player player, boolean isDiHoa) {
        player.sendMessage("§6§l╔══════════ DANH SÁCH " + (isDiHoa ? "DỊ HỎA" : "THẦN LÔI") + " ══════════╗");
        for (BanMenhElement el : BanMenhElement.values()) {
            if (isDiHoa && el.getType() != BanMenhElement.ElementType.DI_HOA) continue;
            if (!isDiHoa && el.getType() != BanMenhElement.ElementType.THAN_LOI) continue;
            player.sendMessage("  " + el.getColor() + "✦ " + el.getDisplayName() + " §7— " + el.getDescription());
        }
        player.sendMessage("§6§l╚══════════════════════════════════════════════╝");
    }

    private void showHelp(Player player, boolean isDiHoa) {
        String base = isDiHoa ? "/dihoa" : "/thanloi";
        player.sendMessage("§6§l╔══════════ HƯỚNG DẪN BẢN MỆNH LINH VẬT ══════════╗");
        player.sendMessage("  §e" + base + " status   §8- §fXem trạng thái linh vật bản mệnh");
        player.sendMessage("  §e" + base + " dung     §8- §fThuần hóa chủng tử đang cầm trên tay");
        player.sendMessage("  §e" + base + " thao     §8- §fThu hồi linh vật trả lại chủng tử");
        player.sendMessage("  §e" + base + " list     §8- §fXem danh sách truyền thuyết");
        player.sendMessage("§6§l╚══════════════════════════════════════════════╝");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("status", "dung", "thao", "list");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> res = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(prefix)) res.add(s);
            }
            return res;
        }
        return Collections.emptyList();
    }

    // ============================================================
    // HIỆU ỨNG CHIẾN ĐẤU (COMBAT EFFECT & AOE)
    // ============================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim.equals(attacker)) return;

        // Bỏ qua nếu đánh trúng pet thuần hóa của chính mình
        if (victim instanceof Tameable t && t.getOwner() != null && attacker.getUniqueId().equals(t.getOwner().getUniqueId())) {
            return;
        }

        PlayerData data = plugin.getDataManager().get(attacker);
        if (data == null || !data.hasClass()) return;

        String dhId = data.getExtraStr(KEY_ACTIVE_DIHOA, null);
        String tlId = data.getExtraStr(KEY_ACTIVE_THANLOI, null);

        Location hitLoc = victim.getLocation().add(0, 1.0, 0);

        // 1. Kích hoạt hiệu ứng Dị Hỏa: Đốt cháy sát thương chuẩn xuyên giáp
        if (dhId != null) {
            BanMenhElement diHoa = BanMenhElement.fromId(dhId);
            if (diHoa != null) {
                double bonusDmg = event.getDamage() * diHoa.getDamageMultiplier();
                event.setDamage(event.getDamage() + bonusDmg);
                victim.setFireTicks(100); // Đốt 5 giây

                // Visual hạt lửa đặc trưng
                if (diHoa.getDustOptions() != null) {
                    victim.getWorld().spawnParticle(Particle.DUST, hitLoc, 15, 0.4, 0.6, 0.4, 0, diHoa.getDustOptions());
                } else {
                    victim.getWorld().spawnParticle(diHoa.getParticle(), hitLoc, 15, 0.3, 0.5, 0.3, 0.05);
                }
                victim.getWorld().playSound(hitLoc, diHoa.getTriggerSound(), 1.0f, 1.2f);
            }
        }

        // 2. Kích hoạt hiệu ứng Thần Lôi: Sét giật lan truyền AOE kẻ địch lân cận
        if (tlId != null) {
            BanMenhElement thanLoi = BanMenhElement.fromId(tlId);
            if (thanLoi != null) {
                double chainDmg = event.getDamage() * thanLoi.getDamageMultiplier();
                event.setDamage(event.getDamage() + (chainDmg * 0.5)); // Bonus cho mục tiêu chính

                // Giật sét lan sang 3 mục tiêu xung quanh 6m
                int chained = 0;
                for (Entity near : victim.getNearbyEntities(6.0, 4.0, 6.0)) {
                    if (!(near instanceof LivingEntity chainTarget)) continue;
                    if (chainTarget.equals(attacker) || chainTarget.equals(victim)) continue;
                    if (chainTarget instanceof Tameable t && t.getOwner() != null && attacker.getUniqueId().equals(t.getOwner().getUniqueId())) continue;

                    chainTarget.damage(chainDmg, attacker);
                    chainTarget.getWorld().spawnParticle(Particle.FIREWORK, chainTarget.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                    chained++;
                    if (chained >= 3) break;
                }

                // Visual sét giật
                if (thanLoi.getDustOptions() != null) {
                    victim.getWorld().spawnParticle(Particle.DUST, hitLoc, 25, 0.5, 0.8, 0.5, 0, thanLoi.getDustOptions());
                } else {
                    victim.getWorld().spawnParticle(thanLoi.getParticle(), hitLoc, 20, 0.5, 0.8, 0.5, 0.1);
                }
                victim.getWorld().playSound(hitLoc, thanLoi.getTriggerSound(), 1.2f, 1.4f);
            }
        }
    }

    /**
     * Tỏa hạt hào quang nhẹ quanh thân thể khi trang bị Dị Hỏa / Thần Lôi.
     */
    private void startPassiveAmbientTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(player);
                    if (data == null || !data.hasClass()) continue;

                    String dhId = data.getExtraStr(KEY_ACTIVE_DIHOA, null);
                    String tlId = data.getExtraStr(KEY_ACTIVE_THANLOI, null);

                    if (dhId != null) {
                        BanMenhElement diHoa = BanMenhElement.fromId(dhId);
                        if (diHoa != null) {
                            Location loc = player.getLocation().add(0, 0.8, 0);
                            if (diHoa.getDustOptions() != null) {
                                player.getWorld().spawnParticle(Particle.DUST, loc, 2, 0.25, 0.4, 0.25, 0, diHoa.getDustOptions());
                            } else {
                                player.getWorld().spawnParticle(diHoa.getParticle(), loc, 2, 0.2, 0.4, 0.2, 0.02);
                            }
                        }
                    }

                    if (tlId != null) {
                        BanMenhElement thanLoi = BanMenhElement.fromId(tlId);
                        if (thanLoi != null) {
                            Location loc = player.getLocation().add(0, 1.2, 0);
                            if (thanLoi.getDustOptions() != null) {
                                player.getWorld().spawnParticle(Particle.DUST, loc, 2, 0.25, 0.3, 0.25, 0, thanLoi.getDustOptions());
                            } else {
                                player.getWorld().spawnParticle(thanLoi.getParticle(), loc, 2, 0.2, 0.3, 0.2, 0.02);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 20L); // Mỗi giây 1 lần, cực kỳ nhẹ
    }

    public double getAlchemySuccessBonus(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return 0.0;
        double bonus = 0.0;
        BanMenhElement diHoa = BanMenhElement.fromId(data.getExtraStr(KEY_ACTIVE_DIHOA, null));
        if (diHoa != null) bonus += diHoa.getAlchemyBonus();
        BanMenhElement thanLoi = BanMenhElement.fromId(data.getExtraStr(KEY_ACTIVE_THANLOI, null));
        if (thanLoi != null) bonus += thanLoi.getAlchemyBonus();
        return bonus;
    }

    public double getForgeSuccessBonus(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return 0.0;
        double bonus = 0.0;
        BanMenhElement diHoa = BanMenhElement.fromId(data.getExtraStr(KEY_ACTIVE_DIHOA, null));
        if (diHoa != null) bonus += diHoa.getForgeBonus();
        BanMenhElement thanLoi = BanMenhElement.fromId(data.getExtraStr(KEY_ACTIVE_THANLOI, null));
        if (thanLoi != null) bonus += thanLoi.getForgeBonus();
        return bonus;
    }
}
