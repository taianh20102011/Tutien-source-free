package com.tutien.commands;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.DaoPath;
import com.tutien.data.PlayerData;
import com.tutien.skills.Skill;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TuTienCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public TuTienCommand(TuTienPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_6df645165c")); return true;
        }
        String name = cmd.getName().toLowerCase();

        if (name.equals("vuottamma")) { handleVuotTamMa(player); return true; }
        if (name.equals("dotpha"))     { handleDotPha(player); return true; }
        if (name.equals("boqua"))   { handleBoQua(player);  return true; }
        if (name.equals("linhkhi") || name.equals("lk")) { showLK(player); return true; }
        if (name.equals("skill")   || name.equals("sk"))  { return handleSkill(player, args); }

        // ── New v4 standalone commands ──
        if (name.equals("karma") || name.equals("congduc")) {
            showKarma(player); return true;
        }
        if (name.equals("quest") || name.equals("nhiemvu") || name.equals("nv")) {
            showQuest(player); return true;
        }
        // Fix: /aura va alias /tuvi la command riêng (đăng ký trong plugin.yml)
        // nên phải kiểm tra theo `name`, không thể trông đợi args[0] == "aura"
        // (args[0] khi gõ /aura off la "off" chứ khong phai "aura" -> code cu
        // không bao giờ tới được nhánh xử lý ben duoi).
        if (name.equals("aura") || name.equals("tuvi")) {
            handleAuraToggle(player, args, 0);
            return true;
        }

        if (args.length == 0) { showHelp(player); return true; }

        return switch (args[0].toLowerCase()) {
            case "help", "huongdan", "guide" -> {
                if (plugin.getGuideGUI() != null) {
                    plugin.getGuideGUI().open(player);
                } else {
                    showHelp(player);
                }
                yield true;
            }
            case "chon"        -> handleClassSelect(player, args);
            case "dao"         -> handleDaoSelect(player, args);
            case "info"        -> showInfo(player);
            case "skill"       -> showSkillList(player);
            case "layky", "skillitem" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.prefix() + "§e/tutien layky <skill-id>");
                    player.sendMessage("§7Dùng ID trong /tutien skill. Ví dụ: §f/tutien layky chienkhiquyet");
                    yield true;
                }
                PlayerData pdata = plugin.getDataManager().get(player);
                if (pdata == null || !pdata.hasClass()) {
                    player.sendMessage(plugin.prefix() + "§cBạn cần chọn class trước."); yield true;
                }
                var skillItem = plugin.getSkillItemManager().createStandaloneSkillItem(pdata, args[1]);
                if (skillItem == null) {
                    player.sendMessage(plugin.prefix() + "§cSkill không tồn tại, không thuộc class của bạn, hoặc bạn chưa đủ rank.");
                    yield true;
                }
                var leftovers = player.getInventory().addItem(skillItem);
                leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                player.sendMessage(plugin.prefix() + "§aĐã nhận vật phẩm kỹ năng §e" + args[1] + "§a. Chuột phải để thi triển.");
                yield true;
            }
            case "item","items","laydogoc","lay" -> {
                var pdata = plugin.getDataManager().get(player);
                if (!pdata.hasClass()) {
                    player.sendMessage(plugin.prefix() + "§cChưa chọn class!"); yield true;
                }
                // Xoa skill items cu trong hotbar 0-4
                for (int i = 0; i < 5; i++) {
                    var cur = player.getInventory().getItem(i);
                    if (cur != null && cur.hasItemMeta()) player.getInventory().setItem(i, null);
                }
                plugin.getSkillItemManager().giveSkillItems(player, pdata);
                player.sendMessage(plugin.prefix() + "§aĐã lấy lại toàn bộ §eskill items§a!");
                yield true;
            }
            case "ngukiem", "phikiem" -> { plugin.getSwordFlightManager().toggle(player); yield true; }
            case "pet", "linhthu", "trieuthu" -> { plugin.getPetGUI().open(player); yield true; }
            case "top"         -> showTop(player);
            case "reset"       -> handleReset(player, args);
            case "admin"       -> handleAdmin(player, args);
            case "linhcan"     -> { showLinhCan(player); yield true; }
            case "toc"         -> { showToc(player); yield true; }
            case "aura"        -> { handleAuraToggle(player, args, 1); yield true; }
            case "phabao"      -> { givePhabaoDemo(player); yield true; }
            case "reroll", "reroll_linhcan" -> { handleRerollLinhCan(player); yield true; }
            case "reroll_toc", "doitoc" -> { handleRerollToc(player); yield true; }
            default            -> { showHelp(player); yield true; }
        };
    }

    // ========================
    //  AURA / TUVI TOGGLE
    // ========================
    /**
     * argIndex = vị trí trong mảng args chứa tham số on/off
     * (0 khi gõ trực tiếp /aura [on|off], 1 khi gõ /tutien aura [on|off]).
     */
    private void handleAuraToggle(Player player, String[] args, int argIndex) {
        var auraMgr = plugin.getAuraManager();
        boolean currentlyEnabled = !auraMgr.isDisabled(player.getUniqueId());

        Boolean want = null; // null = khong co tham so -> toggle binh thuong
        if (args.length > argIndex) {
            String a = args[argIndex].toLowerCase();
            if (a.equals("off") || a.equals("tat"))      want = false;
            else if (a.equals("on") || a.equals("bat"))  want = true;
        }

        boolean now;
        if (want == null || want != currentlyEnabled) {
            // Không có tham số (toggle thường), hoặc trạng thái muốn khác
            // trạng thái hiện tại -> cần 1 lần toggleAura() để đặt đúng trạng thái.
            now = auraMgr.toggleAura(player);
        } else {
            // Đã ở đúng trạng thái muốn rồi, chỉ báo lại cho người chơi biết.
            now = currentlyEnabled;
            player.sendMessage(plugin.prefix() + (now
                    ? "§a✦ Vong Tu Vi §ađã đang bật rồi."
                    : "§7✦ Vong Tu Vi §cđã đang tắt rồi."));
        }

        var pd = plugin.getDataManager().get(player);
        if (pd != null) pd.setAuraEnabled(now);
    }

    // ========================
    //  DOT PHA
    // ========================
    private void handleVuotTamMa(Player player) {
        // Khoa dot pha bang lenh o world chi dinh (config "breakthrough-npc")
        // -> bat buoc phai click NPC Citizens, xem BreakthroughNpcListener.
        if (plugin.getBreakthroughManager().isNpcOnlyWorld(player)) {
            player.sendMessage(plugin.prefix() + plugin.getBreakthroughManager().getNpcBlockMessage());
            return;
        }
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cChưa chọn class!"); return;
        }
        if (data.getStage().isTopStage() && data.getRank() >= data.getStage().getMaxRank()) {
            player.sendMessage(plugin.prefix() + "§6Bạn đã đạt cảnh giới tối cao!"); return;
        }
        plugin.getBreakthroughManager().startThienDinh(player, data);
    }

    private void handleDotPha(Player player) {
        // Khoa dot pha bang lenh o world chi dinh (config "breakthrough-npc")
        // -> bat buoc phai click NPC Citizens, xem BreakthroughNpcListener.
        if (plugin.getBreakthroughManager().isNpcOnlyWorld(player)) {
            player.sendMessage(plugin.prefix() + plugin.getBreakthroughManager().getNpcBlockMessage());
            return;
        }
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cChưa chọn class!"); return;
        }
        if (data.getStage().isTopStage() && data.getRank() >= data.getStage().getMaxRank()) {
            player.sendMessage(plugin.prefix() + "§6Bạn đã đạt cảnh giới tối cao - §eVô Thượng Chí Tôn!"); return;
        }
        if (!data.isPendingBreakthrough()) {
            long need = data.getMaxLinhKhi() - data.getLinhKhi();
            if (need > 0) {
                player.sendMessage(plugin.prefix() + "§cCần thêm §e" + fmt(need) + " §cLinh Khí! ("
                        + fmt(data.getLinhKhi()) + "/" + fmt(data.getMaxLinhKhi()) + ")");
                return;
            }
            // Nếu đủ LK nhưng chưa pending
            data.setPendingBreakthrough(true);
        }
        plugin.getBreakthroughManager().startBreakthroughTrial(player, data);
    }

    private void handleBoQua(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data != null && data.isPendingBreakthrough()) {
            data.setPendingBreakthrough(false);
            player.sendMessage(plugin.prefix() + "§7Đã hủy đột phá. Linh Khí vẫn được giữ.");
        } else {
            player.sendMessage(plugin.prefix() + "§cKhông có đột phá nào đang chờ.");
        }
    }

    // ========================
    //  CHỌN CLASS
    // ========================
    private boolean handleClassSelect(Player player, String[] args) {
        PlayerData data = plugin.getDataManager().get(player);
        if (args.length < 2) { showClassMenu(player); return true; }
        if (data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cĐã chọn class §e" + data.getClassType().getDisplayName()
                    + "§c! /tutien reset xác nhận để chơi lại."); return true;
        }
        ClassType ct = parseClass(args[1]);
        if (ct == null) { player.sendMessage(plugin.prefix() + "§cClass không tồn tại!"); return true; }

        data.setClassType(ct);
        plugin.getCombatListener().updatePlayerStats(player, data);

        // Nếu đã chọn đạo path -> cấp phát items luôn
        if (data.hasDaoPath()) {
            plugin.getSkillItemManager().giveSkillItems(player, data);
        }

        player.sendMessage(plugin.msg("auto_85e9111c89"));
        player.sendMessage(plugin.msg("auto_fb0cbdadd0"));
        player.sendMessage("  " + ct.getColor() + "§l" + ct.getIcon() + " " + ct.getDisplayName()
                + " §8| §7" + ct.getDescription());
        for (String sid : ct.getSkillIds()) {
            Skill s = plugin.getSkillManager().getSkill(sid);
            if (s != null) player.sendMessage("  §a► §f" + s.getDisplayName() + " §8- §7" + s.getDescription());
        }
        if (!data.hasClass()) player.sendMessage(plugin.msg("auto_7469db4673"));
        player.sendMessage(plugin.msg("auto_89d62924aa"));
        return true;
    }

    // ========================
    //  CHỌN ĐẠO PATH
    // ========================
    private boolean handleDaoSelect(Player player, String[] args) {
        PlayerData data = plugin.getDataManager().get(player);
        if (!data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cChọn class trước! /tutien chon"); return true;
        }
        if (data.hasDaoPath()) {
            player.sendMessage(plugin.prefix() + "§cĐã chọn đạo: " + data.getDaoPath().getColoredName()); return true;
        }
        if (args.length < 2) {
            showDaoMenu(player); return true;
        }
        DaoPath dao = parseDaoPath(args[1]);
        if (dao == null) { player.sendMessage(plugin.prefix() + "§cGốc tri: chính đạo hoặc ma đạo"); return true; }

        data.setDaoPath(dao);
        plugin.getSkillItemManager().giveSkillItems(player, data);
        plugin.getCombatListener().updatePlayerStats(player, data);

        player.sendMessage(plugin.msg("auto_236c481f94"));
        player.sendMessage(plugin.msg("auto_e4b28d4f4f"));
        player.sendMessage("  " + dao.getLoreDisplay());
        player.sendMessage("  §7" + dao.getDescription());
        player.sendMessage(plugin.msg("auto_3d8c1c8b47"));
        player.sendMessage(plugin.msg("auto_6f8d12239d"));
        player.sendMessage(plugin.msg("auto_73478678fd"));
        player.sendMessage(plugin.msg("auto_5b9f0a9ea8"));
        return true;
    }

    private void showDaoMenu(Player player) {
        player.sendMessage(plugin.msg("auto_da47a9f770"));
        player.sendMessage(plugin.msg("auto_844e07437e"));
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        for (DaoPath d : DaoPath.values()) {
            player.sendMessage("  " + d.getColoredName() + " §8| §7" + d.getDescription());
            player.sendMessage("  §8└ §a/tutien dao " + d.name().toLowerCase());
        }
        player.sendMessage(plugin.msg("auto_5a07764d64"));
    }

    // ========================
    //  INFO
    // ========================
    private boolean showInfo(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        long max = data.getMaxLinhKhi(), cur = data.getLinhKhi();
        double pct = max > 0 ? (double) cur / max * 100 : 0;
        player.sendMessage(plugin.msg("auto_7ff8713696"));
        player.sendMessage(plugin.msg("auto_572bbb7939"));
        player.sendMessage("  §7Class: " + (data.hasClass() ? data.getClassType().getColoredName() : "§cChưa chọn"));
        player.sendMessage("  §7Đão:   " + (data.hasDaoPath() ? data.getDaoPath().getColoredName() : "§cChưa chọn"));
        player.sendMessage("  §7Cảnh giới: " + data.getStage().getFullDisplay(data.getRank()));
        player.sendMessage("  §7[" + data.getStage().getRealmColor() + data.getStage().getRealm() + "§7]");
        player.sendMessage("  §7LK: §e" + fmt(cur) + "§8/§e" + fmt(max)
                + " §7(" + String.format("%.1f", pct) + "%)");
        player.sendMessage("  §8" + drawBar(cur, max, 20));
        player.sendMessage(plugin.msg("auto_ead4f723c2"));
        player.sendMessage("  §7ATK x§c" + String.format("%.2f", data.calculateDamageMultiplier())
                + "  §7DEF x§b" + String.format("%.2f", data.calculateDefenseMultiplier())
                + "  §7HP §a" + String.format("%.0f", data.calculateMaxHealth()));
        player.sendMessage(plugin.msg("auto_ead4f723c2"));
        player.sendMessage("  §7Kills §a" + data.getKills() + " §8| §7Deaths §c" + data.getDeaths()
                + " §8| §7Boss §6" + data.getBossKills());
        // Linh Can info
        if (data.hasLinhCan()) {
            var roots = data.getLinhCan();
            var grade = com.tutien.lingcan.LinhCanGrade.fromCount(roots.size());
            StringBuilder lc = new StringBuilder();
            for (var el : roots) lc.append(el.getColoredName()).append("§8, ");
            player.sendMessage("  §7Linh Căn: " + grade.getColoredName() + " §8| "
                    + lc.toString().replaceAll(", $",""));
        }
        player.sendMessage("  §7Đột phá: §a" + data.getBreakthroughSuccess()
                + " §7thành công, §c" + data.getBreakthroughFail() + " §7thất bại");
        player.sendMessage(plugin.msg("auto_e5f3c33430"));
        return true;
    }

    private void showLK(Player player) {
        PlayerData d = plugin.getDataManager().get(player);
        player.sendMessage(plugin.prefix() + "§eLK: §a" + fmt(d.getLinhKhi()) + "§7/§e" + fmt(d.getMaxLinhKhi())
                + " §8[" + drawBar(d.getLinhKhi(), d.getMaxLinhKhi(), 20) + "§8]");
    }

    // ========================
    //  SKILL LIST
    // ========================
    private boolean showSkillList(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (!data.hasClass()) { player.sendMessage(plugin.prefix() + "§cChưa chọn class!"); return true; }
        player.sendMessage("§6§l═══ " + data.getClassType().getColoredName() + " §6§l- Skills ═══");
        List<String> ids = data.getClassType().getSkillIds();
        String[] slotLabel = {"§b[Slot A]", "§b[Slot B]", "§b[Slot C]", "§7[Yếu nhất]"};
        for (int i = 0; i < ids.size(); i++) {
            String sid = ids.get(i);
            Skill s = plugin.getSkillManager().getSkill(sid);
            if (s == null) continue;
            boolean cd = data.isSkillOnCooldown(sid);
            String cdStr = cd ? " §c[CD:" + (data.getSkillCooldownRemaining(sid)/1000+1) + "s]" : " §a[Ready]";
            String label = i < 4 ? slotLabel[i] : "§d§l[✦ Tuyệt Kỹ]";
            player.sendMessage("  " + label + " §e" + s.getDisplayName() + cdStr);
            player.sendMessage("   §8└ §7" + s.getDescription());
        }
        player.sendMessage("§5§l── Bí Kíp Bị Động (" + data.getLearnedPassives().size() + "/" + PlayerData.MAX_PASSIVES + ") ──");
        if (data.getLearnedPassives().isEmpty()) {
            player.sendMessage(plugin.msg("auto_556e86c5c1"));
        } else {
            player.sendMessage("§d" + String.join(", ", data.getLearnedPassives()) + " §7(dùng §f/bidong §7xem chi tiết)");
        }
        return true;
    }

    // ========================
    //  SKILL COMMAND (fallback)
    // ========================
    private boolean handleSkill(Player player, String[] args) {
        if (args.length == 0) { showSkillList(player); return true; }
        PlayerData data = plugin.getDataManager().get(player);
        if (!data.hasClass()) { player.sendMessage(plugin.prefix() + "§cChưa chọn class!"); return true; }
        String id = args[0].toLowerCase();
        Skill skill = plugin.getSkillManager().getSkill(id);
        if (skill == null) { player.sendMessage(plugin.prefix() + "§cSkill không tồn tại!"); return true; }
        if (!data.getClassType().getSkillIds().contains(id)) {
            player.sendMessage(plugin.prefix() + "§cSkill này không thuộc class của bạn!"); return true;
        }
        skill.tryUse(player, data);
        return true;
    }

    // ========================
    //  TOP
    // ========================
    private boolean showTop(Player player) {
        player.sendMessage(plugin.msg("auto_57065c3ab4"));
        List<PlayerData> list = new ArrayList<>(plugin.getDataManager().getAllCached());
        list.sort((a, b) -> {
            int s = Integer.compare(b.getStage().ordinal(), a.getStage().ordinal());
            if (s != 0) return s;
            int r = Integer.compare(b.getRank(), a.getRank());
            if (r != 0) return r;
            return Long.compare(b.getLinhKhi(), a.getLinhKhi());
        });
        int rank = 1;
        for (PlayerData pd : list) {
            if (rank > 10) break;
            String pre = rank == 1 ? "§6#1" : rank == 2 ? "§7#2" : rank == 3 ? "§c#3" : "§f#" + rank;
            player.sendMessage(pre + " §e" + pd.getPlayerName()
                    + " §8- " + pd.getStage().getFullDisplay(pd.getRank())
                    + (pd.hasClass() ? " §8[" + pd.getClassType().getColor() + pd.getClassType().getDisplayName() + "§8]" : "")
                    + (pd.hasDaoPath() ? " " + pd.getDaoPath().getColor() + pd.getDaoPath().getIcon() : ""));
            rank++;
        }
        return true;
    }

    // ========================
    //  RESET
    // ========================
    private boolean handleReset(Player player, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("xacnhan")) {
            player.sendMessage(plugin.prefix() + "§c/tutien reset xacnhan §cđể xác nhận reset."); return true;
        }
        PlayerData data = plugin.getDataManager().get(player);
        data.setClassType(null); data.setDaoPath(null);
        data.setStage(com.tutien.data.CultivationStage.LUYEN_KHI);
        data.setRank(1); data.setLinhKhi(0);
        plugin.getCombatListener().updatePlayerStats(player, data);
        plugin.getBreakthroughManager().syncLuckPerms(player, data); // go het permission tutien.* cu, tra ve tutien.luyenkhi
        player.sendMessage(plugin.prefix() + "§aReset xong! /tutien chon để bắt đầu lại.");
        return true;
    }

    // ========================
    //  ADMIN
    // ========================
    private boolean handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("tutien.admin")) {
            player.sendMessage(plugin.prefix() + "§cKhông đủ quyền!"); return true;
        }
        if (args.length < 3) {
            player.sendMessage(plugin.prefix() + "§c/tutien admin <addlk|setlevel> <value>"); return true;
        }
        PlayerData data = plugin.getDataManager().get(player);
        switch (args[1].toLowerCase()) {
            case "addlk" -> {
                try {
                    long amount = Long.parseLong(args[2]);
                    data.addLinhKhi(amount);
                    player.sendMessage(plugin.prefix() + "§a+" + fmt(amount) + " LK");
                    if (data.getLinhKhi() >= data.getMaxLinhKhi() && !data.isPendingBreakthrough()) {
                        plugin.getBreakthroughManager().notifyBreakthroughReady(player, data);
                    }
                    plugin.getSkillItemManager().updateInfoItem(player, data);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.prefix() + "§cSố không hợp lệ!");
                }
            }
            case "maxlk" -> {
                data.setLinhKhi(data.getMaxLinhKhi());
                player.sendMessage(plugin.prefix() + "§aSet LK tối đa!");
                plugin.getBreakthroughManager().notifyBreakthroughReady(player, data);
                plugin.getSkillItemManager().updateInfoItem(player, data);
            }
        }
        return true;
    }

    // ========================
    //  REROLL
    // ========================
    private void handleRerollToc(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;
        var tm = plugin.getTienManager();
        long cost = tm.rerollTocCost();
        if (!tm.spend(player, cost)) {
            player.sendMessage(plugin.prefix() + "§cCần §e" + tm.fmt(cost) + " §c" + tm.tenTien() + " để re-roll Tộc!");
            player.sendMessage(plugin.prefix() + "§7Hiện có: §e" + tm.fmt(tm.getBalance(player)));
            return;
        }
        data.setToc(plugin.getTocManager().rollToc());
        data.setTocRevealed(false);
        player.sendMessage(plugin.prefix() + "§6✦ Đã dùng §e" + tm.fmt(cost) + " §6" + tm.tenTien() + " để re-roll Tộc!");
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getTocManager().revealToc(player, data), 20L);
    }

    private void handleRerollLinhCan(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;
        var tm = plugin.getTienManager();
        long cost = tm.rerollLinhCanCost();
        if (!tm.spend(player, cost)) {
            player.sendMessage(plugin.prefix() + "§cCần §e" + tm.fmt(cost) + " §c" + tm.tenTien() + " để re-roll Linh Căn!");
            player.sendMessage(plugin.prefix() + "§7Hiện có: §e" + tm.fmt(tm.getBalance(player)));
            return;
        }
        data.setLinhCan(plugin.getLinhCanManager().rollLinhCan());
        data.setLinhCanRevealed(false);
        player.sendMessage(plugin.prefix() + "§6✦ Đã dùng §e" + tm.fmt(cost) + " §6" + tm.tenTien() + " để re-roll Linh Căn!");
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getLinhCanManager().revealLinhCan(player, data), 20L);
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getCombatListener().updatePlayerStats(player, data), 25L);
    }

    // ========================
    //  MENUS / HELPERS
    // ========================
    private void showClassMenu(Player player) {
        player.sendMessage(plugin.msg("auto_c4dace2581"));
        player.sendMessage(plugin.msg("auto_3312a64008"));
        player.sendMessage(plugin.msg("auto_f13a7ab3b3"));
        for (ClassType ct : ClassType.values()) {
            player.sendMessage("  " + ct.getColor() + ct.getIcon() + " §l" + ct.getDisplayName()
                    + " §8| §7" + ct.getDescription());
            player.sendMessage("  §8└ §a/tutien chon " + ct.name().toLowerCase());
        }
        player.sendMessage(plugin.msg("auto_e15d9d97af"));
    }

    private void showHelp(Player player) {
        if (plugin.getGuideGUI() != null) {
            plugin.getGuideGUI().open(player);
            return;
        }
        player.sendMessage("§6§l╔═════════════════ LỆNH TU TIÊN ═════════════════╗");
        player.sendMessage("  §e/tutien chon [class]  §8- §fChọn môn phái tu tiên");
        player.sendMessage("  §e/tutien dao [dao]     §8- §fChọn con đường Đạo");
        player.sendMessage("  §e/tutien info          §8- §fXem thông tin tu vi, cảnh giới");
        player.sendMessage("  §e/tutien skill         §8- §fNhận lại skill item môn phái");
        player.sendMessage("  §e/tutien top           §8- §fXem bảng xếp hạng tu vi");
        player.sendMessage("  §e/guide                §8- §aMở Cẩm Nang Tu Tiên Giới (GUI)");
        player.sendMessage("§6§l╚══════════════════════════════════════════════════╝");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        String n = cmd.getName().toLowerCase();
        if (n.equals("skill") && args.length == 1 && sender instanceof Player p) {
            PlayerData d = plugin.getDataManager().get(p);
            if (d != null && d.hasClass()) return d.getClassType().getSkillIds();
        }
        if (n.equals("tutien")) {
            if (args.length == 1) return Arrays.asList("help","huongdan","chon","dao","info","skill","layky","items","top","reset","admin","reroll","reroll_toc","ngukiem","linhthu","pet");
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("layky")) {
                    PlayerData d = sender instanceof Player player ? plugin.getDataManager().get(player) : null;
                    if (d != null && d.hasClass()) return d.getClassType().getSkillIds();
                    return Arrays.asList("chienkhiquyet", "phatthuan", "thienmatrao");
                }
                if (args[0].equals("chon")) {
                    List<String> r = new ArrayList<>();
                    for (ClassType ct : ClassType.values()) r.add(ct.name().toLowerCase());
                    return r;
                }
                if (args[0].equals("dao")) return Arrays.asList("chinh_dao","ma_dao");
            }
        }
        return Collections.emptyList();
    }


    // ========================
    //  LINH CAN
    // ========================
    private void showLinhCan(Player player) {
        var data = plugin.getDataManager().get(player);
        if (!data.hasLinhCan()) {
            player.sendMessage(plugin.prefix() + "§cLinh căn chưa được giám dinh! Hãy đăng xuất và đăng nhập lại.");
            return;
        }
        plugin.getLinhCanManager().revealLinhCan(player, data);
    }

    // ========================
    //  PHAP BAO DEMO / DROP
    // ========================
    private void givePhabaoDemo(Player player) {
        player.sendMessage(plugin.msg("auto_da47a9f770"));
        player.sendMessage(plugin.msg("auto_cb6aea4e9e"));
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        player.sendMessage(plugin.msg("auto_5e7462ad93"));
        player.sendMessage(plugin.msg("auto_e817f86145"));
        player.sendMessage(plugin.msg("auto_457d2bf618"));
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        player.sendMessage(plugin.msg("auto_23c1f94e55"));
        player.sendMessage(plugin.msg("auto_4f20c45a94"));
        player.sendMessage(plugin.msg("auto_7505985d55"));
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        player.sendMessage(plugin.msg("auto_6d45452732"));
        player.sendMessage(plugin.msg("auto_34b60a24ea"));
        player.sendMessage(plugin.msg("auto_0557989058"));
        player.sendMessage(plugin.msg("auto_5a07764d64"));
        // Cap phat 1 phap bao mẫu thu
        var data = plugin.getDataManager().get(player);
        if (data != null) {
            var pb = plugin.getPhaBaoManager().randomPhaBao(data.getStage().ordinal());
            if (pb == null) {
                player.sendMessage(plugin.prefix() + "§cServer đã tắt random Pháp Bảo trong phabao.yml.");
            } else {
                var leftover = player.getInventory().addItem(pb);
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                player.sendMessage(plugin.prefix() + "§aĐã cấp 1 §6Pháp Bảo §amẫu! Kiểm tra inventory.");
            }
        }
    }


    // ========================
    //  TOC
    // ========================
    private void showToc(Player player) {
        var data = plugin.getDataManager().get(player);
        if (!data.hasToc()) {
            player.sendMessage(plugin.prefix() + "§cChưa được giám định Tộc! Thử đăng xuất và đăng nhập lại.");
            return;
        }
        plugin.getTocManager().revealToc(player, data);
    }

    private ClassType parseClass(String s) {
        for (ClassType ct : ClassType.values())
            if (ct.name().equalsIgnoreCase(s)) return ct;
        return null;
    }

    private DaoPath parseDaoPath(String s) {
        for (DaoPath d : DaoPath.values())
            if (d.name().equalsIgnoreCase(s)) return d;
        return null;
    }

    private String fmt(long n) {
        if (n >= 1_000_000_000_000L) return String.format("%.2fT", n/1_000_000_000_000.0);
        if (n >= 1_000_000_000)      return String.format("%.2fB", n/1_000_000_000.0);
        if (n >= 1_000_000)          return String.format("%.2fM", n/1_000_000.0);
        if (n >= 1_000)              return String.format("%.1fK", n/1_000.0);
        return String.valueOf(n);
    }

    private String drawBar(long cur, long max, int len) {
        if (max <= 0) return "§8" + "░".repeat(len);
        int fill = (int)((double) cur / max * len);
        return "§a" + "█".repeat(fill) + "§8" + "░".repeat(len - fill);
    }

    // ══════════════════════════════════════════════
    //  /karma
    // ══════════════════════════════════════════════
    private void showKarma(Player player) {
        var km = plugin.getKarmaManager();
        if (km == null) {
            player.sendMessage(plugin.prefix() + "§cHệ thống Karma chưa khởi động!"); return;
        }
        var d = plugin.getDataManager().get(player);
        long cd = km.getCongDuc(d);
        long nl = km.getNghiepLuc(d);
        double diffMult = km.getBreakthroughDiffMult(d);
        double atkBonus = km.getAtkBonusFromKarma(d);

        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_21990548cc"));
        player.sendMessage("§6§l   ☯  KARMA  —  " + km.getKarmaDisplay(d));
        player.sendMessage(plugin.msg("auto_7bf18676dc"));
        player.sendMessage("  §aCông Đức: §f" + fmt(cd) + " §8/ §7" +
                fmt(com.tutien.karma.KarmaManager.MAX_CONG_DUC));
        player.sendMessage("  §cNghiệp Lực: §f" + fmt(nl) + " §8/ §7" +
                fmt(com.tutien.karma.KarmaManager.MAX_NGHIEP_LUC));
        player.sendMessage(plugin.msg("auto_7bf18676dc"));
        if (diffMult < 1.0)
            player.sendMessage("  §aThiên Kiếp: §a" + String.format("%.0f%%", (1-diffMult)*100) + " dễ hơn");
        else if (diffMult > 1.0)
            player.sendMessage("  §cThiên Kiếp: §c" + String.format("%.0f%%", (diffMult-1)*100) + " khó hơn");
        else
            player.sendMessage(plugin.msg("auto_6044d109df"));
        if (atkBonus > 0)
            player.sendMessage("  §cATK bonus: §f+" + String.format("%.0f%%", atkBonus*100));
        player.sendMessage(plugin.msg("auto_7bf18676dc"));
        player.sendMessage(plugin.msg("auto_fe978b1790"));
        player.sendMessage(plugin.msg("auto_9dc51f121f"));
        player.sendMessage(plugin.msg("auto_781380170b"));
        player.sendMessage(plugin.msg("auto_081989b9d7"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
    }

    // ══════════════════════════════════════════════
    //  /quest
    // ══════════════════════════════════════════════
    private void showQuest(Player player) {
        var qm = plugin.getQuestManager();
        if (qm == null) {
            player.sendMessage(plugin.prefix() + "§cHệ thống Quest chưa khởi động!"); return;
        }
        qm.showQuestStatus(player);
        player.sendMessage(plugin.msg("auto_7831fb6436"));
    }
}