package com.tutien.skills;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.effects.EffectEngine;
import com.tutien.data.ClassType;
import com.tutien.data.PlayerData;
import org.bukkit.entity.Player;

public abstract class Skill {

    protected final TuTienPlugin plugin;
    private final String id;
    private final String displayName;
    private final String description;
    private final ClassType requiredClass;
    private final long cooldownMs;
    private final int minRank; // canh gioi toi thieu de dung

    public Skill(TuTienPlugin plugin, String id, String displayName, String description,
                 ClassType requiredClass, long cooldownMs, int minRank) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.requiredClass = requiredClass;
        this.cooldownMs = cooldownMs;
        this.minRank = minRank;
    }

    /**
     * Thuc thi skill
     * @param player nguoi dung skill
     * @param data du lieu nguoi choi
     * @return true neu thanh cong
     */
    public abstract boolean execute(Player player, PlayerData data);

    public boolean canUse(Player player, PlayerData data) {
        if (player == null || data == null || !player.isOnline() || data.getStage() == null) return false;
        if (!isEnabledByConfig()) {
            player.sendMessage(plugin.prefix() + "§cSkill này hiện đang bị tắt trong cấu hình.");
            return false;
        }
        if (!data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cBạn chưa chọn class!");
            return false;
        }
        // Thiên Đế trở lên được dùng mọi skill; ở cảnh giới thấp hơn vẫn phải đủ cấp rank.
        boolean canUseAll = data.getStage() != null && data.getStage().canUseAllSkills();
        if (!canUseAll && minRank > 1 && data.getRank() < minRank) {
            player.sendMessage(plugin.prefix() + "§cSkill §e" + displayName + " §ccần rank §e" + minRank + "§c trở lên.");
            return false;
        }
        if (!canUseAll && requiredClass != null && data.getClassType() != requiredClass) {
            player.sendMessage(plugin.prefix() + "§cSkill này chỉ dành cho §e" + requiredClass.getDisplayName()
                + "§c! (cảnh giới hiện tại: §e" + data.getStage().getDisplayName() + "§c)");
            return false;
        }
        if (data.isSkillOnCooldown(id)) {
            long remaining = data.getSkillCooldownRemaining(id) / 1000;
            player.sendMessage(plugin.prefix() + "§eSkill §a" + displayName + " §ecòn §c" + (remaining + 1) + "s §ecooldown!");
            return false;
        }
        return true;
    }

    public boolean tryUse(Player player, PlayerData data) {
        if (!canUse(player, data)) return false;
        plugin.debug("Skill attempt: player=" + player.getName() + ", skill=" + id + ", stage=" + data.getStage() + ", rank=" + data.getRank());
        boolean success;
        try {
            success = execute(player, data);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Skill '" + id + "' bị lỗi khi thực thi: " + ex.getClass().getSimpleName());
            player.sendMessage(plugin.prefix() + "§cSkill gặp lỗi và chưa bị trừ hồi chiêu. Hãy báo admin.");
            return false;
        }
        plugin.debug("Skill result: player=" + player.getName() + ", skill=" + id + ", success=" + success);
        if (success) {
            data.setSkillCooldown(id, cooldownMs);
            if (plugin.getEffectEngine() != null) plugin.getEffectEngine().skillCast(player, id);
        }
        return success;
    }

    private boolean isEnabledByConfig() {
        String configKey = switch (id.toLowerCase(java.util.Locale.ROOT)) {
            case "kiemkhitram" -> "kiem-khi-tran";
            case "tockiem" -> "toc-kiem";
            case "thiensat" -> "thien-sat";
            case "linhhoacau" -> "linh-hoa-cau";
            case "dongbang" -> "dong-bang";
            case "phaptran" -> "phap-tran";
            case "linhduoc" -> "linh-duoc";
            case "phuhotran" -> "phu-ho-tran";
            case "giaido" -> "giai-doc";
            case "manhdoctan" -> "manh-doc-tan";
            case "amsatkiem" -> "am-sat-kiem";
            case "doctran" -> "doc-tran";
            default -> id;
        };
        return plugin.getConfig().getBoolean("skills.enabled." + configKey, true);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ClassType getRequiredClass() { return requiredClass; }
    public long getCooldownMs() { return cooldownMs; }
    public int getMinRank() { return minRank; }
}
