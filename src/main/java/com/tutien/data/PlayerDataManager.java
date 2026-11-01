package com.tutien.data;

import com.tutien.TuTienPlugin;
import com.tutien.lingcan.LinhCanElement;
import com.tutien.toc.TocType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class PlayerDataManager {

    private final TuTienPlugin plugin;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final File dataFolder;

    public PlayerDataManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "players");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        migrateLegacyData();
    }

    /**
     * Tự động quét và di trú dữ liệu người chơi từ các thư mục cũ (plugins/TuTien/players, plugins/TuTien)
     * để đảm bảo khi nâng cấp jar mới không bao giờ bị mất dữ liệu.
     */
    private void migrateLegacyData() {
        try {
            File parent = plugin.getDataFolder().getParentFile();
            if (parent == null || !parent.exists()) return;

            List<File> legacyFolders = List.of(
                    new File(parent, "TuTien" + File.separator + "players"),
                    new File(parent, "TuTien_Plugin" + File.separator + "players"),
                    new File(plugin.getDataFolder(), "playerdata")
            );

            for (File legacy : legacyFolders) {
                if (legacy.exists() && legacy.isDirectory()) {
                    File[] files = legacy.listFiles((d, name) -> name.endsWith(".yml"));
                    if (files != null) {
                        for (File lf : files) {
                            File target = new File(dataFolder, lf.getName());
                            if (!target.exists() || target.length() == 0) {
                                Files.copy(lf.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                plugin.getLogger().info("[TuTien Migration] Đã di trú dữ liệu: " + lf.getName());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[TuTien Migration] Lỗi kiểm tra di trú: " + e.getMessage());
        }
    }

    public PlayerData getOrCreate(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(),
                id -> load(id, player.getName()));
    }

    public java.util.List<PlayerData> getAllData() {
        return new java.util.ArrayList<>(cache.values());
    }

    public void resetPlayer(UUID uuid) {
        PlayerData old = cache.get(uuid);
        String name = old != null ? old.getPlayerName() : "Unknown";
        cache.put(uuid, new PlayerData(uuid, name));
    }

    public PlayerData get(UUID uuid)  {
        if (uuid == null) return null;
        PlayerData d = cache.get(uuid);
        if (d != null) return d;
        File file = new File(dataFolder, uuid + ".yml");
        if (file.exists() && file.length() > 0) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : "Unknown";
            d = load(uuid, name);
            cache.put(uuid, d);
            return d;
        }
        return null;
    }

    public PlayerData get(Player p)   {
        if (p == null) return null;
        return getOrCreate(p);
    }

    /** Thư mục chứa file .yml của TẤT CẢ người chơi (kể cả offline) - dùng cho leaderboard. */
    public File getPlayersFolder()    { return dataFolder; }

    public void unload(UUID uuid) {
        PlayerData d = cache.remove(uuid);
        if (d != null) save(d);
    }

    public void saveAll() {
        cache.values().forEach(this::save);
        plugin.getLogger().info("[TuTien] Đã lưu dữ liệu cho " + cache.size() + " tu sĩ trực tuyến.");
    }

    private PlayerData load(UUID uuid, String name) {
        File file = new File(dataFolder, uuid + ".yml");
        File bakFile = new File(dataFolder, uuid + ".yml.bak");

        // Nếu file chính bị 0 byte hoặc mất, cứu dữ liệu từ file backup .bak
        if (!file.exists() || file.length() == 0) {
            if (bakFile.exists() && bakFile.length() > 0) {
                try {
                    Files.copy(bakFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().warning("[TuTien] Khôi phục dữ liệu cho " + name + " từ file backup .bak thành công!");
                } catch (Exception ignored) {}
            }
        }

        PlayerData d = new PlayerData(uuid, name);
        if (!file.exists() || file.length() == 0) return d;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        d.setPlayerName(cfg.getString("name", name));

        // Nạp và phân giải cảnh giới an toàn tuyệt đối (không bao giờ tụt về Luyện Khí do lỗi chuỗi)
        String rawStage = cfg.getString("stage", "LUYEN_KHI");
        d.setStage(parseStage(rawStage));

        String cls = cfg.getString("class", null);
        if (cls != null) try { d.setClassType(ClassType.valueOf(cls)); } catch (Exception ignored) {}

        String dao = cfg.getString("dao", null);
        if (dao != null) try { d.setDaoPath(DaoPath.valueOf(dao)); } catch (Exception ignored) {}

        d.setRank(cfg.getInt("rank", 1));
        d.setLinhKhi(cfg.getLong("linhkhi", 0));
        d.setTotalLinhKhiEarned(cfg.getLong("totalLinhKhiEarned", d.getLinhKhi()));
        d.setBreakthroughSuccess(cfg.getInt("btSuccess", 0));
        d.setBreakthroughFail(cfg.getInt("btFail", 0));

        // Chỉ số chiến đấu & boss
        d.setKills(cfg.getInt("kills", 0));
        d.setDeaths(cfg.getInt("deaths", 0));
        d.setBossKills(cfg.getInt("bossKills", 0));
        d.setPvpKills(cfg.getInt("pvpKills", 0));
        d.setBossToiThuongKills(cfg.getInt("bossToiThuongKills", 0));

        // Sub-skills combo (4 slot)
        List<Integer> subList = cfg.getIntegerList("activeSubSkills");
        if (subList != null && subList.size() >= 4) {
            for (int i = 0; i < 4; i++) d.setActiveSubSkill(i, subList.get(i));
        }

        // Linh Căn
        List<String> lcNames = cfg.getStringList("linhcan");
        List<LinhCanElement> lc = new ArrayList<>();
        for (String s : lcNames) {
            try { lc.add(LinhCanElement.valueOf(s)); } catch (Exception ignored) {}
        }
        d.setLinhCan(lc);
        d.setLinhCanRevealed(cfg.getBoolean("linhcanRevealed", false));

        long legacyTien = cfg.getLong("tien", cfg.getLong("linhThachBag", 0L));
        if (legacyTien > 0) plugin.getTienManager().migrateLegacyBalance(d, legacyTien);

        String toc = cfg.getString("toc", null);
        if (toc != null) try { d.setToc(TocType.valueOf(toc)); } catch (Exception ignored) {}
        d.setTocRevealed(cfg.getBoolean("tocRevealed", false));
        d.setBtAtkBonus(cfg.getDouble("btAtkBonus", 0.0));
        d.setAuraEnabled(cfg.getBoolean("auraEnabled", true));
        d.setLearnedPassives(cfg.getStringList("passives"));

        // Nạp lại 18 ô túi trữ vật TuTienBag
        ItemStack[] bagStore = d.getBagStorage();
        org.bukkit.configuration.ConfigurationSection bagSec = cfg.getConfigurationSection("bagStorage");
        if (bagSec != null && bagStore != null) {
            for (int i = 0; i < bagStore.length; i++) {
                if (bagSec.contains(String.valueOf(i))) {
                    bagStore[i] = bagSec.getItemStack(String.valueOf(i));
                }
            }
        }

        // Nạp lại extraData: Công Đức, Nghiệp Lực, Uy Áp, Dị Hỏa, Thần Lôi, Động Phủ, Quest...
        java.util.Map<String, Object> extra = new HashMap<>();
        org.bukkit.configuration.ConfigurationSection extraSec = cfg.getConfigurationSection("extra");
        if (extraSec != null) {
            for (String key : extraSec.getKeys(false)) {
                extra.put(key, extraSec.get(key));
            }
        }
        d.loadExtraData(extra);

        return d;
    }

    /** Lưu 1 người chơi duy nhất — an toàn tuyệt đối với cơ chế backup tránh hỏng file. */
    public void save(PlayerData d) {
        if (d == null || d.getUuid() == null) return;
        File file = new File(dataFolder, d.getUuid() + ".yml");
        File bakFile = new File(dataFolder, d.getUuid() + ".yml.bak");
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("name",    d.getPlayerName());
        cfg.set("stage",   d.getStage().name());
        cfg.set("class",   d.getClassType() != null ? d.getClassType().name() : null);
        cfg.set("dao",     d.getDaoPath()   != null ? d.getDaoPath().name()   : null);
        cfg.set("rank",    d.getRank());
        cfg.set("linhkhi", d.getLinhKhi());
        cfg.set("totalLinhKhiEarned", d.getTotalLinhKhiEarned());
        cfg.set("btSuccess", d.getBreakthroughSuccess());
        cfg.set("btFail", d.getBreakthroughFail());
        cfg.set("kills",   d.getKills());
        cfg.set("deaths",  d.getDeaths());
        cfg.set("bossKills", d.getBossKills());
        cfg.set("pvpKills", d.getPvpKills());
        cfg.set("bossToiThuongKills", d.getBossToiThuongKills());

        cfg.set("activeSubSkills", List.of(
                d.getActiveSubSkill(0),
                d.getActiveSubSkill(1),
                d.getActiveSubSkill(2),
                d.getActiveSubSkill(3)
        ));

        List<String> lcNames = new ArrayList<>();
        for (LinhCanElement el : d.getLinhCan()) lcNames.add(el.name());
        cfg.set("linhcan", lcNames);
        cfg.set("linhcanRevealed", d.isLinhCanRevealed());

        cfg.set("toc",     d.getToc()     != null ? d.getToc().name()     : null);
        cfg.set("tocRevealed", d.isTocRevealed());
        cfg.set("btAtkBonus",  d.getBtAtkBonus());
        cfg.set("auraEnabled", d.isAuraEnabled());
        cfg.set("passives", new ArrayList<>(d.getLearnedPassives()));

        // Lưu trữ 18 ô túi trữ vật TuTienBag
        ItemStack[] bagStore = d.getBagStorage();
        if (bagStore != null) {
            for (int i = 0; i < bagStore.length; i++) {
                if (bagStore[i] != null) {
                    cfg.set("bagStorage." + i, bagStore[i]);
                }
            }
        }

        // Lưu extraData: Công Đức, Nghiệp Lực, bật/tắt Uy Áp, cấp bản mệnh, v.v.
        for (Map.Entry<String, Object> e : d.getExtraData().entrySet()) {
            cfg.set("extra." + e.getKey(), e.getValue());
        }

        try {
            cfg.save(file);
            // Tạo bản sao lưu dự phòng .bak
            try {
                Files.copy(file.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {}
        } catch (IOException e) {
            plugin.getLogger().warning("[TuTien] Không thể lưu file cho người chơi: " + d.getPlayerName());
        }
    }

    /**
     * Phân giải cảnh giới cực kỳ an toàn: hỗ trợ tên enum, tên hiển thị có dấu/không dấu,
     * chữ hoa/thường, và ánh xạ các tên cảnh giới cũ từ các phiên bản trước.
     */
    public static CultivationStage parseStage(String raw) {
        if (raw == null || raw.isBlank()) return CultivationStage.LUYEN_KHI;
        String clean = raw.trim();

        // 1. Khớp chính xác tên enum
        try {
            return CultivationStage.valueOf(clean.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        // 2. Khớp theo tên chuẩn hóa và tên hiển thị
        String norm = clean.toUpperCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
        for (CultivationStage s : CultivationStage.values()) {
            if (s.name().equalsIgnoreCase(norm) || s.getDisplayName().equalsIgnoreCase(clean)) {
                return s;
            }
        }

        // 3. Khớp không dấu
        String noAccents = stripAccents(clean).toUpperCase(Locale.ROOT).replace(" ", "_");
        for (CultivationStage s : CultivationStage.values()) {
            String sNoAccents = stripAccents(s.getDisplayName()).toUpperCase(Locale.ROOT).replace(" ", "_");
            if (s.name().equalsIgnoreCase(noAccents) || sNoAccents.equalsIgnoreCase(noAccents)) {
                return s;
            }
        }

        // 4. Ánh xạ các tên cảnh giới cũ (hệ 52-70 tầng sang hệ 25 tầng)
        if (norm.contains("VO_THUONG") || norm.contains("CHI_TON")) return CultivationStage.VO_THUONG_CHI_TON;
        if (norm.contains("HON_NGUYEN")) return CultivationStage.HON_NGUYEN;
        if (norm.contains("THIEN_TIEN_DE") || norm.contains("THIEN_DE")) return CultivationStage.THIEN_DE;
        if (norm.contains("THIEN_DAO")) return CultivationStage.THIEN_DAO;
        if (norm.contains("THIEN_TON") || norm.contains("DAO_TO")) return CultivationStage.THIEN_TON;
        if (norm.contains("TIEN_DE")) return CultivationStage.TIEN_DE;
        if (norm.contains("DAI_LA")) return CultivationStage.DAI_LA;
        if (norm.contains("THAI_AT")) return CultivationStage.THAI_AT;
        if (norm.contains("KIM_TIEN")) return CultivationStage.KIM_TIEN;
        if (norm.contains("CHAN_TIEN")) return CultivationStage.CHAN_TIEN;
        if (norm.contains("THANH_DE")) return CultivationStage.THANH_DE;
        if (norm.contains("THANH_TON")) return CultivationStage.THANH_TON;
        if (norm.contains("DAI_THANH")) return CultivationStage.DAI_THANH;
        if (norm.contains("CHUAN_THANH")) return CultivationStage.CHUAN_THANH;
        if (norm.contains("BAN_THANH")) return CultivationStage.BAN_THANH;
        if (norm.contains("DE_CHU")) return CultivationStage.DE_CHU;
        if (norm.contains("DE_TON")) return CultivationStage.DE_TON;
        if (norm.contains("DE_VUONG")) return CultivationStage.DE_VUONG;
        if (norm.contains("DE_HAU")) return CultivationStage.DE_HAU;
        if (norm.contains("DE_TU")) return CultivationStage.DE_TU;
        if (norm.contains("HOA_THAN") || norm.contains("DAI_THUA") || norm.contains("HOP_THE") || norm.contains("LUYEN_HU")) return CultivationStage.HOA_THAN;
        if (norm.contains("NGUYEN_ANH")) return CultivationStage.NGUYEN_ANH;
        if (norm.contains("KET_DAN")) return CultivationStage.KET_DAN;
        if (norm.contains("TRUC_CO")) return CultivationStage.TRUC_CO;

        return CultivationStage.LUYEN_KHI;
    }

    private static String stripAccents(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }

    public Collection<PlayerData> getAllCached() { return List.copyOf(cache.values()); }
}