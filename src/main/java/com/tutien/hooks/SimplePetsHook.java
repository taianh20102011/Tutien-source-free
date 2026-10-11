package com.tutien.hooks;

import com.tutien.TuTienPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Hook toi plugin SimplePets (brainsynder-Dev, https://github.com/brainsynder-Dev/SimplePets).
 *
 * SimplePets update rat thuong xuyen (API noi bo doi build lien tuc: R5-B299 -> R5-B306
 * chi trong thoi gian ngan) nen thay vi goi thang class Java (de gay loi bien dich moi
 * khi ho ra ban moi), hook nay dung LENH CONSOLE da xac nhan chinh xac tu wiki chinh
 * thuc cua plugin (on dinh qua cac ban, khong doi theo tung build noi bo):
 *
 *   /pet summon <type> [player] [nbt]   - VD: /pet summon wolf Steve
 *   /pet remove [player] [type]          - VD: /pet remove Steve wolf
 *
 * Khong can dependency Maven/jar nao ca - chi can server co cai SimplePets that.
 *
 * LUU Y: loai pet (petType) KHONG con hardcode trong hook nay nua — no duoc
 * doc tu config.yml (linh-thu.tiers.<Gioi>.simplepets-type) thong qua
 * PetTierManager, va truyen thang vao day. Muon them Gioi moi hoac doi loai
 * SimplePets cho 1 Gioi -> sua config.yml, khong dung vao code.
 */
public class SimplePetsHook {

    private final TuTienPlugin plugin;
    private final boolean available;

    public SimplePetsHook(TuTienPlugin plugin) {
        this.plugin = plugin;
        boolean ok;
        try {
            ok = Bukkit.getPluginManager().getPlugin("SimplePets") != null;
        } catch (Throwable t) { ok = false; }
        this.available = ok;
        if (ok) plugin.getLogger().info("[TuTien] Đã tìm thấy SimplePets - sẽ dùng làm Linh Thú ngoài.");
    }

    public boolean isAvailable() { return available; }

    /** Trieu hoi pet SimplePets cho nguoi choi, dung loai (petType) lay tu PetTierConfig. */
    public boolean summon(Player player, String petType) {
        if (!available || player == null || petType == null || !petType.matches("[A-Za-z0-9_:-]{1,64}")) return false;
        try {
            // Xóa pet cũ trước; chỉ báo thành công nếu lệnh summon được chấp nhận.
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pet remove " + player.getName());
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "pet summon " + petType + " " + player.getName());
        } catch (Throwable t) {
            plugin.getLogger().warning("[TuTien] Lỗi khi triệu hồi SimplePets: " + t.getMessage());
            return false;
        }
    }

    /** Thu hoi toan bo pet SimplePets hien tai cua nguoi choi. */
    public boolean dismiss(Player player) {
        if (!available || player == null) return false;
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pet remove " + player.getName());
        } catch (Throwable t) {
            plugin.getLogger().warning("[TuTien] Lỗi khi thu hồi SimplePets: " + t.getMessage());
            return false;
        }
    }
}
