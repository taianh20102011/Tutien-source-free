package com.tutien.quest;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.util.*;

public class QuestManager implements Listener {

    private final TuTienPlugin plugin;

    // Keys trong PlayerData.extraData
    private static final String KEY_DAILY_QUEST   = "quest_daily_type";
    private static final String KEY_WEEKLY_QUEST  = "quest_weekly_type";
    private static final String KEY_DAILY_PROG    = "quest_daily_prog";
    private static final String KEY_WEEKLY_PROG   = "quest_weekly_prog";
    private static final String KEY_DAILY_DONE    = "quest_daily_done";
    private static final String KEY_WEEKLY_DONE   = "quest_weekly_done";
    private static final String KEY_LAST_DAILY    = "quest_last_daily_day";
    private static final String KEY_LAST_WEEKLY   = "quest_last_weekly_week";

    public QuestManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Reset quest mỗi ngày lúc 0h
        new BukkitRunnable() {
            @Override public void run() {
                checkDailyReset();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // check every 60s
    }

    private void checkDailyReset() {
        long today  = LocalDate.now().toEpochDay();
        int  week   = LocalDate.now().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = plugin.getDataManager().get(p);
            if (d.getExtraLong(KEY_LAST_DAILY, 0) < today) {
                assignDailyQuest(d);
                d.setExtraLong(KEY_LAST_DAILY, today);
            }
            if (d.getExtraInt(KEY_LAST_WEEKLY, 0) < week) {
                assignWeeklyQuest(d);
                d.setExtraInt(KEY_LAST_WEEKLY, week);
            }
        }
    }

    public void onPlayerJoin(Player player) {
        PlayerData d = plugin.getDataManager().get(player);
        long today = LocalDate.now().toEpochDay();
        int  week  = LocalDate.now().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        if (d.getExtraLong(KEY_LAST_DAILY, 0) < today)  { assignDailyQuest(d);  d.setExtraLong(KEY_LAST_DAILY,  today); }
        if (d.getExtraInt(KEY_LAST_WEEKLY,  0) < week)  { assignWeeklyQuest(d); d.setExtraInt(KEY_LAST_WEEKLY,  week); }
        showQuestStatus(player);
    }

    private void assignDailyQuest(PlayerData d) {
        QuestType[] dailies = Arrays.stream(QuestType.values()).filter(q->q.isDaily).toArray(QuestType[]::new);
        QuestType q = dailies[new Random().nextInt(dailies.length)];
        d.setExtraStr(KEY_DAILY_QUEST, q.name());
        d.setExtraInt(KEY_DAILY_PROG, 0);
        d.setExtraBool(KEY_DAILY_DONE, false);
    }

    private void assignWeeklyQuest(PlayerData d) {
        QuestType[] weeklies = Arrays.stream(QuestType.values()).filter(q->q.isWeekly).toArray(QuestType[]::new);
        QuestType q = weeklies[new Random().nextInt(weeklies.length)];
        d.setExtraStr(KEY_WEEKLY_QUEST, q.name());
        d.setExtraInt(KEY_WEEKLY_PROG, 0);
        d.setExtraBool(KEY_WEEKLY_DONE, false);
    }

    // ── Progress tracking ──────────────────────────────────────────

    public void addProgress(PlayerData d, String eventType, int amount) {
        addProgressFor(d, KEY_DAILY_QUEST,  KEY_DAILY_PROG,  KEY_DAILY_DONE,  eventType, amount);
        addProgressFor(d, KEY_WEEKLY_QUEST, KEY_WEEKLY_PROG, KEY_WEEKLY_DONE, eventType, amount);
    }

