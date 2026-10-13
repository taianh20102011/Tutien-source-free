package com.tutien.skills;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * He thong Sach Hoc Ky Nang Bi Dong.
 *
 * Nguoi choi hoc toi da {@link PlayerData#MAX_PASSIVES} (3) bi dong bang cach
 * dung lenh /hocbidong <loai> khi dang cam Sach Bi Dong dung loai trong tay.
 * Moi bi dong tu dong NANG CAP theo canh gioi (tang % theo ordinal cua
 * CultivationStage - xem PlayerData#passiveOrdinalBonus), khong can hoc lai.
 *
 *   ATK   - Kiếm Khí Hộ Thể   : +% sát thương (calculateDamageMultiplier)
 *   HP    - Bất Tử Chi Thể    : +% mau toi da (calculateMaxHealth)
 *   DEF   - Kim Chung Hộ Pháp : +% phong thu (calculateDefenseMultiplier)
 *   SPEED - Phong Bộ Linh Vân : + toc do di chuyen (potion Speed am tham)
 *   LUCK  - Vận Khí Thiên Sinh: + may man rot do (potion Luck am tham)
 *   REGEN - Ngưng Tụ Sinh Cơ  : + hoi mau tu nhien (potion Regen am tham)
 */
public class PassiveSkillManager implements Listener {

    private final TuTienPlugin plugin;

    private static final Map<String, String> NAMES = Map.of(
            "ATK",   "§cKiếm Khí Hộ Thể",
            "HP",    "§dBất Tử Chi Thể",
            "DEF",   "§7Kim Chung Hộ Pháp",
            "SPEED", "§bPhong Bộ Linh Vân",
            "LUCK",  "§6Vận Khí Thiên Sinh",
            "REGEN", "§aNgưng Tụ Sinh Cơ"
    );
    private static final Map<String, String> DESC = Map.of(
            "ATK",   "Tăng sát thương gây ra, tăng dần theo cảnh giới.",
            "HP",    "Tăng lượng máu tối đa, tăng dần theo cảnh giới.",
            "DEF",   "Tăng khả năng phòng thủ, tăng dần theo cảnh giới.",
            "SPEED", "Tăng tốc độ di chuyển, tăng dần theo cảnh giới.",
            "LUCK",  "Tăng may mắn khi rơi đồ, tăng dần theo cảnh giới.",
            "REGEN", "Hồi máu tự nhiên nhanh hơn, tăng dần theo cảnh giới."
    );

    public PassiveSkillManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startEffectLoop();
    }

    private NamespacedKey bookKey() { return new NamespacedKey(plugin, "passive_book"); }

    public boolean isValidKey(String key) { return NAMES.containsKey(key); }

    public ItemStack createBook(String key) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§5§l✦ Sách Bí Kíp §7- " + NAMES.get(key));
        meta.setLore(List.of(
                "§7" + DESC.get(key),
                "§8(Dùng /hocbidong " + key.toLowerCase() + " để học - tối đa 3 bí kíp)"
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        meta.getPersistentDataContainer().set(bookKey(), PersistentDataType.STRING, key);
        item.setItemMeta(meta);
        return item;
    }

    /** Hoc bi dong tu sach dang cam tren tay. */
    public void learnFromHand(Player player, String key) {
        key = key.toUpperCase();
        if (!isValidKey(key)) {
            player.sendMessage(plugin.prefix() + "§cLoại bí kíp không hợp lệ! (atk/hp/def/speed/luck/regen)");
            return;
        }
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;

        if (data.hasPassive(key)) {
            player.sendMessage(plugin.prefix() + "§eBạn đã học " + NAMES.get(key) + " §erồi!");
            return;
        }
        if (data.getLearnedPassives().size() >= PlayerData.MAX_PASSIVES) {
            player.sendMessage(plugin.prefix() + "§cĐã học đủ §e" + PlayerData.MAX_PASSIVES + " §cbí kíp bị động tối đa!"
                    + " §7(Đã học: " + String.join(", ", data.getLearnedPassives()) + ")");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !hand.hasItemMeta()) { missingBook(player, key); return; }
        String val = hand.getItemMeta().getPersistentDataContainer().get(bookKey(), PersistentDataType.STRING);
        if (val == null || !val.equals(key)) { missingBook(player, key); return; }

        hand.setAmount(hand.getAmount() - 1);
        data.learnPassive(key);
        player.sendMessage(plugin.prefix() + "§d✦ Đã học bí kíp bị động: §f" + NAMES.get(key));
        player.sendTitle(plugin.msg("auto_ce0875283e"), "§d" + NAMES.get(key), 10, 50, 15);
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void missingBook(Player player, String key) {
        player.sendMessage(plugin.prefix() + "§cCần cầm đúng §fSách Bí Kíp - " + NAMES.get(key) + " §ctrên tay!");
    }

    public void showLearned(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;
        player.sendMessage("§5§l── BÍ KÍP BỊ ĐỘNG (" + data.getLearnedPassives().size() + "/" + PlayerData.MAX_PASSIVES + ") ──");
        if (data.getLearnedPassives().isEmpty()) {
            player.sendMessage(plugin.msg("auto_9d3d794959"));
        }
        for (String key : data.getLearnedPassives()) {
            double pct = data.passiveOrdinalBonus() * 100;
            player.sendMessage("§d✦ " + NAMES.getOrDefault(key, key) + " §7(+" + String.format("%.1f", pct) + "%)");
        }
    }

    // ========================
    //  APPLY POTION-BASED PASSIVES (SPEED / LUCK / REGEN)
    // ========================

    private void startEffectLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null) continue;
                    int ord = data.getStage().ordinal();
                    int tier = Math.min(3, ord / 6); // 0-3, tang moi ~6 canh gioi

                    if (data.hasPassive("SPEED")) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 220, tier, true, false));
                    }
                    if (data.hasPassive("LUCK")) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 220, tier, true, false));
                    }
                    if (data.hasPassive("REGEN")) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 220, tier, true, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 40L, 100L); // refresh moi 5s (hieu ung dai 11s, du bu)
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { /* hieu ung se tu ap trong vong lap tren */ }
}
