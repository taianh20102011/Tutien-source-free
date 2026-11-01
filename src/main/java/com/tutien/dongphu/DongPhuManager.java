package com.tutien.dongphu;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Động Phủ Cá Nhân & Dược Viên Độc Quyền (/dongphu):
 * - Mở không gian Động Phủ bỏ túi khi đạt Kết Đan (ordinal >= 2).
 * - Tọa độ pocket riêng biệt trên không trung hoặc thế giới an toàn (không bị quái đánh lén).
 * - Nước Linh Tuyền: Dùng để tưới Dược Điền, tăng tốc sinh trưởng lập tức cho Tiên Dược quý hiếm.
 * - Tụ Linh Trận: Tự động hồi phục linh khí x2 khi tĩnh tọa trong Động Phủ.
 */
public class DongPhuManager implements Listener, CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;
    private final NamespacedKey waterKey;

    // Khoảng cách giữa các Động Phủ của từng người chơi để không bị dính vào nhau (150 blocks)
    private static final int PLOT_SPACING = 150;
    private static final int BASE_Y = 180;

    // Lưu vị trí ngoài đời trước khi vào động phủ để có thể quay về (/dongphu ve)
    private final Map<UUID, Location> previousLocations = new HashMap<>();

    public DongPhuManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.waterKey = new NamespacedKey(plugin, "linh_tuyen_water");
        startDongPhuAuraTask();
    }

    /**
     * Tạo vật phẩm Nước Linh Tuyền.
     */
    public ItemStack createLinhTuyenWater(int amount) {
        ItemStack item = new ItemStack(Material.POTION, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b✦ Nước Linh Tuyền Ngàn Năm ✦");
            List<String> lore = new ArrayList<>();
            lore.add("§7Dòng nước tinh khiết ngưng tụ từ thiên địa linh mạch.");
            lore.add("§7Chuyên dùng để tưới tiêu Dược Điền trong Động Phủ.");
            lore.add("");
            lore.add("§a► Click chuột phải vào Thảo Dược để thúc đẩy sinh trưởng!");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(waterKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isLinhTuyenWater(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(waterKey, PersistentDataType.BYTE);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("auto_fd17b10807"));
            return true;
        }

        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return true;

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "vao", "enter", "tp" -> {
                    teleportToDongPhu(player, data);
                    return true;
                }
                case "ve", "thoat", "exit", "leave" -> {
                    leaveDongPhu(player);
                    return true;
                }
                case "linhtuyen", "nuoc", "water" -> {
                    claimLinhTuyenWater(player, data);
                    return true;
                }
                case "info", "status" -> {
                    showStatus(player, data);
                    return true;
                }
                default -> {
                    showHelp(player);
                    return true;
                }
            }
        }

        // Gõ /dongphu không tham số: Tự động vào động phủ
        teleportToDongPhu(player, data);
        return true;
    }

    /**
     * Dịch chuyển người chơi vào Động Phủ cá nhân.
     */
    public void teleportToDongPhu(Player player, PlayerData data) {
        // Cần đạt Kết Đan trở lên (ordinal >= 2)
        if (data.getStage().ordinal() < 2) {
            player.sendMessage(plugin.prefix() + "§cTu vi chưa đủ để mở Động Phủ! Cần đạt cảnh giới §eKết Đan §ctrở lên.");
            return;
        }

        World targetWorld = Bukkit.getWorlds().get(0); // Thế giới chính
        int plotIndex = Math.abs(player.getUniqueId().hashCode() % 1000);
        int originX = 10000 + (plotIndex * PLOT_SPACING);
        int originZ = 10000 + ((plotIndex * 31) % 1000) * PLOT_SPACING;

        Location spawnLoc = new Location(targetWorld, originX + 0.5, BASE_Y + 1, originZ + 0.5, 0f, 0f);

        // Xây dựng hoặc đảm bảo kết cấu Động Phủ nền tảng an toàn nếu chưa có
        buildDongPhuPlatform(targetWorld, originX, BASE_Y, originZ);

        previousLocations.put(player.getUniqueId(), player.getLocation());
        player.teleport(spawnLoc);

        player.playSound(spawnLoc, Sound.BLOCK_PORTAL_TRAVEL, 0.8f, 1.2f);
        player.spawnParticle(Particle.PORTAL, spawnLoc.clone().add(0, 1, 0), 40, 1.0, 1.0, 1.0, 0.1);

        player.sendMessage("§6§l╔════════════ ĐỘNG PHỦ TIÊN GIA ════════════╗");
        player.sendMessage("  §a✦ Chào mừng Đạo Hữu trở về Động Thiên Phúc Địa!");
        player.sendMessage("  §7• Dược Điền: Đã chuẩn bị sẵn luống đất phì nhiêu để trồng Tiên Dược.");
        player.sendMessage("  §7• Tụ Linh Trận: Tĩnh tọa tại đây tốc độ hồi phục Linh Khí x2.");
        player.sendMessage("  §e• Lệnh nhanh: §f/dongphu ve §7(Rời khỏi) | §f/dongphu nuoc §7(Nhận Nước Linh Tuyền)");
        player.sendMessage("§6§l╚════════════════════════════════════════╝");
    }

    /**
     * Rời khỏi Động Phủ quay lại vị trí cũ.
     */
    public void leaveDongPhu(Player player) {
        Location prev = previousLocations.remove(player.getUniqueId());
        if (prev != null && prev.getWorld() != null) {
            player.teleport(prev);
            player.playSound(prev, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage(plugin.prefix() + "§aĐã rời khỏi Động Phủ trở về hồng trần thế tục.");
        } else {
            // Vị trí mặc định nếu không lưu được
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            player.teleport(spawn);
            player.sendMessage(plugin.prefix() + "§aĐã trở về điểm hồi sinh thế giới.");
        }
    }

    /**
     * Nhận Nước Linh Tuyền mỗi ngày từ Linh Mạch Động Phủ.
     */
    public void claimLinhTuyenWater(Player player, PlayerData data) {
        long lastClaim = data.getExtraLong("dongphu_water_claim", 0L);
        long now = System.currentTimeMillis();
        long cooldown = 3_600_000L; // 1 tiếng nhận 1 lần

        if (now - lastClaim < cooldown) {
            long waitMinutes = (cooldown - (now - lastClaim)) / 60_000L;
            player.sendMessage(plugin.prefix() + "§eMắt Linh Tuyền đang tích lũy linh khí, vui lòng quay lại sau §c" + waitMinutes + " phút§e!");
            return;
        }

        data.setExtraLong("dongphu_water_claim", now);
        plugin.getDataManager().save(data);

        ItemStack water = createLinhTuyenWater(2);
        player.getInventory().addItem(water).values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));

        player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL, 1.0f, 1.0f);
        player.sendMessage(plugin.prefix() + "§a✦ Thu thập thành công §b2x Nước Linh Tuyền Ngàn Năm§a từ đáy suối Động Phủ!");
    }

    /**
     * Tự động tạo nền tảng Động Phủ trang nhã trên cao:
     * Nền đá thạch anh, bao quanh cột ngọc, trung tâm Dược Điền đất Farmland có nước tưới.
     */
    private void buildDongPhuPlatform(World world, int cx, int cy, int cz) {
        // Chỉ xây nếu chưa có sàn
        if (world.getBlockAt(cx, cy, cz).getType() == Material.SMOOTH_QUARTZ) {
            return;
        }

        int radius = 6;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block b = world.getBlockAt(cx + x, cy, cz + z);
                int distSq = x * x + z * z;
                if (distSq <= radius * radius) {
                    // Trung tâm 3x3 làm Dược Điền
                    if (Math.abs(x) <= 2 && Math.abs(z) <= 2) {
                        if (x == 0 && z == 0) {
                            b.setType(Material.WATER); // Mắt nước ở giữa
                        } else {
                            b.setType(Material.FARMLAND); // Đất trồng dược điền
                        }
                    } else if (distSq >= (radius - 1) * (radius - 1)) {
                        b.setType(Material.QUARTZ_PILLAR); // Viền ngọc
                    } else {
                        b.setType(Material.SMOOTH_QUARTZ); // Sàn ngọc thạch anh
                    }
                }
            }
        }
        // Đặt đèn lồng linh khí chiếu sáng
        world.getBlockAt(cx + 4, cy + 1, cz + 4).setType(Material.SEA_LANTERN);
        world.getBlockAt(cx - 4, cy + 1, cz - 4).setType(Material.SEA_LANTERN);
        world.getBlockAt(cx + 4, cy + 1, cz - 4).setType(Material.SEA_LANTERN);
        world.getBlockAt(cx - 4, cy + 1, cz + 4).setType(Material.SEA_LANTERN);
    }

    /**
     * Sự kiện dùng Nước Linh Tuyền tưới cây dược điền -> Cây lớn lập tức.
     */
    @EventHandler
    public void onWaterHerb(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isLinhTuyenWater(hand)) return;

        if (block.getBlockData() instanceof Ageable ageable) {
            event.setCancelled(true);
            if (ageable.getAge() >= ageable.getMaximumAge()) {
                player.sendMessage(plugin.prefix() + "§aDược thảo này đã hoàn toàn chín rộ, có thể thu hoạch ngay!");
                return;
            }

            // Đẩy tuổi cây lên max
            ageable.setAge(ageable.getMaximumAge());
            block.setBlockData(ageable);

            // Tiêu hao 1 bình Nước Linh Tuyền
            hand.setAmount(hand.getAmount() - 1);

            // Hiệu ứng Visual nước xanh & âm thanh tưới tiêu
            Location loc = block.getLocation().add(0.5, 0.6, 0.5);
            block.getWorld().spawnParticle(Particle.SPLASH, loc, 30, 0.4, 0.4, 0.4, 0.1);
            block.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 15, 0.3, 0.5, 0.3, 0);
            block.getWorld().playSound(loc, Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.2f);
            block.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.5f);

            player.sendMessage(plugin.prefix() + "§a✦ Nước Linh Tuyền thấm nhuần! Dược thảo đã lập tức trưởng thành vượt bậc!");
        }
    }

    /**
     * Tụ Linh Trận: Tăng tốc hồi phục Linh Khí khi người chơi ở trong Động Phủ (Y >= 170).
     */
    private void startDongPhuAuraTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getLocation().getY() >= 175 && player.getLocation().getX() >= 9000) {
                        PlayerData data = plugin.getDataManager().get(player);
                        if (data != null && data.hasClass()) {
                            long bonusLk = Math.max(50L, (long)(data.getMaxLinhKhi() * 0.005));
                            data.addLinhKhi(bonusLk);
                            plugin.getSkillItemManager().updateInfoItem(player, data);

                            Location pLoc = player.getLocation().add(0, 0.2, 0);
                            player.getWorld().spawnParticle(Particle.ENCHANT, pLoc, 3, 0.4, 0.2, 0.4, 0.1);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, 60L); // Mỗi 3 giây
    }

    private void showStatus(Player player, PlayerData data) {
        player.sendMessage("§6§l╔════════════ TRẠNG THÁI ĐỘNG PHỦ ════════════╗");
        player.sendMessage("  §7• Chủ sở hữu: §f" + player.getName());
        player.sendMessage("  §7• Đẳng cấp Động Phủ: §eNhất Phẩm Động Thiên");
        player.sendMessage("  §7• Mắt Linh Tuyền: §aHoạt động bình thường");
        player.sendMessage("  §7• Lệnh: §e/dongphu vao §7(Vào động) | §e/dongphu ve §7(Trở về)");
        player.sendMessage("§6§l╚════════════════════════════════════════╝");
    }

    private void showHelp(Player player) {
        player.sendMessage("§6§l╔══════════ HƯỚNG DẪN ĐỘNG PHỦ TIÊN GIA ══════════╗");
        player.sendMessage("  §e/dongphu vao       §8- §fDịch chuyển vào Động Phủ cá nhân");
        player.sendMessage("  §e/dongphu ve        §8- §fRời Động Phủ quay lại vị trí cũ");
        player.sendMessage("  §e/dongphu nuoc      §8- §fLĩnh nhận Nước Linh Tuyền tưới tiêu");
        player.sendMessage("  §e/dongphu status    §8- §fXem thông tin Động Phủ");
        player.sendMessage("§6§l╚════════════════════════════════════════╝");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("vao", "ve", "nuoc", "status");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> res = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(prefix)) res.add(s);
            }
            return res;
        }
        return Collections.emptyList();
    }
}
