package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PhaBaoListener implements Listener {

    private final TuTienPlugin plugin;

    public PhaBaoListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Shift + Chuot Phai -> kich hoat phap bao.
     * Su dung LOWEST priority de chay truoc SkillItemManager (NORMAL).
     * Chi xu ly event o tay chinh (HAND) de tranh double trigger.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPhaBaoUse(PlayerInteractEvent event) {
        // Chi xu ly tay chinh
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;

        // Phai Shift
        if (!event.getPlayer().isSneaking()) return;

        // Phai la click phai
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        var player = event.getPlayer();
        PhaBaoManager pm = plugin.getPhaBaoManager();

        // Tim phap bao: uu tien tay chinh, sau do off-hand
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off  = player.getInventory().getItemInOffHand();
        ItemStack pbItem = pm.isPhaBao(main) ? main : (pm.isPhaBao(off) ? off : null);

        if (pbItem == null) return;

        // Co phap bao -> chan event de skill item khong xu ly
        event.setCancelled(true);

        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) {
            player.sendMessage(plugin.prefix() + "§cLỗi dữ liệu! Thử đăng xuất và đăng nhập lại.");
            return;
        }
        if (!data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cBạn cần chọn class trước! Dùng §e/tutien chon");
            return;
        }

        // Goi su dung phap bao
        pm.useAbility(player, data, pbItem);
    }
}
