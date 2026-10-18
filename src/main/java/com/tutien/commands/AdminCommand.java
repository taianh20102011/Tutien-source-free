package com.tutien.commands;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.ClassType;
import com.tutien.data.DaoPath;
import com.tutien.data.PlayerData;
import com.tutien.lingcan.LinhCanElement;
import com.tutien.lingcan.LinhCanGrade;
import com.tutien.phabao.PhaBaoTier;
import com.tutien.phabao.PhaBaoType;
import com.tutien.toc.TocType;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * /ttadmin - Lệnh admin đầy đủ cho TuTien plugin.
 *
 * Quyền: tutien.admin (hoặc op)
 *
 * Lệnh:
 *   /ttadmin info <player>          - Xem info đầy đủ
 *   /ttadmin set canh_gioi <p> <cap> - Đặt cảnh giới
 *   /ttadmin set tang <p> <tang>    - Đặt tầng
 *   /ttadmin set class <p> <class>  - Đặt class
 *   /ttadmin set dao <p> <dao>      - Đặt đạo path
 *   /ttadmin set linhkhi <p> <so>   - Đặt linh khí
 *   /ttadmin set tien <p> <so>      - Đặt số dư Tiền
 *   /ttadmin set toc <p> <toc>      - Đặt tộc
 *   /ttadmin give phabao <p> [tier] [type] - Tặng pháp bảo
 *   /ttadmin give tien <p> <so>     - Tặng Tiền
 *   /ttadmin give tuvi <p>          - Reset linh căn
 *   /ttadmin reset <p>              - Reset toàn bộ nhân vật
 *   /ttadmin reload                 - Reload dữ liệu
 *   /ttadmin top [stat]             - Bảng xếp hạng
 *   /ttadmin broadcast <msg>        - Broadcast có màu
 *   /ttadmin dotpha <p>             - Force đột phá cho player
 *   /ttadmin maxlk <p>              - Max LK cho player
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public AdminCommand(TuTienPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("tutien.admin") && !(sender instanceof Player p && p.isOp())
                && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(plugin.msg("auto_ad98635907"));
            return true;
        }

        if (args.length == 0) { showHelp(sender); return true; }

        return switch (args[0].toLowerCase()) {

            // ======================== INFO ========================
            case "info" -> {
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_bd624637cd")); yield true; }
                Player target = resolvePlayer(sender, args[1]);
                if (target == null) { sender.sendMessage(plugin.msg("auto_096b62eb8f")); yield true; }
                PlayerData data = plugin.getDataManager().get(target);
                showPlayerInfo(sender, target, data);
                yield true;
            }

            // ======================== SET ========================
            case "set" -> {
                if (args.length < 4) { sender.sendMessage(plugin.msg("auto_d07613065e")); yield true; }
                Player target = resolvePlayer(sender, args[2]);
                if (target == null) { sender.sendMessage(plugin.msg("auto_8f8551f79e")); yield true; }
                PlayerData data = plugin.getDataManager().get(target);
                handleSet(sender, target, data, args[1].toLowerCase(), args[3]);
                yield true;
            }

            // ======================== GIVE ========================
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.msg("auto_b2763e2505"));
                    sender.sendMessage(plugin.msg("auto_dc75e9218c"));
                    sender.sendMessage(plugin.msg("auto_06b1cd667b"));
                    yield true;
                }

                // ── Cách nhanh: /ttadmin give <player> <PHABAO_ID> [so] ──
                // Tự động nhận diện nếu args[2] khớp 1 tên PhaBaoType (vd: TRAN_THIEN_GIAP)
                PhaBaoType directType = tryParsePhaBaoTypeExact(args[2]);
                if (directType != null) {
                    Player target = resolvePlayer(sender, args[1]);
                    if (target == null) {
                        sender.sendMessage(playerNotFoundMsg(args[1]));
                        yield true;
                    }
                    int count = args.length >= 4 ? parseInt(args[3], 1) : 1;
                    givePhaBaoDirect(sender, target, directType, count);
                    yield true;
                }

                // ── Cú pháp gốc: /ttadmin give <loại> <player> [...] ──
                Player target = resolvePlayer(sender, args[2]);
                if (target == null) { sender.sendMessage(playerNotFoundMsg(args[2])); yield true; }
                PlayerData data = plugin.getDataManager().get(target);
                handleGive(sender, target, data, args[1].toLowerCase(), args);
                yield true;
            }

            // ======================== RESET ========================
            case "reset" -> {
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_9923fa65b2")); yield true; }
                if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                    sender.sendMessage("§c/ttadmin reset " + args[1] + " confirm");
                    yield true;
                }
                Player target = resolvePlayer(sender, args[1]);
                if (target == null) { sender.sendMessage(plugin.msg("auto_8f8551f79e")); yield true; }
                plugin.getDataManager().resetPlayer(target.getUniqueId());
                target.sendMessage(plugin.prefix() + "§cNhân vật của bạn đã bị admin reset!");
                sender.sendMessage("§aĐã reset nhân vật " + target.getName());
                plugin.getSkillItemManager().giveSkillItems(target, plugin.getDataManager().get(target));
                yield true;
            }

            // ======================== RELOAD ========================
            case "zone" -> {
                // /tta zone create <radius> <mult> <name>
                // /tta zone list
                // /tta zone remove <id>
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_610c9f9d42")); yield true; }
                switch (args[1].toLowerCase()) {
                    case "create" -> {
                        if (!(sender instanceof org.bukkit.entity.Player p)) { sender.sendMessage(plugin.msg("auto_9306c7020f")); break; }
                        double rad = 30.0;
                        if (args.length > 2) {
                            try { rad = Double.parseDouble(args[2]); }
                            catch (NumberFormatException ex) { sender.sendMessage("§cBán kính phải là số trong khoảng 1–500."); break; }
                        }
                        if (!Double.isFinite(rad) || rad < 1.0 || rad > 500.0) { sender.sendMessage("§cBán kính phải nằm trong khoảng 1–500."); break; }
                        String type = args.length > 3 ? args[3] : "LINH_MACH";
                        String name = args.length > 4 ? String.join(" ", java.util.Arrays.copyOfRange(args,4,args.length)) : "Khu Vực Tu Luyện";
                        com.tutien.zone.ZoneManager.Zone zone = plugin.getZoneManager().createZone(p.getLocation(), rad, type, name);
                        sender.sendMessage("§aZone §e#" + zone.id + " §f" + name + " §abán kính §e" + rad + "m §aloại §e" + type);
                    }
                    case "list" -> {
                        plugin.getZoneManager().showZoneList(sender);
                    }
                    case "remove" -> {
                        int id = args.length > 2 ? Integer.parseInt(args[2]) : -1;
                        if (plugin.getZoneManager().removeZone(id)) sender.sendMessage("§aXóa zone #" + id);
                        else sender.sendMessage("§cKhông tìm thấy zone #" + id);
                    }
                }
                yield true;
            }
            case "arena" -> {
                if (!(sender instanceof Player arenaAdmin)) { sender.sendMessage(plugin.msg("auto_7414355e90")); yield true; }
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("auto_dc9d0e04f5"));
                    sender.sendMessage(plugin.msg("auto_58e71007dd"));
                    yield true;
                }
                if (args[1].equalsIgnoreCase("add")) {
                    sender.sendMessage(plugin.msg("auto_e6754816bb"));
                } else {
                    sender.sendMessage(plugin.msg("auto_e6754816bb"));
                }
                yield true;
            }
            case "bloodmoon", "bm" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.msg("auto_a0bad53666"));
                    sender.sendMessage(plugin.msg("auto_e9b6a70888"));
                    sender.sendMessage(plugin.msg("auto_1e0d970691"));
                    sender.sendMessage(plugin.msg("auto_e1383b5e45"));
                    yield true;
                }
                switch (args[1].toLowerCase()) {
                    case "start"  -> { plugin.getBloodMoonManager().startBloodMoon(); sender.sendMessage(plugin.msg("auto_6546f27669")); }
                    case "stop"   -> { plugin.getBloodMoonManager().stopBloodMoon();  sender.sendMessage(plugin.msg("auto_3d33906381")); }
                    case "status" -> sender.sendMessage("§7Active: " + plugin.getBloodMoonManager().isActive()
                            + " | HT: " + plugin.getBloodMoonManager().getHuyetTinhPct() + "%");
                    default -> sender.sendMessage(plugin.msg("auto_fdd08e6d00"));
                }
                yield true;
            }
            case "worldboss", "wb" -> {
                if (args.length < 2) {
                    sender.sendMessage("§e/ttadmin worldboss spawn §7- ép Boss Tối Thượng xuất hiện ngay");
                    sender.sendMessage("§e/ttadmin worldboss despawn §7- xóa boss đang hoạt động (không thưởng)");
                    sender.sendMessage("§e/ttadmin worldboss status §7- xem trạng thái hiện tại");
                    yield true;
                }
                switch (args[1].toLowerCase()) {
                    case "spawn"   -> { plugin.getWorldBossManager().forceSpawn(); sender.sendMessage("§a[WorldBoss] Đã yêu cầu spawn."); }
                    case "despawn" -> { plugin.getWorldBossManager().forceDespawn(); sender.sendMessage("§a[WorldBoss] Đã xóa boss (nếu có)."); }
                    case "status"  -> sender.sendMessage(plugin.getWorldBossManager().status());
                    default -> sender.sendMessage("§cDùng: /ttadmin worldboss <spawn|despawn|status>");
                }
                yield true;
            }
            // ======================== SEASON (META MUA) ========================
            case "season" -> {
                if (args.length < 2) {
                    sender.sendMessage(plugin.getSeasonManager().status());
                    sender.sendMessage("§e/ttadmin season set <class> <dao> §7- đặt class + đạo mạnh nhất mùa này");
                    sender.sendMessage("§e/ttadmin season clear §7- xóa season (không ai được bonus)");
                    sender.sendMessage("§e/ttadmin season info §7- xem season hiện tại");
                    yield true;
                }
                switch (args[1].toLowerCase()) {
                    case "set" -> {
                        if (args.length < 4) {
                            sender.sendMessage("§cDùng: /ttadmin season set <class> <dao>");
                            sender.sendMessage("§7Class: §e" + Arrays.toString(ClassType.values()));
                            sender.sendMessage("§7Dao: §e" + Arrays.toString(DaoPath.values()));
                            break;
                        }
                        ClassType ct;
                        try { ct = ClassType.valueOf(args[2].toUpperCase()); }
                        catch (IllegalArgumentException ex) { sender.sendMessage("§cClass không hợp lệ: §e" + args[2]); break; }
                        DaoPath dp;
                        try { dp = DaoPath.valueOf(args[3].toUpperCase()); }
                        catch (IllegalArgumentException ex) { sender.sendMessage("§cĐạo không hợp lệ: §e" + args[3]); break; }
                        plugin.getSeasonManager().setSeason(ct, dp);
                        sender.sendMessage("§aĐã đặt season: §e" + ct.getDisplayName() + " §7+ §e" + dp.getDisplayName());
                    }
                    case "clear" -> {
                        plugin.getSeasonManager().clearSeason();
                        sender.sendMessage("§aĐã xóa season hiện tại.");
                    }
                    case "info" -> sender.sendMessage(plugin.getSeasonManager().status());
                    default -> sender.sendMessage("§cDùng: /ttadmin season <set|clear|info>");
                }
                yield true;
            }

            case "reload" -> {
                plugin.getDataManager().saveAll();
                plugin.reloadConfig();
                plugin.refreshConfigState();
                plugin.getPetTierManager().reload();
                plugin.getAlchemyManager().reload();
                plugin.getHerbManager().reload();
                int customStages = com.tutien.data.CultivationStage.loadCustomizations(
                        new java.io.File(plugin.getDataFolder(), "custom-canh-gioi.yml"), plugin.getLogger());
                for (com.tutien.data.PlayerData data : plugin.getDataManager().getAllData()) {
                    if (data.getRank() > data.getStage().getMaxRank()) {
                        data.setRank(data.getStage().getMaxRank());
                    }
                    long maxLinhKhi = data.getMaxLinhKhi();
                    if (maxLinhKhi > 0 && data.getLinhKhi() > maxLinhKhi) data.setLinhKhi(maxLinhKhi);
                }
                plugin.getDataManager().saveAll();
                sender.sendMessage("§aĐã reload: §eLinh Thú + Luyện Đan + Vườn Tiên Thảo + "
                        + customStages + " slot cảnh giới tuỳ biến§a.");
                yield true;
            }
            case "vuon", "herbgarden" -> {
                if (args.length < 2) {
                    sender.sendMessage("§6/ttadmin vuon tao <tên> <bán_kính>");
                    sender.sendMessage("§6/ttadmin vuon list");
                    sender.sendMessage("§6/ttadmin vuon xoa <tên>");
                    yield true;
                }
                switch (args[1].toLowerCase()) {
                    case "tao", "create" -> {
                        if (!(sender instanceof Player p)) { sender.sendMessage("§cLệnh này cần đứng trong game."); break; }
                        String name = args.length >= 3 ? args[2] : "vuon_1";
                        int radius;
                        try { radius = args.length >= 4 ? Integer.parseInt(args[3]) : 25; }
                        catch (NumberFormatException ex) { radius = 25; }
                        plugin.getHerbManager().createGarden(name, p, radius);
                        sender.sendMessage("§aĐã tạo vườn §e"+name+" §abán kính §e"+Math.max(2,Math.min(radius,500))+"m§a.");
                    }
                    case "list", "ds" -> sender.sendMessage("§aVườn hiện có: §e"+String.join("§7, §e", plugin.getHerbManager().getGardenNames()));
                    case "xoa", "remove" -> {
                        if (args.length < 3 || !plugin.getHerbManager().removeGarden(args[2])) sender.sendMessage("§cKhông tìm thấy vườn.");
                        else sender.sendMessage("§aĐã xóa vườn §e"+args[2]);
                    }
                    default -> sender.sendMessage("§cDùng: /ttadmin vuon tao <tên> <bán_kính>");
                }
                yield true;
            }
            // ======================== THẢO DƯỢC / LINH CHỦNG ========================
            case "thao", "herb" -> {
                if (args.length < 4) {
                    sender.sendMessage("§6/ttadmin thao give <player> <herbId> [soLuong]");
                    sender.sendMessage("§6/ttadmin thao seed <player> <herbId> [soLuong]");
                    yield true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) { sender.sendMessage("§cKhông tìm thấy người chơi."); yield true; }
                String herbId = args[3];
                int amount;
                try { amount = args.length >= 5 ? Integer.parseInt(args[4]) : 1; }
                catch (NumberFormatException ex) { amount = 1; }
                amount = Math.max(1, amount);
                var opt = plugin.getHerbManager().getHerbType(herbId);
                if (opt.isEmpty()) { sender.sendMessage("§cKhông có loại thảo dược ID §e"+herbId); yield true; }
                switch (args[1].toLowerCase()) {
                    case "give" -> {
                        target.getInventory().addItem(plugin.getHerbManager().createHerb(opt.get(), amount));
                        sender.sendMessage("§aĐã đưa §e"+amount+"x "+opt.get().name()+" §acho "+target.getName());
                    }
                    case "seed" -> {
                        if (!opt.get().plantable()) { sender.sendMessage("§cLoại thảo dược này không trồng được."); yield true; }
                        target.getInventory().addItem(plugin.getHerbManager().createSeed(opt.get(), amount));
                        sender.sendMessage("§aĐã đưa §e"+amount+"x Linh Chủng "+opt.get().name()+" §acho "+target.getName());
                    }
                    default -> sender.sendMessage("§cDùng: /ttadmin thao give|seed <player> <herbId> [soLuong]");
                }
                yield true;
            }

            // ======================== TAI XIU ON/OFF ========================
            case "top" -> {
                showTop(sender, args.length >= 2 ? args[1] : "stage");
                yield true;
            }

            // ======================== BROADCAST ========================
            case "broadcast", "bc" -> {
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_71e150d195")); yield true; }
                String msg = "§6§l[Tu Tiên] §r" + String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                        .replace("&", "§");
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
                if (!(sender instanceof Player)) sender.sendMessage("Broadcast: " + msg);
                yield true;
            }

            // ======================== FORCE DOTPHA ========================
            case "dotpha" -> {
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_1911d3ba75")); yield true; }
                Player target = resolvePlayer(sender, args[1]);
                if (target == null) { sender.sendMessage(plugin.msg("auto_8f8551f79e")); yield true; }
                PlayerData data = plugin.getDataManager().get(target);
                data.setPendingBreakthrough(true);
                plugin.getBreakthroughManager().startBreakthroughTrial(target, data);
                sender.sendMessage("§aĐã force đột phá cho " + target.getName());
                yield true;
            }

            // ==================== DOT PHA LENH (vat pham cong quest dot pha) ====================
            case "dotpha_token" -> {
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_4542640aa8")); yield true; }
                Player target = resolvePlayer(sender, args[1]);
                if (target == null) { sender.sendMessage(plugin.msg("auto_9e147827f4")); yield true; }
                PlayerData data = plugin.getDataManager().get(target);
                com.tutien.data.CultivationStage stage = data.getStage();
                if (args.length >= 3) {
                    try { stage = com.tutien.data.CultivationStage.valueOf(args[2].toUpperCase()); }
                    catch (Exception e) { sender.sendMessage("§cCảnh giới không hợp lệ: " + args[2]); yield true; }
                }
                target.getInventory().addItem(plugin.getBreakthroughManager().createBreakthroughToken(stage));
                sender.sendMessage("§aĐã phát Đột Phá Lệnh (" + stage.getDisplayName() + ") cho " + target.getName());
                target.sendMessage(plugin.prefix() + "§d✦ Bạn nhận được §fĐột Phá Lệnh §7- " + stage.getColor() + stage.getDisplayName());
                yield true;
            }

            // ==================== SACH BI KIP BI DONG ====================
            case "bidong_sach" -> {
                if (args.length < 3) { sender.sendMessage(plugin.msg("auto_661c411c18")); yield true; }
                Player target = resolvePlayer(sender, args[1]);
                if (target == null) { sender.sendMessage(plugin.msg("auto_9e147827f4")); yield true; }
                String key = args[2].toUpperCase();
                if (!plugin.getPassiveSkillManager().isValidKey(key)) {
                    sender.sendMessage(plugin.msg("auto_395a23b3e5")); yield true;
                }
                target.getInventory().addItem(plugin.getPassiveSkillManager().createBook(key));
                sender.sendMessage("§aĐã phát Sách Bí Kíp (" + key + ") cho " + target.getName());
                target.sendMessage(plugin.prefix() + "§d✦ Bạn nhận được §fSách Bí Kíp §7- " + key);
                yield true;
            }

            // ======================== MAX LK ========================
            case "maxlk" -> {
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_e4b4dbf571")); yield true; }
                Player target = resolvePlayer(sender, args[1]);
                if (target == null) { sender.sendMessage(plugin.msg("auto_8f8551f79e")); yield true; }
                PlayerData data = plugin.getDataManager().get(target);
                long max = data.getMaxLinhKhi();
                data.setLinhKhi(max);
                data.setPendingBreakthrough(true);
                plugin.getBreakthroughManager().notifyBreakthroughReady(target, data);
                plugin.getSkillItemManager().updateInfoItem(target, data);
                sender.sendMessage("§aĐã max LK cho " + target.getName() + " (" + fmt(max) + ")");
                yield true;
            }

            // ======================== LIST PLAYERS ========================
            case "list" -> {
                sender.sendMessage(plugin.msg("auto_c95bef26d9"));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData d = plugin.getDataManager().get(p);
                    if (d == null) continue;
                    String stage = d.hasClass() ? d.getStage().getFullDisplay(d.getRank()) : "§7Chưa chọn class";
                    sender.sendMessage("  §e" + p.getName() + " §8| " + stage);
                }
                yield true;
            }

            // ======================== HEAL ========================
            case "heal" -> {
                if (args.length < 2) { sender.sendMessage(plugin.msg("auto_428972aed4")); yield true; }
                if (args[1].equalsIgnoreCase("all")) {
                    Bukkit.getOnlinePlayers().forEach(p -> p.setHealth(p.getMaxHealth()));
                    sender.sendMessage(plugin.msg("auto_5498b20542"));
                } else {
                    Player target = resolvePlayer(sender, args[1]);
                    if (target == null) { sender.sendMessage(plugin.msg("auto_8f8551f79e")); yield true; }
                    target.setHealth(target.getMaxHealth());
                    sender.sendMessage("§aĐã hồi phục " + target.getName());
                    target.sendMessage(plugin.prefix() + "§aAdmin đã hồi phục HP cho bạn!");
                }
                yield true;
            }

            // ======================== SETLINHKHI ALL ========================
            case "maxlk_all" -> {
                int cnt = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData d = plugin.getDataManager().get(p);
                    if (d == null) continue;
                    d.setLinhKhi(d.getMaxLinhKhi());
                    d.setPendingBreakthrough(true);
                    plugin.getBreakthroughManager().notifyBreakthroughReady(p, d);
                    plugin.getSkillItemManager().updateInfoItem(p, d);
                    cnt++;
                }
                sender.sendMessage("§aĐã max LK cho " + cnt + " player online!");
                yield true;
            }

            default -> { showHelp(sender); yield true; }
        };
    }

    // ========================
    //  HANDLE SET
    // ========================

    private void handleSet(CommandSender sender, Player target, PlayerData data, String type, String value) {
        switch (type) {
            case "canh_gioi", "stage" -> {
                try {
                    CultivationStage stage = CultivationStage.valueOf(value.toUpperCase());
                    data.setStage(stage);
                    data.setRank(1);
                    data.setLinhKhi(0);
                    plugin.getCombatListener().updatePlayerStats(target, data);
                    plugin.getFlightListener().updateFlight(target, data);
                    plugin.getSkillItemManager().giveSkillItems(target, data);
                    plugin.getNametagManager().updateNametag(target, data);
                    plugin.getBreakthroughManager().syncLuckPerms(target, data); // go permission cu, set permission "tutien.<canh_gioi>" moi
                    sender.sendMessage("§aSet cảnh giới " + target.getName() + " → " + stage.getFullDisplay(1));
                    target.sendMessage(plugin.prefix() + "§aAdmin đã set cảnh giới của bạn: " + stage.getFullDisplay(1));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin.msg("auto_76ecfd8fe3"));
                    listStages(sender);
                }
            }
            case "tang", "rank" -> {
                try {
                    int rank = Integer.parseInt(value);
                    if (rank < 1 || rank > data.getStage().getMaxRank()) {
                        sender.sendMessage("§cTầng phải từ 1-" + data.getStage().getMaxRank());
                    } else {
                        data.setRank(rank);
                        data.setLinhKhi(0);
                        plugin.getCombatListener().updatePlayerStats(target, data);
                        plugin.getSkillItemManager().updateInfoItem(target, data);
                        sender.sendMessage("§aSet tầng " + target.getName() + " → Tầng " + rank);
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.msg("auto_7f267da93c"));
                }
            }
            case "class" -> {
                try {
                    ClassType ct = ClassType.valueOf(value.toUpperCase());
                    data.setClassType(ct);
                    plugin.getSkillItemManager().giveSkillItems(target, data);
                    sender.sendMessage("§aSet class " + target.getName() + " → " + ct.getDisplayName());
                    target.sendMessage(plugin.prefix() + "§aAdmin đã set class của bạn: " + ct.getColor() + ct.getDisplayName());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin.msg("auto_085c84441e"));
                }
            }
            case "dao" -> {
                try {
                    DaoPath dao = DaoPath.valueOf(value.toUpperCase());
                    data.setDaoPath(dao);
                    plugin.getCombatListener().updatePlayerStats(target, data);
                    sender.sendMessage("§aSet đạo " + target.getName() + " → " + dao.getDisplayName());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin.msg("auto_3e7670ca88"));
                }
            }
            case "linhkhi", "lk" -> {
                try {
                    long lk = Long.parseLong(value);
                    data.setLinhKhi(Math.min(lk, data.getMaxLinhKhi()));
                    plugin.getSkillItemManager().updateInfoItem(target, data);
                    if (data.getLinhKhi() >= data.getMaxLinhKhi())
                        plugin.getBreakthroughManager().notifyBreakthroughReady(target, data);
                    sender.sendMessage("§aSet LK " + target.getName() + " → " + fmt(data.getLinhKhi()));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.msg("auto_7f267da93c"));
                }
            }
            case "tien", "linhthach", "lt" -> {
                try {
                    long v = Long.parseLong(value);
                    data.setTien(v);
                    sender.sendMessage("§aSet Tiền " + target.getName() + " → " + fmt(v));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.msg("auto_7f267da93c"));
                }
            }
            case "toc" -> {
                try {
                    TocType toc = TocType.valueOf(value.toUpperCase());
                    data.setToc(toc);
                    data.setTocRevealed(true);
                    plugin.getTocManager().revealToc(target, data);
                    sender.sendMessage("§aSet Tộc " + target.getName() + " → " + toc.getColoredName());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(plugin.msg("auto_60a11cd293"));
                }
            }
            case "linhcan", "linh_can" -> {
                // /ttadmin set linhcan <player> <cap>
                // value = THIEN | NHI | CHAN | TAM | NGUY  hoặc số 1-5
                try {
                    int count;
                    switch (value.toLowerCase()) {
                        case "thien","thien_linh_can","1" -> count = 1;
                        case "nhi","nhi_linh_can","2"     -> count = 2;
                        case "chan","chan_linh_can","3"    -> count = 3;
                        case "tam","tam_linh_can","4"     -> count = 4;
                        default                           -> count = 5; // nguy
                    }
                    com.tutien.lingcan.LinhCanGrade grade = com.tutien.lingcan.LinhCanGrade.fromCount(count);
                    // Tạo list linh căn elements random với số lượng tương ứng
                    java.util.List<com.tutien.lingcan.LinhCanElement> elements = new java.util.ArrayList<>();
                    com.tutien.lingcan.LinhCanElement[] all = com.tutien.lingcan.LinhCanElement.values();
                    java.util.Collections.shuffle(java.util.Arrays.asList(all));
                    for (int i = 0; i < count && i < all.length; i++) elements.add(all[i]);
                    data.setLinhCan(elements);
                    data.setLinhCanRevealed(true);
                    plugin.getSkillItemManager().updateInfoItem(target, data);
                    sender.sendMessage("§aSet Linh Căn " + target.getName() + " → §e"
                            + grade.getDisplayName() + " §7(" + count + " nguyên tố)");
                    target.sendMessage(plugin.prefix() + "§aAdmin đã set Linh Căn của bạn: §e"
                            + grade.getDisplayName());
                } catch (Exception e) {
                    sender.sendMessage(plugin.msg("auto_fff682f8bd"));
                }
            }
            default -> sender.sendMessage(plugin.msg("auto_e684520d07"));
        }
    }

    // ========================
    //  HANDLE GIVE
    // ========================

    private void handleGive(CommandSender sender, Player target, PlayerData data, String type, String[] args) {
        switch (type) {
            case "phabao" -> {
                PhaBaoTier tier = args.length >= 4 ? parseTier(args[3]) : PhaBaoTier.HA_PHAM;
                PhaBaoType pbType = args.length >= 5 ? parsePbType(args[4]) : null;
                int count = args.length >= 6 ? parseInt(args[5], 1) : 1;
                for (int i = 0; i < count; i++) {
                    PhaBaoType t = pbType != null ? pbType
                            : PhaBaoType.values()[new Random().nextInt(PhaBaoType.values().length)];
                    ItemStack pb = plugin.getPhaBaoManager().createPhaBao(t, tier);
                    target.getInventory().addItem(pb);
                }
                sender.sendMessage("§aTặng §e" + count + "x §6" + tier.getDisplayName()
                        + " §aPháp Bảo → §e" + target.getName());
                target.sendMessage(plugin.prefix() + "§6Admin tặng §e" + count
                        + "x §6Pháp Bảo " + tier.getColoredName() + "§6!");
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            case "tien", "linhthach", "lt" -> {
                if (args.length < 4) { sender.sendMessage(plugin.msg("auto_27b2f22b4a")); return; }
                try {
                    long amount = Long.parseLong(args[3]);
                    data.addTien(amount);
                    sender.sendMessage("§aTặng §e" + fmt(amount) + " §6Tiền → §e" + target.getName());
                    target.sendMessage(plugin.prefix() + "§6Admin tặng §e" + fmt(amount) + " §6Tiền!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.msg("auto_7f267da93c"));
                }
            }
            case "linhkhi", "lk" -> {
                if (args.length < 4) { sender.sendMessage(plugin.msg("auto_a61bc173b0")); return; }
                try {
                    long amount = Long.parseLong(args[3]);
                    data.addLinhKhi(amount);
                    plugin.getSkillItemManager().updateInfoItem(target, data);
                    if (data.getLinhKhi() >= data.getMaxLinhKhi())
                        plugin.getBreakthroughManager().notifyBreakthroughReady(target, data);
                    sender.sendMessage("§aTặng §e" + fmt(amount) + " §aLK → §e" + target.getName());
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.msg("auto_7f267da93c"));
                }
            }
            case "all_phabao" -> {
                // Tặng 1 pháp bảo mỗi loại
                int given = 0;
                PhaBaoTier tier = args.length >= 4 ? parseTier(args[3]) : PhaBaoTier.THUONG_PHAM;
                for (PhaBaoType t : PhaBaoType.values()) {
                    target.getInventory().addItem(plugin.getPhaBaoManager().createPhaBao(t, tier));
                    given++;
                }
                sender.sendMessage("§aTặng §e" + given + " §aPháp Bảo → §e" + target.getName());
                target.sendMessage(plugin.prefix() + "§6Admin tặng đầy đủ §e" + given + " §6Pháp Bảo!");
            }
            default -> sender.sendMessage(plugin.msg("auto_0995451901"));
        }
    }

    // ========================
    //  SHOW INFO
    // ========================

    private void showPlayerInfo(CommandSender sender, Player target, PlayerData data) {
        sender.sendMessage("§6§l╔══════ INFO: " + target.getName() + " ══════╗");
        sender.sendMessage("  §7Cảnh Giới: " + (data.hasClass() ? data.getStage().getFullDisplay(data.getRank()) : "§8Chưa chọn"));
        sender.sendMessage("  §7Class: " + (data.hasClass() ? data.getClassType().getColor() + data.getClassType().getDisplayName() : "§8Chưa chọn"));
        sender.sendMessage("  §7Đạo Path: " + (data.hasDaoPath() ? data.getDaoPath().getDisplayName() : "§8Chưa chọn"));
        sender.sendMessage("  §7Tộc: " + (data.hasToc() ? data.getToc().getColoredName() : "§8Chưa có"));
        sender.sendMessage("  §7Linh Khí: §e" + fmt(data.getLinhKhi()) + "§7/§e" + fmt(data.getMaxLinhKhi()));
        sender.sendMessage("  §7Tiền: §e" + fmt(data.getTien()));
        if (data.hasLinhCan()) {
            LinhCanGrade grade = LinhCanGrade.fromCount(data.getLinhCan().size());
            sender.sendMessage("  §7Linh Căn: §e" + grade.getDisplayName()
                    + " §7(" + data.getLinhCan().size() + " nguyên tố)");
        }
        sender.sendMessage("  §7Pending Đột Phá: §e" + data.isPendingBreakthrough());
        sender.sendMessage("  §7In Trial: §e" + data.isInBreakthroughTrial());
        sender.sendMessage(plugin.msg("auto_e15d9d97af"));
    }

    // ========================
    //  TOP
    // ========================

    private void showTop(CommandSender sender, String stat) {
        sender.sendMessage("§6§l=== TOP 10 - " + stat.toUpperCase() + " ===");
        List<PlayerData> allData = plugin.getDataManager().getAllData();
        allData.sort((a, b) -> {
            if (stat.equals("tien") || stat.equals("linhthach")) return Long.compare(b.getTien(), a.getTien());
            return Integer.compare(b.getStage().ordinal() * 10 + b.getRank(),
                    a.getStage().ordinal() * 10 + a.getRank());
        });
        int rank = 1;
        for (PlayerData d : allData) {
            if (rank > 10) break;
            if (!d.hasClass()) continue;
            String val = (stat.equals("tien") || stat.equals("linhthach")) ? fmt(d.getTien()) + " Tiền"
                    : d.getStage().getFullDisplay(d.getRank());
            sender.sendMessage("  §e#" + rank + " §f" + d.getPlayerName() + " §8| " + val);
            rank++;
        }
    }

    // ========================
    //  HELPERS
    // ========================

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.msg("auto_76826dad30"));
        sender.sendMessage(plugin.msg("auto_7bca6bd775"));
        sender.sendMessage(plugin.msg("auto_444fcdfcbc"));
        sender.sendMessage(plugin.msg("auto_de5c5ae198"));
        sender.sendMessage(plugin.msg("auto_aae5226266"));
        sender.sendMessage(plugin.msg("auto_83373edbfc"));
        sender.sendMessage(plugin.msg("auto_515df2e2ea"));
        sender.sendMessage(plugin.msg("auto_eb28a249cf"));
        sender.sendMessage(plugin.msg("auto_9d82b5555a"));
        sender.sendMessage(plugin.msg("auto_0955f8d27d"));
        sender.sendMessage(plugin.msg("auto_3638afa78e"));
        sender.sendMessage(plugin.msg("auto_9f2fb70a66"));
        sender.sendMessage(plugin.msg("auto_458a67bce4"));
        sender.sendMessage(plugin.msg("auto_9c44042f2f"));
        sender.sendMessage(plugin.msg("auto_afcb4027b6"));
        sender.sendMessage(plugin.msg("auto_de078c84df"));
        sender.sendMessage(plugin.msg("auto_1bcbd19509"));
        sender.sendMessage(plugin.msg("auto_d3a8f7d82e"));
        sender.sendMessage(plugin.msg("auto_1c5c98d80c"));
        sender.sendMessage(plugin.msg("auto_ca52792715"));
        sender.sendMessage(plugin.msg("auto_18937bc00b"));
        sender.sendMessage(plugin.msg("auto_9edb57a473"));
        sender.sendMessage(plugin.msg("auto_045f213664"));
    }

    private void listStages(CommandSender sender) {
        sender.sendMessage(plugin.msg("auto_073334d36d"));
        for (CultivationStage s : CultivationStage.values()) {
            sender.sendMessage("  §e" + s.name() + " §8→ §7" + s.getDisplayName());
        }
    }

    private PhaBaoTier parseTier(String s) {
        try { return PhaBaoTier.valueOf(s.toUpperCase()); } catch (Exception e) {
            return switch (s.toLowerCase()) {
                case "dia"    -> PhaBaoTier.DIA_PHAM;
                case "ha"     -> PhaBaoTier.HA_PHAM;
                case "trung"  -> PhaBaoTier.TRUNG_PHAM;
                case "thuong" -> PhaBaoTier.THUONG_PHAM;
                case "de"     -> PhaBaoTier.DE_PHAM;
                case "tien"   -> PhaBaoTier.TIEN_PHAM;
                default       -> PhaBaoTier.HA_PHAM;
            };
        }
    }

    private PhaBaoType parsePbType(String s) {
        try { return PhaBaoType.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String fmt(long n) {
        if (n >= 1_000_000_000L) return String.format("%.2fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L)     return String.format("%.2fM", n / 1_000_000.0);
        if (n >= 1_000L)         return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return Arrays.asList("info","set","give","reset","reload","top","broadcast",
                    "dotpha","maxlk","maxlk_all","list","heal","bc","season","worldboss");
        if (args.length == 2 && args[0].equals("season"))
            return Arrays.asList("set","clear","info");
        if (args.length == 3 && args[0].equals("season") && args[1].equals("set")) {
            List<String> classes = new ArrayList<>();
            for (ClassType c : ClassType.values()) classes.add(c.name());
            return classes;
        }
        if (args.length == 4 && args[0].equals("season") && args[1].equals("set")) {
            List<String> daos = new ArrayList<>();
            for (DaoPath d : DaoPath.values()) daos.add(d.name());
            return daos;
        }
        if (args.length == 2 && args[0].equals("set"))
            return Arrays.asList("canh_gioi","tang","class","dao","linhkhi","tien","toc","linhcan");
        if (args.length == 2 && args[0].equals("give"))
            return Arrays.asList("phabao","tien","linhkhi","all_phabao");
        if (args.length == 2 && (args[0].equals("info")||args[0].equals("reset")||args[0].equals("dotpha")||args[0].equals("maxlk")||args[0].equals("heal"))) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 4 && args[0].equals("set") && args[1].equals("canh_gioi")) {
            List<String> stages = new ArrayList<>();
            for (CultivationStage st : CultivationStage.values()) stages.add(st.name());
            return stages;
        }
        if (args.length == 4 && args[0].equals("set") && args[1].equals("linhcan"))
            return Arrays.asList("thien","nhi","chan","tam","nguy","1","2","3","4","5");
        if (args.length == 4 && args[0].equals("give") && args[1].equals("phabao"))
            return Arrays.asList("DIA_PHAM","HA_PHAM","TRUNG_PHAM","THUONG_PHAM","DE_PHAM","TIEN_PHAM","dia","ha","trung","thuong","de","tien");
        return Collections.emptyList();
    }

    // ══════════════════════════════════════════════════════════
    //  HELPER: Resolve player từ tên hoặc target selector
    // ══════════════════════════════════════════════════════════

    /**
     * Resolve một Player từ chuỗi tên hoặc target selector Minecraft (@p, @a, @s, @r, @e).
     *
     * LƯU Ý: Selector (@p, @a...) chỉ hoạt động khi lệnh được chạy bởi một nguồn
     * CÓ VỊ TRÍ trong game (player, command block). Nếu chạy từ console/RCON/panel
     * web (không có vị trí), Bukkit không thể xác định "nearest player" từ đâu
     * nên selector sẽ KHÔNG resolve được - đây là giới hạn của chính Minecraft,
     * không phải lỗi của plugin. Trong trường hợp đó hãy gõ đúng tên người chơi.
     */
    private Player resolvePlayer(CommandSender sender, String arg) {
        if (arg == null || arg.isEmpty()) return null;
        if (arg.startsWith("@")) {
            try {
                for (Entity e : Bukkit.selectEntities(sender, arg)) {
                    if (e instanceof Player p) return p;
                }
            } catch (Exception ignored) {
                // Sender không có vị trí (console/RCON) -> không thể resolve selector
            }
            return null;
        }
        return Bukkit.getPlayer(arg);
    }

    /** Thông báo lỗi rõ ràng khi không resolve được player, giải thích nếu là selector */
    private String playerNotFoundMsg(String arg) {
        if (arg != null && arg.startsWith("@")) {
            return "§cKhông thể dùng selector §e" + arg + " §ctừ console/panel!\n"
                    + "§7→ Selector (@p, @a...) chỉ hoạt động khi gõ trong chat game.\n"
                    + "§7→ Hãy gõ đúng tên người chơi, ví dụ: §e/ttadmin give TenBan ...";
        }
        return "§cPlayer §e" + arg + " §cđang offline hoặc không tồn tại!";
    }

    /** Thử parse một chuỗi thành PhaBaoType, trả về null nếu không khớp enum nào */
    private PhaBaoType tryParsePhaBaoTypeExact(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return PhaBaoType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Cấp pháp bảo trực tiếp theo PhaBaoType, mặc định Tiên Phẩm (tier cao nhất) */
    private void givePhaBaoDirect(CommandSender sender, Player target, PhaBaoType type, int count) {
        count = Math.max(1, count);
        for (int i = 0; i < count; i++) {
            ItemStack pb = plugin.getPhaBaoManager().createPhaBao(type, PhaBaoTier.TIEN_PHAM);
            target.getInventory().addItem(pb);
        }
        sender.sendMessage("§aTặng §e" + count + "x §6" + type.getDisplayName()
                + " §a(Tiên Phẩm) → §e" + target.getName());
        target.sendMessage(plugin.prefix() + "§6Admin tặng §e" + count
                + "x §6" + type.getDisplayName() + " §6(Tiên Phẩm)!");
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }
}