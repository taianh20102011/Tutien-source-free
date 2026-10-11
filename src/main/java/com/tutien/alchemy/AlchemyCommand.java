package com.tutien.alchemy;

import com.tutien.TuTienPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class AlchemyCommand implements CommandExecutor, TabCompleter {
    private final TuTienPlugin plugin;
    public AlchemyCommand(TuTienPlugin p) { this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_7414355e90"));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            plugin.getAlchemyGUI().open(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("danh_sach")
                || args[0].equalsIgnoreCase("danhsach")) {
            plugin.getAlchemyManager().showRecipeList(player, 0);
            return true;
        }
        String query = String.join(" ", args).trim();
        AlchemyManager.DanDuoc recipe = findRecipe(query);
        if (recipe == null) {
            player.sendMessage(plugin.prefix() + "§cKhông tìm thấy đan dược §e" + query + "§c.");
            player.sendMessage(plugin.prefix() + "§7Dùng §e/luyen §7để xem 32 đan dược hoặc §e/luyen danh_sach§7.");
            return true;
        }
        plugin.getAlchemyManager().luyenDan(player, recipe.id);
        return true;
    }

    private AlchemyManager.DanDuoc findRecipe(String query) {
        String q = normalize(query);
        for (AlchemyManager.DanDuoc r : plugin.getAlchemyManager().getAllRecipes().values()) {
            if (normalize(r.id).equals(q) || normalize(r.displayName).equals(q)) return r;
        }
        return null;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return ChatColor.stripColor(s).toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace('-', ' ').trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String lbl, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>(Arrays.asList("gui", "list", "danh_sach"));
            out.addAll(plugin.getAlchemyManager().getAllRecipes().keySet());
            return out;
        }
        return Collections.emptyList();
    }
}
