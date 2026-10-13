package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI Khi Linh - "click-to-craft" giong PhaBaoEnhanceGUI/PhaBaoLinhThucGUI:
 * Phap Bao + do rac deu nam san tren 2 tay nguoi choi (1 tay cam Phap Bao,
 * tay kia cam do rac), GUI chi hien thi + nut "Cho Ăn" - KHONG dat item vao
 * GUI nen khong co rui ro dup/mat item.
 */
public class PhaBaoKhiLinhGUI implements Listener {

    private final TuTienPlugin plugin;
    public static final String TITLE = "§2✦ Khí Linh";

    private static final int SLOT_PHABAO = 11;
    private static final int SLOT_JUNK   = 15;
    private static final int SLOT_INFO   = 22;
    private static final int SLOT_FEED   = 31;
    private static final int SLOT_CLOSE  = 40;
    private static final int SIZE        = 45;

    public PhaBaoKhiLinhGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    /** true = pha bao o tay chinh, junk o tay phu. false = nguoc lai. null = khong tim thay pha bao. */
    private Boolean phaBaoInMainHand(Player player) {
        PhaBaoManager pbm = plugin.getPhaBaoManager();
        if (pbm.isPhaBao(player.getInventory().getItemInMainHand())) return true;
        if (pbm.isPhaBao(player.getInventory().getItemInOffHand())) return false;
        return null;
    }

    public void open(Player player) {
        Boolean mainHand = phaBaoInMainHand(player);
        if (mainHand == null) {
            player.sendMessage(plugin.prefix() + "§cBạn cần cầm 1 Pháp Bảo ở 1 trong 2 tay để cho Khí Linh ăn.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, SIZE,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));
        ItemStack bg = glass(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < SIZE; i++) inv.setItem(i, bg);

        refresh(player, inv, mainHand);

        inv.setItem(SLOT_FEED, btn(Material.HOPPER, "§a✔ Cho Ăn",
                "§7Tiêu hao toàn bộ vật phẩm ở ô bên phải", "§7để cộng điểm Linh Tính cho Pháp Bảo."));
        inv.setItem(SLOT_CLOSE, btn(Material.BARRIER, "§c✘ Đóng", ""));
        player.openInventory(inv);
    }

    private void refresh(Player player, Inventory inv, boolean mainHand) {
        ItemStack pb   = mainHand ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        ItemStack junk = mainHand ? player.getInventory().getItemInOffHand()  : player.getInventory().getItemInMainHand();

        inv.setItem(SLOT_PHABAO, pb.clone());
        inv.setItem(SLOT_JUNK, (junk == null || junk.getType() == Material.AIR) ? air() : junk.clone());

        PhaBaoKhiLinhManager km = plugin.getPhaBaoKhiLinhManager();
        int sp = km.getSpirit(pb);
        int tier = km.getTierUnlocked(pb);
        boolean isJunk = junk != null && km.getJunkMaterials().contains(junk.getType());
        int gain = isJunk ? junk.getAmount() * km.getPointsPerItem() : 0;

        inv.setItem(SLOT_INFO, btn(Material.BOOK, "§2✦ Linh Tính: §a" + sp + " §7(Bậc §2" + tier + "§7/3)",
                "§7Bậc 1 (" + km.getTier1() + "đ): §fhào quang hạt",
                "§7Bậc 2 (" + km.getTier2() + "đ): §ftự động nhặt đồ",
                "§7Bậc 3 (" + km.getTier3() + "đ): §ftự hỗ trợ đánh quái",
                "",
                isJunk ? "§eSẽ cộng: §a+" + gain + " điểm" : "§cÔ bên tay kia không phải đồ rác hợp lệ"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player) || !isOur(e.getView())) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();

        if (slot == SLOT_CLOSE) { player.closeInventory(); return; }
        if (slot != SLOT_FEED) return;

        Boolean mainHand = phaBaoInMainHand(player);
        if (mainHand == null) {
            player.sendMessage(plugin.prefix() + "§cBạn không còn cầm Pháp Bảo nào nữa.");
            player.closeInventory();
            return;
        }

        ItemStack pb   = mainHand ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        ItemStack junk = mainHand ? player.getInventory().getItemInOffHand()  : player.getInventory().getItemInMainHand();

        PhaBaoKhiLinhManager.FeedResult result = plugin.getPhaBaoKhiLinhManager().feed(pb, junk);
        switch (result) {
            case OK -> {
                int gained = junk.getAmount() * plugin.getPhaBaoKhiLinhManager().getPointsPerItem();
                if (mainHand) player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                player.sendMessage(plugin.prefix() + "§2✦ Khí Linh đã hấp thụ! §a+" + gained + " điểm Linh Tính.");
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1.2f);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
            }
            case NOT_JUNK -> player.sendMessage(plugin.prefix() + "§cVật phẩm này không phải \"đồ rác\" hợp lệ.");
            case EMPTY -> player.sendMessage(plugin.prefix() + "§cTay kia đang trống - cần cầm đồ rác để cho ăn.");
            case NOT_PHABAO -> player.sendMessage(plugin.prefix() + "§cVật phẩm không phải Pháp Bảo hợp lệ.");
        }
        refresh(player, e.getInventory(), mainHand);
    }

    private boolean isOur(InventoryView v) {
        try {
            return LegacyComponentSerializer.legacySection().serialize(v.title()).equals(TITLE);
        } catch (Exception ex) {
            return false;
        }
    }

    private ItemStack btn(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack air() {
        return new ItemStack(Material.AIR);
    }
}
