package com.tutien.alchemy;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Luyện đan config-driven: admin chỉ cần sửa config.yml. */
public class AlchemyManager {
    private final TuTienPlugin plugin;
    private static final String KEY_DAN = "tutien_dan";
    private final Map<String, DanDuoc> recipes = new LinkedHashMap<>();
    private final Set<UUID> brewing = new HashSet<>();

    public static class DanDuoc {
        public final String id, displayName, color, description, difficulty;
        public final ItemStack[] materials;
        public final String[] herbIds;
        public final int[] herbAmounts;
        public final int minStageOrdinal;
        public final long ltCost, lkBonus;
        public final double successRate;
        public final int craftTimeSeconds;
        public final String[] effectLines, effects;

        public DanDuoc(String id, String displayName, String color, String description, String difficulty,
                       ItemStack[] materials, String[] herbIds, int[] herbAmounts, int minStageOrdinal,
                       long ltCost, long lkBonus, double successRate, int craftTimeSeconds,
                       String[] effectLines, String[] effects) {
            this.id=id; this.displayName=displayName; this.color=color; this.description=description;
            this.difficulty=difficulty; this.materials=materials; this.herbIds=herbIds; this.herbAmounts=herbAmounts;
            this.minStageOrdinal=minStageOrdinal; this.ltCost=ltCost; this.lkBonus=lkBonus;
            this.successRate=successRate; this.craftTimeSeconds=craftTimeSeconds;
            this.effectLines=effectLines; this.effects=effects;
        }
    }

    public AlchemyManager(TuTienPlugin plugin) { this.plugin=plugin; reload(); }

    public void reload() {
        recipes.clear();
        ConfigurationSection rs = plugin.getConfig().getConfigurationSection("alchemy.recipes");
        if (rs != null) for (String id : rs.getKeys(false)) {
            ConfigurationSection s = rs.getConfigurationSection(id);
            if (s == null) continue;
            try { recipes.put(id, parseRecipe(id, s)); }
            catch (Exception e) { plugin.getLogger().warning("[Alchemy] Công thức '"+id+"' lỗi: "+e.getMessage()); }
        }
        plugin.getLogger().info("[Alchemy] Đã nạp " + recipes.size() + "/32 công thức.");
    }

    private DanDuoc parseRecipe(String id, ConfigurationSection s) {
        List<ItemStack> mats = new ArrayList<>();
        for (String raw : s.getStringList("materials")) {
            String[] p = raw.split(":",2);
            Material m = Material.valueOf(p[0].trim().toUpperCase());
            mats.add(new ItemStack(m, Integer.parseInt(p.length>1?p[1].trim():"1")));
        }
        List<String> herbIds = new ArrayList<>(); List<Integer> herbAmounts = new ArrayList<>();
        for (String raw : s.getStringList("herbs")) {
            String[] p = raw.split(":",2);
            herbIds.add(p[0].trim()); herbAmounts.add(Integer.parseInt(p.length>1?p[1].trim():"1"));
        }
        return new DanDuoc(id,
                s.getString("name", id), s.getString("color", "§a"), s.getString("description", ""),
                s.getString("difficulty", "Hạ Phẩm"),
                mats.toArray(new ItemStack[0]), herbIds.toArray(new String[0]), herbAmounts.stream().mapToInt(i->i).toArray(),
                s.getInt("min-stage",0), s.getLong("lt-cost",0), s.getLong("lk-bonus",0),
                Math.max(0, Math.min(1, s.getDouble("success-rate", .8))),
                Math.max(1, s.getInt("craft-time-seconds", 5)),
                s.getStringList("effect-lines").toArray(new String[0]),
                s.getStringList("effects").toArray(new String[0]));
    }

    public Map<String, DanDuoc> getAllRecipes() { return Collections.unmodifiableMap(recipes); }
    public boolean isBrewing(Player p) { return brewing.contains(p.getUniqueId()); }

