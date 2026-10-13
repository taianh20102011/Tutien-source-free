package com.tutien.breakthrough;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * BreakthroughNpcListener — cho phep nguoi choi CLICK CHUOT PHAI vao 1 NPC
 * Citizens (ID khai bao trong config "breakthrough-npc.npc-ids") de bat dau
 * Thien Dinh / Thien Kiep, THAY VI go lenh /vuottamma hoac /dotpha.
 *
 * Chi duoc dang ky (xem TuTienPlugin#onEnable) khi:
 *   1. Plugin Citizens dang chay tren server, VA
 *   2. config "breakthrough-npc.enabled: true"
 * => Neu Citizens khong duoc cai, class nay KHONG BAO GIO duoc load nen
 *    khong the gay loi NoClassDefFoundError.
 *
 * Logic click NPC "thong minh": tu quyet dinh nen goi Buoc 1 (Thien Dinh)
 * hay Buoc 2 (Thien Kiep) dua tren trang thai hien tai cua nguoi choi,
 * y het nhu 2 lenh /vuottamma + /dotpha gop lai — nguoi choi chi can
 * click NPC lap lai la du, khong can nho lenh nao.
 */
public class BreakthroughNpcListener implements Listener {

    private final TuTienPlugin plugin;

    public BreakthroughNpcListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcRightClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        List<Integer> allowedIds = plugin.getConfig().getIntegerList("breakthrough-npc.npc-ids");
        if (allowedIds == null || !allowedIds.contains(npc.getId())) return;

        Player player = event.getClicker();
        tryBreakthrough(player);
    }

    private void tryBreakthrough(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cChưa chọn class!");
            return;
        }
        if (data.getStage().isTopStage() && data.getRank() >= data.getStage().getMaxRank()) {
            player.sendMessage(plugin.prefix() + "§6Bạn đã đạt cảnh giới tối cao!");
            return;
        }

        // Da xong Buoc 1 (Thien Dinh), dang cho vao Buoc 2 (Thien Kiep)
        if (data.isPendingDotPha()) {
            plugin.getBreakthroughManager().startBreakthroughTrial(player, data);
            return;
        }
        // Dang giua chung Buoc 1 hoac Buoc 2 roi -> khong lam gi, tranh chong lenh
        if (data.isInThienDinh() || data.isInBreakthroughTrial()) {
            player.sendMessage(plugin.prefix() + "§cBạn đang trong quá trình Đột Phá!");
            return;
        }

        // Chua co Linh Khi day du -> kiem tra + bao thieu
        if (!data.isPendingBreakthrough()) {
            long need = data.getMaxLinhKhi() - data.getLinhKhi();
            if (need > 0) {
                player.sendMessage(plugin.prefix() + "§cCần thêm §e" + fmt(need) + " §cLinh Khí! ("
                        + fmt(data.getLinhKhi()) + "/" + fmt(data.getMaxLinhKhi()) + ")");
                return;
            }
            data.setPendingBreakthrough(true);
        }

        // Tu Thanh Gioi tro len phai qua Thien Dinh (Buoc 1) truoc; cac canh
        // gioi thap hon di thang vao Thien Kiep (Buoc 2) — giong het logic
        // trong BreakthroughManager#startThienDinh.
        if (data.getStage().ordinal() >= CultivationStage.BAN_THANH.ordinal()) {
            plugin.getBreakthroughManager().startThienDinh(player, data);
        } else {
            plugin.getBreakthroughManager().startBreakthroughTrial(player, data);
        }
    }

    private String fmt(long n) {
        if (n >= 1_000_000) return String.format("%.2fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}
