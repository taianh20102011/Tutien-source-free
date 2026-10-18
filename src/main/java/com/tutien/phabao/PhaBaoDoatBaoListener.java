package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import com.tutien.zone.ZoneManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * DOAT BAO PVP (Nhat Dich Tich Huyet) - khong co lenh, hoat dong tu dong.
 *
 * Khi 1 nguoi choi CHET DUOI TAY 1 NGUOI CHOI KHAC (PVP that su, khong tinh
 * mob) TRONG 1 VUNG DUOC XEM LA "VUNG PVP", moi Phap Bao trong tui do (tru
 * dong Ban Menh - vi lore cua chung da ghi ro "khong the mat") co 1 ti le
 * (config) de:
 *   a) ROI RA DAT ngay vi tri chet (nguoi khac - vd hung thu - co the nhat), HOAC
 *   b) neu Phap Bao do co diem Khi Linh > 0: bi TRU LINH TINH (phat, khong roi do).
 *
 * "Vung PVP" duoc xac dinh qua 1 trong 2 cach (config
 * "phabao.doatbao.pvp-zone-keywords"):
 *   - Neu danh sach keyword KHONG rong: vi tri chet phai nam trong 1 Zone
 *     (xem ZoneManager) co TEN chua 1 trong cac keyword do (khong phan biet
 *     hoa/thuong, vd "PVP", "Chien Truong").
 *   - Neu danh sach keyword RONG (mac dinh): fallback dung co PVP cua chinh
 *     THE GIOI Minecraft (world.getPVP()) - tuc la ap dung o BAT KY dau co
 *     bat PVP.
 */
public class PhaBaoDoatBaoListener implements Listener {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    public PhaBaoDoatBaoListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("phabao.doatbao.enabled", true)) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        if (!isPvpZone(victim)) return;

        PhaBaoManager pbm = plugin.getPhaBaoManager();
        double rollChance = plugin.getConfig().getDouble("phabao.doatbao.chance", 0.15);
        int maxItems = plugin.getConfig().getInt("phabao.doatbao.max-items-per-death", 1);
        double linhTinhPenaltyPercent = plugin.getConfig().getDouble("phabao.doatbao.linhtinh-penalty-percent", 0.30);

        PlayerInventory inv = victim.getInventory();
        List<ItemStack[]> sources = List.of(inv.getStorageContents(), inv.getArmorContents());

        int affected = 0;
        outer:
        for (ItemStack[] contents : sources) {
            for (int i = 0; i < contents.length; i++) {
                if (affected >= maxItems) break outer;
                ItemStack item = contents[i];
                if (item == null || !pbm.isPhaBao(item)) continue;

                PhaBaoType type = pbm.getType(item);
                if (type != null && type.name().startsWith("BAN_MENH")) continue; // Ban Menh: khong the mat

                if (random.nextDouble() >= rollChance) continue;

                PhaBaoKhiLinhManager klm = plugin.getPhaBaoKhiLinhManager();
                int spirit = klm.getSpirit(item);
                if (spirit > 0 && random.nextBoolean()) {
                    int penalty = (int) Math.round(spirit * linhTinhPenaltyPercent);
                    klm.reduceSpirit(item, penalty);
                    victim.sendMessage(plugin.prefix() + "§c✦ Đoạt Bảo! §7Pháp Bảo §f" + type.getDisplayName()
                            + " §7bị mất §c" + penalty + " §7điểm Linh Tính khi chết trong vùng PVP.");
                } else {
                    contents[i] = null;
                    // Neu keepInventory dang TAT, item nay co the DA nam san trong
                    // event.getDrops() (server dung inventory GOC de dung danh sach
                    // do roi TRUOC khi handler nay chay) - xoa 1 ban trung khoi do
                    // de tranh ROI DUP (rot ra dat 2 lan).
                    removeOneMatch(event.getDrops(), item);
                    victim.getWorld().dropItemNaturally(victim.getLocation(), item);
                    victim.sendMessage(plugin.prefix() + "§c✦ Đoạt Bảo! §7Pháp Bảo §f" + type.getDisplayName()
                            + " §7đã RƠI RA ĐẤT khi bạn chết trong vùng PVP!");
                    if (killer.isOnline()) {
                        killer.sendMessage(plugin.prefix() + "§a✦ " + victim.getName() + " đã làm rơi 1 Pháp Bảo gần vị trí bạn vừa hạ gục!");
                    }
                }
                affected++;
            }
        }
        inv.setStorageContents(sources.get(0));
        inv.setArmorContents(sources.get(1));
    }

    private void removeOneMatch(List<ItemStack> drops, ItemStack target) {
        for (Iterator<ItemStack> it = drops.iterator(); it.hasNext(); ) {
            ItemStack d = it.next();
            if (d != null && d.isSimilar(target)) { it.remove(); return; }
        }
    }

    private boolean isPvpZone(Player victim) {
        List<String> keywords = plugin.getConfig().getStringList("phabao.doatbao.pvp-zone-keywords");
        if (keywords.isEmpty()) {
            return victim.getWorld().getPVP();
        }
        ZoneManager.Zone zone = plugin.getZoneManager().getZoneAt(victim.getLocation());
        if (zone == null || zone.name == null) return false;
        String zoneNameLower = zone.name.toLowerCase();
        for (String kw : keywords) {
            if (zoneNameLower.contains(kw.toLowerCase())) return true;
        }
        return false;
    }
}
