package com.tutien.lingcan;

import com.tutien.TuTienPlugin;
import com.tutien.effects.EffectEngine;
import com.tutien.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class LinhCanManager {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    public LinhCanManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  PHAN CONG NGAU NHIEN
    // ========================

    /**
     * Phan cong linh can ngau nhien cho nguoi choi lan dau.
     * Xac suat: Thien (3%), Nhi (8%), Chan (22%), Tam (40%), Nguy (27%)
     * Bien di: ~4% tong cong cho moi loai bien di.
     */
    public List<LinhCanElement> rollLinhCan() {
        // Quyet dinh so luong linh can
        int count = rollRootCount();
        List<LinhCanElement> result = new ArrayList<>();

        // Co xac suat bien di
        if (random.nextDouble() < 0.06) { // 6% co it nhat 1 bien di
            // Bien di thay the 1 loai binh thuong
            LinhCanElement bienDi = rollBienDi();
            result.add(bienDi);
            count = Math.max(1, count - 1);
        }

        // Chon cac loai binh thuong (khong trung nhau)
        List<LinhCanElement> normal = getNormalElements();
        Collections.shuffle(normal, random);
        for (int i = 0; i < count && i < normal.size(); i++) {
            if (!result.contains(normal.get(i))) result.add(normal.get(i));
        }

        return result;
    }

    private int rollRootCount() {
        double r = random.nextDouble();
        if (r < 0.03)  return 1; // 3%  Thien Linh Can
        if (r < 0.11)  return 2; // 8%  Nhi Linh Can
        if (r < 0.33)  return 3; // 22% Chan Linh Can
        if (r < 0.73)  return 4; // 40% Tam Linh Can
        return 5;                 // 27% Nguy Linh Can
    }

    private LinhCanElement rollBienDi() {
        LinhCanElement[] bienDiArr = {
            LinhCanElement.LOI, LinhCanElement.BANG,
            LinhCanElement.PHONG, LinhCanElement.AM, LinhCanElement.QUANG
        };
        // Weights: Loi 35%, Bang 25%, Phong 25%, Am 8%, Quang 7%
        double r = random.nextDouble();
        if (r < 0.35) return LinhCanElement.LOI;
        if (r < 0.60) return LinhCanElement.BANG;
        if (r < 0.85) return LinhCanElement.PHONG;
        if (r < 0.93) return LinhCanElement.AM;
        return LinhCanElement.QUANG;
    }

    private List<LinhCanElement> getNormalElements() {
        return new ArrayList<>(Arrays.asList(
            LinhCanElement.KIM, LinhCanElement.MOC,
            LinhCanElement.THUY, LinhCanElement.HOA, LinhCanElement.THO
        ));
    }

    // ========================
    //  TINH BONUS
    // ========================

    /** Nhan vao LK khi nhan duoc (tu linh can) */
    public double getLkMultiplier(PlayerData data) {
        if (data.getLinhCan().isEmpty()) return 1.0;
        LinhCanGrade grade = LinhCanGrade.fromCount(data.getLinhCan().size());
        double gradeMulti = grade.getLkMultiplier();
        double elemMulti  = LinhCanGrade.combinedLkBonus(data.getLinhCan());
        return gradeMulti * elemMulti;
    }

    /** He so do kho dot pha (< 1 = de hon) */
    public double getBreakthroughDiffMult(PlayerData data) {
        if (data.getLinhCan().isEmpty()) return 1.0;
        LinhCanGrade grade = LinhCanGrade.fromCount(data.getLinhCan().size());
        double gradeDiff  = grade.getBreakDifficulty();
        double elemEase   = LinhCanGrade.combinedBreakEase(data.getLinhCan());
        return gradeDiff * elemEase;
    }

    public double getAtkBonus(PlayerData data) {
        if (data.getLinhCan().isEmpty()) return 1.0;
        return LinhCanGrade.combinedAtkBonus(data.getLinhCan());
    }

    public double getDefBonus(PlayerData data) {
        if (data.getLinhCan().isEmpty()) return 1.0;
        return LinhCanGrade.combinedDefBonus(data.getLinhCan());
    }

    // ========================
    //  HIEN THI CHO NGUOI CHOI
    // ========================

    public void revealLinhCan(Player player, PlayerData data) {
        List<LinhCanElement> roots = data.getLinhCan();
        LinhCanGrade grade = LinhCanGrade.fromCount(roots.size());
        boolean hasBienDi = roots.stream().anyMatch(LinhCanElement::isBienDi);

        plugin.getEffectEngine().linhCanReveal(player, roots);
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_da47a9f770"));
        player.sendMessage(plugin.msg("auto_1c0bd996eb"));
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        player.sendMessage("  §7Cấp bậc: " + grade.getColoredName());
        player.sendMessage("  §7Số lượng: §e" + roots.size() + " §7loại linh căn");
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_29eeaf0699"));
        for (LinhCanElement el : roots) {
            String tag = el.isBienDi() ? " §5[BIẾN DỊ]" : "";
            player.sendMessage("   " + el.getIcon() + " " + el.getColoredName() + tag
                + " §8- §7" + el.getDescription());
        }
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage("  " + grade.getDescription());
        player.sendMessage("  §8\"" + grade.getFlavorText() + "§8\"");
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        player.sendMessage("  §7Tốc độ tu luyện: §a×" + String.format("%.2f", getLkMultiplier(data)));
        player.sendMessage("  §7Độ khó đột phá: §e×" + String.format("%.2f", getBreakthroughDiffMult(data)));
        player.sendMessage("  §7ATK bonus: §c×" + String.format("%.2f", getAtkBonus(data)));
        player.sendMessage("  §7DEF bonus: §b×" + String.format("%.2f", getDefBonus(data)));
        if (hasBienDi) {
            player.sendMessage(plugin.msg("auto_bab4cf3001"));
        }
        player.sendMessage(plugin.msg("auto_5a07764d64"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));

        // Hieu ung gio phut kiem dinh
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);
        if (grade == LinhCanGrade.THIEN_LINH_CAN || hasBienDi) {
            player.sendTitle(grade.getColoredName(), plugin.msg("auto_8edb7e957a"), 10, 80, 20);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
            String bc = plugin.prefix() + "§6✦ " + player.getName()
                + " §elà " + grade.getColoredName() + "§e"
                + (hasBienDi ? " §5[BIẾN DỊ]" : "") + "§e! Tiến độ phi thường!";
            Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(bc));
        }
        spawnRevealParticles(player, grade);
    }

    private void spawnRevealParticles(Player player, LinhCanGrade grade) {
        Particle p = switch (grade) {
            case THIEN_LINH_CAN -> Particle.ENCHANTED_HIT;
            case NHI_LINH_CAN   -> Particle.CRIT;
            case CHAN_LINH_CAN  -> Particle.ENCHANTED_HIT;
            default             -> Particle.WITCH;
        };
        player.getWorld().spawnParticle(p, player.getLocation().add(0, 1, 0),
            60, 0.8, 0.8, 0.8, 0.1);
    }
}
