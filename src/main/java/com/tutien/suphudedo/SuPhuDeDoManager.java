package com.tutien.suphudedo;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * SuPhuDeDoManager — Hệ thống Sư Đồ (Master-Disciple), xây dựng hoàn toàn mới.
 * Trước đây package này rỗng, lệnh /suphu và /dedoan chưa từng tồn tại.
 *
 * Cơ chế:
 *  - Người chơi cảnh giới cao hơn tối thiểu 3 bậc mới có thể nhận đệ tử
 *  - Mỗi Sư Phụ nhận tối đa MAX_DE_TU (5) đệ tử
 *  - Mỗi lần đệ tử nhận Linh Khí (thiền định), Sư Phụ đang online nhận thêm
 *    SUPHU_LK_SHARE_PCT (10%) số LK đó — khích lệ dẫn dắt lẫn nhau
 *  - Dữ liệu lưu trong PlayerData.extraData (không cần sửa schema)
 *
 * Lệnh:
 *   /suphu <player>       — nhận player làm đệ tử
 *   /dedoan                — xem thông tin Sư Phụ / danh sách Đệ Tử
 *   /dedoan roi             — đệ tử tự rời khỏi Sư Phụ
 *   /dedoan kick <player>  — Sư Phụ đuổi 1 đệ tử
 */
public class SuPhuDeDoManager implements CommandExecutor, TabCompleter, Listener {

    private final TuTienPlugin plugin;
    private final File npcFile;
    private YamlConfiguration npcYaml;
    private final Map<Integer, NpcDisciple> npcDisciples = new LinkedHashMap<>();
    private final Map<Integer, String> appliedNpcStatSignature = new HashMap<>();
    private final Map<Integer, UUID> appliedNpcEntityId = new HashMap<>();
    private int followerTaskId = -1;

    public static final int    MAX_DE_TU          = 5;
    public static final int    MIN_STAGE_GAP       = 3;
    public static final double SUPHU_LK_SHARE_PCT  = 0.10; // 10%

    private static final String KEY_SUPHU_UUID   = "suphu_uuid";
    private static final String KEY_DEDOAN_LIST  = "dedoan_list"; // comma-separated UUID

