package com.tutien.listeners;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import com.tutien.lingcan.LinhCanElement;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final TuTienPlugin plugin;

    public PlayerListener(TuTienPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getOrCreate(player);
        data.setPlayerName(player.getName());

        // Fix: dong bo lai trang thai bat/tat Vong Tu Vi (aura) da luu
        // trong PlayerData vao AuraManager. Neu khong co dong nay,
        // AuraManager luon coi moi player la "aura bat" moi lan join,
        // bat ke ho da /tutien aura de tat truoc do.
        plugin.getAuraManager().saveDisabled(player.getUniqueId(), !data.isAuraEnabled());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getCombatListener().updatePlayerStats(player, data);
            plugin.getFlightListener().updateFlight(player, data);

            // Hook v4: Quest + Karma init on join
            if (plugin.getQuestManager() != null) plugin.getQuestManager().onPlayerJoin(player);
            // Dong bo full (group + prefix + permission "tutien.<canh_gioi>") moi lan join,
            // phong truong hop LuckPerms bi reload/mat data luc server restart.
            plugin.getBreakthroughManager().syncLuckPerms(player, data);

            // Lan dau: phan cong TOC (race)
            if (!data.hasToc()) {
                var toc = plugin.getTocManager().rollToc();
                data.setToc(toc);
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        plugin.getTocManager().revealToc(player, data), 60L);
            }

            // Lan dau: phan cong linh can
            if (!data.hasLinhCan()) {
                List<LinhCanElement> roots = plugin.getLinhCanManager().rollLinhCan();
                data.setLinhCan(roots);
                // Hien thi sau 2 giay cho dep
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(plugin.prefix() + "§e✨ Linh Căn giám định bắt đầu...");
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 0.7f);
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        plugin.getLinhCanManager().revealLinhCan(player, data);
                        data.setLinhCanRevealed(true);
                    }, 40L);
                }, 20L);
            }

            if (data.hasClass()) {
                if (player.getInventory().getItem(0) == null) {
                    plugin.getSkillItemManager().giveSkillItems(player, data);
                }
                player.sendMessage(plugin.prefix() + "§7Chào mừng, "
                        + data.getClassType().getColoredName() + "§7!");
                player.sendMessage(plugin.prefix() + data.getStage().getFullDisplay(data.getRank())
                        + " §7| §eLK: §a" + data.getLinhKhi() + "§7/§e" + data.getMaxLinhKhi());
            } else {
                player.sendMessage(plugin.prefix() + "§eDùng §a/tutien chon §eđể bắt đầu tu luyện!");
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDataManager().unload(event.getPlayer().getUniqueId());
        // Don dep goc quay aura trong bo nho de tranh ri ro (memory leak)
        // sau thoi gian dai nhieu player vao/ra server.
        plugin.getAuraManager().cleanup(event.getPlayer().getUniqueId());
        // Don cache dong bo LuckPerms cua nguoi choi nay (khong lien quan toi
        // permission that su, chi la bo nho tam trong RAM de tranh set/unset thua).
        if (plugin.getLuckPermsHook() != null) plugin.getLuckPermsHook().clearCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;
        data.addDeath();
        data.setInvisible(false);
        data.setCounterBuff(false);
        data.setDodgeBuff(false);

        if (data.isInBreakthroughTrial()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                data.setInBreakthroughTrial(false);
                player.sendMessage(plugin.prefix() + "§cChết trong Thiên Kiếp! Đột phá thất bại!");
                data.failBreakthrough();
                plugin.getCombatListener().updatePlayerStats(player, data);
                plugin.getFlightListener().updateFlight(player, data);
            }, 5L);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getCombatListener().updatePlayerStats(player, data);
            plugin.getFlightListener().updateFlight(player, data);
        }, 10L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDanUse(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;
        if (!plugin.getAlchemyManager().isDanItem(item)) return;
        event.setCancelled(true);
        plugin.getAlchemyManager().useDan(p, item);
    }
}