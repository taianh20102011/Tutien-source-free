package com.tutien.listeners;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * He thong bay:
 *  - Canh Tien Gioi (Chan Tien tro len): duoc phep bay
 *  - Dao To: bay nhanh hon, hieu ung dac biet
 */
public class FlightListener implements Listener {

    private final TuTienPlugin plugin;

    // Ordinal cua Chan Tien (canh gioi dau tien o Tien Gioi)
    // TIEN_GIOI_ORDINAL removed - use canFly()
    // private static final int TIEN_GIOI_ORDINAL = CultivationStage.DE_TU.ordinal();

    public FlightListener(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Goi khi nguoi choi dot pha hoac login de cap nhat flight permission.
     */
    public void updateFlight(Player player, PlayerData data) {
        if (!player.isOnline()) return;
        boolean canFly = data.getStage().canFly();

        if (canFly) {
            player.setAllowFlight(true);
            double flySpeed = getFlySpeed(data);
            player.setFlySpeed((float) flySpeed);

            if (!player.isFlying()) {
                player.sendMessage(plugin.prefix()
                    + "§b✈ Bay đã mở khóa! §fCó thể bay tự do từ §fThánh Giới. (Space x2)");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
            }

            // Xoa Roi Cham vinh vien tu ban cu (neu con sot lai tren nguoi choi da nhan truoc day)
            player.removePotionEffect(PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft("slow_falling")));

            // Dao To: them aura dac biet
            // Chan Tien+ co aura (bay duoc la co aura)
            if (data.getStage().canFly()) {
                applyDaoToAura(player, data.getStage());
            }
        } else {
            // Chua len Thanh Gioi -> khong duoc bay
            if (player.getAllowFlight() && !player.isOp()) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;

        boolean canFly = data.getStage().canFly();
        if (!canFly) {
            event.setCancelled(true);
            player.sendMessage(plugin.prefix()
                + "§cCần cảnh giới §f Thánh Giới §c(Bán Thánh) trở lên để bay!");
            return;
        }

        // Hieu ung khi bat/tat bay
        if (event.isFlying()) {
            player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.8f);
        }
    }

    private double getFlySpeed(PlayerData data) {
        int ord = data.getStage().ordinal();
        if (ord < 11) return 0.10f;
        if (ord <= 14) return 0.10f + (ord-11) * 0.03f;  // Thanh Gioi
        if (ord <= 20) return 0.20f + (ord-15) * 0.03f;  // Tien (Chan-DaoTo)
        if (ord <= 24) return 0.40f + (ord-21) * 0.03f;  // Tien (Quan-De)
        if (ord <= 30) return 0.55f + (ord-25) * 0.05f;  // Thien
        return 1.0f;
    }

    private void applyDaoToAura(Player player, com.tutien.data.CultivationStage stage) {
        int ord = stage.ordinal();
        int regen = Math.min(Math.max(ord - 10, 0), 9);
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft("regeneration")),
            Integer.MAX_VALUE, regen, true, false, false));
        if (ord >= com.tutien.data.CultivationStage.CHAN_TIEN.ordinal()) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft("fire_resistance")),
                Integer.MAX_VALUE, 0, true, false, false));
        }
        if (ord >= com.tutien.data.CultivationStage.THIEN_DE.ordinal()) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft("water_breathing")),
                Integer.MAX_VALUE, 0, true, false, false));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft("night_vision")),
                Integer.MAX_VALUE, 0, true, false, false));
        }
        if (stage.isTopStage()) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft("strength")),
                Integer.MAX_VALUE, 1, true, false, false));
        }
    }
}
