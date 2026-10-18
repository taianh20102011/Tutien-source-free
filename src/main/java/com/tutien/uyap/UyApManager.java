package com.tutien.uyap;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * UyApManager - Hệ thống Uy Áp Cảnh Giới.
 */
public class UyApManager implements Listener {

    private final TuTienPlugin plugin;

    // Track người chơi đang chịu uy áp: UUID -> ordinal của người cấp uy áp
    private final Map<UUID, Integer> underPressure = new HashMap<>();

    // Khớp với UyApSystem (CHECK_RADIUS=10, áp effect từ gap>=2) để 2 hệ thống
    // luôn kích hoạt cùng lúc — tránh trường hợp có debuff (potion effect từ
    // UyApSystem) nhưng không có thông báo/giảm LK (từ UyApManager) hoặc ngược lại.
    private static final double RADIUS = 10.0;
    private static final int MIN_DIFF  = 2;
    private static final String KEY_UYAP_ENABLED = "uyap_enabled";

    public UyApManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startPressureLoop();
    }

    private void startPressureLoop() {
        new BukkitRunnable() {
            @Override public void run() {
                Map<UUID, Integer> newPressure = new HashMap<>();

                for (Player p : Bukkit.getOnlinePlayers()) {
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null || !data.hasClass()) continue;

                    // Nếu player tắt uy áp: không nhận pressure, và nếu đang chịu thì kết thúc ngay
                    if (!data.getExtraBool(KEY_UYAP_ENABLED, true)) {
                        if (underPressure.containsKey(p.getUniqueId())) {
                            onPressureEnd(p);
                            underPressure.remove(p.getUniqueId());
                        }
                        continue;
                    }

                    int myOrd = data.getStage().ordinal();

                    // Tìm người cao nhất gần đây
                    int maxNearbyOrd = myOrd;
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (other == p) continue;
                        if (p.getWorld() != other.getWorld()) continue;
                        if (p.getLocation().distance(other.getLocation()) > RADIUS) continue;
                        PlayerData od = plugin.getDataManager().get(other);
                        if (od == null || !od.hasClass()) continue;

                        // Nếu người phát uy áp tắt Uy Áp thì cũng không gây áp lực lên người khác (tùy chọn)
                        if (!od.getExtraBool(KEY_UYAP_ENABLED, true)) continue;

                        int theirOrd = od.getStage().ordinal();
                        if (theirOrd > maxNearbyOrd) maxNearbyOrd = theirOrd;
                    }

                    int diff = maxNearbyOrd - myOrd;
                    if (diff >= MIN_DIFF) {
                        newPressure.put(p.getUniqueId(), maxNearbyOrd);
                    }
                }

                // Notify thay đổi
                for (Player p : Bukkit.getOnlinePlayers()) {
                    UUID uid = p.getUniqueId();
                    boolean hadPressure  = underPressure.containsKey(uid);
                    boolean hasPressure  = newPressure.containsKey(uid);

                    if (!hadPressure && hasPressure) {
                        onPressureStart(p, newPressure.get(uid));
                    } else if (hadPressure && !hasPressure) {
                        onPressureEnd(p);
                    }
                }

                underPressure.clear();
                underPressure.putAll(newPressure);
            }
        }.runTaskTimer(plugin, 60L, 80L);
    }

    private void onPressureStart(Player p, int presserOrd) {
        p.sendMessage(plugin.prefix()
                + "§8⚠ §7Bạn cảm nhận §cuy áp cảnh giới§7 từ người cao cấp hơn gần đây!");
        p.sendMessage(plugin.prefix()
                + "§7→ LK tăng §c-20%§7, tốc độ §c-10%§7 nhưng §aĐột Phá dễ hơn +5%§7.");

        Location loc = p.getLocation().clone().add(0, 1, 0);
        p.getWorld().spawnParticle(Particle.DUST, loc, 30, 0.5,1,0.5, 0,
                new Particle.DustOptions(Color.fromRGB(180,0,0), 1.5f));
        p.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 0.5f);
    }

    private void onPressureEnd(Player p) {
        p.sendMessage(plugin.prefix() + "§a✦ Không còn chịu uy áp cảnh giới.");
    }

    public boolean isUnderPressure(Player p) {
        PlayerData data = plugin.getDataManager().get(p);
        if (data != null && !data.getExtraBool(KEY_UYAP_ENABLED, true)) {
            return false; // Nếu đã tắt bằng lệnh /uyap off thì trả về false
        }
        return underPressure.containsKey(p.getUniqueId());
    }

    public double getLKMultiplier(Player p) {
        if (!isUnderPressure(p)) return 1.0;
        PlayerData data = plugin.getDataManager().get(p);
        if (data == null) return 1.0;
        int diff = underPressure.get(p.getUniqueId()) - data.getStage().ordinal();
        double reduction = Math.min(0.5, diff * 0.03);
        return 1.0 - reduction;
    }

    public double getBreakthroughBonus(Player p) {
        return isUnderPressure(p) ? 0.05 : 0.0;
    }
}