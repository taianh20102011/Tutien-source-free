package com.tutien.tongmon;

import com.tutien.TuTienPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;

public class TongMonManager {

    private final TuTienPlugin plugin;
    private final Map<String, TongMon> tongMons = new LinkedHashMap<>();  // id -> TongMon
    private final Map<UUID, String>    playerTongMon = new HashMap<>();   // player UUID -> tong mon id
    private final File dataFile;

    public TongMonManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "tongmon.yml");
        load();
    }

    // ========================
    //  TAO TONG MON
    // ========================

    public boolean create(Player founder, String name, String tag) {
        // Kiem tra da co tong mon chua
        if (playerTongMon.containsKey(founder.getUniqueId())) {
            founder.sendMessage(plugin.prefix() + "§cBạn đã có Tông Môn! Ra tông môn trước.");
            return false;
        }
        // Kiem tra ten trung
        for (TongMon tm : tongMons.values()) {
            if (tm.getName().equalsIgnoreCase(name)) {
                founder.sendMessage(plugin.prefix() + "§cTên Tông Môn §e" + name + " §cđã tồn tại!");
                return false;
            }
            if (tm.getTag().equalsIgnoreCase(tag)) {
                founder.sendMessage(plugin.prefix() + "§cTag §e[" + tag + "] §cđã được sử dụng!");
                return false;
            }
        }
        // Tag 2-5 chu
        if (tag.length() < 2 || tag.length() > 5) {
            founder.sendMessage(plugin.prefix() + "§cTag phải từ 2-5 ký tự!");
            return false;
        }
        // Phi thanh lap: dung config economy.tong-mon-create-cost
        var data = plugin.getDataManager().get(founder);
        long cost = plugin.getConfig().getLong("economy.tong-mon-create-cost", 1000L);
        if (data.getTien() < cost) {
            founder.sendMessage(plugin.prefix() + "§cCan §e" + fmt(cost) + " §c" + plugin.getTienManager().tenTien() + " để lập Tông Môn!");
            return false;
        }
        data.spendTien(cost);

        String id = UUID.randomUUID().toString().substring(0, 8);
        TongMon tm = new TongMon(id, name, tag, founder.getUniqueId());
        tongMons.put(id, tm);
        playerTongMon.put(founder.getUniqueId(), id);
        save();

        // Thong bao
        String msg = plugin.prefix() + "§6§l✦ " + founder.getName()
            + " §ađã lập Tông Môn §e" + name + " §a[" + tag + "]§a!";
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));

        founder.sendMessage(plugin.msg("auto_00897ee573"));
        founder.sendMessage(plugin.msg("auto_4cd900fe5a"));
        founder.sendMessage("  §7Tên: §e" + name);
        founder.sendMessage("  §7Tag: §e[" + tag + "]");
        founder.sendMessage(plugin.msg("auto_714bffced8"));
        founder.sendMessage(plugin.msg("auto_a2fa899173"));
        founder.sendMessage(plugin.msg("auto_e17b6394d7"));
        founder.playSound(founder.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        return true;
    }

    // ========================
    //  GIA NHAP / RO TU
    // ========================

    public boolean join(Player player, String tongMonId) {
        if (playerTongMon.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.prefix() + "§cBạn đã có Tông Môn! /tongmon roi để ra trước.");
            return false;
        }
        TongMon tm = tongMons.get(tongMonId);
        if (tm == null) {
            player.sendMessage(plugin.prefix() + "§cTông Môn không tồn tại!");
            return false;
        }
        tm.addMember(player.getUniqueId());
        playerTongMon.put(player.getUniqueId(), tongMonId);
        save();

        notifyMembers(tm, plugin.prefix() + "§a➕ §e" + player.getName() + " §avừa nhập Tông Môn!");
        player.sendMessage(plugin.prefix() + "§aĐã gia nhập §e" + tm.getColoredName() + "§a!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        return true;
    }

    public boolean leave(Player player) {
        String tmId = playerTongMon.get(player.getUniqueId());
        if (tmId == null) {
            player.sendMessage(plugin.prefix() + "§cBạn không ở trong Tông Môn nào!");
            return false;
        }
        TongMon tm = tongMons.get(tmId);
        if (tm == null) return false;
        if (tm.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.prefix() + "§cBạn là Môn Chủ! /tongmon giaithe để giải thể Tông Môn.");
            return false;
        }
        tm.removeMember(player.getUniqueId());
        playerTongMon.remove(player.getUniqueId());
        save();
        notifyMembers(tm, plugin.prefix() + "§c➖ §e" + player.getName() + " §cđã rời Tông Môn.");
        player.sendMessage(plugin.prefix() + "§aĐã rời §e" + tm.getColoredName() + "§a.");
        return true;
    }

    public boolean disband(Player player) {
        String tmId = playerTongMon.get(player.getUniqueId());
        if (tmId == null) { player.sendMessage(plugin.prefix() + "§cKhong co Tong Mon!"); return false; }
        TongMon tm = tongMons.get(tmId);
        if (tm == null) return false;
        if (!tm.isOwner(player.getUniqueId())) {
            player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ mới có thể giải thể Tông Môn!");
            return false;
        }
        // Hoan tra 50% phi
        var data = plugin.getDataManager().get(player);
        long refund = plugin.getConfig().getLong("economy.tong-mon-create-cost", 1000L) / 2;
        data.addTien(refund);

        // Xoa tat ca thanh vien
        for (UUID uuid : tm.getMembers().keySet()) {
            playerTongMon.remove(uuid);
        }
        tongMons.remove(tmId);
        save();

        String msg = plugin.prefix() + "§c§l✦ Tong Mon §e" + tm.getName() + " §c§lđã bị giải thể!";
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        return true;
    }

    // ========================
    //  INVITE / KICK
    // ========================

    public boolean invite(Player manager, Player target) {
        String tmId = playerTongMon.get(manager.getUniqueId());
        if (tmId == null) { manager.sendMessage(plugin.prefix() + "§cBạn không có Tông Môn!"); return false; }
        TongMon tm = tongMons.get(tmId);
        if (!tm.canManage(manager.getUniqueId())) {
            manager.sendMessage(plugin.prefix() + "§cCần hạng Trưởng Lão trở lên mới có thể mời!"); return false;
        }
        if (playerTongMon.containsKey(target.getUniqueId())) {
            manager.sendMessage(plugin.prefix() + "§c" + target.getName() + " đã có Tông Môn!"); return false;
        }
        // Gui loi moi
        target.sendMessage(plugin.msg("auto_00897ee573"));
        target.sendMessage(plugin.msg("auto_46b2b9e33b"));
        target.sendMessage("  §7" + manager.getName() + " mời bạn gia nhập:");
        target.sendMessage("  " + tm.getColoredName() + " §7[Lv." + tm.getLevel() + "] (" + tm.getMemberCount() + " thành viên)");
        target.sendMessage("  §aDùng §f/tongmon chap " + tmId + " §ađể chấp nhận");
        target.sendMessage(plugin.msg("auto_e17b6394d7"));
        manager.sendMessage(plugin.prefix() + "§aĐã gửi lời mời đến §e" + target.getName() + "§a!");
        return true;
    }

    public boolean kick(Player manager, String targetName) {
        String tmId = playerTongMon.get(manager.getUniqueId());
        if (tmId == null) { manager.sendMessage(plugin.prefix() + "§cBạn không có Tông Môn!"); return false; }
        TongMon tm = tongMons.get(tmId);
        if (!tm.canManage(manager.getUniqueId())) {
            manager.sendMessage(plugin.prefix() + "§cKhông đủ quyền!"); return false;
        }
        // Tim thanh vien theo ten
        UUID targetUuid = null;
        for (UUID uuid : tm.getMembers().keySet()) {
            Player tp = Bukkit.getPlayer(uuid);
            if (tp != null && tp.getName().equalsIgnoreCase(targetName)) { targetUuid = uuid; break; }
            // Offline: check cache
            var tdata = plugin.getDataManager().get(uuid);
            if (tdata != null && tdata.getPlayerName().equalsIgnoreCase(targetName)) { targetUuid = uuid; break; }
        }
        if (targetUuid == null) { manager.sendMessage(plugin.prefix() + "§cKhông tìm thấy: " + targetName); return false; }
        if (tm.isOwner(targetUuid)) { manager.sendMessage(plugin.prefix() + "§cKhông thể kick Môn Chủ!"); return false; }
        tm.removeMember(targetUuid);
        playerTongMon.remove(targetUuid);
        save();
        notifyMembers(tm, plugin.prefix() + "§c" + targetName + " đã bị kick khỏi Tông Môn.");
        manager.sendMessage(plugin.prefix() + "§aKick §e" + targetName + " §athành công!");
        return true;
    }

    // ========================
    //  INFO / THONG BAO
    // ========================

    public void showInfo(Player player) {
        String tmId = playerTongMon.get(player.getUniqueId());
        if (tmId == null) { player.sendMessage(plugin.prefix() + "§cBạn chưa có Tông Môn!"); return; }
        TongMon tm = tongMons.get(tmId);
        if (tm == null) return;

        player.sendMessage(plugin.msg("auto_da47a9f770"));
        player.sendMessage("  " + tm.getColoredName() + " §7[" + tm.getTag() + "]  §eLv." + tm.getLevel());
        if (tm.getDescription() != null && !tm.getDescription().isEmpty())
            player.sendMessage("  §7" + tm.getDescription());
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        player.sendMessage("  §7Thành viên: §e" + tm.getMemberCount());
        player.sendMessage("  §7Kho Tiền: §e" + fmt(tm.getTienKho()));
        player.sendMessage("  §7Bonus: §a+" + String.format("%.0f%%", tm.getLkBonus()*100) + " LK  "
            + "§c+" + String.format("%.0f%%", tm.getAtkBonus()*100) + " ATK  "
            + "§b+" + String.format("%.0f%%", tm.getDefBonus()*100) + " DEF");
        if (!tm.getAnnouncement().isEmpty())
            player.sendMessage("  §e[TB] §f" + tm.getAnnouncement());
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        // List members
        TongMon.Rank myRank = tm.getRank(player.getUniqueId());
        player.sendMessage("  §7Hạng vị của bạn: " + myRank.getColored());
        int shown = 0;
        for (Map.Entry<UUID, TongMon.Rank> e : tm.getMembers().entrySet()) {
            if (shown++ >= 10) { player.sendMessage("  §8... và " + (tm.getMemberCount()-10) + " người khác"); break; }
            var pdata = plugin.getDataManager().get(e.getKey());
            String pName = pdata != null ? pdata.getPlayerName() : e.getKey().toString().substring(0,8);
            boolean online = Bukkit.getPlayer(e.getKey()) != null;
            player.sendMessage("  " + e.getValue().getColored() + " §7" + pName
                + (online ? " §a[Online]" : " §8[Offline]"));
        }
        player.sendMessage(plugin.msg("auto_5a07764d64"));
    }

    public void showList(Player player) {
        player.sendMessage(plugin.msg("auto_59ffcc5384"));
        if (tongMons.isEmpty()) { player.sendMessage(plugin.msg("auto_d546ae123d")); return; }
        List<TongMon> sorted = new ArrayList<>(tongMons.values());
        sorted.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));
        int i = 1;
        for (TongMon tm : sorted) {
            if (i > 15) break;
            player.sendMessage("  §e#" + i + " " + tm.getColoredName()
                + " §7[" + tm.getTag() + "] Lv." + tm.getLevel()
                + " §8| §7" + tm.getMemberCount() + " thành viên");
            i++;
        }
    }

    // ========================
    //  KHO
    // ========================

    public boolean depositKho(Player player, long amount) {
        String tmId = playerTongMon.get(player.getUniqueId());
        if (tmId == null) { player.sendMessage(plugin.prefix() + "§cBạn chưa có Tông Môn!"); return false; }
        var data = plugin.getDataManager().get(player);
        if (data.getTien() < amount) {
            player.sendMessage(plugin.prefix() + "§cKhông đủ " + plugin.getTienManager().tenTien() + "!"); return false;
        }
        data.spendTien(amount);
        TongMon tm = tongMons.get(tmId);
        tm.depositKho(amount);
        save();
        player.sendMessage(plugin.prefix() + "§aGóp §e" + fmt(amount) + " §a" + plugin.getTienManager().tenTien() + " vào Kho Tông Môn. Kho: §e" + fmt(tm.getTienKho()));
        tm.addExp(amount / 100); // EXP = 1% so tien dong gop
        return true;
    }

    // ========================
    //  HELPERS
    // ========================

    public void notifyMembers(TongMon tm, String msg) {
        for (UUID uuid : tm.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    public TongMon getTongMon(UUID playerUuid) {
        String id = playerTongMon.get(playerUuid);
        return id != null ? tongMons.get(id) : null;
    }

    public TongMon getTongMonById(String id) { return tongMons.get(id); }

    public boolean hasTongMon(UUID uuid) { return playerTongMon.containsKey(uuid); }

    private String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n/1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n/1_000.0);
        return String.valueOf(n);
    }

    // ========================
    //  SAVE / LOAD
    // ========================

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (TongMon tm : tongMons.values()) {
            String path = "tongmons." + tm.getId();
            cfg.set(path + ".name",         tm.getName());
            cfg.set(path + ".tag",          tm.getTag());
            cfg.set(path + ".color",        tm.getColor());
            cfg.set(path + ".description",  tm.getDescription());
            cfg.set(path + ".owner",        tm.getOwner().toString());
            cfg.set(path + ".level",        tm.getLevel());
            cfg.set(path + ".totalExp",     tm.getTotalExp());
            cfg.set(path + ".kho",          tm.getTienKho());
            cfg.set(path + ".createdAt",    tm.getCreatedAt());
            cfg.set(path + ".announcement", tm.getAnnouncement());
            List<String> memberList = new ArrayList<>();
            for (Map.Entry<UUID, TongMon.Rank> e : tm.getMembers().entrySet()) {
                memberList.add(e.getKey() + ":" + e.getValue().name());
            }
            cfg.set(path + ".members", memberList);
        }
        try { cfg.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("Lỗi lưu Tông Môn: " + e.getMessage()); }
    }

    private void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("tongmons");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String name  = sec.getString(id + ".name", "Unknown");
            String tag   = sec.getString(id + ".tag", "TM");
            String owner = sec.getString(id + ".owner", "");
            try {
                UUID ownerUuid = UUID.fromString(owner);
                TongMon tm = new TongMon(id, name, tag, ownerUuid);
                tm.setColor(sec.getString(id + ".color", "§e"));
                tm.setDescription(sec.getString(id + ".description", ""));
                tm.setLevel(sec.getInt(id + ".level", 1));
                tm.setTotalExp(sec.getLong(id + ".totalExp", 0));
                tm.setTienKho(sec.getLong(id + ".kho", 0));
                tm.setCreatedAt(sec.getLong(id + ".createdAt", System.currentTimeMillis()));
                tm.setAnnouncement(sec.getString(id + ".announcement", ""));
                // Members
                List<String> memberList = sec.getStringList(id + ".members");
                for (String entry : memberList) {
                    String[] parts = entry.split(":");
                    if (parts.length < 2) continue;
                    try {
                        UUID muuid = UUID.fromString(parts[0]);
                        TongMon.Rank rank = TongMon.Rank.valueOf(parts[1]);
                        tm.getMembers(); // trigger init
                        // Add directly via reflection workaround: use setRank after addMember
                        if (!muuid.equals(ownerUuid)) {
                            tm.addMember(muuid);
                            tm.setRank(muuid, rank);
                        }
                        playerTongMon.put(muuid, id);
                    } catch (Exception ignored) {}
                }
                playerTongMon.put(ownerUuid, id);
                tongMons.put(id, tm);
            } catch (Exception e) {
                plugin.getLogger().warning("Lỗi load Tông Môn " + id + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("[TuTien] Loaded " + tongMons.size() + " Tông Môn.");
    }

    public void saveAll() { save(); }
}