    private void addProgressFor(PlayerData d, String questKey, String progKey,
                                 String doneKey, String event, int amount) {
        if (d.getExtraBool(doneKey, false)) return;
        String qName = d.getExtraStr(questKey, "");
        if (qName.isEmpty()) return;
        QuestType q;
        try { q = QuestType.valueOf(qName); } catch (Exception e) { return; }
        // Check if event matches quest type
        boolean match = switch (event) {
            case "MOB_KILL" -> q == QuestType.DAILY_KILL_MOB || q == QuestType.WEEKLY_KILL_1000;
            case "BREAKTHROUGH" -> q == QuestType.DAILY_BREAKTHROUGH || q == QuestType.WEEKLY_BREAKTHROUGH_5;
            case "ALCHEMY" -> q == QuestType.DAILY_ALCHEMY || q == QuestType.WEEKLY_ALCHEMY_20;
            case "HERB" -> q == QuestType.DAILY_HERB;
            case "MEDITATE" -> q == QuestType.DAILY_MEDITATE;
            case "BOSS_KILL" -> q == QuestType.WEEKLY_BOSS;
            case "PVP_FIGHT" -> q == QuestType.WEEKLY_PVP;
            default -> false;
        };
        if (!match) return;
        int prog = d.getExtraInt(progKey, 0) + amount;
        d.setExtraInt(progKey, prog);
        if (prog >= q.targetCount) {
            d.setExtraBool(doneKey, true);
            completeQuest(d, q);
        }
    }

    private void completeQuest(PlayerData d, QuestType q) {
        d.addTien(q.rewardLT);
        Player p = Bukkit.getPlayer(d.getUuid());
        if (p != null) {
            p.sendMessage(plugin.msg("auto_da39a3ee5e"));
            p.sendMessage(plugin.msg("auto_7fcdcd636f"));
            p.sendMessage(plugin.msg("auto_f63ebf864d"));
            p.sendMessage("  §7" + q.displayDesc());
            p.sendMessage("  §6+" + q.rewardLT + " " + plugin.getTienManager().tenTien() + "  §b+" + q.rewardLK + " LK bonus");
            p.sendMessage(plugin.msg("auto_045f213664"));
            p.sendTitle(plugin.msg("auto_0dbd1bacbb"), "§e+" + q.rewardLT + " LT", 10, 60, 20);
            p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            // Bonus LK vào tích lũy
            d.setLinhKhi(d.getLinhKhi() + (long)q.rewardLK);
        }
    }

    public void showQuestStatus(Player p) {
        PlayerData d = plugin.getDataManager().get(p);
        String dq = d.getExtraStr(KEY_DAILY_QUEST, "");
        String wq = d.getExtraStr(KEY_WEEKLY_QUEST, "");
        p.sendMessage(plugin.msg("auto_a15d2d4d36"));
        if (!dq.isEmpty()) {
            try {
                QuestType q = QuestType.valueOf(dq);
                int prog = d.getExtraInt(KEY_DAILY_PROG, 0);
                boolean done = d.getExtraBool(KEY_DAILY_DONE, false);
                String status = done ? "§a[XONG]" : "§e[" + prog + "/" + q.targetCount + "]";
                p.sendMessage("§eNgày: §7" + q.displayDesc() + " " + status);
            } catch (Exception ignored) {}
        }
        if (!wq.isEmpty()) {
            try {
                QuestType q = QuestType.valueOf(wq);
                int prog = d.getExtraInt(KEY_WEEKLY_PROG, 0);
                boolean done = d.getExtraBool(KEY_WEEKLY_DONE, false);
                String status = done ? "§a[XONG]" : "§e[" + prog + "/" + q.targetCount + "]";
                p.sendMessage("§dTuần: §7" + q.displayDesc() + " " + status);
            } catch (Exception ignored) {}
        }
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player player) {
            if (event.getEntity() instanceof Player) return;
            PlayerData d = plugin.getDataManager().get(player);
            addProgress(d, "MOB_KILL", 1);
        }
    }

    public void onBreakthrough(Player p)  { addProgress(plugin.getDataManager().get(p), "BREAKTHROUGH", 1); }
    public void onAlchemy(Player p)       { addProgress(plugin.getDataManager().get(p), "ALCHEMY", 1); }
    public void onHerb(Player p)          { addProgress(plugin.getDataManager().get(p), "HERB", 1); }
    public void onMeditate(Player p, int min){ addProgress(plugin.getDataManager().get(p), "MEDITATE", min); }
    public void onBossKill(Player p)      { addProgress(plugin.getDataManager().get(p), "BOSS_KILL", 1); }
    public void onPvpFight(Player p)      { addProgress(plugin.getDataManager().get(p), "PVP_FIGHT", 1); }
}
