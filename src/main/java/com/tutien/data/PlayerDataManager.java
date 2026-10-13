package com.tutien.data;

import com.tutien.TuTienPlugin;
import com.tutien.lingcan.LinhCanElement;
import com.tutien.toc.TocType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {

    private final TuTienPlugin plugin;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final File dataFolder;

    public PlayerDataManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "players");
        if (!dataFolder.exists()) dataFolder.mkdirs();
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

    public PlayerData get(UUID uuid)  { return cache.get(uuid); }
    public PlayerData get(Player p)   { return cache.get(p.getUniqueId()); }
    /** Thư mục chứa file .yml của TẤT CẢ người chơi (kể cả offline) - dùng cho leaderboard. */
    public File getPlayersFolder()    { return dataFolder; }

    public void unload(UUID uuid) {
        PlayerData d = cache.remove(uuid);
        if (d != null) save(d);
    }

    public void saveAll() {
        cache.values().forEach(this::save);
        plugin.getLogger().info("[TuTien] Saved " + cache.size() + " players.");
    }

    private PlayerData load(UUID uuid, String name) {
        File file = new File(dataFolder, uuid + ".yml");
        PlayerData d = new PlayerData(uuid, name);
        if (!file.exists()) return d;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        d.setPlayerName(cfg.getString("name", name));

        try { d.setStage(CultivationStage.valueOf(cfg.getString("stage","LUYEN_KHI"))); }
        catch (IllegalArgumentException e) { d.setStage(CultivationStage.LUYEN_KHI); }

        String cls = cfg.getString("class", null);
        if (cls != null) try { d.setClassType(ClassType.valueOf(cls)); } catch (Exception ignored) {}

        String dao = cfg.getString("dao", null);
        if (dao != null) try { d.setDaoPath(DaoPath.valueOf(dao)); } catch (Exception ignored) {}

        d.setRank(cfg.getInt("rank", 1));
        d.setLinhKhi(cfg.getLong("linhkhi", 0));
        // Cac chi so nay TRUOC DAY duoc ghi vao file (save()) nhung khong bao gio
        // duoc doc lai o day -> mat het ve 0 moi lan restart server.
        d.setKills(cfg.getInt("kills", 0));
        d.setDeaths(cfg.getInt("deaths", 0));
        d.setBossKills(cfg.getInt("bossKills", 0));
        d.setPvpKills(cfg.getInt("pvpKills", 0));
        d.setBossToiThuongKills(cfg.getInt("bossToiThuongKills", 0));

        // Linh Can
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

        // Nạp lại extraData: Công Đức, Nghiệp Lực, bật/tắt Uy Áp, cấp bản mệnh, v.v.
        // (trước đây phần này KHÔNG được nạp -> mất hết khi relog/restart server)
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

    /** Lưu 1 người chơi duy nhất — dùng cho các lệnh nhỏ (VD /uyap) thay vì saveAll(),
     *  tránh việc ghi đĩa toàn bộ người chơi online chỉ vì 1 người đổi 1 cờ trạng thái. */
    public void save(PlayerData d) {
        File file = new File(dataFolder, d.getUuid() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("name",    d.getPlayerName());
        cfg.set("stage",   d.getStage().name());
        cfg.set("class",   d.getClassType() != null ? d.getClassType().name() : null);
        cfg.set("dao",     d.getDaoPath()   != null ? d.getDaoPath().name()   : null);
        cfg.set("rank",    d.getRank());
        cfg.set("linhkhi", d.getLinhKhi());
        cfg.set("kills",   d.getKills());
        cfg.set("deaths",  d.getDeaths());
        cfg.set("bossKills", d.getBossKills());
        cfg.set("pvpKills", d.getPvpKills());
        cfg.set("bossToiThuongKills", d.getBossToiThuongKills());

        List<String> lcNames = new ArrayList<>();
        for (LinhCanElement el : d.getLinhCan()) lcNames.add(el.name());
        cfg.set("linhcan", lcNames);
        cfg.set("linhcanRevealed", d.isLinhCanRevealed());
        // Money is stored by Vault; do not persist a second balance here.
        cfg.set("toc",     d.getToc()     != null ? d.getToc().name()     : null);
        cfg.set("tocRevealed", d.isTocRevealed());
        cfg.set("btAtkBonus",  d.getBtAtkBonus());
        cfg.set("auraEnabled", d.isAuraEnabled());
        cfg.set("passives", new ArrayList<>(d.getLearnedPassives()));

        // Lưu extraData: Công Đức, Nghiệp Lực, bật/tắt Uy Áp, cấp bản mệnh, v.v.
        for (Map.Entry<String, Object> e : d.getExtraData().entrySet()) {
            cfg.set("extra." + e.getKey(), e.getValue());
        }

        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Cannot save: " + d.getPlayerName()); }
    }

    public Collection<PlayerData> getAllCached() { return List.copyOf(cache.values()); }
}