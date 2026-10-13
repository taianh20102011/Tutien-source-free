package com.tutien.economy;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Economy bridge for Tu Tien.
 *
 * The plugin no longer owns a separate currency balance. Vault is the single
 * source of truth, so every feature that previously used Tien automatically
 * uses the server's configured economy provider.
 *
 * /tien has intentionally been removed. Economy features remain available
 * internally for reroll, breakthrough rewards, crafting, sect costs, /trade,
 * /tang, admin tools, etc.
 */
public class TienManager {

    private final TuTienPlugin plugin;
    private Economy economy;

    public TienManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        setupVault();
        PlayerData.bindEconomy(this);
    }

    public void refreshVault() {
        setupVault();
        if (economy != null && plugin.getDataManager() != null) {
            for (PlayerData data : plugin.getDataManager().getAllCached()) {
                long legacy = data.getLegacyTien();
                if (legacy > 0) migrateLegacyBalance(data, legacy);
            }
        }
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("[Economy] Vault is not installed. Money features will be unavailable.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();

        if (economy != null) {
            plugin.getLogger().info("[Economy] Vault connected: " + economy.getName());
        } else {
            plugin.getLogger().severe("[Economy] No Vault economy provider found. Install Vault + an economy plugin.");
        }
    }

    public boolean isAvailable() { return economy != null; }

    private OfflinePlayer offline(PlayerData data) {
        return Bukkit.getOfflinePlayer(data.getUuid());
    }

    public long getBalance(Player player) {
        return player == null ? 0L : getBalance(plugin.getDataManager().get(player));
    }

    public long getBalance(PlayerData data) {
        if (data == null || economy == null) return 0L;
        return Math.max(0L, Math.round(economy.getBalance(offline(data))));
    }

    /** Dung cho placeholder/leaderboard: lay so du CA KHI nguoi choi dang offline. */
    public long getBalance(OfflinePlayer op) {
        if (op == null || economy == null) return 0L;
        return Math.max(0L, Math.round(economy.getBalance(op)));
    }

    public void give(Player player, long amount) {
        if (player == null || amount <= 0 || economy == null) return;
        economy.depositPlayer(player, amount);
    }

    public boolean spend(Player player, long amount) {
        if (player == null || amount <= 0) return true;
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void giveOffline(PlayerData data, long amount) {
        if (data == null || amount <= 0 || economy == null) return;
        economy.depositPlayer(offline(data), amount);
    }

    public boolean spendOffline(PlayerData data, long amount) {
        if (data == null || amount <= 0) return true;
        if (economy == null) return false;
        return economy.withdrawPlayer(offline(data), amount).transactionSuccess();
    }

    /**
     * Migration helper for old PlayerData files. It never overwrites existing
     * Vault money; it only deposits the old balance when Vault is below it.
     */
    public void migrateLegacyBalance(PlayerData data, long legacyBalance) {
        if (data == null || legacyBalance <= 0 || economy == null) return;
        OfflinePlayer p = offline(data);
        double current = economy.getBalance(p);
        if (current < legacyBalance) {
            economy.depositPlayer(p, legacyBalance - current);
        }
    }

    public void setBalance(PlayerData data, long amount) {
        if (data == null || economy == null) return;
        OfflinePlayer p = offline(data);
        double current = economy.getBalance(p);
        double target = Math.max(0L, amount);
        if (target > current) economy.depositPlayer(p, target - current);
        else if (target < current) economy.withdrawPlayer(p, current - target);
    }

    public String tenTien() {
        return plugin.getConfig().getString("economy.tien-ten-hien-thi", "Tiền");
    }

    public String fmt(long n) {
        if (n >= 1_000_000_000_000L) return String.format("%.2fT", n / 1_000_000_000_000.0);
        if (n >= 1_000_000_000L)     return String.format("%.2fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L)         return String.format("%.2fM", n / 1_000_000.0);
        if (n >= 1_000L)             return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    // ========================
    //  TINH SO TIEN DROP TU MOB (config-driven)
    // ========================

    public long calculateMobDrop(LivingEntity mob, PlayerData data) {
        var cfg = plugin.getConfig();
        int stageIdx = data.getStage().ordinal();
        int perMob = cfg.getInt("economy.tien-per-mob-base", 2);
        int cap = cfg.getInt("economy.tien-per-mob-cap", 50);
        long base = Math.min((long) (stageIdx + 1) * perMob, cap);

        double mult = switch (mob.getType()) {
            case ZOMBIE, SKELETON, SPIDER    -> cfg.getDouble("economy.tien-mob-multiplier-thuong", 1.0);
            case CREEPER, CAVE_SPIDER        -> cfg.getDouble("economy.tien-mob-multiplier-kha", 1.3);
            case WITCH, PILLAGER, VINDICATOR -> cfg.getDouble("economy.tien-mob-multiplier-nguy-hiem", 1.8);
            case BLAZE, GHAST                -> cfg.getDouble("economy.tien-mob-multiplier-cao", 2.5);
            case ENDERMAN, DROWNED           -> cfg.getDouble("economy.tien-mob-multiplier-am", 2.0);
            case PIGLIN_BRUTE                -> cfg.getDouble("economy.tien-mob-multiplier-tinh-anh", 3.0);
            case RAVAGER, WARDEN             -> cfg.getDouble("economy.tien-mob-multiplier-boss", 6.0);
            default -> 1.0;
        };

        if (mob.customName() != null) {
            mult *= cfg.getDouble("economy.tien-mob-boss-ten-rieng-multiplier", 5.0);
        }

        if (data.getDaoPath() != null
            && data.getDaoPath() == com.tutien.data.DaoPath.MA_DAO) {
            mult *= cfg.getDouble("economy.tien-ma-dao-bonus", 1.2);
        }

        return Math.round(base * mult);
    }

    // ========================
    //  TINH THUONG DOT PHA (config-driven multiplier tren bang goc)
    // ========================

    public long calculateBreakthroughReward(CultivationStage stage) {
        int ord = stage.ordinal();
        long base = switch (ord) {
            case 0  -> 10L;      case 1  -> 30L;      case 2  -> 80L;
            case 3  -> 200L;     case 4  -> 500L;
            case 5  -> 1_200L;   case 6  -> 3_000L;   case 7  -> 7_500L;
            case 8  -> 15_000L;  case 9  -> 35_000L;  case 10 -> 80_000L;
            case 11 -> 180_000L; case 12 -> 400_000L; case 13 -> 900_000L; case 14 -> 2_000_000L;
            case 15 -> 5_000_000L;   case 16 -> 10_000_000L;  case 17 -> 20_000_000L;
            case 18 -> 40_000_000L;  case 19 -> 80_000_000L;  case 20 -> 150_000_000L;
            case 21 -> 250_000_000L; case 22 -> 400_000_000L; case 23 -> 600_000_000L;
            case 24 -> 900_000_000L;
            default -> 1_000_000_000L;
        };
        double mult = plugin.getConfig().getDouble("economy.tien-breakthrough-base-multiplier", 1.0);
        return Math.round(base * mult);
    }

    public long playerKillReward() {
        return plugin.getConfig().getLong("economy.tien-per-player-kill", 50);
    }

    public long dailyLoginReward() {
        return plugin.getConfig().getLong("economy.tien-daily-login", 100);
    }

    public long rerollLinhCanCost() {
        return plugin.getConfig().getLong("economy.reroll-linh-can-cost", 500);
    }

    public long rerollTocCost() {
        return plugin.getConfig().getLong("economy.reroll-toc-cost", 2000);
    }

    public long shopHoiMauCost() {
        return plugin.getConfig().getLong("economy.shop-hoi-mau-cost", 50);
    }

    public long shopResetCdCost() {
        return plugin.getConfig().getLong("economy.shop-reset-cd-cost", 200);
    }
}