    public boolean canCraft(Player player, DanDuoc r, boolean sendMessage) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return false;
        if (data.getStage().ordinal() < r.minStageOrdinal) {
            if (sendMessage) player.sendMessage(plugin.prefix()+"§cCảnh giới chưa đủ. Cần ít nhất §e"+r.minStageOrdinal+"§c.");
            return false;
        }
        if (data.getTien() < r.ltCost) {
            if (sendMessage) player.sendMessage(plugin.prefix()+"§cThiếu §e"+r.ltCost+" "+plugin.getTienManager().tenTien()+"§c.");
            return false;
        }
        for (ItemStack mat : r.materials) if (!player.getInventory().containsAtLeast(mat, mat.getAmount())) {
            if (sendMessage) player.sendMessage(plugin.prefix()+"§cThiếu §e"+mat.getAmount()+"x "+pretty(mat.getType().name())+"§c.");
            return false;
        }

        for (int i = 0; i < r.herbIds.length; i++) {
            final String herbId = r.herbIds[i];
            final int requiredAmount = r.herbAmounts[i];
            final int have = plugin.getHerbManager().countHerb(player, herbId);

            if (have < requiredAmount) {
                if (sendMessage) {
                    plugin.getHerbManager().getHerbType(herbId).ifPresentOrElse(
                            h -> player.sendMessage(plugin.prefix() + "§cThiếu §e" + requiredAmount + "x " + h.name() + "§c (có " + have + ")."),
                            () -> player.sendMessage(plugin.prefix() + "§cThiếu thảo dược ID §e" + herbId + "§c.")
                    );
                }
                return false;
            }
        }
        return true;
    }

    public void luyenDan(Player player, String recipeId) {
        DanDuoc r = recipes.get(recipeId);
        if (r == null) { player.sendMessage(plugin.prefix()+"§cCông thức không tồn tại!"); return; }
        if (isBrewing(player)) { player.sendMessage(plugin.prefix()+"§eBạn đang luyện một lò đan khác!"); return; }
        if (!canCraft(player, r, true)) return;
        PlayerData data = plugin.getDataManager().get(player);

        // Consume ALL requirements before starting: luyện thất bại vẫn mất nguyên liệu.
        for (ItemStack mat : r.materials) player.getInventory().removeItem(mat.clone());
        for (int i=0;i<r.herbIds.length;i++) plugin.getHerbManager().removeHerb(player, r.herbIds[i], r.herbAmounts[i]);
        if (r.ltCost > 0 && !data.spendTien(r.ltCost)) {
            player.sendMessage(plugin.prefix()+"§cKhông thể trừ phí luyện đan.");
            return;
        }
        brewing.add(player.getUniqueId());
        player.sendMessage(plugin.prefix()+"§6⚗ Đang luyện §e"+r.displayName+" §7("+r.difficulty+", §e"+percent(r.successRate)+"%§7)...");
        player.sendActionBar("§6⚗ §e"+r.displayName+" §7— §f0%");
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, .8f);

        new BukkitRunnable() {
            int ticks=0; final int total=Math.max(1,r.craftTimeSeconds*20);
            @Override public void run() {
                if (!player.isOnline()) { brewing.remove(player.getUniqueId()); cancel(); return; }
                if (ticks++ >= total) { cancel(); brewing.remove(player.getUniqueId()); finishBrew(player,data,r); return; }
                if (ticks % 5 == 0) player.sendActionBar("§6⚗ §e"+r.displayName+" §7— §f"+(ticks*100/total)+"%");
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0,1.5,0), 2,.3,.3,.3,0);
                if (ticks % 20 == 0) player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, .5f, 1.0f);
            }
        }.runTaskTimer(plugin,0L,1L);
    }

    private void finishBrew(Player player, PlayerData data, DanDuoc r) {
        boolean success = ThreadLocalRandom.current().nextDouble() <= r.successRate;
        if (!success) {
            player.sendTitle("§c§lLÒ ĐAN THẤT BẠI", "§7"+r.displayName+" — mất toàn bộ nguyên liệu", 5, 50, 10);
            player.sendMessage(plugin.prefix()+"§c✗ Luyện đan thất bại! §7Tỉ lệ thành công: §e"+percent(r.successRate)+"%§7.");
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, .7f);
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0,1,0), 25,.6,.8,.6,.05);
            return;
        }
        ItemStack dan=createDanItem(r);
        Map<Integer,ItemStack> left=player.getInventory().addItem(dan);
        left.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(),it));
        data.addLinhKhi(r.lkBonus);
        plugin.getSkillItemManager().updateInfoItem(player,data);
        player.sendTitle("§a§lLUYỆN ĐAN THÀNH CÔNG", "§e"+r.displayName, 5, 60, 15);
        player.sendMessage(plugin.prefix()+"§a✓ Thành công! §e"+r.displayName+" §7| +§b"+r.lkBonus+" LK");
        player.sendMessage("§7Độ khó: "+r.difficulty+" §8| §7Tỉ lệ: §e"+percent(r.successRate)+"%" );
        for(String line:r.effectLines) player.sendMessage("  §a▸ "+line);
        player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,1f,1.2f);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,player.getLocation().add(0,1,0),30,.5,.5,.5,.3);
    }

    public ItemStack createDanItem(DanDuoc r) {
        ItemStack item=new ItemStack(Material.HONEY_BOTTLE); ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(r.color+"💊 "+r.displayName);
        List<String> lore=new ArrayList<>();
        lore.add("§7"+r.description); lore.add("§8─────────────────────");
        lore.add("§6+§e"+r.lkBonus+" LK §7khi uống");
        for(String line:r.effectLines) lore.add("§a▸ "+line);
        lore.add("§8─────────────────────"); lore.add("§7Chuột phải để uống đan");
        meta.setLore(lore); meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin,KEY_DAN),PersistentDataType.STRING,r.id);
        item.setItemMeta(meta); return item;
    }

    public boolean isDanItem(ItemStack item) { return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin,KEY_DAN),PersistentDataType.STRING); }
    public String getDanId(ItemStack item) { return isDanItem(item)?item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin,KEY_DAN),PersistentDataType.STRING):null; }

    public void useDan(Player player, ItemStack item) {
        String id=getDanId(item); if(id==null)return; DanDuoc r=recipes.get(id); if(r==null)return;
        PlayerData data=plugin.getDataManager().get(player); if(data==null)return;
        if(item.getAmount()>1)item.setAmount(item.getAmount()-1); else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        data.addLinhKhi(r.lkBonus); applyEffects(player,r.effects); plugin.getSkillItemManager().updateInfoItem(player,data);
        player.sendMessage(plugin.prefix()+"§aUống §e"+r.displayName+" §7→ +§b"+r.lkBonus+" LK");
        player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_BURP,.8f,1f);
        player.getWorld().spawnParticle(Particle.WITCH,player.getLocation().add(0,1,0),12,.3,.3,.3,0);
    }

    private void applyEffects(Player player, String[] effects) {
        for (String raw : effects) {
            String[] p = raw.split(":");
            String key = p[0].trim().toUpperCase();
            try {
                if (key.equals("HEAL")) {
                    double pct = Double.parseDouble(p[1]);
                    player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + player.getMaxHealth() * pct));
                    continue;
                }
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type == null) continue;
                int amp = p.length > 1 ? Integer.parseInt(p[1]) : 0;
                int sec = p.length > 2 ? Integer.parseInt(p[2]) : 30;
                player.addPotionEffect(new PotionEffect(type, Math.max(1, sec * 20), Math.max(0, amp), false, true, true));
            } catch (Exception ignored) {}
        }
    }

    public void showRecipeList(Player player, int page) {
        PlayerData data = plugin.getDataManager().get(player); if(data==null)return;
        player.sendMessage("§6§l═══ 32 LINH ĐAN ═══");
        for (DanDuoc r : recipes.values()) {
            boolean ok = data.getStage().ordinal() >= r.minStageOrdinal;
            player.sendMessage((ok?"§a✓ ":"§8✗ ")+r.color+r.displayName+" §8| "+r.difficulty+" §8| §7"+percent(r.successRate)+"% §8| §7"+r.description);
        }
    }
    private String percent(double d) { return String.format(Locale.US, "%.1f", d * 100); }
    private String pretty(String s) { return s.toLowerCase(Locale.ROOT).replace('_', ' '); }
}