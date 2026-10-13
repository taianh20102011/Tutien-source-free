package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PhaBaoEnhanceCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public PhaBaoEnhanceCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        // /cuonghoa dabaoho [so-luong] - admin phat Da Bao Ho de test/thuong.
        if (args.length >= 1 && (args[0].equalsIgnoreCase("dabaoho") || args[0].equalsIgnoreCase("stone"))) {
            if (!player.hasPermission("tutien.admin")) {
                player.sendMessage(plugin.prefix() + "§cBạn không có quyền dùng lệnh này.");
                return true;
            }
            int amount = 1;
            if (args.length >= 2) {
                try { amount = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignored) {}
            }
            var stone = plugin.getPhaBaoEnhanceManager().createProtectionStone();
            stone.setAmount(amount);
            player.getInventory().addItem(stone);
            player.sendMessage(plugin.prefix() + "§aĐã nhận §d" + amount + " Đá Bảo Hộ§a.");
            return true;
        }

        plugin.getPhaBaoEnhanceGUI().open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return List.of("dabaoho");
        return List.of();
    }
}
