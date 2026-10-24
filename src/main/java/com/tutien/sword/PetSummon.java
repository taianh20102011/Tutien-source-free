package com.tutien.sword;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PetCombatMode;
import com.tutien.data.PetTierConfig;
import com.tutien.data.PlayerData;
import com.tutien.hooks.SimplePetsHook;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * PetSummon - Hệ thống Linh Thú Đồng Hành Tu Tiên (Nâng Cấp Toàn Diện)
 *
 * Tính năng chính:
 * - Triệu hồi và điều khiển Linh Thú với AI thông minh, bám sát người chơi mượt mà.
 * - Khắc phục hoàn toàn lỗi pet bị đông cứng (setAware) và lỗi tính tọa độ đào đất/lơ lửng.
 * - Hỗ trợ 3 chế độ tác chiến: Cuồng Bạo (⚔), Hộ Thể (🛡), Đồng Hành (🕊).
 * - Hệ thống cấp độ và kinh nghiệm (Level & Exp) bồi dưỡng qua GUI.
 * - Hiệu ứng kỹ năng, âm thanh và hạt particle mãn nhãn ngay cả trên máy chủ thuần Vanilla/Paper.
 * - Tương thích hoàn hảo với MythicMobs, SimplePets và MCPets khi được cài đặt.
 */
public class PetSummon implements Listener {

    private final TuTienPlugin plugin;
    private record ActivePet(LivingEntity entity, boolean flies) {}
    private final Map<UUID, ActivePet> playerPets = new HashMap<>();
    private final Set<UUID> usingExternalPet = new HashSet<>();
    private final SimplePetsHook simplePetsHook;
    private final Map<UUID, Long> attackCooldown = new HashMap<>();
    private final Map<UUID, Long> defendCooldown = new HashMap<>();
    private BukkitTask followTask;

    private static final String OWNER_KEY = "tutien_pet_owner";

    public PetSummon(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.simplePetsHook = new SimplePetsHook(plugin);
        startFollowLoop();
    }

    // ============================================================
    // TRIỆU HỒI & THU HỒI
    // ============================================================

    public void togglePet(Player player) {
        if (hasPet(player)) {
            dismissPet(player);
            player.sendMessage(plugin.prefix() + "§7Đã thu hồi §dLinh Thú §7về đan điền nghỉ ngơi.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.4f);
        } else {
            summonPet(player);
        }
    }

    public void summonPet(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cCần chọn môn phái trước! (/tutien chon)");
            return;
        }

        if (hasPet(player)) {
            dismissPet(player);
        }

        PetTierConfig tier = getActiveTier(player);
        PetTierConfig.PetMode mode = resolveMode(tier);

        // 1. SimplePets
        if (mode == PetTierConfig.PetMode.SIMPLEPETS) {
            if (simplePetsHook.summon(player, tier.simplePetsType())) {
                usingExternalPet.add(player.getUniqueId());
                applyBuff(player, tier);
                spawnSummonVfx(player.getLocation(), tier);
                player.sendMessage(plugin.prefix() + "§d✦ Triệu hồi Linh Thú §f" + tier.displayName() + " §d(SimplePets)!");
                return;
            }
            mode = PetTierConfig.PetMode.VANILLA;
        }

        // 2. MCPets
        if (mode == PetTierConfig.PetMode.MCPETS) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mcpets summon " + tier.mythicMobsId() + " " + player.getName());
            usingExternalPet.add(player.getUniqueId());
            applyBuff(player, tier);
            spawnSummonVfx(player.getLocation(), tier);
            player.sendMessage(plugin.prefix() + "§d✦ Triệu hồi Linh Thú §f" + tier.displayName() + " §d(MCPets)!");
            return;
        }

        // 3. MythicMobs hoặc Vanilla Fallback
        LivingEntity pet = (mode == PetTierConfig.PetMode.MYTHICMOBS) ? spawnMythicPet(player, tier) : null;
        if (pet == null) {
            pet = spawnVanillaPet(player, tier);
        }

        if (pet == null) {
            player.sendMessage(plugin.prefix() + "§cKhông thể triệu hồi Linh Thú này. Hãy kiểm tra lại cấu hình!");
            return;
        }

        finalizePet(pet, player, tier);
        playerPets.put(player.getUniqueId(), new ActivePet(pet, tier.flies()));
        attackCooldown.remove(player.getUniqueId());
        defendCooldown.remove(player.getUniqueId());
        applyBuff(player, tier);

