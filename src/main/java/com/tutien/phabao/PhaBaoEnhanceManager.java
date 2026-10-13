package com.tutien.phabao;

import com.tutien.TuTienPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CUONG HOA (+1 -> +10) cho Phap Bao.
 *
 * - Moi cap CONG THEM % ATK/DEF/HP va sat thuong ky nang (cong don, doc tu
 *   config "phabao.enhance.bonus-per-level", ap dung trong
 *   PhaBaoManager#getPassiveAtkMult/DefMult/HpMult va executeAbility()).
 * - Ti le thanh cong GIAM DAN theo cap dich, doc tu
 *   "phabao.enhance.success-rate.<cap>" trong config.yml.
 * - That bai MA CO Da Bao Ho trong tui (tu dong tieu 1 vien) -> KHONG mat
 *   gi ca, chi mat phi + vien da.
 * - That bai MA KHONG CO Da Bao Ho -> roi xac suat "fail-downgrade-chance":
 *   phan lon thi ROT 1 CAP (khong mat han item), phan con lai thi VO DO
 *   (item bi xoa hoan toan) - dung y "co ti le xit rot cap/vo do neu khong
 *   co Da Bao Ho" nhu yeu cau.
 *
 * Tat ca cac con so deu doc tu config.yml, khong hardcode, de admin de
 * dang can chinh do kho/kinh te ma khong can sua code.
 */
public class PhaBaoEnhanceManager {

    private final TuTienPlugin plugin;
    private final Random random = new Random();

    private static final String KEY_ENHANCE = "pb_enhance";
    private static final String KEY_PROTECTION_STONE = "pb_protection_stone";

    // Danh dau dong lore "Cuong Hoa" de biet ma thay the moi lan cap nhat,
    // khong bi trung/chong lore qua nhieu lan cuong hoa lien tiep.
    private static final String ENHANCE_LORE_MARKER = "§d✦ Cường Hóa: ";

    public PhaBaoEnhanceManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  DOC/GHI CAP CUONG HOA
    // ========================

    public int getEnhanceLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        NamespacedKey key = new NamespacedKey(plugin, KEY_ENHANCE);
        Integer v = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    private void setEnhanceLevel(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY_ENHANCE);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, Math.max(0, level));
        item.setItemMeta(meta);
        refreshEnhanceLore(item);
    }

    public int getMaxLevel() {
        return Math.max(1, plugin.getConfig().getInt("phabao.enhance.max-level", 10));
    }

    /** % cong them MOI cap (vd 0.03 = +3%/cap, cong don). */
    public double getBonusPerLevel() {
        return plugin.getConfig().getDouble("phabao.enhance.bonus-per-level", 0.03);
    }

    /**
     * He so nhan THEM do Cuong Hoa (1.0 = khong co gi, 1.30 = +30%). Nhan
     * truc tiep vao ATK/DEF/HP mult va sat thuong ky nang cua Phap Bao -
     * xem PhaBaoManager#getPassiveAtkMult/DefMult/HpMult va executeAbility().
     */
    public double getBonusMultiplier(ItemStack item) {
        int lvl = getEnhanceLevel(item);
        if (lvl <= 0) return 1.0;
        return 1.0 + lvl * getBonusPerLevel();
    }

    // ========================
    //  DA BAO HO
    // ========================

    public ItemStack createProtectionStone() {
        Material mat = com.tutien.PlatformUtil.safeMaterial(
                plugin.getConfig().getString("phabao.enhance.protection-stone-material", "AMETHYST_SHARD"),
                Material.AMETHYST_SHARD);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d✦ Đá Bảo Hộ");
        List<String> lore = new ArrayList<>();
        lore.add("§7Dùng khi §dCường Hóa Pháp Bảo§7.");
        lore.add("§7Nếu cường hóa §cthất bại§7, viên đá này sẽ");
        lore.add("§7tự tiêu hao để §aBẢO VỆ§7 Pháp Bảo khỏi");
        lore.add("§7bị §crớt cấp§7 hoặc §4vỡ đồ§7.");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_PROTECTION_STONE),
                PersistentDataType.BYTE, (byte) 1);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isProtectionStone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, KEY_PROTECTION_STONE), PersistentDataType.BYTE);
    }

    /** Dem so Da Bao Ho nguoi choi dang co trong túi do (khong tinh o offhand/armor). */
    public int countProtectionStones(Player player) {
        int total = 0;
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (isProtectionStone(it)) total += it.getAmount();
        }
        return total;
    }

    /** Tru 1 Da Bao Ho khoi tui do (dung khi that bai va da duoc bao ve). */
    private void consumeOneProtectionStone(Player player) {
        for (ItemStack it : player.getInventory().getStorageContents()) {
            if (isProtectionStone(it)) {
                it.setAmount(it.getAmount() - 1);
                return;
            }
        }
    }

    // ========================
    //  CHI PHI + TI LE THANH CONG
    // ========================

    public long getCost(int targetLevel) {
        long base = plugin.getConfig().getLong("phabao.enhance.cost-tien-base", 500);
        long perLevel = plugin.getConfig().getLong("phabao.enhance.cost-tien-per-level", 300);
        return base + perLevel * (long) targetLevel;
    }

    public double getSuccessRate(int targetLevel) {
        double def = switch (targetLevel) {
            case 1 -> 0.95; case 2 -> 0.90; case 3 -> 0.85; case 4 -> 0.75; case 5 -> 0.65;
            case 6 -> 0.55; case 7 -> 0.45; case 8 -> 0.35; case 9 -> 0.25; default -> 0.15;
        };
        return plugin.getConfig().getDouble("phabao.enhance.success-rate." + targetLevel, def);
    }

    public double getFailDowngradeChance() {
        return plugin.getConfig().getDouble("phabao.enhance.fail-downgrade-chance", 0.7);
    }

    // ========================
    //  KET QUA CUONG HOA
    // ========================

    public enum Result { SUCCESS, FAIL_SAFE, FAIL_DOWNGRADE, FAIL_BROKEN, MAX_LEVEL, NOT_ENOUGH_MONEY }

    /**
     * Thu Cuong Hoa item nguoi choi dang cam tren tay chinh. TRUC TIEP sua
     * item trong tay (hoac xoa het neu vo do) - KHONG can GUI drag-drop, chi
     * can dang cam Phap Bao + du tien + (tuy chon) co san Da Bao Ho trong tui.
     */
    public Result attemptEnhance(Player player, ItemStack item) {
        int current = getEnhanceLevel(item);
        int max = getMaxLevel();
        if (current >= max) return Result.MAX_LEVEL;

        int target = current + 1;
        long cost = getCost(target);
        if (!plugin.getTienManager().spend(player, cost)) return Result.NOT_ENOUGH_MONEY;

        double chance = getSuccessRate(target);
        boolean success = random.nextDouble() < chance;

        if (success) {
            setEnhanceLevel(item, target);
            return Result.SUCCESS;
        }

        boolean hasStone = countProtectionStones(player) > 0;
        if (hasStone) {
            consumeOneProtectionStone(player);
            return Result.FAIL_SAFE; // khong mat gi them, chi mat phi + da
        }

        if (random.nextDouble() < getFailDowngradeChance()) {
            setEnhanceLevel(item, Math.max(0, current - 1));
            return Result.FAIL_DOWNGRADE;
        }

        // VO DO: xoa item khoi tay nguoi choi
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.isSimilar(item)) {
            player.getInventory().setItemInMainHand(null);
        }
        return Result.FAIL_BROKEN;
    }

    // ========================
    //  CAP NHAT LORE/TEN HIEN THI
    // ========================

    /** Goi lai sau moi lan doi cap - cap nhat "+N" tren ten va 1 dong lore rieng. */
    public void refreshEnhanceLore(ItemStack item) {
        if (!plugin.getPhaBaoManager().isPhaBao(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int lvl = getEnhanceLevel(item);
        PhaBaoTier tier = plugin.getPhaBaoManager().getTier(item);

        // --- Ten hien thi: bo "+N" cu (neu co) roi gan lai ---
        String name = meta.getDisplayName();
        name = name.replaceAll("\\s*§d\\+\\d+$", "");
        if (lvl > 0) name = name + " §d+" + lvl;
        meta.setDisplayName(name);

        // --- Lore: thay dong marker cu (neu co), hoac chen sau dong tier ---
        List<String> lore = meta.hasLore() && meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(l -> l != null && l.startsWith(ENHANCE_LORE_MARKER));

        if (lvl > 0 && tier != null) {
            double bonus = getBonusMultiplier(item) - 1.0;
            String line = ENHANCE_LORE_MARKER + "§f+" + lvl + " §7(§a+" + Math.round(bonus * 100) + "% §7ATK/DEF/HP)";
            int insertAt = 1; // ngay sau dong "════" dau tien, gan dau item cho de thay
            lore.add(Math.min(insertAt, lore.size()), line);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
