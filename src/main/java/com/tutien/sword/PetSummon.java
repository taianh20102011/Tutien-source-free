package com.tutien.sword;

import com.tutien.TuTienPlugin;
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

import java.util.*;

public class PetSummon implements Listener {
    private final TuTienPlugin plugin;
    private record ActivePet(LivingEntity entity, boolean flies) {}
    private final Map<UUID, ActivePet> playerPets = new HashMap<>();
    private final Set<UUID> usingExternalPet = new HashSet<>();
    private final SimplePetsHook simplePetsHook;
    private final Map<UUID, Long> attackCooldown = new HashMap<>();
    private final Map<UUID, Long> defendCooldown = new HashMap<>();
    private static final String OWNER_KEY = "tutien_pet_owner";

    public PetSummon(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.simplePetsHook = new SimplePetsHook(plugin);
        startFollowLoop();
    }

    public void togglePet(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cChưa chọn class!");
            return;
        }
        if (hasPet(player)) {
            dismissPet(player);
            player.sendMessage(plugin.prefix() + "§7Thu hồi Linh Thú.");
            return;
        }
        PetTierConfig tier = plugin.getPetTierManager().getTier(data.getStage());
        PetTierConfig.PetMode mode = resolveMode(tier);

        // Xử lý SimplePets
        if (mode == PetTierConfig.PetMode.SIMPLEPETS) {
            if (simplePetsHook.summon(player, tier.simplePetsType())) {
                usingExternalPet.add(player.getUniqueId());
                applyBuff(player, tier);
                player.sendMessage(plugin.prefix() + "§d✦ Triệu hồi §f" + tier.displayName());
                return;
            }
            mode = PetTierConfig.PetMode.VANILLA;
        }

