package com.tutien.forge;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import com.tutien.phabao.PhaBaoManager;
import com.tutien.phabao.PhaBaoTier;
import com.tutien.phabao.PhaBaoType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * RenDucCommand — /renduc: Nang cap Phap Bao dang cam tren tay len tier cao hon.
 *
 * Truoc day /renduc bi wire nham vao AlchemyCommand (luyen dan), khien
 * bam /renduc chi mo GUI luyen dan thay vi ren duc that su.
 *
 * Co che:
 *   Dia Pham -> Ha Pham   : mien phi, luon thanh cong
 *   Ha Pham  -> Trung Pham: 300 LT   , 90% thanh cong
 *   Trung    -> Thuong    : 800 LT   , 75% thanh cong
 *   Thuong   -> De Pham   : 2000 LT  , 50% thanh cong
 *   De Pham  -> Tien Pham : 8000 LT  , 25% thanh cong
 *
 * That bai: mat LT da tra, Phap Bao GIU NGUYEN (khong bi pha huy).
 * Gia va ti le doc tu config.yml (economy.renduc-tier*-cost, economy.renduc-success-rate).
 */
public class RenDucCommand implements CommandExecutor {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    public RenDucCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_fd17b10807"));
            return true;
        }

        PhaBaoManager pbm = plugin.getPhaBaoManager();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand == null || !pbm.isPhaBao(hand)) {
            player.sendMessage(plugin.prefix() + "§cBạn phải cầm Pháp Bảo trên tay để rèn đúc!");
            player.sendMessage(plugin.prefix() + "§7Ví dụ: cầm Trấn Thiên Giáp, sau đó gõ §e/renduc");
            return true;
        }

        PhaBaoTier curTier = pbm.getTier(hand);
        PhaBaoType type = pbm.getType(hand);
        if (curTier == null || type == null) {
            player.sendMessage(plugin.prefix() + "§cKhông đọc được thông tin Pháp Bảo này!");
            return true;
        }

        if (curTier == PhaBaoTier.TIEN_PHAM) {
            player.sendMessage(plugin.prefix() + "§6✦ " + type.getDisplayName()
                + " §6đã đạt Tiên Phẩm - cấp cao nhất, không thể rèn đúc thêm!");
            return true;
        }

        PhaBaoTier nextTier = PhaBaoTier.values()[curTier.ordinal() + 1];

        long cost;
        double rate;
        switch (curTier) {
            case DIA_PHAM -> { cost = 0; rate = 1.0; }
            case HA_PHAM -> {
                cost = plugin.getConfig().getLong("economy.renduc-tier1-cost", 300);
                rate = plugin.getConfig().getDouble("economy.renduc-success-rate.ha-to-trung", 0.90);
            }
            case TRUNG_PHAM -> {
                cost = plugin.getConfig().getLong("economy.renduc-tier2-cost", 800);
                rate = plugin.getConfig().getDouble("economy.renduc-success-rate.trung-to-thuong", 0.75);
            }
            case THUONG_PHAM -> {
                cost = plugin.getConfig().getLong("economy.renduc-tier3-cost", 2000);
                rate = plugin.getConfig().getDouble("economy.renduc-success-rate.thuong-to-de", 0.50);
            }
            case DE_PHAM -> {
                cost = plugin.getConfig().getLong("economy.renduc-tier4-cost", 8000);
                rate = plugin.getConfig().getDouble("economy.renduc-success-rate.de-to-tien", 0.25);
            }
            default -> { cost = 0; rate = 0; }
        }

        PlayerData data = plugin.getDataManager().get(player);

        // Xem truoc (khong tham so) -> hien thong tin, khong tru tien
        if (args.length > 0 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("xem"))) {
            showInfo(player, type, curTier, nextTier, cost, rate, data.getTien());
            return true;
        }

        if (data.getTien() < cost) {
            player.sendMessage(plugin.prefix() + "§cKhông đủ " + plugin.getTienManager().tenTien() + "! Cần §e" + fmt(cost)
                + " §c(bạn có §e" + fmt(data.getTien()) + "§c)");
            return true;
        }

        // Tru Tien truoc (du thanh cong hay that bai deu mat phi ren)
        data.spendTien(cost);

        boolean success = random.nextDouble() < rate;

        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_a6cf4c1f04"));
        player.sendMessage("  §7" + type.getDisplayName());
        player.sendMessage("  " + curTier.getColor() + curTier.getDisplayName()
            + " §7→ " + nextTier.getColor() + nextTier.getDisplayName());
        player.sendMessage("  §7Tỉ lệ: §e" + (int) (rate * 100) + "%   §7Chi phí: §6" + fmt(cost) + " LT");

        if (success) {
            ItemStack upgraded = pbm.createPhaBao(type, nextTier);
            player.getInventory().setItemInMainHand(upgraded);

            player.sendMessage(plugin.msg("auto_f5693d8870"));
            player.sendMessage(plugin.msg("auto_546c3cc62b"));
            player.sendTitle(plugin.msg("auto_06fc031466"), nextTier.getColoredName(), 5, 50, 15);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.5f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                player.getLocation().add(0, 1, 0), 40, 0.5, 0.6, 0.5, 0.3);
            player.getWorld().spawnParticle(Particle.END_ROD,
                player.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
        } else {
            player.sendMessage(plugin.msg("auto_f60e315a45"));
            player.sendMessage(plugin.msg("auto_546c3cc62b"));
            player.sendTitle(plugin.msg("auto_ab79e24e64"), plugin.msg("auto_09ee58c2d8"), 5, 40, 10);

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.7f);
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0),
                25, 0.4, 0.4, 0.4, 0.1);
        }
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        return true;
    }

    private void showInfo(Player player, PhaBaoType type, PhaBaoTier cur, PhaBaoTier next,
                           long cost, double rate, long haveLT) {
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_66f9276e57"));
        player.sendMessage("  §7Pháp Bảo: §f" + type.getDisplayName());
        player.sendMessage("  §7Hiện tại: " + cur.getColoredName());
        player.sendMessage("  §7Nâng lên: " + next.getColoredName());
        player.sendMessage("  §7Chi phí:  §6" + fmt(cost) + " LT §7(bạn có §e" + fmt(haveLT) + " LT§7)");
        player.sendMessage("  §7Tỉ lệ:    §e" + (int) (rate * 100) + "%");
        player.sendMessage(plugin.msg("auto_3356d4ddb1"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
    }

    private String fmt(long n) {
        if (n >= 1_000_000_000L) return String.format("%.2fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L) return String.format("%.2fM", n / 1_000_000.0);
        if (n >= 1_000L) return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}
