package com.tutien.breakthrough;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.DaoPath;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class BreakthroughManager implements Listener {

    private final TuTienPlugin plugin;
    private final Random random = new Random();
    private final TribulationBossArena bossArena;

    /** PDC key luu ten canh gioi (VD "BAN_THANH") tren item "Dot Pha Lenh". */
    private final NamespacedKey tokenKey;

    public BreakthroughManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.bossArena = new TribulationBossArena(plugin);
        this.tokenKey = new NamespacedKey(plugin, "dotpha_token");
    }

    public TribulationBossArena getBossArena() { return bossArena; }

    // ========================
    //  DOT PHA QUA NPC (KHOA LENH THEO WORLD)
    // ========================

    /**
     * True neu world hien tai cua player nam trong danh sach bi khoa
     * (config "breakthrough-npc.enabled" + "breakthrough-npc.worlds" —
     * co the khai bao NHIEU world cung luc), tuc la KHONG cho phep
     * /vuottamma va /dotpha chay truc tiep — bat buoc phai click NPC
     * (xem BreakthroughNpcListener) de dot pha.
     */
    public boolean isNpcOnlyWorld(Player player) {
        if (!plugin.getConfig().getBoolean("breakthrough-npc.enabled", false)) return false;
        List<String> worlds = plugin.getConfig().getStringList("breakthrough-npc.worlds");
        if (worlds == null || worlds.isEmpty()) return false;
        String cur = player.getWorld().getName();
        for (String w : worlds) {
            if (w != null && w.equalsIgnoreCase(cur)) return true;
        }
        return false;
    }

    /** Tin nhan bao nguoi choi phai den NPC (config "breakthrough-npc.block-message"). */
    public String getNpcBlockMessage() {
        String raw = plugin.getConfig().getString("breakthrough-npc.block-message",
                "&cKhu vực này bạn phải đến gặp NPC để tiến hành Đột Phá!");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Chay cac lenh CONSOLE TUY CHINH (khong bat buoc) cau hinh trong config.yml
     * (muc "breakthrough-commands"), ngay khi nguoi choi vua DAT MOT CANH GIOI MOI.
     * %player% duoc thay bang ten nguoi choi. CHI dung cho lenh THEM ngoai LuckPerms
     * (vd broadcast, phan thuong...) — permission/group/prefix LuckPerms KHONG con
     * khai bao o day nua, xem ghi chu trong config.yml.
     */
    private void runBreakthroughCommands(Player player, CultivationStage stage) {
        List<String> cmds = plugin.getConfig().getStringList("breakthrough-commands." + stage.name());
        if (cmds == null || cmds.isEmpty()) return;
        for (String raw : cmds) {
            if (raw == null || raw.isBlank()) continue;
            String cmd = raw.replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }
    }

    /**
     * Dong bo ĐẦY ĐỦ permission LuckPerms cho player theo cảnh giới HIỆN TẠI của họ:
     *   group "tt_realm_*" + prefix + permission node "tutien.<canh_gioi>" — tất cả
     *   do LuckPermsHook.syncPlayer() xử lý qua LuckPerms API trong 1 lần ghi duy
     *   nhất (có self-heal + cache RAM để không ghi lại nếu không đổi gì).
     *
     * (v7.3.0) KHÔNG còn gọi runBreakthroughCommands() ở đây nữa: trước đây hàm
     * này chạy thêm ~25 lệnh console "lp user ... permission unset/set" mỗi lần
     * sync (trùng lặp hoàn toàn với LuckPermsHook), khiến LuckPerms phải xếp hàng
     * xử lý lệnh ("Another command is being executed...") và tốn RAM/CPU server
     * khi nhiều người chơi join/đột phá cùng lúc. runBreakthroughCommands() vẫn
     * còn đó cho admin nào muốn chạy lệnh KHÁC (không phải LuckPerms) theo cảnh
     * giới — chỉ cần khai báo trong config.yml (mục "breakthrough-commands").
     *
     * PUBLIC vì mọi nơi làm thay đổi data.getStage() KHÔNG thông qua completeBreakthrough()
     * (VD: /ttadmin set canh_gioi, /tutien reset, đột phá thất bại tụt hạng) đều phải gọi
     * hàm này ngay sau khi setStage(), nếu không permission "tutien.<canh_gioi>" trên
     * LuckPerms sẽ bị lệch so với cảnh giới thật của player.
     */
    public void syncLuckPerms(Player player, PlayerData data) {
        if (plugin.getLuckPermsHook() != null) plugin.getLuckPermsHook().syncPlayer(player, data);
        if (!plugin.getConfig().getStringList("breakthrough-commands." + data.getStage().name()).isEmpty()) {
            runBreakthroughCommands(player, data.getStage());
        }
    }

    // ========================
    //  DOT PHA LENH (vat pham cong nhiem vu dot pha)
    // ========================

    /**
     * Tao 1 "Dot Pha Lenh" danh rieng cho 1 canh gioi. Vat pham nay se do
     * plugin nhiem vu (quest plugin ngoai) phat cho nguoi choi khi ho hoan
     * thanh nhiem vu dot pha cua canh gioi hien tai. Nguoi choi phai co vat
     * pham nay trong tui do (se bi tieu hao 1 cai) thi moi bat dau duoc
     * Thien Kiep (Buoc 2) du da du Linh Khi.
     */
    public ItemStack createBreakthroughToken(CultivationStage stage) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§l✦ Đột Phá Lệnh §7- " + stage.getColor() + stage.getDisplayName());
        meta.setLore(java.util.List.of(
                "§7Vật phẩm nhận được sau khi hoàn thành",
                "§7nhiệm vụ đột phá của cảnh giới này.",
                "§7Dùng cùng Linh Khí để tiến hành Thiên Kiếp.",
                "§8(Tự tiêu hao khi đột phá thành công)"
        ));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.STRING, stage.name());
        item.setItemMeta(meta);
        return item;
    }

    /** Kiem tra + tieu hao 1 "Dot Pha Lenh" dung canh gioi hien tai trong tui do. */
    private boolean consumeToken(Player player, CultivationStage stage) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null || !it.hasItemMeta()) continue;
            ItemMeta meta = it.getItemMeta();
            String val = meta.getPersistentDataContainer().get(tokenKey, PersistentDataType.STRING);
            if (val != null && val.equals(stage.name())) {
                it.setAmount(it.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    public boolean hasToken(Player player, CultivationStage stage) {
        for (ItemStack it : player.getInventory().getContents()) {
            if (it == null || !it.hasItemMeta()) continue;
            String val = it.getItemMeta().getPersistentDataContainer().get(tokenKey, PersistentDataType.STRING);
            if (val != null && val.equals(stage.name())) return true;
        }
        return false;
    }

    // ========================
    //  NOTIFY
    // ========================

    public void notifyBreakthroughReady(Player player, PlayerData data) {
        if (data.isPendingBreakthrough()) return;
        data.setPendingBreakthrough(true);
        String stageName = data.getStage().getFullDisplay(data.getRank());
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        int waves = getWaveCount(data.getStage());
        double diff = data.getStage().getBreakthroughDifficulty();
        String diffStr = diff > 0.70 ? "§aDễ" : diff > 0.50 ? "§eVừa" : diff > 0.30 ? "§6Khó" : diff > 0.05 ? "§cRất Khó" : "§4§lHuyền Thiên - Tuyệt Diệt";
        player.sendMessage(plugin.msg("auto_236c481f94"));
        player.sendMessage(plugin.msg("auto_7302663dde"));
        player.sendMessage(plugin.msg("auto_3d8c1c8b47"));
        player.sendMessage("  §7Cảnh giới hiện tại: " + stageName);
        player.sendMessage("  §7Số sóng Thiên Kiếp: §e" + waves + " §7sóng  §8| Độ khó: " + diffStr);
        // Canh bao dac biet khi sap dot pha len canh gioi cuoi cung
        boolean isFinalStageCrossing = !data.getStage().hasNextRank(data.getRank())
                && data.getStage().nextStage() != null
                && data.getStage().nextStage().isTopStage();
        if (isFinalStageCrossing) {
            player.sendMessage(plugin.msg("auto_59ecef68ef"));
            player.sendMessage(plugin.msg("auto_e6cdaefa6d"));
            player.sendMessage(plugin.msg("auto_d5b359f6f2"));
        } else {
            player.sendMessage(plugin.msg("auto_18eaa70b4c"));
        }
        player.sendMessage(plugin.msg("auto_0c59e56f48"));
        player.sendMessage(plugin.msg("auto_9f711699eb"));
        player.sendMessage(plugin.msg("auto_22083dca80"));
        player.sendMessage(plugin.msg("auto_566b5c0877"));
        player.sendMessage(plugin.msg("auto_5b9f0a9ea8"));
        player.sendTitle(plugin.msg("auto_471799fa47"), plugin.msg("auto_9a39ddbad2"), 10, 60, 20);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0.8f);
    }

    // ========================
    //  START TRIAL
    // ========================

    // ========================
    //  BUOC 1: THIEN DINH (Vuot Tam Ma)
    // ========================

    public void startThienDinh(Player player, PlayerData data) {
        if (!data.isPendingBreakthrough()) {
            player.sendMessage(plugin.prefix() + "§cLinh Khí chưa đủ! Tiếp tục tu luyện."); return;
        }
        if (data.isInThienDinh()) {
            player.sendMessage(plugin.prefix() + "§cĐang trong Thiền Định!"); return;
        }
        if (data.isPendingDotPha()) {
            player.sendMessage(plugin.prefix() + "§eThiền Định đã hoàn thành! Dùng §a/dotpha §eđể bắt đầu Thiên Kiếp."); return;
        }

        // Neu stage chua len Thanh Gioi, khong can Thien Dinh → cho dot pha thang
        if (data.getStage().ordinal() < CultivationStage.BAN_THANH.ordinal()) {
            player.sendMessage(plugin.prefix() + "§7Cảnh giới này không cần Thiền Định. Dùng §a/dotpha §7trực tiếp!");
            return;
        }

        data.setInThienDinh(true);

        player.sendMessage(plugin.msg("auto_8760d95161"));
        player.sendMessage(plugin.msg("auto_26f115ecd7"));
        player.sendMessage(plugin.msg("auto_e022d24082"));
        player.sendMessage("  §7Cảnh giới: §e" + data.getStage().getFullDisplay(data.getRank()));
        player.sendMessage(plugin.msg("auto_cad0ab1dd1"));
        player.sendMessage(plugin.msg("auto_b493deaf2f"));
        player.sendTitle(plugin.msg("auto_42f2d33d72"), plugin.msg("auto_a367ba169c"), 10, 60, 20);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.5f);

        // Hieu ung hat den/do quay nhieu trong luc thien dinh
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !data.isInThienDinh()) return;
            player.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, player.getLocation().add(0,1,0), 60, 0.5, 1, 0.5, 0.02);
            player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation().add(0,1,0), 30, 0.4, 0.8, 0.4, 0.01);
        }, 20L);

        // Kích hoat TamMaManager
        if (plugin.getTamMaManager() != null) {
            boolean triggered = plugin.getTamMaManager().tryTrigger(player, data,
                () -> onThienDinhPass(player, data),   // Pass
                () -> onThienDinhFail(player, data)    // Fail
            );
            if (triggered) return; // Cho GUI Tam Ma
        }
        // Neu khong co Tam Ma (stage thap or xac suat khong kích hoat) → qua thang
        onThienDinhPass(player, data);
    }

    private void onThienDinhPass(Player player, PlayerData data) {
        data.setInThienDinh(false);
        data.setPendingDotPha(true);
        player.sendMessage(plugin.prefix() + "§a§l✓ Thiền Định thành công! §7Dùng §a/dotpha §7để bắt đầu Thiên Kiếp!");
        player.sendTitle(plugin.msg("auto_41be926c0e"), plugin.msg("auto_808a23f1fe"), 10, 80, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    private void onThienDinhFail(Player player, PlayerData data) {
        data.setInThienDinh(false);
        data.setPendingDotPha(false);
        // That bai Thien Dinh: tru 20% EXP (Linh Khi)
        long loss = (long)(data.getLinhKhi() * 0.20);
        data.setLinhKhi(Math.max(0, data.getLinhKhi() - loss));
        player.sendMessage(plugin.prefix() + "§c§l✗ Tâm Ma chiếm hữu! §7Mất §e20%§7 Linh Khí. Tiếp tục tu luyện!");
        player.sendTitle(plugin.msg("auto_97d77faf34"), plugin.msg("auto_ef0a2f0898"), 10, 80, 20);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 0.5f);
    }

    // ========================
    //  BUOC 2: DOT PHA (Vuot Loi Kiep)
    // ========================

    public void startBreakthroughTrial(Player player, PlayerData data) {
        // Neu chua thien dinh va stage >= Thanh Gioi, bat buoc phai thien dinh truoc
        if (data.getStage().ordinal() >= CultivationStage.BAN_THANH.ordinal() && !data.isPendingDotPha()) {
            if (!data.isPendingBreakthrough()) {
                player.sendMessage(plugin.prefix() + "§cLinh Khí chưa đủ! Tiếp tục tu luyện."); return;
            }
            player.sendMessage(plugin.prefix() + "§cPhải Thiền Định trước! Dùng §e/vuottamma §cđể bắt đầu bước 1!"); return;
        }
        if (!data.isPendingBreakthrough() && !data.isPendingDotPha()) {
            player.sendMessage(plugin.prefix() + "§cLinh Khí chưa đủ! Tiếp tục tu luyện."); return;
        }
        if (data.isInBreakthroughTrial()) {
            player.sendMessage(plugin.prefix() + "§cĐang trong Thiên Kiếp!"); return;
        }
        // Bat buoc phai co "Dot Pha Lenh" cua canh gioi hien tai (nhan tu nhiem vu dot pha)
        CultivationStage curStage = data.getStage();
        if (!consumeToken(player, curStage)) {
            player.sendMessage(plugin.prefix() + "§c✦ Cần có §dĐột Phá Lệnh §7- " + curStage.getColor() + curStage.getDisplayName()
                    + " §cmới có thể bắt đầu Thiên Kiếp!");
            player.sendMessage(plugin.prefix() + "§7Hoàn thành nhiệm vụ đột phá của cảnh giới hiện tại để nhận vật phẩm này.");
            return;
        }
        data.setInBreakthroughTrial(true);
        data.setPendingBreakthrough(false);
        data.setPendingDotPha(false);
        CultivationStage stage = data.getStage();
        int waves = getWaveCount(stage);
        double difficulty = calculateDifficulty(data);
        plugin.getEffectEngine().breakthroughCharge(player, data);
        player.sendMessage(plugin.msg("auto_c64f9d2597"));
        player.sendMessage(plugin.msg("auto_9a357c49cc"));
        player.sendMessage(plugin.msg("auto_52be7d878b"));
        player.sendMessage("  §7Cảnh giới: §e" + stage.getFullDisplay(data.getRank()));
        player.sendMessage("  §7Số sóng: §e" + waves + "  §7Độ khó: §e" + String.format("%.1f%%", difficulty * 100));
        player.sendMessage(plugin.msg("auto_83cc0b8e42"));
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> runTrial(player, data, waves, difficulty), 60L);
    }

    private void runTrial(Player player, PlayerData data, int totalWaves, double difficulty) {
        if (!player.isOnline()) { failTrial(player, data); return; }
        double maxHp = player.getMaxHealth();
        int ord = data.getStage().ordinal();

        // Sat thuong moi song: tang manh theo canh gioi - Thien Kiep ngay cang hung han.
        // TAT CA cac so ben duoi doc tu config.yml (cultivation.breakthrough.trial.*)
        // de admin de dang chinh do kho theo TUNG khoang canh gioi ma khong can
        // sua code/build lai jar. He 25 canh gioi moi, ordinal 0-24:
        //   Linh+De (0-9) | Thanh (10-14) | Tien (15-19) | Thien (20-23) | Vo Thuong (24)
        org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig();
        double dmgBase;
        if (ord <= 9) {
            dmgBase = cfg.getDouble("cultivation.breakthrough.trial.dmg-base.linh-de-start", 0.11)
                    + ord * cfg.getDouble("cultivation.breakthrough.trial.dmg-base.linh-de-step", 0.008);
        } else if (ord <= 14) {
            dmgBase = cfg.getDouble("cultivation.breakthrough.trial.dmg-base.thanh-start", 0.18)
                    + (ord - 10) * cfg.getDouble("cultivation.breakthrough.trial.dmg-base.thanh-step", 0.015);
        } else if (ord <= 19) {
            dmgBase = cfg.getDouble("cultivation.breakthrough.trial.dmg-base.tien-start", 0.28)
                    + (ord - 15) * cfg.getDouble("cultivation.breakthrough.trial.dmg-base.tien-step", 0.04);
        } else if (ord <= 23) {
            dmgBase = cfg.getDouble("cultivation.breakthrough.trial.dmg-base.thien-start", 0.50)
                    + (ord - 20) * cfg.getDouble("cultivation.breakthrough.trial.dmg-base.thien-step", 0.13);
        } else {
            dmgBase = cfg.getDouble("cultivation.breakthrough.trial.dmg-base.vo-thuong", 0.95);
        }

        // Hoi phuc sau moi song: doc tu config, mac dinh THAP HON ban cu (0.26 -> 0.12)
        // vi 0.26 la ly do chinh khien Thien Kiep qua de - hoi lai gan het sat
        // thuong vua nhan moi song nen gan nhu khong the thua.
        double healPerWave = cfg.getDouble("cultivation.breakthrough.trial.heal-per-wave", 0.12);
        double healCapPercent = cfg.getDouble("cultivation.breakthrough.trial.heal-cap-percent", 0.75);
        double lowHpFailMultiplier = cfg.getDouble("cultivation.breakthrough.trial.low-hp-fail-chance-multiplier", 0.65);

        // Random fail KHONG con nua - thay bang: neu HP duoi 15% thi co 50% that bai
        // (khuyen khich dung dan truoc, nhung van co hy vong)

        final int[] waveRef = {0};
        final double dmgPct = dmgBase;
        long waveIntervalTicks = Math.max(1L,
                plugin.getConfig().getLong("cultivation.breakthrough.wave-interval-seconds", 2) * 20L);

        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline()) { cancel(); failTrial(player, data); return; }
                if (!data.isInBreakthroughTrial()) { cancel(); return; }

                waveRef[0]++;
                int wave = waveRef[0];

                if (wave > totalWaves) { cancel(); successTrial(player, data); return; }

                // Gay sát thương (co bien dong +-20%)
                double dmg = maxHp * dmgPct * (0.8 + random.nextDouble() * 0.4);

                // Trấn Thiên Giáp: chỉ cần trong túi đồ -> giảm 50% sát thương Thiên Kiếp
                if (plugin.getCombatListener().hasTranThienGiapInInventory(player)) {
                    dmg *= 0.5;
                }

                double newHp = player.getHealth() - dmg;

                if (newHp <= 0.5) {
                    // Chet trong thien kiep
                    cancel();
                    player.setHealth(0.5);
                    failTrial(player, data);
                    return;
                }

                player.setHealth(newHp);
                plugin.getEffectEngine().breakthroughTrialWave(player, wave, totalWaves);

                // Hoi phuc nho sau moi song (tao cam giac thu vi, khong bi thua mat)
                double healAmount = maxHp * healPerWave;
                double afterHeal  = Math.min(player.getHealth() + healAmount, maxHp * healCapPercent);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && data.isInBreakthroughTrial()) {
                        player.setHealth(Math.max(afterHeal, player.getHealth()));
                    }
                }, 10L); // Hoi sau 0.5 giay

                // Chi that bai khi HP = 15% trai xuong VA xac suat theo canh gioi
                // ("difficulty" o day thuc chat la "do de" - cang cao cang de, xem
                // CultivationStage.getBreakthroughDifficulty()). Nhan he so tu config
                // (mac dinh 0.65, truoc la 0.5) de tang nguy co that bai khi HP thap.
                double hpFrac = player.getHealth() / maxHp;
                if (hpFrac < 0.15 && random.nextDouble() < (1.0 - difficulty) * lowHpFailMultiplier) {
                    cancel();
                    plugin.getServer().getScheduler().runTaskLater(plugin,
                            () -> failTrial(player, data), 5L);
                }
            }
        }.runTaskTimer(plugin, 20L, waveIntervalTicks); // config "cultivation.breakthrough.wave-interval-seconds"
    }

    // ========================
    //  SUCCESS / FAIL
    // ========================

    private void successTrial(Player player, PlayerData data) {
        data.setInBreakthroughTrial(false);
        CultivationStage oldStage = data.getStage();
        int oldRank = data.getRank();
        // Tam Ma da duoc xu ly o Buoc 1 (Thien Dinh) nen di thang vao completeBreakthrough
        completeBreakthrough(player, data, oldStage, oldRank);
    }

    /** Actually advance the player stage/rank and give rewards */
    private void completeBreakthrough(Player player, PlayerData data,
                                      CultivationStage oldStage, int oldRank) {
        boolean stageChanged = false;
        if (oldStage.hasNextRank(oldRank)) {
            data.setRank(oldRank + 1);
        } else {
            CultivationStage next = oldStage.nextStage();
            if (next != null) { data.setStage(next); data.setRank(1); stageChanged = true; }
        }
        data.setLinhKhi(0);
        long reward = plugin.getTienManager().calculateBreakthroughReward(oldStage);
        plugin.getTienManager().give(player, reward);
        plugin.getCombatListener().updatePlayerStats(player, data);
        plugin.getFlightListener().updateFlight(player, data);
        plugin.getTocManager().onBreakthrough(player, data);
        plugin.getEffectEngine().breakthroughSuccess(player, data);
        if (plugin.getPartyManager() != null) plugin.getPartyManager().onBreakthrough(player);
        plugin.getNametagManager().updateNametag(player, data);
        plugin.getSkillItemManager().giveSkillItems(player, data);
        if (plugin.getFlyingSwordManager() != null)
            plugin.getFlyingSwordManager().updateSwords(player, data);
        if (plugin.getSwordSummon() != null)
            plugin.getSwordSummon().refreshIfActive(player);
        if (plugin.getPetSummon() != null)
            plugin.getPetSummon().refreshIfActive(player);

        // ── Hook v4 systems ──
        if (plugin.getQuestManager() != null)  plugin.getQuestManager().onBreakthrough(player);
        if (plugin.getKarmaManager() != null)  plugin.getKarmaManager().addCongDuc(data, 50L);
        if (stageChanged) syncLuckPerms(player, data);
        else if (plugin.getLuckPermsHook() != null) plugin.getLuckPermsHook().syncPlayer(player, data);
        String newLevel = data.getStage().getFullDisplay(data.getRank());
        if (data.getStage().isTopStage()) {
            String bc = "§4§l╔══════════════════════════════════════╗\n"
                    + "§c§l  ☠ " + player.getName() + " đã đạt ĐỈNH CAO TUYỆT ĐỐI!\n"
                    + "§6§l  " + data.getStage().getColor() + data.getStage().getDisplayName() + "\n"
                    + "§4§l╚══════════════════════════════════════╝";
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(bc));
            player.sendTitle(data.getStage().getColor() + "§l☠ " + data.getStage().getDisplayName(), plugin.msg("auto_9551040024"), 10, 120, 20);
        } else if (data.getStage().ordinal() >= CultivationStage.THIEN_DE.ordinal()) {
            String bc = "§6§l╔══════════════════════════════════╗\n"
                    + "§e§l  🌟 " + player.getName() + " đạt cảnh giới\n"
                    + "§6§l  " + data.getStage().getColor() + data.getStage().getDisplayName() + "§6§l!\n"
                    + "§6§l╚══════════════════════════════════╝";
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(bc));
            player.sendTitle(data.getStage().getColor() + "§l🌟 " + data.getStage().getDisplayName(), plugin.msg("auto_6856c7609d"), 10, 100, 20);
        } else {
            player.sendTitle(plugin.msg("auto_60a172cb9c"), newLevel, 10, 80, 20);
        }
        player.sendMessage("§a✦ Đột phá thành công! Cấp mới: " + newLevel + " | +" + fmt(reward) + " " + plugin.getTienManager().tenTien() + "!");
        if (plugin.getCoDuyenManager() != null) plugin.getCoDuyenManager().rollForBreakthrough(player);
    }

    private void failTrial(Player player, PlayerData data) {
        data.setInBreakthroughTrial(false);
        data.setPendingBreakthrough(false);
        data.setPendingDotPha(false);
        data.setInThienDinh(false);
        int newRank = data.getRank() - 1;
        if (newRank < 1) {
            CultivationStage prev = data.getStage().prevStage();
            if (prev != null) {
                data.setStage(prev);
                data.setRank(prev.getMaxRank());
                syncLuckPerms(player, data); // tut canh gioi -> phai go permission cu, khong thi con giu quyen cao hon that
            } else {
                data.setRank(1);
            }
        } else {
            data.setRank(newRank);
        }
        data.setLinhKhi(0);
        plugin.getCombatListener().updatePlayerStats(player, data);
        plugin.getEffectEngine().breakthroughFail(player);
        plugin.getNametagManager().updateNametag(player, data);
        String newLevel = data.getStage().getFullDisplay(data.getRank());
        player.sendMessage("§c§l✗ ĐỘT PHÁ THẤT BẠI! §7Tụt xuống: " + newLevel);
        player.sendTitle(plugin.msg("auto_97d77faf34"), "§7Tut xuong: " + newLevel, 10, 80, 20);
    }

    // ========================
    //  DEATH DURING TRIAL
    // ========================

    @EventHandler
    public void onDeathDuringTrial(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.isInBreakthroughTrial()) return;
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            data.setInBreakthroughTrial(false);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> failTrial(player, data), 1L);
        }
    }

    // ========================
    //  HELPERS
    // ========================

    private double calculateDifficulty(PlayerData data) {
        double base = data.getStage().getBreakthroughDifficulty();
        if (data.hasLinhCan()) {
            com.tutien.lingcan.LinhCanGrade grade =
                    com.tutien.lingcan.LinhCanGrade.fromCount(data.getLinhCan().size());
            base += switch (grade) {
                case THIEN_LINH_CAN -> 0.30;
                case NHI_LINH_CAN   -> 0.10;
                case CHAN_LINH_CAN  -> 0.00;
                case TAM_LINH_CAN  -> -0.05;
                case NGUY_LINH_CAN -> -0.20;
            };
        }
        if (data.hasDaoPath()) {
            if (data.getDaoPath() == DaoPath.CHINH_DAO) base += 0.15;
            else if (data.getDaoPath() == DaoPath.MA_DAO) base -= 0.20;
        }
        if (data.getToc() != null && data.getToc() == com.tutien.toc.TocType.LINH_TOC) base += 0.20;
        return Math.max(0.05, Math.min(base, 0.99));
    }

    private int getWaveCount(CultivationStage stage) {
        /**
         * So song Thien Kiep (he 25 canh gioi moi, ordinal 0-24):
         * Linh Gioi (0-4): 3-7 | De Gioi (5-9): 8-12 | Thanh Gioi (10-14): 13-17
         * Tien Gioi (15-19): 20-28 | Thien Gioi (20-23): 35-56
         * Vo Thuong Chi Ton (24): 100 song - kho nhat server (dinh cao tuyet doi)
         */
        int ord = stage.ordinal();
        if (ord <= 4)  return 3 + ord;                     // Linh: 3-7
        if (ord <= 9)  return 8 + (ord - 5);                // De: 8-12
        if (ord <= 14) return 13 + (ord - 10);              // Thanh: 13-17
        if (ord <= 19) return 20 + (ord - 15) * 2;          // Tien: 20-28
        if (ord <= 23) return 35 + (ord - 20) * 7;          // Thien: 35-56
        return 100;                                          // Vo Thuong Chi Ton
    }

    private String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.2fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}