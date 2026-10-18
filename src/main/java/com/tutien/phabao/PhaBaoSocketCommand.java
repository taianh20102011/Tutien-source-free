package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PhaBaoSocketCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public PhaBaoSocketCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        // /khamnam ngoc <hoa|bang|loi> [so-luong] - admin phat Ngoc de test/thuong.
        if (args.length >= 2 && (args[0].equalsIgnoreCase("ngoc") || args[0].equalsIgnoreCase("gem"))) {
            if (!player.hasPermission("tutien.admin")) {
                player.sendMessage(plugin.prefix() + "§cBạn không có quyền dùng lệnh này.");
                return true;
            }
            GemType type = switch (args[1].toLowerCase()) {
                case "hoa", "fire" -> GemType.HOA;
                case "bang", "ice" -> GemType.BANG;
                case "loi", "lightning" -> GemType.LOI;
                default -> null;
            };
            if (type == null) {
                player.sendMessage(plugin.prefix() + "§cLoại Ngọc không hợp lệ. Dùng: hoa | bang | loi");
                return true;
            }
            int amount = 1;
            if (args.length >= 3) {
                try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
            }
            ItemStack gem = plugin.getPhaBaoSocketManager().createGem(type);
            gem.setAmount(amount);
            player.getInventory().addItem(gem);
            player.sendMessage(plugin.prefix() + "§aĐã nhận §f" + amount + " " + type.getColor() + type.getDisplayName() + "§a.");
            return true;
        }

        plugin.getPhaBaoSocketGUI().open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return List.of("ngoc");
        if (args.length == 2 && args[0].equalsIgnoreCase("ngoc")) return List.of("hoa", "bang", "loi");
        return List.of();
    }
}
