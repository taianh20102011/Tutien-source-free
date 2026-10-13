package com.tutien.bag;

import com.tutien.TuTienPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TuTienBagCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public TuTienBagCommand(TuTienPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_3da9667838")); return true;
        }
        plugin.getTuTienBag().openBag(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        return Collections.emptyList();
    }
}
