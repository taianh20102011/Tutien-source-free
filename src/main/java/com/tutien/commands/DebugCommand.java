package com.tutien.commands;

import com.tutien.TuTienPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class DebugCommand implements CommandExecutor, TabCompleter {
    private final TuTienPlugin plugin;

    public DebugCommand(TuTienPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tutien.admin")) {
            sender.sendMessage(plugin.prefix() + "§cBạn không có quyền dùng lệnh này.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(plugin.prefix() + "§7Debug: " + (plugin.isDebugEnabled() ? "§aBẬT" : "§cTẮT") + "§7. Dùng /debug <on|off|toggle>.");
            return true;
        }
        String action = args[0].toLowerCase(java.util.Locale.ROOT);
        if (!List.of("on", "bat", "true", "1", "off", "tat", "false", "0", "toggle", "doi").contains(action)) {
            sender.sendMessage(plugin.prefix() + "§cDùng: /debug <on|off|toggle|status>");
            return true;
        }
        boolean next = switch (action) {
            case "on", "bat", "true", "1" -> true;
            case "off", "tat", "false", "0" -> false;
            default -> !plugin.isDebugEnabled();
        };
        plugin.setDebugEnabled(next);
        sender.sendMessage(plugin.prefix() + "§aDebug đã " + (next ? "bật" : "tắt") + " và được lưu vào config.yml.");
        plugin.debug("Debug mode đã được bật bởi " + sender.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission("tutien.admin")) return List.of();
        String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
        return List.of("on", "off", "toggle", "status").stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