    public SuPhuDeDoManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.npcFile = new File(plugin.getDataFolder(), "dedoan-npc.yml");
        loadNpcDisciples();
        // Citizens is an optional soft-dependency. Reflection avoids hard-linking its classes at startup.
        this.followerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tickNpcFollowers, 20L, 20L);
    }

    // ══════════════════════════════════════════════
    //  COMMAND DISPATCH
    // ══════════════════════════════════════════════
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_fd17b10807"));
            return true;
        }

        String name = cmd.getName().toLowerCase();

        if (name.equals("suphu")) {
            if (args.length < 1) {
                player.sendMessage(plugin.prefix() + "§cCú pháp: §e/suphu <player>");
                return true;
            }
            handleNhanDeTu(player, args[0]);
            return true;
        }

        if (name.equals("dedoan")) {
            if (args.length == 0) { showInfo(player); return true; }
            if (args[0].equalsIgnoreCase("roi")) { handleRoiSuPhu(player); return true; }
            if (args[0].equalsIgnoreCase("kick") && args.length >= 2) { handleKick(player, args[1]); return true; }
            if (args[0].equalsIgnoreCase("npc")) { handleNpcCommand(player, args); return true; }
            showInfo(player);
            return true;
        }

        return false;
    }

    // ══════════════════════════════════════════════
    //  NHẬN ĐỆ TỬ
    // ══════════════════════════════════════════════
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("dedoan")) return Collections.emptyList();
        if (args.length == 1) return matchPrefix(List.of("roi", "kick", "npc"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("npc")) {
            return matchPrefix(List.of("tao", "list", "stats", "randomskip", "thechat", "linhcan", "tuchat", "xoa"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("npc")
                && List.of("stats", "randomskip", "skip", "thechat", "the_chat", "linhcan", "linh_can", "tuchat", "tu_chat", "aptitude", "xoa")
                .contains(args[1].toLowerCase(Locale.ROOT))) {
            List<String> ids = new ArrayList<>();
            for (NpcDisciple d : npcDisciples.values()) {
                if (sender instanceof Player player && (d.owner.equals(player.getUniqueId()) || player.hasPermission("tutien.admin"))) ids.add(String.valueOf(d.id));
            }
            return matchPrefix(ids, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("npc")) {
            if (List.of("thechat", "the_chat").contains(args[1].toLowerCase(Locale.ROOT)))
                return matchPrefix(npcYaml.getStringList("settings.body-types"), args[3]);
            if (List.of("linhcan", "linh_can").contains(args[1].toLowerCase(Locale.ROOT)))
                return matchPrefix(npcYaml.getStringList("settings.spiritual-roots"), args[3]);
            if (List.of("tuchat", "tu_chat", "aptitude").contains(args[1].toLowerCase(Locale.ROOT))) {
                List<String> values = new ArrayList<>(); for (int i = 1; i <= 100; i++) values.add(String.valueOf(i));
                return matchPrefix(values, args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> matchPrefix(Collection<String> values, String token) {
        String prefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return values.stream().filter(Objects::nonNull).filter(v -> v.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }

    private void handleNhanDeTu(Player suPhu, String targetName) {
        Player deTu = Bukkit.getPlayer(targetName);
        if (deTu == null || !deTu.isOnline()) {
            suPhu.sendMessage(plugin.prefix() + "§cKhông tìm thấy người chơi §e" + targetName + " §c(đang offline)!");
            return;
        }
        if (deTu.getUniqueId().equals(suPhu.getUniqueId())) {
            suPhu.sendMessage(plugin.prefix() + "§cBạn không thể nhận chính mình làm đệ tử!");
            return;
        }

        PlayerData spData = plugin.getDataManager().get(suPhu);
        PlayerData dtData = plugin.getDataManager().get(deTu);
        if (spData == null || dtData == null || !spData.hasClass() || !dtData.hasClass()) {
            suPhu.sendMessage(plugin.prefix() + "§cCả hai phải chọn class trước!");
            return;
        }

        // Đã có sư phụ chưa?
        String existingSuPhu = dtData.getExtraStr(KEY_SUPHU_UUID, "");
        if (!existingSuPhu.isEmpty()) {
            suPhu.sendMessage(plugin.prefix() + "§c" + deTu.getName() + " §cđã có Sư Phụ rồi!");
            return;
        }

        // Cảnh giới phải cao hơn tối thiểu MIN_STAGE_GAP bậc
        int gap = spData.getStage().ordinal() - dtData.getStage().ordinal();
        if (gap < MIN_STAGE_GAP) {
            suPhu.sendMessage(plugin.prefix() + "§cCảnh giới của bạn phải cao hơn §e"
                    + deTu.getName() + " §ctối thiểu §e" + MIN_STAGE_GAP + " bậc§c! (Hiện tại chênh " + gap + " bậc)");
            return;
        }

        // Kiểm tra số lượng đệ tử hiện tại
        List<UUID> deTuList = getDeTuList(spData);
        if (deTuList.size() >= MAX_DE_TU) {
            suPhu.sendMessage(plugin.prefix() + "§cBạn đã đạt tối đa §e" + MAX_DE_TU + " §cđệ tử!");
            return;
        }

        // Thành công -> lưu 2 chiều
        dtData.setExtraStr(KEY_SUPHU_UUID, suPhu.getUniqueId().toString());
        deTuList.add(deTu.getUniqueId());
        saveDeTuList(spData, deTuList);
        plugin.getDataManager().save(spData);
        plugin.getDataManager().save(dtData);

        // Thông báo + hiệu ứng
        suPhu.sendMessage(plugin.msg("auto_da39a3ee5e"));
        suPhu.sendMessage(plugin.msg("auto_7fcdcd636f"));
        suPhu.sendMessage(plugin.msg("auto_d4de3df054"));
        suPhu.sendMessage("  §7Đệ tử: §e" + deTu.getName());
        suPhu.sendMessage("  §7Số đệ tử: §e" + deTuList.size() + "§7/§e" + MAX_DE_TU);
        suPhu.sendMessage(plugin.msg("auto_045f213664"));
        suPhu.sendMessage(plugin.msg("auto_da39a3ee5e"));

        deTu.sendMessage(plugin.msg("auto_da39a3ee5e"));
        deTu.sendMessage(plugin.prefix() + "§6§l✦ Bạn đã trở thành đệ tử của §e" + suPhu.getName() + "§6!");
        deTu.sendMessage(plugin.prefix() + "§7Sư Phụ sẽ nhận §e10%§7 Linh Khí mỗi khi bạn thiền định.");
        deTu.sendMessage(plugin.msg("auto_da39a3ee5e"));

        for (Player p : new Player[]{suPhu, deTu}) {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0),
                    20, 0.5, 0.5, 0.5, 0);
        }
    }

    // ══════════════════════════════════════════════
    //  RỜI SƯ PHỤ / ĐUỔI ĐỆ TỬ
    // ══════════════════════════════════════════════
    private void handleRoiSuPhu(Player deTu) {
        PlayerData dtData = plugin.getDataManager().get(deTu);
        String suPhuUuidStr = dtData.getExtraStr(KEY_SUPHU_UUID, "");
        if (suPhuUuidStr.isEmpty()) {
            deTu.sendMessage(plugin.prefix() + "§cBạn chưa có Sư Phụ!");
            return;
        }
        dtData.setExtraStr(KEY_SUPHU_UUID, "");
        plugin.getDataManager().save(dtData);

        // Xóa khỏi danh sách của sư phụ (nếu sư phụ đang online, cập nhật cache trực tiếp)
        UUID suPhuUuid;
        try { suPhuUuid = UUID.fromString(suPhuUuidStr); }
        catch (IllegalArgumentException ex) {
            deTu.sendMessage(plugin.prefix() + "§cDữ liệu Sư Phụ không hợp lệ; đã xóa liên kết lỗi.");
            dtData.setExtraStr(KEY_SUPHU_UUID, "");
            plugin.getDataManager().save(dtData);
            return;
        }
        Player suPhu = Bukkit.getPlayer(suPhuUuid);
        PlayerData spData = suPhu != null ? plugin.getDataManager().get(suPhu) : plugin.getDataManager().get(suPhuUuid);
        if (spData != null) {
            List<UUID> list = getDeTuList(spData);
            list.remove(deTu.getUniqueId());
            saveDeTuList(spData, list);
            plugin.getDataManager().save(spData);
            if (suPhu != null) suPhu.sendMessage(plugin.prefix() + "§7Đệ tử §e" + deTu.getName() + " §7đã rời khỏi môn hạ của bạn.");
        } else {
            // Master may be offline and not present in cache: preserve all existing player data
            // and update only this extra key in the persisted YAML.
            File file = new File(plugin.getDataManager().getPlayersFolder(), suPhuUuid + ".yml");
            if (file.isFile()) {
                org.bukkit.configuration.file.YamlConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                String rawList = cfg.getString("extra." + KEY_DEDOAN_LIST, "");
                List<String> tokens = new ArrayList<>(Arrays.asList(rawList.split(",")));
                tokens.removeIf(token -> token.trim().equalsIgnoreCase(deTu.getUniqueId().toString()));
                cfg.set("extra." + KEY_DEDOAN_LIST, String.join(",", tokens.stream().filter(t -> !t.isBlank()).toList()));
                try { cfg.save(file); } catch (java.io.IOException ex) { plugin.getLogger().warning("Không thể cập nhật danh sách đệ tử offline: " + ex.getMessage()); }
            }
        }
        deTu.sendMessage(plugin.prefix() + "§7Bạn đã rời khỏi Sư Phụ.");
    }

    private void handleKick(Player suPhu, String targetName) {
        Player deTu = Bukkit.getPlayer(targetName);
        PlayerData spData = plugin.getDataManager().get(suPhu);
        List<UUID> list = getDeTuList(spData);

        UUID targetUuid = null;
        if (deTu != null) targetUuid = deTu.getUniqueId();
        else {
            suPhu.sendMessage(plugin.prefix() + "§cĐệ tử phải đang online để đuổi!");
            return;
        }

        if (!list.contains(targetUuid)) {
            suPhu.sendMessage(plugin.prefix() + "§c" + targetName + " §ckhông phải đệ tử của bạn!");
            return;
        }

        list.remove(targetUuid);
        saveDeTuList(spData, list);
        plugin.getDataManager().save(spData);

        PlayerData dtData = plugin.getDataManager().get(deTu);
        if (dtData != null) { dtData.setExtraStr(KEY_SUPHU_UUID, ""); plugin.getDataManager().save(dtData); }

        suPhu.sendMessage(plugin.prefix() + "§7Đã đuổi §e" + deTu.getName() + " §7khỏi môn hạ.");
        deTu.sendMessage(plugin.prefix() + "§cBạn đã bị Sư Phụ §e" + suPhu.getName() + " §cđuổi khỏi môn hạ!");
    }

    // ══════════════════════════════════════════════
    //  XEM THÔNG TIN
    // ══════════════════════════════════════════════
    private void showInfo(Player player) {
        PlayerData data = plugin.getDataManager().get(player);

        String suPhuUuidStr = data.getExtraStr(KEY_SUPHU_UUID, "");
        List<UUID> deTuList = getDeTuList(data);

        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_81030847ba"));

        if (!suPhuUuidStr.isEmpty()) {
            try {
                OfflinePlayerName spName = resolveName(UUID.fromString(suPhuUuidStr));
                player.sendMessage("  §7Sư Phụ của bạn: " + (spName.online ? "§a● " : "§7○ ") + "§e" + spName.name);
            } catch (IllegalArgumentException ex) {
                player.sendMessage("  §7Sư Phụ của bạn: §cDữ liệu không hợp lệ");
            }
        } else {
            player.sendMessage(plugin.msg("auto_cd6e66db25"));
        }

        if (!deTuList.isEmpty()) {
            player.sendMessage("  §7Đệ tử (§e" + deTuList.size() + "§7/§e" + MAX_DE_TU + "§7):");
            for (UUID uid : deTuList) {
                OfflinePlayerName dt = resolveName(uid);
                Player online = Bukkit.getPlayer(uid);
                String stageInfo = "";
                if (online != null) {
                    PlayerData d = plugin.getDataManager().get(online);
                    if (d != null) stageInfo = " §8- " + d.getStage().getFullDisplay(d.getRank());
                }
                player.sendMessage("    " + (dt.online ? "§a● " : "§7○ ") + "§f" + dt.name + stageInfo);
            }
        } else {
            player.sendMessage(plugin.msg("auto_63fe46fe5f"));
        }

        player.sendMessage("  §8/suphu <player> §7— nhận người chơi làm đệ tử (cần cao hơn " + MIN_STAGE_GAP + " bậc)");
        player.sendMessage("  §8/dedoan npc tao <tên> §7— tạo NPC đệ tử (cần Citizens)");
        player.sendMessage("  §8/dedoan npc list §7— danh sách NPC đệ tử");
        player.sendMessage(plugin.msg("auto_e2288cc371"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
    }

    // ══════════════════════════════════════════════
    //  HOOK: Chia sẻ LK khi đệ tử tu luyện
    //  (gọi từ MeditationSystem mỗi lần đệ tử nhận LK)
    // ══════════════════════════════════════════════
    public void onDeTuGainLK(Player deTu, PlayerData dtData, long gainedAmount) {
        String suPhuUuidStr = dtData.getExtraStr(KEY_SUPHU_UUID, "");
        if (suPhuUuidStr.isEmpty()) return;

        UUID suPhuUuid;
        try { suPhuUuid = UUID.fromString(suPhuUuidStr); } catch (IllegalArgumentException ex) {
            dtData.setExtraStr(KEY_SUPHU_UUID, ""); plugin.getDataManager().save(dtData);
            deTu.sendMessage(plugin.prefix() + "§cDữ liệu Sư Phụ không hợp lệ đã được làm sạch."); return;
        }
        Player suPhu = Bukkit.getPlayer(suPhuUuid);
        if (suPhu == null || !suPhu.isOnline()) return; // Sư Phụ offline -> không nhận được

        PlayerData spData = plugin.getDataManager().get(suPhu);
        if (spData == null) return;

        long share = (long) (gainedAmount * SUPHU_LK_SHARE_PCT);
        if (share <= 0) return;

        spData.addLinhKhi(share);
        if (spData.getLinhKhi() >= spData.getMaxLinhKhi()) {
            plugin.getBreakthroughManager().notifyBreakthroughReady(suPhu, spData);
        }
    }

    // ══════════════════════════════════════════════
    //  CITIZENS NPC DISCIPLES (optional hook, isolated per master)
    //  Stored independently in plugins/TuTienPlugin/dedoan-npc.yml.
    // ══════════════════════════════════════════════
    private void loadNpcDisciples() {
        npcYaml = YamlConfiguration.loadConfiguration(npcFile);
        npcYaml.addDefault("settings.max-followers-per-player", 3);
        npcYaml.addDefault("settings.follow-distance", 5.5);
        npcYaml.addDefault("settings.randomskip-min-ranks", 1);
        npcYaml.addDefault("settings.randomskip-max-ranks", 3);
        npcYaml.addDefault("settings.randomskip-cooldown-seconds", 300);
        npcYaml.addDefault("settings.body-types", List.of("PHAM_THE", "LINH_THE", "THANH_THE", "TIEN_THE", "HONG_MONG_THE"));
        npcYaml.addDefault("settings.spiritual-roots", List.of("KIM", "MOC", "THUY", "HOA", "THO", "LOI", "BANG", "PHONG", "AM", "QUANG"));
        npcYaml.options().copyDefaults(true);
        ConfigurationSection section = npcYaml.getConfigurationSection("disciples");
        if (section != null) for (String rawId : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(rawId); if (s == null) continue;
            try {
                int id = Integer.parseInt(rawId);
                UUID owner = UUID.fromString(s.getString("owner", ""));
                String stageName = s.getString("stage", "LUYEN_KHI");
                com.tutien.data.CultivationStage stage = com.tutien.data.CultivationStage.valueOf(stageName);
                int rank = Math.max(1, Math.min(stage.getMaxRank(), s.getInt("rank", 1)));
                NpcDisciple disciple = new NpcDisciple(id, owner, s.getString("name", "Đệ Tử"),
                        s.getString("npc-name", "DeTu"), stage, rank,
                        s.getString("the-chat", "PHAM_THE"), s.getString("linh-can", "KIM"),
                        Math.max(1, Math.min(100, s.getInt("tu-chat", 50))),
                        s.getLong("created-at", System.currentTimeMillis()), s.getLong("last-randomskip-at", 0L));
                npcDisciples.put(id, disciple);
            } catch (Exception ex) {
                plugin.getLogger().warning("Bỏ qua NPC đệ tử không hợp lệ tại dedoan-npc.yml, ID=" + rawId);
            }
        }
        saveNpcDisciples();
    }

    private void saveNpcDisciples() {
        npcYaml.set("disciples", null);
        for (NpcDisciple d : npcDisciples.values()) {
            String path = "disciples." + d.id;
            npcYaml.set(path + ".owner", d.owner.toString());
            npcYaml.set(path + ".name", d.name);
            npcYaml.set(path + ".npc-name", d.npcName);
            npcYaml.set(path + ".stage", d.stage.name());
            npcYaml.set(path + ".rank", d.rank);
            npcYaml.set(path + ".the-chat", d.theChat);
            npcYaml.set(path + ".linh-can", d.linhCan);
            npcYaml.set(path + ".tu-chat", d.tuChat);
            npcYaml.set(path + ".created-at", d.createdAt);
            npcYaml.set(path + ".last-randomskip-at", d.lastRandomSkipAt);
        }
        try { npcYaml.save(npcFile); }
        catch (IOException ex) { plugin.getLogger().warning("Không lưu được dedoan-npc.yml: " + ex.getMessage()); }
    }

    private void handleNpcCommand(Player owner, String[] args) {
        if (args.length < 2) {
            owner.sendMessage("§6§lĐỆ TỬ NPC §8— §7Cần cài Citizens");
            owner.sendMessage("§e/dedoan npc tao <tên> §7— tạo NPC đi theo bạn");
            owner.sendMessage("§e/dedoan npc list §7— xem danh sách NPC của bạn");
            owner.sendMessage("§e/dedoan npc stats <id> §7— xem Thể Chất/Linh Căn/Tư Chất riêng");
            owner.sendMessage("§e/dedoan npc randomskip <id> §7— RandomSkip riêng cho NPC");
            owner.sendMessage("§e/dedoan npc thechat <id> <loại> §7— đổi Thể Chất riêng");
            owner.sendMessage("§e/dedoan npc linhcan <id> <loại> §7— đổi Linh Căn riêng");
            owner.sendMessage("§e/dedoan npc tuchat <id> <1-100> §7— đổi Tư Chất riêng");
            owner.sendMessage("§e/dedoan npc xoa <id> §7— xóa NPC của bạn");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "tao", "create" -> {
                if (args.length < 3) { owner.sendMessage("§e/dedoan npc tao <tên>"); return; }
                createNpcDisciple(owner, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }
            case "list", "danhsach" -> listNpcDisciples(owner);
            case "stats", "info" -> {
                if (args.length < 3) { owner.sendMessage("§e/dedoan npc stats <id>"); return; }
                NpcDisciple d = ownedNpc(owner, args[2]); if (d != null) showNpcStats(owner, d);
            }
            case "randomskip", "skip" -> {
                if (args.length < 3) { owner.sendMessage("§e/dedoan npc randomskip <id>"); return; }
                NpcDisciple d = ownedNpc(owner, args[2]); if (d != null) randomSkipNpc(owner, d);
            }
            case "thechat", "the_chat" -> {
                if (args.length < 4) { owner.sendMessage("§e/dedoan npc thechat <id> <" + String.join("|", npcYaml.getStringList("settings.body-types")) + ">"); return; }
                NpcDisciple d = ownedNpc(owner, args[2]); if (d == null) return;
                String type = args[3].toUpperCase(Locale.ROOT);
                boolean allowed = npcYaml.getStringList("settings.body-types").stream().anyMatch(v -> v.equalsIgnoreCase(type));
                if (!allowed) { owner.sendMessage("§cThể Chất không hợp lệ. Cho phép: §e" + String.join(", ", npcYaml.getStringList("settings.body-types"))); return; }
                d.theChat = type; saveNpcDisciples(); updateNpcDisplay(d);
                owner.sendMessage("§aĐã đổi Thể Chất riêng của NPC §e#" + d.id + " §athành §e" + type + "§a.");
            }
            case "linhcan", "linh_can" -> {
                if (args.length < 4) { owner.sendMessage("§e/dedoan npc linhcan <id> <" + String.join("|", npcYaml.getStringList("settings.spiritual-roots")) + ">"); return; }
                NpcDisciple d = ownedNpc(owner, args[2]); if (d == null) return;
                String root = args[3].toUpperCase(Locale.ROOT);
                boolean allowed = npcYaml.getStringList("settings.spiritual-roots").stream().anyMatch(v -> v.equalsIgnoreCase(root));
                if (!allowed) { owner.sendMessage("§cLinh Căn không hợp lệ. Cho phép: §e" + String.join(", ", npcYaml.getStringList("settings.spiritual-roots"))); return; }
                d.linhCan = root; saveNpcDisciples(); updateNpcDisplay(d);
                owner.sendMessage("§aĐã đổi Linh Căn riêng của NPC §e#" + d.id + " §athành §b" + root + "§a.");
            }
            case "tuchat", "tu_chat", "aptitude" -> {
                if (args.length < 4) { owner.sendMessage("§e/dedoan npc tuchat <id> <1-100>"); return; }
                NpcDisciple d = ownedNpc(owner, args[2]); if (d == null) return;
                int aptitude;
                try { aptitude = Integer.parseInt(args[3]); } catch (NumberFormatException ex) { owner.sendMessage("§cTư Chất phải là số từ 1 đến 100."); return; }
                if (aptitude < 1 || aptitude > 100) { owner.sendMessage("§cTư Chất phải nằm trong khoảng 1–100."); return; }
                d.tuChat = aptitude; saveNpcDisciples();
                owner.sendMessage("§aĐã đổi Tư Chất riêng của NPC §e#" + d.id + " §athành §d" + aptitude + "§a.");
            }
            case "xoa", "delete", "remove" -> {
                if (args.length < 3) { owner.sendMessage("§e/dedoan npc xoa <id>"); return; }
                NpcDisciple d = ownedNpc(owner, args[2]); if (d != null) deleteNpcDisciple(owner, d);
            }
            default -> owner.sendMessage("§cKhông rõ lệnh. Dùng §e/dedoan npc§c để xem hướng dẫn.");
        }
    }

    private Object citizensRegistry() throws Exception {
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) throw new IllegalStateException("Chưa cài Citizens");
        Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
        return api.getMethod("getNPCRegistry").invoke(null);
    }

    private Object getCitizensNpc(int id) {
        try {
            Object registry = citizensRegistry();
            Method method = registry.getClass().getMethod("getById", int.class);
            return method.invoke(registry, id);
        } catch (Throwable ex) { return null; }
    }

    private void createNpcDisciple(Player owner, String requestedName) {
        int ownedCount = getNpcCount(owner.getUniqueId());
        int max = Math.max(1, Math.min(20, npcYaml.getInt("settings.max-followers-per-player", 3)));
        if (ownedCount >= max) { owner.sendMessage(plugin.prefix() + "§cBạn đã đạt giới hạn " + max + " NPC đệ tử."); return; }
        if (requestedName == null || requestedName.isBlank()) { owner.sendMessage("§cTên NPC không hợp lệ."); return; }
        String displayName = requestedName.trim().replaceAll("[§&][0-9a-fk-or]", "");
        if (displayName.length() > 32) displayName = displayName.substring(0, 32);
        String npcName = displayName.replaceAll("[^A-Za-z0-9_]", "_").replaceAll("_+", "_");
        if (npcName.length() < 3) npcName = "DeTu" + (System.currentTimeMillis() % 1000);
        if (npcName.length() > 16) npcName = npcName.substring(0, 16);
        try {
            Object registry = citizensRegistry();
            Method create = Arrays.stream(registry.getClass().getMethods())
                    .filter(m -> m.getName().equals("createNPC") && m.getParameterCount() == 2
                            && m.getParameterTypes()[0].isAssignableFrom(EntityType.class)
                            && m.getParameterTypes()[1] == String.class)
                    .findFirst().orElseThrow(() -> new IllegalStateException("Citizens API createNPC(EntityType,String) không tồn tại"));
            Object npc = create.invoke(registry, EntityType.PLAYER, npcName);
            int id = ((Number) npc.getClass().getMethod("getId").invoke(npc)).intValue();
            Location spawn = owner.getLocation().clone().add(1.2, 0, 0);
            npc.getClass().getMethod("spawn", Location.class).invoke(npc, spawn);
            try { npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, true); } catch (Throwable ignored) { }
            List<String> roots = npcYaml.getStringList("settings.spiritual-roots");
            List<String> bodies = npcYaml.getStringList("settings.body-types");
            String root = roots.isEmpty() ? "KIM" : roots.get(new Random().nextInt(roots.size()));
            String body = bodies.isEmpty() ? "PHAM_THE" : bodies.get(new Random().nextInt(bodies.size()));
            int aptitude = 25 + new Random().nextInt(76);
            NpcDisciple d = new NpcDisciple(id, owner.getUniqueId(), displayName, npcName,
                    com.tutien.data.CultivationStage.LUYEN_KHI, 1, body, root, aptitude, System.currentTimeMillis(), 0L);
            npcDisciples.put(id, d); saveNpcDisciples(); updateNpcDisplay(d);
            owner.sendMessage(plugin.prefix() + "§aĐã tạo NPC đệ tử §e" + displayName + " §a(ID §e" + id + "§a). NPC sẽ đi theo bạn khi cùng online.");
            owner.sendMessage(plugin.prefix() + "§7Dữ liệu riêng: Thể Chất §e" + body + "§7, Linh Căn §b" + root + "§7, Tư Chất §d" + aptitude + "§7.");
            owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.3f);
        } catch (Throwable ex) {
            owner.sendMessage(plugin.prefix() + "§cKhông tạo được NPC. Hãy cài Citizens tương thích và kiểm tra console.");
            plugin.getLogger().warning("Tạo NPC đệ tử lỗi: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private NpcDisciple ownedNpc(Player player, String rawId) {
        int id;
        try { id = Integer.parseInt(rawId); } catch (NumberFormatException ex) { player.sendMessage("§cID NPC phải là số."); return null; }
        NpcDisciple d = npcDisciples.get(id);
        if (d == null) { player.sendMessage("§cKhông tìm thấy NPC đệ tử #" + id + "."); return null; }
        if (!d.owner.equals(player.getUniqueId()) && !player.hasPermission("tutien.admin")) {
            player.sendMessage("§cBạn không sở hữu NPC này."); return null;
        }
        return d;
    }

    private void listNpcDisciples(Player player) {
        List<NpcDisciple> owned = npcDisciples.values().stream().filter(d -> d.owner.equals(player.getUniqueId()))
                .sorted(Comparator.comparingLong(d -> d.createdAt)).toList();
        player.sendMessage("§6§lNPC ĐỆ TỬ §8— §7" + owned.size() + "/" + npcYaml.getInt("settings.max-followers-per-player", 3));
        if (owned.isEmpty()) { player.sendMessage("§7Chưa có NPC. Dùng §e/dedoan npc tao <tên>§7."); return; }
        for (int i = 0; i < owned.size(); i++) {
            NpcDisciple d = owned.get(i);
            player.sendMessage("§e#" + d.id + " §f" + d.name + " §8| §b" + d.stage.getFullDisplay(d.rank)
                    + " §8| §7Thể Chất: §d" + d.theChat + " §8| §7Linh Căn: §b" + d.linhCan + " §8| §7Tư Chất: §e" + d.tuChat);
        }
    }

    private void showNpcStats(Player player, NpcDisciple d) {
        player.sendMessage("§6§l✦ ĐỆ TỬ NPC: " + d.name + " §8(#" + d.id + ")");
        player.sendMessage("§7Chủ nhân: §f" + Bukkit.getOfflinePlayer(d.owner).getName());
        player.sendMessage("§7Cảnh giới: §b" + d.stage.getFullDisplay(d.rank));
        player.sendMessage("§7Thể Chất riêng: §d" + d.theChat + " §8| §7Linh Căn riêng: §b" + d.linhCan + " §8| §7Tư Chất riêng: §e" + d.tuChat);
        long remaining = Math.max(0, (d.lastRandomSkipAt + npcYaml.getLong("settings.randomskip-cooldown-seconds", 300) * 1000L - System.currentTimeMillis()) / 1000L);
        player.sendMessage("§7RandomSkip: " + (remaining == 0 ? "§aSẵn sàng" : "§eCòn " + remaining + " giây hồi chiêu"));
    }

    private void randomSkipNpc(Player player, NpcDisciple d) {
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, npcYaml.getLong("settings.randomskip-cooldown-seconds", 300)) * 1000L;
        if (now - d.lastRandomSkipAt < cooldown) {
            player.sendMessage("§cNPC này còn hồi RandomSkip " + ((d.lastRandomSkipAt + cooldown - now + 999) / 1000L) + " giây."); return;
        }
        int min = Math.max(1, Math.min(6, npcYaml.getInt("settings.randomskip-min-ranks", 1)));
        int max = Math.max(min, Math.min(6, npcYaml.getInt("settings.randomskip-max-ranks", 3)));
        int requested = min + new Random().nextInt(max - min + 1);
        int advanced = 0;
        for (int i = 0; i < requested; i++) {
            if (d.stage.isTopStage()) break;
            if (d.rank < d.stage.getMaxRank()) d.rank++;
            else {
                com.tutien.data.CultivationStage next = d.stage.nextStage();
                if (next == null) break;
                d.stage = next; d.rank = 1;
            }
            advanced++;
        }
        d.lastRandomSkipAt = now;
        saveNpcDisciples(); updateNpcDisplay(d);
        if (advanced <= 0) player.sendMessage("§7Đệ tử đã chạm đỉnh cảnh giới; RandomSkip không thể tiến thêm.");
        else player.sendMessage(plugin.prefix() + "§d✦ RandomSkip đệ tử §e" + d.name + "§d: tiến thêm §e" + advanced + " §dbậc, hiện tại §b" + d.stage.getFullDisplay(d.rank) + "§d.");
    }

    private void deleteNpcDisciple(Player owner, NpcDisciple d) {
        try {
            Object npc = getCitizensNpc(d.id);
            if (npc != null) {
                try { npc.getClass().getMethod("destroy").invoke(npc); }
                catch (NoSuchMethodException ignored) { try { npc.getClass().getMethod("despawn").invoke(npc); } catch (Throwable ignoredAgain) { } }
            }
        } catch (Throwable ignored) { }
        npcDisciples.remove(d.id);
        appliedNpcStatSignature.remove(d.id); appliedNpcEntityId.remove(d.id);
        saveNpcDisciples();
        owner.sendMessage(plugin.prefix() + "§aĐã xóa dữ liệu NPC đệ tử §e#" + d.id + "§a.");
    }

    private void updateNpcDisplay(NpcDisciple d) {
        Object npc = getCitizensNpc(d.id); if (npc == null) return;
        try {
            Object entity = npc.getClass().getMethod("getEntity").invoke(npc);
            if (entity instanceof Entity bukkitEntity) {
                bukkitEntity.setCustomName("§6" + d.name + " §8[§b" + d.stage.getDisplayName() + " " + d.rank + "§8]");
                bukkitEntity.setCustomNameVisible(true);
                if (bukkitEntity instanceof LivingEntity living) applyNpcStatsIfChanged(d, living);
            }
        } catch (Throwable ex) {
            plugin.getLogger().fine("Không cập nhật được hiển thị/chỉ số NPC #" + d.id + ": " + ex.getClass().getSimpleName());
        }
    }

    /** Apply the disciple's independently stored body/root/aptitude to Bukkit attributes where supported. */
    private void applyNpcStatsIfChanged(NpcDisciple d, LivingEntity npc) {
        String signature = d.stage.name() + "|" + d.rank + "|" + d.theChat + "|" + d.linhCan + "|" + d.tuChat;
        UUID entityId = npc.getUniqueId();
        boolean newEntity = !entityId.equals(appliedNpcEntityId.get(d.id));
        if (!newEntity && signature.equals(appliedNpcStatSignature.get(d.id))) return;

        String body = d.theChat == null ? "PHAM_THE" : d.theChat.toUpperCase(Locale.ROOT);
        double bodyHealth = switch (body) {
            case "LINH_THE" -> 1.12; case "THANH_THE" -> 1.25; case "TIEN_THE" -> 1.45;
            case "HONG_MONG_THE", "HONGMONG_THE" -> 1.70; default -> 1.0;
        };
        double bodyAttack = switch (body) {
            case "LINH_THE" -> 1.03; case "THANH_THE" -> 1.08; case "TIEN_THE" -> 1.14;
            case "HONG_MONG_THE", "HONGMONG_THE" -> 1.22; default -> 1.0;
        };
        String root = d.linhCan == null ? "KIM" : d.linhCan.toUpperCase(Locale.ROOT);
        double rootHealth = switch (root) { case "MOC", "THUY", "QUANG", "HON", "HON_MANG" -> 1.10; default -> 1.0; };
        double rootAttack = switch (root) { case "HOA", "LOI", "AM", "HUYET", "VO_THUONG_CAN" -> 1.12; default -> 1.0; };
        double rootArmor = switch (root) { case "KIM", "THO", "BANG", "QUANG" -> 1.12; default -> 1.0; };
        double rootSpeed = switch (root) { case "PHONG", "LOI" -> 0.025; default -> 0.0; };
        double aptitude = 0.75 + Math.max(1, Math.min(100, d.tuChat)) / 200.0;
        double maxHealth = Math.max(20.0, Math.min(1024.0, (20.0 + d.stage.ordinal() * 1.8 + d.rank * 2.0) * aptitude * bodyHealth * rootHealth));
        double attack = Math.max(1.0, Math.min(200.0, (2.0 + d.stage.ordinal() * 0.55 + d.rank * 0.4) * aptitude * bodyAttack * rootAttack));
        double armor = Math.max(0.0, Math.min(30.0, (d.stage.ordinal() * 0.35 + d.rank * 0.25) * rootArmor));
        double speed = Math.max(0.08, Math.min(0.35, 0.09 + d.tuChat / 2000.0 + rootSpeed));
        setBaseAttribute(npc, Attribute.GENERIC_MAX_HEALTH, maxHealth);
        setBaseAttribute(npc, Attribute.GENERIC_ATTACK_DAMAGE, attack);
        setBaseAttribute(npc, Attribute.GENERIC_ARMOR, armor);
        setBaseAttribute(npc, Attribute.GENERIC_MOVEMENT_SPEED, speed);
        double effectiveMaxHealth = Math.max(1.0, npc.getMaxHealth());
        if (newEntity) npc.setHealth(effectiveMaxHealth);
        else if (npc.getHealth() > effectiveMaxHealth) npc.setHealth(effectiveMaxHealth);
        appliedNpcEntityId.put(d.id, entityId);
        appliedNpcStatSignature.put(d.id, signature);
    }

    private void setBaseAttribute(LivingEntity entity, Attribute attribute, double value) {
        try {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) instance.setBaseValue(value);
        } catch (Throwable ignored) { /* Some Citizens NPC entity implementations omit optional attributes. */ }
    }

    private void tickNpcFollowers() {
        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) return;
        for (NpcDisciple d : npcDisciples.values()) {
            Player owner = Bukkit.getPlayer(d.owner); if (owner == null || !owner.isOnline()) continue;
            Object npc = getCitizensNpc(d.id); if (npc == null) continue;
            try {
                boolean spawned = Boolean.TRUE.equals(npc.getClass().getMethod("isSpawned").invoke(npc));
                if (!spawned) {
                    npc.getClass().getMethod("spawn", Location.class).invoke(npc, owner.getLocation().clone().add(1.0, 0, 0));
                }
                Object entityRaw = npc.getClass().getMethod("getEntity").invoke(npc);
                if (!(entityRaw instanceof Entity entity) || !entity.isValid()) continue;
                updateNpcDisplay(d);
                if (!entity.getWorld().equals(owner.getWorld()) || entity.getLocation().distanceSquared(owner.getLocation()) > 36.0 * 36.0) {
                    // Teleport only when separated by worlds or very far; otherwise preserve navigation movement.
                    entity.teleport(owner.getLocation().clone().add(1.0, 0, 0));
                    continue;
                }
                double followDistance = Math.max(2.5, Math.min(16.0, npcYaml.getDouble("settings.follow-distance", 5.5)));
                Object navigator = npc.getClass().getMethod("getNavigator").invoke(npc);
                boolean navigating = false;
                try { navigating = Boolean.TRUE.equals(navigator.getClass().getMethod("isNavigating").invoke(navigator)); } catch (Throwable ignored) { }
                if (entity.getLocation().distanceSquared(owner.getLocation()) > followDistance * followDistance) {
                    if (!navigating) {
                        Method setTarget = Arrays.stream(navigator.getClass().getMethods()).filter(m -> m.getName().equals("setTarget") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(owner.getClass())).findFirst().orElse(null);
                        if (setTarget != null) setTarget.invoke(navigator, owner);
                        else {
                            Method setTargetAggressive = Arrays.stream(navigator.getClass().getMethods()).filter(m -> m.getName().equals("setTarget") && m.getParameterCount() == 2 && m.getParameterTypes()[0].isAssignableFrom(owner.getClass()) && m.getParameterTypes()[1] == boolean.class).findFirst().orElse(null);
                            if (setTargetAggressive != null) setTargetAggressive.invoke(navigator, owner, false);
                        }
                    }
                } else if (navigating) {
                    try { navigator.getClass().getMethod("cancelNavigation").invoke(navigator); } catch (Throwable ignored) { }
                }
            } catch (Throwable ex) {
                // Per-NPC errors must not stop followers belonging to other players.
                plugin.getLogger().fine("NPC disciple tick failed for #" + d.id + ": " + ex.getClass().getSimpleName());
            }
        }
    }

    public int getNpcCount(UUID owner) {
        int count = 0; for (NpcDisciple d : npcDisciples.values()) if (d.owner.equals(owner)) count++; return count;
    }

    public NpcDisciple getNpcDisciple(UUID owner, int ordinal) {
        if (ordinal < 1) return null;
        List<NpcDisciple> list = npcDisciples.values().stream().filter(d -> d.owner.equals(owner)).sorted(Comparator.comparingLong(d -> d.createdAt)).toList();
        return ordinal <= list.size() ? list.get(ordinal - 1) : null;
    }

    public static final class NpcDisciple {
        public final int id; public final UUID owner; public final String name; public final String npcName; public final long createdAt;
        public com.tutien.data.CultivationStage stage; public int rank; public String theChat; public String linhCan; public int tuChat; public long lastRandomSkipAt;
        private NpcDisciple(int id, UUID owner, String name, String npcName, com.tutien.data.CultivationStage stage, int rank,
                            String theChat, String linhCan, int tuChat, long createdAt, long lastRandomSkipAt) {
            this.id=id; this.owner=owner; this.name=name; this.npcName=npcName; this.stage=stage; this.rank=rank;
            this.theChat=theChat; this.linhCan=linhCan; this.tuChat=tuChat; this.createdAt=createdAt; this.lastRandomSkipAt=lastRandomSkipAt;
        }
    }

    // ══════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════
    private List<UUID> getDeTuList(PlayerData data) {
        String raw = data.getExtraStr(KEY_DEDOAN_LIST, "");
        List<UUID> list = new ArrayList<>();
        if (raw.isEmpty()) return list;
        for (String s : raw.split(",")) {
            if (s.isBlank()) continue;
            try { list.add(UUID.fromString(s)); } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    private void saveDeTuList(PlayerData data, List<UUID> list) {
        StringBuilder sb = new StringBuilder();
        for (UUID u : list) {
            if (sb.length() > 0) sb.append(",");
            sb.append(u.toString());
        }
        data.setExtraStr(KEY_DEDOAN_LIST, sb.toString());
    }

    private OfflinePlayerName resolveName(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return new OfflinePlayerName(online.getName(), true);
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return new OfflinePlayerName(name != null ? name : "???", false);
    }

    private record OfflinePlayerName(String name, boolean online) {}
}
