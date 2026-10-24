package com.tutien.gui;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PetCombatMode;
import com.tutien.data.PetTierConfig;
import com.tutien.data.PlayerData;
import com.tutien.sword.PetSummon;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * TuTienPetGUI - Giao diện Quản Lý & Bồi Dưỡng Linh Thú Tu Tiên
 * 
 * Tính năng chính:
 * - Triệu hồi / Thu hồi Linh Thú chỉ với 1 click.
 * - Chuyển đổi 3 chế độ tác chiến: Cuồng Bạo (⚔), Hộ Thể (🛡), Đồng Hành (🕊).
 * - Bồi dưỡng tăng cấp Linh Thú bằng Linh Thảo hoặc Linh Khí / Tu Vi.
 * - Danh mục chọn lựa Linh Thú từ các Cảnh Giới đã mở khóa (tự do chuyển đổi).
 * - Thử nghiệm tuyệt kỹ Linh Thú trực tiếp trong game.
 */
public final class TuTienPetGUI implements Listener {

    private final TuTienPlugin plugin;

    public TuTienPetGUI(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public static final class PetHolder implements InventoryHolder {
        private final UUID viewer;
        private final int page;
        private final Map<Integer, Runnable> actions = new HashMap<>();
        private Inventory inventory;

        public PetHolder(UUID viewer, int page) {
            this.viewer = viewer;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cCần chọn môn phái trước khi sử dụng Linh Thú! (/tutien chon)");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        PetSummon summoner = plugin.getPetSummon();
        if (summoner == null) {
            player.sendMessage(plugin.prefix() + "§cHệ thống Linh Thú chưa sẵn sàng!");
            return;
        }

        PetHolder holder = new PetHolder(player.getUniqueId(), page);
        String title = "§8✦ §5§lLinh Thú Tu Tiên §8✦";
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inv;

        // 1. Viền trang trí
        ItemStack borderPurple = makeItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        ItemStack borderMagenta = makeItem(Material.MAGENTA_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, (i % 2 == 0) ? borderPurple : borderMagenta);
            }
        }

        // Lấy thông tin Pet hiện tại
        PetTierConfig activeTier = summoner.getActiveTier(player);
        boolean isActive = summoner.hasPet(player);
        PetCombatMode mode = summoner.getCombatMode(player);
        int petLevel = summoner.getPetLevel(player);
        long petExp = summoner.getPetExp(player);
        long maxExp = summoner.getMaxExpForLevel(petLevel);
        double levelBonus = 1.0 + (petLevel - 1) * 0.08;

        // 2. Slot 4: Thông tin Tu Sĩ
        inv.setItem(4, makeItem(Material.PLAYER_HEAD,
                "§6✦ §eChủ Nhân: §f" + player.getName(),
                List.of(
                        "§7Cảnh giới: " + data.getStage().getColor() + data.getStage().getDisplayName() + " §7(Tầng " + data.getRank() + ")",
                        "§7Linh Khí hiện tại: §a" + String.format("%,d", data.getLinhKhi()),
                        "§7Môn phái: §e" + data.getClassType().getDisplayName(),
                        "",
                        "§7Linh Thú trung tâm liên kết với nguyên thần của đạo hữu."
                )
        ));

        // 3. Slot 13: Thẻ Thông Tin Linh Thú Trung Tâm
        Material petMaterial = getDisplayMaterial(activeTier);
        double totalAtk = activeTier.attackDamage() * levelBonus * data.calculateDamageMultiplier();
        double totalHeal = activeTier.defendHealPercent() * levelBonus * 100.0;

        inv.setItem(13, makeItem(petMaterial,
                "§d✦ §f" + activeTier.displayName() + " §e[Cấp " + petLevel + "]",
                List.of(
                        "§7Trạng thái: " + (isActive ? "§a✔ Đang giáng lâm chiến đấu" : "§7✖ Đang thu hồi trong đan điền"),
                        "§7Chế độ: " + mode.getIcon() + " " + mode.getDisplayName(),
                        "§8--------------------------------",
                        "§f⚔ Sát thương: §c" + String.format("%.1f", totalAtk) + " §7(Cơ bản: " + activeTier.attackDamage() + ")",
                        "§f🛡 Trị liệu: §a" + String.format("%.1f%%", totalHeal) + " HP §7khi bị đánh",
                        "§f✨ Hiệu ứng: " + (activeTier.regenAmplifier() >= 0 ? "§aRegen +" + (activeTier.regenAmplifier() + 1) + " " : "")
                                + (activeTier.luckAmplifier() >= 0 ? "§eMay Mắn +" + (activeTier.luckAmplifier() + 1) : ""),
                        "§f🕊 Khả năng bay: " + (activeTier.flies() ? "§bCó (Phi hành)" : "§7Không (Mặt đất)"),
                        "§8--------------------------------",
                        "§f⭐ Kinh nghiệm: §e" + petExp + " §7/ §e" + maxExp + " §8(" + (int) ((petExp * 100) / Math.max(1, maxExp)) + "%)",
                        getExpBar(petExp, maxExp),
                        "",
                        "§e✦ Bồi dưỡng bằng Linh Thảo hoặc Tu Vi bên dưới để tăng cấp!"
                )
        ));

        // 4. Slot 11: Nút Triệu Hồi / Thu Hồi
        if (isActive) {
            inv.setItem(11, makeItem(Material.RED_DYE,
                    "§c✦ Thu Hồi Linh Thú",
                    List.of(
                            "§7Linh Thú hiện đang xuất chiến cùng người.",
                            "",
                            "§e▶ Click để thu hồi về đan điền nghỉ ngơi."
                    )
            ));
        } else {
            inv.setItem(11, makeItem(Material.LIME_DYE,
                    "§a✦ Triệu Hồi Linh Thú",
                    List.of(
                            "§7Linh Thú đang ẩn mình trong đan điền.",
                            "",
                            "§e▶ Click để triệu hồi Linh Thú giáng lâm!"
                    )
            ));
        }
        holder.actions.put(11, () -> {
            summoner.togglePet(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            open(player, page);
        });

        // 5. Slot 15: Nút Chuyển Đổi Chế Độ Chiến Đấu
        inv.setItem(15, makeItem(Material.NETHER_STAR,
                "§6✦ Chế Độ Chiến Đấu: " + mode.getIcon() + " " + mode.getDisplayName(),
                List.of(
                        "§7Mô tả: " + mode.getDescription(),
                        "",
                        "§8[Nhấn để chuyển đổi]:",
                        (mode == PetCombatMode.AGGRESSIVE ? "§a● " : "§8○ ") + "§c⚔ Cuồng Bạo §7(Tự động phản công mục tiêu)",
                        (mode == PetCombatMode.GUARDIAN ? "§a● " : "§8○ ") + "§b🛡 Hộ Thể §7(Hồi máu & tạo khiên kháng đòn)",
                        (mode == PetCombatMode.PASSIVE ? "§a● " : "§8○ ") + "§e🕊 Đồng Hành §7(Tỏa tiên khí, không tham chiến)",
                        "",
                        "§e▶ Click để chuyển chế độ kế tiếp."
                )
        ));
        holder.actions.put(15, () -> {
            PetCombatMode next = mode.next();
            summoner.setCombatMode(player, next);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 0.8f, 1.3f);
            player.sendMessage(plugin.prefix() + "§7Chuyển chế độ Linh Thú sang: " + next.getIcon() + " " + next.getDisplayName());
            open(player, page);
        });

        // 6. Slot 29: Nút Bồi Dưỡng Bằng Linh Thảo (Feed Herb)
        inv.setItem(29, makeItem(Material.FERN,
                "§a✦ Bồi Dưỡng Bằng Linh Thảo",
                List.of(
                        "§7Cho Linh Thú hấp thu dược tính từ Tiên Thảo.",
                        "§7Tiêu hao: §f1 Linh Thảo §7ngẫu nhiên trong túi.",
                        "§7Nhận: §e+100 EXP §7Linh Thú.",
                        "",
                        "§e▶ Click để cho Linh Thú ăn thảo dược."
                )
        ));
        holder.actions.put(29, () -> {
            boolean fed = feedHerb(player, summoner);
            if (fed) open(player, page);
        });

        // 7. Slot 31: Nút Bồi Dưỡng Bằng Tu Vi / Linh Khí (Feed Cultivation)
        inv.setItem(31, makeItem(Material.EXPERIENCE_BOTTLE,
                "§b✦ Bồi Dưỡng Bằng Linh Khí",
                List.of(
                        "§7Truyền dẫn nguyên khí đan điền bồi dưỡng thần thú.",
                        "§7Tiêu hao: §b1,000 Linh Khí §7của bản thân.",
                        "§7Nhận: §e+150 EXP §7Linh Thú.",
                        "",
                        "§e▶ Click để truyền linh khí bồi dưỡng."
                )
        ));
        holder.actions.put(31, () -> {
            boolean fed = feedLinhKhi(player, summoner, data);
            if (fed) open(player, page);
        });

        // 8. Slot 33: Nút Thử Nghiệm Tuyệt Kỹ (Demo Skill)
        inv.setItem(33, makeItem(Material.BLAZE_POWDER,
                "§e✦ Diễn Võ Tuyệt Kỹ",
                List.of(
                        "§7Kích phát kiếm ý và tuyệt chiêu của Linh Thú",
                        "§7ngay tại chỗ để quan sát uy lực và hiệu ứng hào quang.",
                        "",
                        "§e▶ Click để Linh Thú trình diễn tuyệt kỹ!"
                )
        ));
        holder.actions.put(33, () -> {
            summoner.playSkillDemo(player);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
        });

        // 9. Danh Sách Linh Thú Theo Cảnh Giới Đã Mở Khóa (Slots 37 - 43: 7 slots)
        CultivationStage[] allStages = CultivationStage.values();
        int playerStageOrd = data.getStage().ordinal();
        int totalPets = allStages.length;
        int pageSize = 7;
        int maxPages = (int) Math.ceil((double) totalPets / pageSize);
        int validPage = Math.max(0, Math.min(page, maxPages - 1));

        int startIdx = validPage * pageSize;
        int endIdx = Math.min(startIdx + pageSize, totalPets);

        for (int i = startIdx; i < endIdx; i++) {
            int slot = 37 + (i - startIdx);
            CultivationStage st = allStages[i];
            PetTierConfig tier = plugin.getPetTierManager().getTier(st);
            boolean unlocked = (playerStageOrd >= st.ordinal());
            boolean isSelected = (activeTier.realmKey().equalsIgnoreCase(tier.realmKey())
                    || activeTier.displayName().equalsIgnoreCase(tier.displayName()));

            Material icon = getDisplayMaterial(tier);
            String name = (unlocked ? (isSelected ? "§a✔ " : "§f") : "§8🔒 ") + tier.displayName();
            List<String> lore = new ArrayList<>();
            lore.add("§7Cảnh giới yêu cầu: " + st.getColor() + st.getDisplayName());
            lore.add("§7Loại hình: §f" + (tier.flies() ? "Phi Thiên" : "Trì Địa") + " §7(Mob: " + tier.entityType() + ")");
            lore.add("§f⚔ Công: §c" + tier.attackDamage() + " §7| §f🛡 Thủ: §a" + (int)(tier.defendHealPercent() * 100) + "% HP");

            if (unlocked) {
                if (isSelected) {
                    lore.add("");
                    lore.add("§a✦ ĐANG TRANG BỊ LÀM LINH THÚ CHÍNH ✦");
                } else {
                    lore.add("");
                    lore.add("§e▶ Click để đổi sang Linh Thú này!");
                }
            } else {
                lore.add("");
                lore.add("§c✖ Chưa đủ cảnh giới để thuần phục Linh Thú này.");
            }

            inv.setItem(slot, makeItem(unlocked ? icon : Material.GRAY_DYE, name, lore));

            if (unlocked && !isSelected) {
                holder.actions.put(slot, () -> {
                    summoner.setSelectedPetStage(player, st);
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
                    player.sendMessage(plugin.prefix() + "§aĐã đổi Linh Thú chính sang: §f" + tier.displayName());
                    open(player, validPage);
                });
            }
        }

        // 10. Phân Trang (Slots 36 & 44)
        if (validPage > 0) {
            inv.setItem(36, makeItem(Material.ARROW, "§e◀ Trang Trước §7(" + validPage + "/" + maxPages + ")", null));
            holder.actions.put(36, () -> open(player, validPage - 1));
        }
        if (validPage < maxPages - 1) {
            inv.setItem(44, makeItem(Material.ARROW, "§e▶ Trang Kế §7(" + (validPage + 2) + "/" + maxPages + ")", null));
            holder.actions.put(44, () -> open(player, validPage + 1));
        }

        // 11. Slot 49: Đóng / Quay Lại
        inv.setItem(49, makeItem(Material.BARRIER, "§c✖ Đóng Giao Diện", List.of("§7Đóng bảng quản lý Linh Thú.")));
        holder.actions.put(49, player::closeInventory);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 0.7f, 1.1f);
    }

    // ============================================================
    // BỒI DƯỠNG LINH THÚ
    // ============================================================

    private boolean feedHerb(Player player, PetSummon summoner) {
        Inventory pInv = player.getInventory();
        ItemStack found = null;
        for (ItemStack item : pInv.getContents()) {
            if (item != null && item.getType() != Material.AIR && item.hasItemMeta()
                    && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().contains("Thảo")) {
                found = item;
                break;
            }
        }

        if (found == null) {
            player.sendMessage(plugin.prefix() + "§cTrong túi đồ không có Linh Thảo nào để cho Linh Thú ăn!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return false;
        }

        found.setAmount(found.getAmount() - 1);
        summoner.addPetExp(player, 100);
        player.sendMessage(plugin.prefix() + "§aCho Linh Thú ăn §f" + found.getItemMeta().getDisplayName() + " §anhận §e+100 EXP§a!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.8f, 1.2f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.2, 0), 6, 0.3, 0.3, 0.3, 0.05);
        return true;
    }

    private boolean feedLinhKhi(Player player, PetSummon summoner, PlayerData data) {
        long cost = 1000L;
        if (data.getLinhKhi() < cost) {
            player.sendMessage(plugin.prefix() + "§cKhông đủ Linh Khí! Cần ít nhất §e1,000 Linh Khí §cđể bồi dưỡng.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return false;
        }

        data.addLinhKhi(-cost);
        summoner.addPetExp(player, 150);
        player.sendMessage(plugin.prefix() + "§bTruyền dẫn §e1,000 Linh Khí §bcho Linh Thú, nhận §e+150 EXP§b!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1.2, 0), 12, 0.4, 0.4, 0.4, 0.5);
        return true;
    }

    // ============================================================
    // TIỆN ÍCH HIỂN THỊ
    // ============================================================

    private Material getDisplayMaterial(PetTierConfig tier) {
        if (tier == null) return Material.BONE;
        return switch (tier.entityType()) {
            case WOLF -> Material.BONE;
            case CAT, OCELOT -> Material.COD;
            case FOX -> Material.SWEET_BERRIES;
            case POLAR_BEAR -> Material.SALMON;
            case PARROT -> Material.FEATHER;
            case ALLAY -> Material.AMETHYST_SHARD;
            case PHANTOM -> Material.PHANTOM_MEMBRANE;
            case TURTLE -> Material.TURTLE_HELMET;
            case IRON_GOLEM -> Material.IRON_BLOCK;
            case WARDEN -> Material.ECHO_SHARD;
            case ENDER_DRAGON -> Material.DRAGON_BREATH;
            case GOAT -> Material.GOAT_HORN;
            default -> Material.LEAD;
        };
    }

    private String getExpBar(long cur, long max) {
        int totalBars = 20;
        int filled = (max <= 0) ? totalBars : (int) Math.min(totalBars, (cur * totalBars) / max);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == filled) sb.append("§7");
            sb.append("■");
        }
        return sb.toString();
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PetHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Runnable action = holder.actions.get(event.getRawSlot());
        if (action != null) {
            action.run();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PetHolder) {
            event.setCancelled(true);
        }
    }
}