        spawnSummonVfx(pet.getLocation(), tier);
        player.sendMessage(plugin.prefix() + "§d✦ Linh Thú §f" + tier.displayName() + " §dđã giáng lâm trợ chiến!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
    }

    private PetTierConfig.PetMode resolveMode(PetTierConfig tier) {
        if (isModeAvailable(tier.mode())) return tier.mode();
        for (PetTierConfig.PetMode candidate : plugin.getPetTierManager().getFallbackOrder()) {
            if (isModeAvailable(candidate)) return candidate;
        }
        return PetTierConfig.PetMode.VANILLA;
    }

    private boolean isModeAvailable(PetTierConfig.PetMode mode) {
        return switch (mode) {
            case MYTHICMOBS -> plugin.getMythicMobsHook() != null && plugin.getMythicMobsHook().isAvailable();
            case MCPETS -> Bukkit.getPluginManager().isPluginEnabled("MCPets");
            case SIMPLEPETS -> simplePetsHook.isAvailable();
            case VANILLA -> true;
        };
    }

    private LivingEntity spawnMythicPet(Player player, PetTierConfig tier) {
        if (plugin.getMythicMobsHook() == null || !plugin.getMythicMobsHook().isAvailable()) return null;
        return plugin.getMythicMobsHook().spawnGenericMob(
                tier.mythicMobsId(),
                getInitialSpawnLocation(player, tier.flies()),
                tier.mythicMobsLevel()
        );
    }

