package com.tutien.commands;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import com.tutien.herb.HerbManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Player-facing shop for Linh Chung.
 *
 * Security rule:
 * BUY is always checked server-side against PlayerData.getStage().ordinal()
 * and HerbType.minStageOrd(). DeluxeMenus cannot bypass this check.
 *
 * SELL is allowed independently of cultivation stage, because the player
 * already owns the Linh Chung. This can be changed later if desired.
 */
public final class LinhChungShopCommand implements CommandExecutor {

    private final TuTienPlugin plugin;

    public LinhChungShopCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLệnh này chỉ dùng trong game.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.prefix() + "§7Dùng: §e/linhchungshop <buy|sell> <herbId> [số lượng]");
            return true;
        }

        String action = args[0].toLowerCase();
        String herbId = args[1];

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                player.sendMessage(plugin.prefix() + "§cSố lượng không hợp lệ.");
                return true;
            }
        }

        if (amount < 1 || amount > 2304) {
            player.sendMessage(plugin.prefix() + "§cSố lượng phải từ §e1 §cđến §e2304.");
            return true;
        }

        HerbManager herbs = plugin.getHerbManager();
        Optional<HerbManager.HerbType> found = herbs.getHerbType(herbId);
        if (found.isEmpty()) {
            player.sendMessage(plugin.prefix() + "§cKhông tìm thấy Linh Chủng: §e" + herbId);
            return true;
        }

        HerbManager.HerbType herb = found.get();

        if (!herb.plantable() || herb.seedBlock() == null) {
            player.sendMessage(plugin.prefix() + "§cThảo Dược này chưa có Linh Chủng gieo trồng.");
            return true;
        }

        if ("buy".equals(action)) {
            return buy(player, herb, amount);
        }

        if ("sell".equals(action)) {
            return sell(player, herb, amount);
        }

        player.sendMessage(plugin.prefix() + "§7Dùng: §e/linhchungshop <buy|sell> <herbId> [số lượng]");
        return true;
    }

    private boolean buy(Player player, HerbManager.HerbType herb, int amount) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass() || data.getStage() == null) {
            player.sendMessage(plugin.prefix() + "§cBạn chưa có cảnh giới tu luyện.");
            return true;
        }

        int current = data.getStage().ordinal();
        int required = herb.minStageOrd();

        // HARD SERVER-SIDE LOCK. This is the important part:
        // no DeluxeMenus command, console command or client packet can bypass it.
        if (current < required) {
            String requiredName = stageName(required);
            player.sendMessage(plugin.prefix()
                    + "§cCảnh giới chưa đủ để mua Linh Chủng này.");
            player.sendMessage("§7Yêu cầu: §e" + requiredName
                    + " §8| §7Hiện tại: §e" + data.getStage().getDisplayName());
            return true;
        }

        long unitPrice = herb.ltValue();
        if (unitPrice <= 0) {
            player.sendMessage(plugin.prefix() + "§cLinh Chủng này chưa được cấu hình giá.");
            return true;
        }

        long total;
        try {
            total = Math.multiplyExact(unitPrice, amount);
        } catch (ArithmeticException ex) {
            player.sendMessage(plugin.prefix() + "§cTổng giá quá lớn.");
            return true;
        }

        if (!plugin.getTienManager().isAvailable()) {
            player.sendMessage(plugin.prefix() + "§cHệ thống Linh Thạch/Vault chưa sẵn sàng.");
            return true;
        }

        if (plugin.getTienManager().getBalance(player) < total) {
            player.sendMessage(plugin.prefix()
                    + "§cKhông đủ Linh Thạch. §7Cần §e"
                    + plugin.getTienManager().fmt(total)
                    + "§7.");
            return true;
        }

        ItemStack seed = plugin.getHerbManager().createSeed(herb, amount);
        if (seed == null) {
            player.sendMessage(plugin.prefix() + "§cKhông thể tạo Linh Chủng.");
            return true;
        }

        // Withdraw only after all checks passed.
        if (!plugin.getTienManager().spend(player, total)) {
            player.sendMessage(plugin.prefix() + "§cGiao dịch thất bại, Linh Thạch chưa bị trừ.");
            return true;
        }

        // Snapshot inventory so a partial add can be rolled back safely.
        ItemStack[] before = new ItemStack[player.getInventory().getSize()];
        for (int i = 0; i < before.length; i++) {
            ItemStack existing = player.getInventory().getItem(i);
            before[i] = existing == null ? null : existing.clone();
        }

        var leftovers = player.getInventory().addItem(seed);
        if (!leftovers.isEmpty()) {
            player.getInventory().setContents(before);
            plugin.getTienManager().give(player, total);
            player.sendMessage(plugin.prefix() + "§eTúi đồ không đủ chỗ. Đã hoàn tiền.");
            return true;
        }

        player.sendMessage(plugin.prefix()
                + "§aMua thành công §e" + herb.name()
                + " §a×" + amount + " §7| Trừ §6"
                + plugin.getTienManager().fmt(total) + " Linh Thạch§7.");
        return true;
    }

    private boolean sell(Player player, HerbManager.HerbType herb, int amount) {
        int owned = herbsCount(player, herb.id());
        if (owned < amount) {
            player.sendMessage(plugin.prefix()
                    + "§cBạn chỉ có §e" + owned + " §cLinh Chủng loại này.");
            return true;
        }

        long unitPrice = herb.ltValue();
        long total;
        try {
            total = Math.multiplyExact(unitPrice, amount);
        } catch (ArithmeticException ex) {
            player.sendMessage(plugin.prefix() + "§cTổng giá quá lớn.");
            return true;
        }

        if (!removeSeeds(player, herb.id(), amount)) {
            player.sendMessage(plugin.prefix() + "§cKhông thể lấy Linh Chủng khỏi túi đồ.");
            return true;
        }

        plugin.getTienManager().give(player, total);
        player.sendMessage(plugin.prefix()
                + "§aBán thành công §e" + herb.name()
                + " §a×" + amount + " §7| Nhận §6"
                + plugin.getTienManager().fmt(total) + " Linh Thạch§7.");
        return true;
    }

    private int herbsCount(Player player, String herbId) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !plugin.getHerbManager().isSeed(item)) continue;
            if (herbId.equalsIgnoreCase(plugin.getHerbManager().getSeedHerbId(item))) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private boolean removeSeeds(Player player, String herbId, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || !plugin.getHerbManager().isSeed(item)) continue;
            if (!herbId.equalsIgnoreCase(plugin.getHerbManager().getSeedHerbId(item))) continue;

            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            if (item.getAmount() <= 0) player.getInventory().setItem(i, null);
            remaining -= take;

            if (remaining <= 0) return true;
        }
        return false;
    }

    private String stageName(int ordinal) {
        var stages = com.tutien.data.CultivationStage.values();
        if (ordinal >= 0 && ordinal < stages.length) {
            return stages[ordinal].getDisplayName();
        }
        return "Cảnh giới " + ordinal;
    }
}