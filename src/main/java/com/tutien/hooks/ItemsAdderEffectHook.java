package com.tutien.hooks;

import com.tutien.TuTienPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Hook toi ItemsAdder (https://itemsadder.devs.beer) de cho phep Admin server
 * TU THAY hieu ung particle mac dinh cua TuTienPlugin bang mot "Custom Entity"
 * VFX rieng lam trong ItemsAdder (model rieng + particle bone + animation rieng),
 * ma KHONG can sua 1 dong code Java nao ca — chi sua config.yml.
 *
 * QUY TAC HOAT DONG (rat quan trong, doc ky):
 *   - Moi "su kien hieu ung" (dot pha thanh cong, boss xuat hien, dung skill...)
 *     co 1 "effectKey" rieng, khai bao trong config.yml duoi
 *     effects.itemsadder.map.<effectKey>: "namespace:id"
 *
 *   - Khi mot su kien xay ra, EffectEngine se GOI hook nay TRUOC TIEN qua
 *     tryPlay(effectKey, location):
 *       + Neu ID de trong "" -> tryPlay() tra ve false ngay, EffectEngine
 *         chay tiep hieu ung particle CO SAN nhu binh thuong.
 *       + Neu ID co dien nhung ItemsAdder CHUA nap duoc Custom Entity do
 *         (ban chua lam xong model, hoac go sai ten, hoac chua /iareload)
 *         -> tryPlay() cung tra ve false -> van roi ve hieu ung mac dinh,
 *         KHONG BAO GIO bi loi hay bi "mat hieu ung" ca.
 *       + Chi khi ID hop le VA da duoc ItemsAdder dang ky -> tryPlay() moi
 *         spawn Custom Entity do va tra ve true -> EffectEngine se return
 *         ngay, KHONG chay hieu ung particle mac dinh nua (tranh chay 2 lop
 *         hieu ung chong len nhau).
 *
 *   => Ket qua: ban co the vua choi vua lam hieu ung rieng tung cai mot.
 *      Cai nao chua lam xong thi tu dong dung ban mac dinh co san, khong
 *      anh huong gi den trai nghiem nguoi choi trong luc cho.
 *
 * An toan neu server KHONG cai ItemsAdder: moi loi (ClassNotFound, v.v.)
 * deu duoc bat lai, tryPlay() chi tra ve false, khong bao gio lam crash
 * hay lam gian doan hieu ung mac dinh.
 */
public class ItemsAdderEffectHook {

    private final TuTienPlugin plugin;
    private final boolean pluginPresent;

    public ItemsAdderEffectHook(TuTienPlugin plugin) {
        this.plugin = plugin;
        boolean ok;
        try {
            ok = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        } catch (Throwable t) {
            ok = false;
        }
        this.pluginPresent = ok;
        if (ok) {
            plugin.getLogger().info("[TuTien] Đã tìm thấy ItemsAdder - hiệu ứng tùy chỉnh " +
                    "sẽ được ưu tiên dùng nếu bạn cấu hình trong config.yml (effects.itemsadder).");
        }
    }

    /** true neu server co cai plugin ItemsAdder (chi kiem tra su hien dien, chua chac da co hieu ung nao duoc cau hinh). */
    public boolean isPluginPresent() {
        return pluginPresent;
    }

    private boolean isFeatureEnabled() {
        return plugin.getConfig().getBoolean("effects.itemsadder.enabled", true);
    }

    /** Doc namespace:id da cau hinh cho 1 effectKey. Rong neu chua cau hinh. */
    private String idFor(String effectKey) {
        String id = plugin.getConfig().getString("effects.itemsadder.map." + effectKey, "");
        return id == null ? "" : id.trim();
    }

    /**
     * Thu phat hieu ung ItemsAdder cho su kien {@code effectKey} tai vi tri {@code loc}.
     *
     * @param effectKey ten su kien, vi du "dot-pha-thanh-cong" (xem day du trong config.yml)
     * @param loc       vi tri se spawn Custom Entity
     * @return true neu DA spawn thanh cong hieu ung ItemsAdder (goi noi tiep phai return
     *         ngay, khong chay hieu ung mac dinh nua). false neu chua cau hinh / chua co /
     *         ItemsAdder khong cai -> noi goi phai tu chay tiep hieu ung particle mac dinh.
     */
    public boolean tryPlay(String effectKey, Location loc) {
        if (!pluginPresent || !isFeatureEnabled() || loc == null || loc.getWorld() == null) return false;

        String id = idFor(effectKey);
        if (id.isEmpty()) return false;
        // Premium VFX/skill skins are entitlement-gated in the compiled release build.
        if (plugin.getLicenseManager() != null && !plugin.getLicenseManager().hasPremiumAccess()) return false;

        try {
            if (!dev.lone.itemsadder.api.CustomEntity.isInRegistry(id)) {
                // ID sai ten hoac chua duoc ItemsAdder nap (chua /iareload) -> fallback mac dinh.
                return false;
            }

            dev.lone.itemsadder.api.CustomEntity entity =
                    dev.lone.itemsadder.api.CustomEntity.spawn(id, loc);
            if (entity == null) return false; // Spawn that bai vi ly do nao do -> fallback mac dinh.

            // Neu co cau hinh ten animation va Custom Entity co animation do -> phat luon.
            String anim = plugin.getConfig().getString("effects.itemsadder.animation", "");
            if (anim != null && !anim.isEmpty()) {
                try {
                    if (entity.hasAnimation(anim)) entity.playAnimation(anim);
                } catch (Throwable ignored) { /* model khong co animation nay, bo qua */ }
            }

            // Tu dong don dep Custom Entity sau X tick de khong de rac ngoai map.
            long lifetime = plugin.getConfig().getLong("effects.itemsadder.lifetime-ticks", 100L);
            if (lifetime > 0) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        Entity base = entity.getEntity();
                        if (base != null && base.isValid()) entity.destroy();
                    } catch (Throwable ignored) { /* da bi xoa boi nguyen nhan khac, bo qua */ }
                }, lifetime);
            }

            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[TuTien] Lỗi khi phát hiệu ứng ItemsAdder '" + id +
                    "' (" + effectKey + "): " + t.getMessage() + " -> dùng tạm hiệu ứng mặc định.");
            return false;
        }
    }
}