    private LivingEntity spawnVanillaPet(Player player, PetTierConfig tier) {
        try {
            Location loc = getInitialSpawnLocation(player, tier.flies());
            Entity spawned = player.getWorld().spawnEntity(loc, tier.entityType());
            if (spawned instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
            spawned.remove();
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("[PetSummon] Không thể spawn vanilla entity " + tier.entityType() + ": " + e.getMessage());
            return null;
        }
    }

    private Location getInitialSpawnLocation(Player player, boolean flies) {
        Location pLoc = player.getLocation();
        double rad = Math.toRadians(pLoc.getYaw());
        double dx = Math.sin(rad) * 1.5;
        double dz = -Math.cos(rad) * 1.5;
        double dy = flies ? 1.5 : 0.0;
        Location loc = pLoc.clone().add(dx, dy, dz);
        loc.setYaw(pLoc.getYaw());
        loc.setPitch(0);
        return loc;
    }

    private void finalizePet(LivingEntity pet, Player player, PetTierConfig tier) {
        pet.setInvulnerable(true);
        pet.setSilent(false);
        pet.setPersistent(false);
        pet.setRemoveWhenFarAway(false);
        pet.setCollidable(false);

        // Thiết lập trọng lực: Linh thú bay thì thả nổi, linh thú mặt đất giữ trọng lực tự nhiên
        pet.setGravity(!tier.flies());

        // Duy trì AI để cử động tự nhiên, không đóng băng (setAware=true)
        if (pet instanceof Mob mob) {
            mob.setAware(true);
        }

        if (pet instanceof Ageable ageable) {
            ageable.setBaby();
        }

        if (pet instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(player);
        }

        if (pet instanceof Sittable sittable) {
            sittable.setSitting(false);
        }

        if (pet.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(9999);
            pet.setHealth(9999);
        }

        int petLevel = getPetLevel(player);
        pet.setCustomName("§d✦ " + tier.displayName() + " §e[Lv." + petLevel + "]");
        pet.setCustomNameVisible(true);

        pet.getPersistentDataContainer().set(
                new NamespacedKey(plugin, OWNER_KEY),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );
    }

    public void dismissPet(Player player) {
        UUID id = player.getUniqueId();
        if (usingExternalPet.remove(id)) {
            simplePetsHook.dismiss(player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mcpets revoke " + player.getName());
        }

        ActivePet active = playerPets.remove(id);
        attackCooldown.remove(id);
        defendCooldown.remove(id);

        if (active != null && !active.entity().isDead()) {
            Location loc = active.entity().getLocation();
            loc.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 0.5, 0), 16, 0.4, 0.4, 0.4, 0.5);
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.3, 0), 8, 0.2, 0.2, 0.2, 0.05);
            active.entity().remove();
        }

        clearBuff(player);
    }

    public boolean hasPet(Player player) {
        if (usingExternalPet.contains(player.getUniqueId())) return true;
        ActivePet active = playerPets.get(player.getUniqueId());
        return active != null && !active.entity().isDead();
    }

    public void refreshIfActive(Player player) {
        if (!hasPet(player)) return;
        dismissPet(player);
        summonPet(player);
        player.sendMessage(plugin.prefix() + "§d✦ Linh Thú đã đồng bộ sức mạnh với cảnh giới mới!");
    }

    // ============================================================
    // VÒNG LẶP DI CHUYỂN THEO CHỦ NHÂN (FOLLOW LOOP)
    // ============================================================

    private void startFollowLoop() {
        if (followTask != null) followTask.cancel();

        followTask = new BukkitRunnable() {
            double tick = 0;

            @Override
            public void run() {
                tick += 0.2;

                for (Map.Entry<UUID, ActivePet> entry : new HashMap<>(playerPets).entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    LivingEntity pet = entry.getValue().entity();

                    if (p == null || !p.isOnline() || pet == null || pet.isDead()) {
                        playerPets.remove(entry.getKey());
                        continue;
                    }

                    if (!pet.getWorld().equals(p.getWorld())) {
                        pet.teleport(p.getLocation());
                        continue;
                    }

                    // Tọa độ mục tiêu tính thuần 2D ngang theo Yaw người chơi
                    Location pLoc = p.getLocation();
                    double rad = Math.toRadians(pLoc.getYaw());
                    // 1.5m phía sau, 0.8m lệch sang phải để không cản tầm nhìn
                    double dx = Math.sin(rad) * 1.5 + Math.cos(rad) * 0.8;
                    double dz = -Math.cos(rad) * 1.5 + Math.sin(rad) * 0.8;
                    double dy = entry.getValue().flies() ? (1.5 + Math.sin(tick) * 0.25) : 0.0;

                    Location target = pLoc.clone().add(dx, dy, dz);
                    target.setYaw(pLoc.getYaw());
                    target.setPitch(0);

                    double distSq = pet.getLocation().distanceSquared(target);

                    // Quá xa (> 7 block) hoặc khác world -> teleport bám theo
                    if (distSq > 49.0) {
                        pet.teleport(target);
                        pet.getWorld().spawnParticle(Particle.PORTAL, target, 8, 0.2, 0.2, 0.2, 0.05);
                    } else if (entry.getValue().flies()) {
                        // Thần thú bay: bay lơ lửng uyển chuyển
                        pet.teleport(target);
                        if ((int) tick % 4 == 0) {
                            pet.getWorld().spawnParticle(Particle.END_ROD, pet.getLocation().add(0, 0.3, 0), 1, 0.05, 0.05, 0.05, 0.01);
                        }
                    } else if (distSq > 4.0) {
                        // Thần thú mặt đất: di chuyển bằng pathfinding
                        if (pet instanceof Mob mob && mob.getPathfinder() != null) {
                            mob.getPathfinder().moveTo(target, 1.45);
                        } else {
                            pet.teleport(target);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    // ============================================================
    // CƠ CHẾ TÁC CHIẾN (COMBAT ENGINE)
    // ============================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerAttack(EntityDamageByEntityEvent event) {
        Player owner = getPlayerAttacker(event.getDamager());
        if (owner == null || !hasPet(owner)) return;

        if (event.getEntity() instanceof LivingEntity victim) {
            if (victim == owner || isPet(victim)) return;
            triggerAttack(owner, victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player owner)) return;
        if (!hasPet(owner) || event.getFinalDamage() <= 0) return;
        triggerDefend(owner);
    }

    private Player getPlayerAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) return p;
        return null;
    }

    private void triggerAttack(Player owner, LivingEntity victim) {
        UUID id = owner.getUniqueId();
        PlayerData data = plugin.getDataManager().get(owner);
        if (data == null) return;

        PetCombatMode mode = getCombatMode(owner);
        if (mode == PetCombatMode.PASSIVE) return; // Đồng hành thuần túy, không tham chiến

        PetTierConfig tier = getActiveTier(owner);
        if (!ready(attackCooldown, id, tier.attackCooldownSeconds())) return;
        mark(attackCooldown, id, tier.attackCooldownSeconds());

        ActivePet active = playerPets.get(id);
        int petLevel = getPetLevel(owner);
        double levelBonus = 1.0 + (petLevel - 1) * 0.08;

        Location spawnLoc = (active != null) ? active.entity().getLocation().add(0, 0.5, 0) : owner.getLocation().add(0, 1.0, 0);
        Location victimLoc = victim.getLocation().add(0, 1.0, 0);

        // Hiệu ứng tia kiếm khí / đòn đánh từ pet tới kẻ địch
        Vector dir = victimLoc.toVector().subtract(spawnLoc.toVector()).normalize();
        double dist = Math.min(12.0, spawnLoc.distance(victimLoc));
        for (double d = 0; d < dist; d += 0.5) {
            Location p = spawnLoc.clone().add(dir.clone().multiply(d));
            p.getWorld().spawnParticle(Particle.CRIT, p, 1, 0.02, 0.02, 0.02, 0.01);
        }

        victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victimLoc, 2, 0.2, 0.2, 0.2, 0.05);
        victim.getWorld().spawnParticle(Particle.FIREWORK, victimLoc, 8, 0.3, 0.3, 0.3, 0.05);
        victim.getWorld().playSound(victimLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.85f, 1.4f);

        if (active != null) {
            castPetSkill(active.entity(), tier.attackSkill());
        }

        double baseDamage = tier.attackDamage() * levelBonus;
        double damage = Math.max(0.1, baseDamage * data.calculateDamageMultiplier());
        victim.damage(damage, owner);
    }

    private void triggerDefend(Player owner) {
        UUID id = owner.getUniqueId();
        PlayerData data = plugin.getDataManager().get(owner);
        if (data == null) return;

        PetCombatMode mode = getCombatMode(owner);
        if (mode == PetCombatMode.PASSIVE) return;

        PetTierConfig tier = getActiveTier(owner);
        if (!ready(defendCooldown, id, tier.defendCooldownSeconds())) return;
        mark(defendCooldown, id, tier.defendCooldownSeconds());

        ActivePet active = playerPets.get(id);
        int petLevel = getPetLevel(owner);
        double levelBonus = 1.0 + (petLevel - 1) * 0.08;

        Location ownerLoc = owner.getLocation().add(0, 1.0, 0);
        owner.getWorld().spawnParticle(Particle.HEART, ownerLoc, 8, 0.4, 0.4, 0.4, 0.1);
        owner.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, ownerLoc, 16, 0.3, 0.5, 0.3, 0.1);
        owner.getWorld().playSound(ownerLoc, Sound.ENTITY_ALLAY_ITEM_TAKEN, 0.9f, 1.5f);

        if (active != null) {
            castPetSkill(active.entity(), tier.defendSkill());
        }

        double heal = owner.getMaxHealth() * Math.max(0, tier.defendHealPercent() * levelBonus);
        owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + heal));

        int resAmp = Math.max(0, tier.defendResistanceAmplifier());
        int durSec = Math.max(1, tier.defendDurationSeconds());
        owner.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durSec * 20, resAmp, true, false));

        owner.sendMessage(plugin.prefix() + "§d✦ Linh Thú §f" + tier.displayName()
                + " §agiáng hộ thể! Hồi phục §e" + String.format("%.1f", heal) + " HP §avà nhận Kháng đòn!");
    }

    public void playSkillDemo(Player player) {
        PetTierConfig tier = getActiveTier(player);
        Location pLoc = player.getLocation().add(0, 1.0, 0);

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, pLoc, 25, 0.5, 0.5, 0.5, 0.15);
        player.getWorld().spawnParticle(Particle.ENCHANT, pLoc, 30, 0.6, 0.6, 0.6, 0.8);
        player.getWorld().spawnParticle(Particle.FIREWORK, pLoc, 12, 0.4, 0.4, 0.4, 0.05);

        player.getWorld().playSound(pLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.5f);
        player.getWorld().playSound(pLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);

        ActivePet active = playerPets.get(player.getUniqueId());
        if (active != null) {
            castPetSkill(active.entity(), tier.attackSkill());
        }

        player.sendMessage(plugin.prefix() + "§d✦ Linh Thú §f" + tier.displayName() + " §etung tuyệt kỹ chấn nhiếp bát hoang!");
    }

    private void spawnSummonVfx(Location loc, PetTierConfig tier) {
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.PORTAL, loc.clone().add(0, 0.5, 0), 30, 0.4, 0.5, 0.4, 0.2);
        world.spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.3, 0), 12, 0.3, 0.3, 0.3, 0.05);
        world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 0.8, 0), 20, 0.5, 0.5, 0.5, 0.5);
        world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.6f);
    }

    private void applyBuff(Player player, PetTierConfig tier) {
        if (tier.regenAmplifier() >= 0)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE,
                    tier.regenAmplifier(), true, false));
        if (tier.luckAmplifier() >= 0)
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, Integer.MAX_VALUE,
                    tier.luckAmplifier(), true, false));
    }

    private void clearBuff(Player player) {
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.LUCK);
    }

    private boolean ready(Map<UUID, Long> map, UUID id, int seconds) {
        return System.currentTimeMillis() >= map.getOrDefault(id, 0L);
    }

    private void mark(Map<UUID, Long> map, UUID id, int seconds) {
        map.put(id, System.currentTimeMillis() + Math.max(0, seconds) * 1000L);
    }

    private void castPetSkill(LivingEntity pet, String skill) {
        if (plugin.getMythicMobsHook() != null && plugin.getMythicMobsHook().isAvailable())
            plugin.getMythicMobsHook().castSkillOnEntity(pet, skill);
    }

    private boolean isPet(Entity entity) {
        if (entity instanceof LivingEntity le) {
            return le.getPersistentDataContainer().has(
                    new NamespacedKey(plugin, OWNER_KEY), PersistentDataType.STRING);
        }
        return false;
    }

    // ============================================================
    // GETTERS & SETTERS (TÍCH HỢP GUI & EXTRA DATA)
    // ============================================================

    public PetTierConfig getActiveTier(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return plugin.getPetTierManager().getTier((CultivationStage) null);

        String selectedStageStr = data.getExtraStr("pet.selected_stage", null);
        CultivationStage targetStage = data.getStage();
        if (selectedStageStr != null) {
            try {
                CultivationStage picked = CultivationStage.valueOf(selectedStageStr);
                // Chỉ dùng pet đã chọn nếu cảnh giới người chơi >= cảnh giới của pet đó
                if (data.getStage().ordinal() >= picked.ordinal()) {
                    targetStage = picked;
                }
            } catch (Exception ignored) {}
        }
        return plugin.getPetTierManager().getTier(targetStage);
    }

    public void setSelectedPetStage(Player player, CultivationStage stage) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;

        data.setExtraStr("pet.selected_stage", (stage != null) ? stage.name() : null);

        if (hasPet(player)) {
            dismissPet(player);
            summonPet(player);
        }
    }

    public PetCombatMode getCombatMode(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return PetCombatMode.AGGRESSIVE;
        String modeStr = data.getExtraStr("pet.combat_mode", "AGGRESSIVE");
        try {
            return PetCombatMode.valueOf(modeStr);
        } catch (Exception e) {
            return PetCombatMode.AGGRESSIVE;
        }
    }

    public void setCombatMode(Player player, PetCombatMode mode) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data != null && mode != null) {
            data.setExtraStr("pet.combat_mode", mode.name());
        }
    }

    public int getPetLevel(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        return (data != null) ? Math.max(1, data.getExtraInt("pet.level", 1)) : 1;
    }

    public long getPetExp(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        return (data != null) ? Math.max(0L, data.getExtraLong("pet.exp", 0L)) : 0L;
    }

    public long getMaxExpForLevel(int level) {
        return Math.max(100L, level * 500L);
    }

    public void addPetExp(Player player, long amount) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || amount <= 0) return;

        int curLevel = getPetLevel(player);
        long curExp = getPetExp(player) + amount;
        long needed = getMaxExpForLevel(curLevel);

        boolean levelUp = false;
        while (curExp >= needed && curLevel < 50) {
            curExp -= needed;
            curLevel++;
            needed = getMaxExpForLevel(curLevel);
            levelUp = true;
        }

        data.setExtraInt("pet.level", curLevel);
        data.setExtraLong("pet.exp", curExp);

        if (levelUp) {
            player.sendMessage(plugin.prefix() + "§6✦ §e§lTHĂNG CẤP! §dLinh Thú của bạn đã đột phá lên §eCấp " + curLevel + "§d!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.3f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1.2, 0), 20, 0.4, 0.4, 0.4, 0.1);

            ActivePet active = playerPets.get(player.getUniqueId());
            if (active != null && !active.entity().isDead()) {
                PetTierConfig tier = getActiveTier(player);
                active.entity().setCustomName("§d✦ " + tier.displayName() + " §e[Lv." + curLevel + "]");
            }
        }
    }

    public void cleanup() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        for (ActivePet active : playerPets.values()) {
            if (active.entity() != null && !active.entity().isDead()) {
                active.entity().remove();
            }
        }
        playerPets.clear();
        usingExternalPet.clear();
    }

    // ============================================================
    // BUKKIT EVENTS
    // ============================================================

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        dismissPet(e.getPlayer());
    }

    @EventHandler
    public void onPetDamage(EntityDamageEvent e) {
        if (isPet(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPetTarget(EntityTargetEvent e) {
        if (isPet(e.getEntity())) {
            e.setCancelled(true);
        }
    }
}
