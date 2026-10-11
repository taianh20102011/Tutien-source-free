package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PhaBaoFusionCommand implements CommandExecutor {

    private final TuTienPlugin plugin;

    public PhaBaoFusionCommand(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này.");
            return true;
        }
        plugin.getPhaBaoFusionGUI().open(player);
        return true;
    }
}