        // Xử lý MCPets (Triệu hồi qua Command System của MCPets)
        if (mode == PetTierConfig.PetMode.MCPETS) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mcpets summon " + tier.mythicMobsId() + " " + player.getName());
            usingExternalPet.add(player.getUniqueId());
            applyBuff(player, tier);
            player.sendMessage(plugin.prefix() + "§d✦ Triệu hồi Linh Thú (MCPets) §f" + tier.displayName());
            return;
        }

        summonPet(player, tier, mode);
    }

    private PetTierConfig.PetMode resolveMode(PetTierConfig tier) {
        if (isModeAvailable(tier.mode())) return tier.mode();
        for (PetTierConfig.PetMode candidate : plugin.getPetTierManager().getFallbackOrder())
            if (isModeAvailable(candidate)) return candidate;
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

    private void summonPet(Player player, PetTierConfig tier, PetTierConfig.PetMode mode) {
        LivingEntity pet = mode == PetTierConfig.PetMode.MYTHICMOBS
                ? spawnMythicPet(player, tier) : null;
        if (pet == null) pet = spawnVanillaPet(player, tier);
        if (pet == null) {
            player.sendMessage(plugin.prefix() + "§cKhông thể triệu hồi Linh Thú này.");
            return;
        }
        finalizePet(pet, player, tier);
        playerPets.put(player.getUniqueId(), new ActivePet(pet, tier.flies()));
        attackCooldown.remove(player.getUniqueId());
        defendCooldown.remove(player.getUniqueId());
        applyBuff(player, tier);
        player.sendMessage(plugin.prefix() + "§d✦ Linh Thú §f" + tier.displayName() + " §dđã giáng lâm!");
    }

    private LivingEntity spawnMythicPet(Player player, PetTierConfig tier) {
        return plugin.getMythicMobsHook().spawnGenericMob(tier.mythicMobsId(),
                player.getLocation().clone().add(0, 1.2, 0), tier.mythicMobsLevel());
    }

    private LivingEntity spawnVanillaPet(Player player, PetTierConfig tier) {
        try {
            Entity spawned = player.getWorld().spawnEntity(
                    player.getLocation().clone().add(0, 1.2, 0), tier.entityType());
            if (spawned instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
            spawned.remove();
            return null;
        } catch (Exception e) { return null; }
    }

    private void finalizePet(LivingEntity pet, Player player, PetTierConfig tier) {
        pet.setInvulnerable(true);
        pet.setSilent(false);
        pet.setPersistent(false);
        pet.setRemoveWhenFarAway(false);
        pet.setGravity(false);
        pet.setCollidable(false);
        if (pet instanceof Mob mob) {
            mob.setAware(false);
            try { mob.getPathfinder().stopPathfinding(); } catch (Exception ignored) {}
        }
        if (pet instanceof Ageable ageable) ageable.setBaby();
        if (pet instanceof Tameable tameable) tameable.setOwner(player);
        if (pet.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            pet.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(9999);
            pet.setHealth(9999);
        }
        pet.setCustomName("§d✦ " + tier.displayName());
        pet.setCustomNameVisible(true);
        pet.getPersistentDataContainer().set(new NamespacedKey(plugin, OWNER_KEY),
                PersistentDataType.STRING, player.getUniqueId().toString());
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

    public void dismissPet(Player player) {
        UUID id = player.getUniqueId();
        if (usingExternalPet.remove(id)) {
            simplePetsHook.dismiss(player);
            // Lệnh revoke/revoke-all thu hồi pet MCPets
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mcpets revoke " + player.getName());
        }
        ActivePet active = playerPets.remove(id);
        attackCooldown.remove(id);
        defendCooldown.remove(id);
        if (active != null && !active.entity().isDead()) {
            active.entity().getWorld().spawnParticle(Particle.ENCHANTED_HIT, active.entity().getLocation(),
                    12, .3, .3, .3, .2);
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
        togglePet(player);
        player.sendMessage(plugin.prefix() + "§d✦ Linh Thú đã tiến hóa theo cảnh giới mới!");
    }

    private void startFollowLoop() {
        new BukkitRunnable() {
            double tick;
            @Override public void run() {
                tick += .1;
                for (Map.Entry<UUID, ActivePet> entry : new HashMap<>(playerPets).entrySet()) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    LivingEntity pet = entry.getValue().entity();
                    if (p == null || !p.isOnline() || pet.isDead()) {
                        playerPets.remove(entry.getKey());
                        continue;
                    }
                    double y = entry.getValue().flies() ? 2.2 + Math.sin(tick) * .3 : .1;
                    Location behind = p.getLocation().clone()
                            .subtract(p.getLocation().getDirection().normalize().multiply(1.6))
                            .add(0, y, 0);
                    if (pet.getLocation().distanceSquared(behind) > 1.0) pet.teleport(behind);
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

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
        PetTierConfig tier = plugin.getPetTierManager().getTier(plugin.getDataManager().get(owner).getStage());
        if (!ready(attackCooldown, id, tier.attackCooldownSeconds())) return;
        mark(attackCooldown, id, tier.attackCooldownSeconds());
        ActivePet active = playerPets.get(id);
        if (active != null) {
            castPetSkill(active.entity(), tier.attackSkill());
        }
        double damage = Math.max(0.1, tier.attackDamage() * plugin.getDataManager().get(owner).calculateDamageMultiplier());
        victim.damage(damage, owner);
    }

    private void triggerDefend(Player owner) {
        UUID id = owner.getUniqueId();
        PetTierConfig tier = plugin.getPetTierManager().getTier(plugin.getDataManager().get(owner).getStage());
        if (!ready(defendCooldown, id, tier.defendCooldownSeconds())) return;
        mark(defendCooldown, id, tier.defendCooldownSeconds());
        ActivePet active = playerPets.get(id);
        if (active != null) {
            castPetSkill(active.entity(), tier.defendSkill());
        }
        double heal = owner.getMaxHealth() * Math.max(0, tier.defendHealPercent());
        owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + heal));
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

    @EventHandler public void onQuit(PlayerQuitEvent e) { dismissPet(e.getPlayer()); }
    @EventHandler public void onPetDamage(EntityDamageEvent e) { if (isPet(e.getEntity())) e.setCancelled(true); }
    @EventHandler public void onPetTarget(EntityTargetEvent e) { if (isPet(e.getEntity())) e.setCancelled(true); }
}