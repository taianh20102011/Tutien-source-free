package com.tutien.skills;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillManager {

    private final TuTienPlugin plugin;
    private final Map<String, Skill> skills = new HashMap<>();

    // Helper: lay PotionEffectType theo ten Minecraft (tuong thich moi phien ban)
    private static PotionEffectType ef(String key) {
        PotionEffectType t = PotionEffectType.getByName(key);
        if (t == null) t = PotionEffectType.getByKey(NamespacedKey.minecraft(key));
        return t;
    }

    public SkillManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        registerAll();
    }

    private void register(Skill skill) {
        skills.put(skill.getId().toLowerCase(), skill);
    }

    public Skill getSkill(String id) {
        return skills.get(id.toLowerCase());
    }

    public Map<String, Skill> getAllSkills() {
        return Collections.unmodifiableMap(skills);
    }

    // ==========================================
    //  REGISTER ALL 26 SKILLS
    // ==========================================
    private void registerAll() {

        // ===== KIEM TU (3 skills) =====

        // 1. Kiếm Khí Trảm - Phong kiem khi tien len tua nhu projectile
        register(new Skill(plugin, "kiemkhitram", "Kiếm Khí Trảm",
                "Phóng kiếm khí phá hủy vật cản, gây sát thương cao", ClassType.KIEM_TU, 8000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                double damage = 12.0 * data.calculateDamageMultiplier();
                Location loc = player.getEyeLocation();
                Vector dir = loc.getDirection().normalize();

                // Tao projectile (ArmorStand vo hinh lam "kiem khi")
                for (int i = 1; i <= 20; i++) {
                    final int step = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location pos = loc.clone().add(dir.clone().multiply(step * 0.6));
                            player.getWorld().spawnParticle(Particle.CRIT, pos, 5, 0.1, 0.1, 0.1, 0);
                            player.getWorld().spawnParticle(Particle.CRIT, pos, 1, 0, 0, 0, 0);
                            // Check entity trung don
                            for (Entity e : player.getWorld().getNearbyEntities(pos, 0.8, 0.8, 0.8)) {
                                if (e instanceof LivingEntity target && e != player) {
                                    if (target instanceof Player tp) {
                                        PlayerData td = plugin.getDataManager().get(tp);
                                        double finalDmg = (td != null) ? damage / td.calculateDefenseMultiplier() : damage;
                                        target.damage(finalDmg, player);
                                    } else {
                                        target.damage(damage, player);
                                    }
                                    cancel();
                                }
                            }
                        }
                    }.runTaskLater(plugin, i * 1L);
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.5f);
                player.sendMessage(plugin.prefix() + "§c⚔ Kiếm Khí Trảm! §7Kiếm khí phóng lên!");
                return true;
            }
        });

        // 2. Tốc Kiếm - Tan cong nhanh x3 trong 5s
        register(new Skill(plugin, "tockiem", "Tốc Kiếm",
                "Tốc độ tấn công tăng x3 trong 5 giây", ClassType.KIEM_TU, 15000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                player.addPotionEffect(new PotionEffect(ef("haste"), 100, 3, false, true, true));
                player.addPotionEffect(new PotionEffect(ef("speed"), 100, 1, false, true, true));
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.2);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.8f);
                player.sendMessage(plugin.prefix() + "§c⚡ Tốc Kiếm! §7Kiếm pháp vũ bão trong 5 giây!");
                return true;
            }
        });

        // 3. Thiên Sát - Sat thuong khung bo qua giap
        register(new Skill(plugin, "thiensat", "Thiên Sát",
                "Đòn đánh chia trẻ, bỏ qua 80% armor", ClassType.KIEM_TU, 30000, 3) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                LivingEntity target = getNearestEnemy(player, 5.0);
                if (target == null) {
                    player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu trong tầm 5 blocks!");
                    return false;
                }
                double damage = 25.0 * data.calculateDamageMultiplier();
                target.damage(damage, player); // bo qua armor vi dung damage truc tiep
                target.setNoDamageTicks(0);
                Location tLoc = target.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, tLoc, 1, 0, 0, 0, 0);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, tLoc, 20, 0.3, 0.3, 0.3, 0.1);
                player.getWorld().playSound(tLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);
                player.sendMessage(plugin.prefix() + "§c☠ Thiên Sát! §7Gây " + (int) damage + " sát thương!");
                return true;
            }
        });

        // ===== PHAP TU (3 skills) =====

        // 4. Linh Hỏa Cầu - Cau lua AoE
        register(new Skill(plugin, "linhhoacau", "Linh Hỏa Cầu",
                "Ném cầu lửa phát nổ AoE", ClassType.PHAP_TU, 10000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Fireball fb = (Fireball) player.getWorld().spawnEntity(
                        player.getEyeLocation(), EntityType.FIREBALL);
                fb.setShooter(player);
                fb.setDirection(player.getEyeLocation().getDirection().multiply(1.5));
                fb.setYield(3.0f);
                fb.setIsIncendiary(true);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.2f, 0.8f);
                player.sendMessage(plugin.prefix() + "§6🔮 Linh Hỏa Cầu! §7Cầu lửa linh thiêng phóng ra!");
                return true;
            }
        });

        // 5. Đóng Băng - Dong bang tat ca ke thu xung quanh
        register(new Skill(plugin, "dongbang", "Đóng Băng",
                "Đóng băng tất cả kẻ thù trong 4m trong 3 giây", ClassType.PHAP_TU, 18000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int count = 0;
                for (Entity e : player.getNearbyEntities(4, 4, 4)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(ef("slowness"), 60, 10, false, true, true));
                        le.addPotionEffect(new PotionEffect(ef("weakness"), 60, 2, false, true, true));
                        player.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0);
                        count++;
                    }
                }
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 60, 2, 1, 2, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.5f, 0.5f);
                player.sendMessage(plugin.prefix() + "§b❄ Đóng Băng! §7Đóng băng " + count + " kẻ thù!");
                return true;
            }
        });

        // 6. Pháp Trận - Tao vung phep gay sát thương
        register(new Skill(plugin, "phaptran", "Pháp Trận",
                "Đặt pháp trận gây sát thương liên tục 5 giây", ClassType.PHAP_TU, 20000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Location center = player.getLocation();
                double damage = 4.0 * data.calculateDamageMultiplier();
                player.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1, 0.8f);
                player.sendMessage(plugin.prefix() + "§9✦ Pháp Trận! §7Pháp trận xuất hiện!");

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 100) { cancel(); return; } // 5 giay
                        ticks += 10;
                        // Hieu ung vong tron
                        for (double angle = 0; angle < 360; angle += 30) {
                            double rad = Math.toRadians(angle + ticks * 3);
                            Location p = center.clone().add(Math.cos(rad) * 3, 0, Math.sin(rad) * 3);
                            center.getWorld().spawnParticle(Particle.PORTAL, p, 3, 0.1, 0.1, 0.1, 0);
                        }
                        // Gay sát thương ke thu trong tran
                        for (Entity e : center.getWorld().getNearbyEntities(center, 3, 2, 3)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.damage(damage, player);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 10);
                return true;
            }
        });

        // ===== Y TU (3 skills) =====

        // 7. Linh Dược - Hoi mau ban than va dong doi
        register(new Skill(plugin, "linhduoc", "Linh Dược",
                "Hồi phục 50% máu tối đa cho bạn và đồng đội 5m", ClassType.Y_TU, 20000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int healed = 0;
                for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                    if (e instanceof Player ally) {
                        double maxHp = ally.getMaxHealth();
                        double heal = maxHp * 0.5;
                        ally.setHealth(Math.min(ally.getHealth() + heal, maxHp));
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);
                        ally.sendMessage(plugin.prefix() + "§a💊 Linh Dược hồi phục " + (int) heal + " máu!");
                        healed++;
                    }
                }
                // Hoi ban than
                double selfHeal = player.getMaxHealth() * 0.5;
                player.setHealth(Math.min(player.getHealth() + selfHeal, player.getMaxHealth()));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 15, 0.5, 0.5, 0.5, 0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                player.sendMessage(plugin.prefix() + "§a💊 Linh Dược! §7Hồi phục " + (int) selfHeal + " máu! (" + healed + " đồng đội)");
                return true;
            }
        });

        // 8. Phù Hộ Trận - Buff dong doi
        register(new Skill(plugin, "phuhotran", "Phù Hộ Trận",
                "Buff Regeneration + Strength cho toàn đội 8 giây", ClassType.Y_TU, 25000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int count = 0;
                for (Entity e : player.getNearbyEntities(6, 6, 6)) {
                    if (e instanceof Player ally) {
                        ally.addPotionEffect(new PotionEffect(ef("regeneration"), 160, 1, false, true, true));
                        ally.addPotionEffect(new PotionEffect(ef("strength"), 160, 1, false, true, true));
                        ally.getWorld().spawnParticle(Particle.ENCHANTED_HIT, ally.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0);
                        ally.sendMessage(plugin.prefix() + "§a✨ Phù Hộ Trận gia trì!");
                        count++;
                    }
                }
                player.addPotionEffect(new PotionEffect(ef("regeneration"), 160, 1, false, true, true));
                player.addPotionEffect(new PotionEffect(ef("strength"), 160, 1, false, true, true));
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1, 1.2f);
                player.sendMessage(plugin.prefix() + "§a✨ Phù Hộ Trận! §7Buff " + (count + 1) + " người trong đội!");
                return true;
            }
        });

        // 9. Giải Độc - Xoa hieu ung xau
        register(new Skill(plugin, "giaido", "Giải Độc",
                "Xóa toàn bộ hiệu ứng xấu cho bạn và đồng đội 5m", ClassType.Y_TU, 12000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                clearNegativeEffects(player);
                for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                    if (e instanceof Player ally) {
                        clearNegativeEffects(ally);
                        ally.sendMessage(plugin.prefix() + "§a🍃 Giải Độc! Hiệu ứng xấu đã được xóa!");
                    }
                }
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1, 1.5f);
                player.sendMessage(plugin.prefix() + "§a🍃 Giải Độc! §7Toàn bộ độc xấu đã bị xóa!");
                return true;
            }

            private void clearNegativeEffects(Player p) {
                p.removePotionEffect(ef("poison"));
                p.removePotionEffect(ef("wither"));
                p.removePotionEffect(ef("slowness"));
                p.removePotionEffect(ef("weakness"));
                p.removePotionEffect(ef("blindness"));
                p.removePotionEffect(ef("hunger"));
                p.removePotionEffect(ef("mining_fatigue"));
            }
        });

        // ===== DOC TU (3 skills) =====

        // 10. Mãnh Độc Tán - Doc AoE
        register(new Skill(plugin, "manhdoctan", "Mãnh Độc Tán",
                "Phun độc AoE vòng quanh 3m, độc nặng 8 giây", ClassType.DOC_TU, 12000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int count = 0;
                for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(ef("poison"), 160, 2, false, true, true));
                        le.addPotionEffect(new PotionEffect(ef("slowness"), 80, 1, false, true, true));
                        count++;
                    }
                }
                player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 50, 1.5, 0.5, 1.5, 0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 1.5f, 0.5f);
                player.sendMessage(plugin.prefix() + "§2☠ Mãnh Độc Tán! §7Tung độc lên " + count + " kẻ!");
                return true;
            }
        });

        // 11. Ám Sát Kiếm - Danh phia sau sát thương x2
        register(new Skill(plugin, "amsatkiem", "Ám Sát Kiếm",
                "Xích lại phía sau mục tiêu trong 6m, gây sát thương x2.5", ClassType.DOC_TU, 15000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                LivingEntity target = getNearestEnemy(player, 6.0);
                if (target == null) {
                    player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu!");
                    return false;
                }
                // Teleport sau lung ke thu
                Vector behind = target.getLocation().getDirection().normalize().multiply(-1.5);
                Location behindLoc = target.getLocation().clone().add(behind);
                player.teleport(behindLoc);

                double damage = 20.0 * data.calculateDamageMultiplier();
                target.damage(damage, player);
                target.addPotionEffect(new PotionEffect(ef("poison"), 120, 1, false, true, true));

                player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.2);
                player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.8f);
                player.sendMessage(plugin.prefix() + "§2🗡 Ám Sát Kiếm! §7Tấn công gây " + (int) damage + " sát thương!");
                return true;
            }
        });

        // 12. Độc Trận - Dat bep buoc doc
        register(new Skill(plugin, "doctran", "Độc Trận",
                "Đặt bẫy bước, kẻ thân vào bị độc chết người 5 giây", ClassType.DOC_TU, 20000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Location trap = player.getLocation();
                player.getWorld().playSound(trap, Sound.ENTITY_WITCH_CELEBRATE, 1, 0.8f);
                player.sendMessage(plugin.prefix() + "§2🕷 Độc Trận! §7Đặt bẫy bước tại vị trí hiện tại!");

                new BukkitRunnable() {
                    int ticks = 0;
                    final Location center = trap.clone();
                    @Override
                    public void run() {
                        if (ticks >= 300) { cancel(); return; } // 15s
                        ticks += 5;
                        center.getWorld().spawnParticle(Particle.WITCH, center, 3, 0.3, 0.05, 0.3, 0);
                        for (Entity e : center.getWorld().getNearbyEntities(center, 1.5, 1, 1.5)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.addPotionEffect(new PotionEffect(ef("poison"), 100, 3, false, true, true));
                                le.addPotionEffect(new PotionEffect(ef("wither"), 60, 1, false, true, true));
                                center.getWorld().playSound(center, Sound.ENTITY_WITCH_HURT, 1, 1);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 5);
                return true;
            }
        });

        // ===== THE TU (3 skills) =====

        // 13. Thiết Bích - Khien giap
        register(new Skill(plugin, "thietbich", "Thiết Bích",
                "Giảm 80% sát thương nhận vào trong 5 giây", ClassType.THE_TU, 20000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                data.addBuff("thietbich", 5000);
                player.addPotionEffect(new PotionEffect(ef("resistance"), 100, 4, false, true, true));
                player.getWorld().spawnParticle(Particle.POOF, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
                player.sendMessage(plugin.prefix() + "§7🛡 Thiết Bích! §fKhiên không phá trong 5 giây!");
                return true;
            }
        });

        // 14. Hộ Pháp Trận - Buff phong thu dong doi
        register(new Skill(plugin, "hophaptran", "Hộ Pháp Trận",
                "Tạo khiên bảo vệ đồng đội 6m trong 8 giây", ClassType.THE_TU, 30000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int count = 0;
                for (Entity e : player.getNearbyEntities(6, 6, 6)) {
                    if (e instanceof Player ally) {
                        ally.addPotionEffect(new PotionEffect(ef("resistance"), 160, 2, false, true, true));
                        ally.addPotionEffect(new PotionEffect(ef("absorption"), 160, 3, false, true, true));
                        ally.sendMessage(plugin.prefix() + "§7🛡 Hộ Pháp Trận gia trì! Giảm sát thương!");
                        count++;
                    }
                }
                player.addPotionEffect(new PotionEffect(ef("resistance"), 160, 2, false, true, true));
                player.addPotionEffect(new PotionEffect(ef("absorption"), 160, 3, false, true, true));
                spawnCircleParticles(player.getLocation(), 6, Particle.ENCHANT);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.6f);
                player.sendMessage(plugin.prefix() + "§7✦ Hộ Pháp Trận! §fBảo vệ " + (count + 1) + " người!");
                return true;
            }
        });

        // 15. Phản Đòn - Phan don sát thương
        register(new Skill(plugin, "phandon", "Phản Đòn",
                "Đòn tiếp theo phản đòn lại 200% sát thương", ClassType.THE_TU, 18000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                data.setCounterBuff(true);
                data.addBuff("phandon", 8000);
                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1.5f);
                player.sendMessage(plugin.prefix() + "§7⚡ Phản Đòn! §fSẵn sàng phản đòn đòn tiếp theo!");
                return true;
            }
        });

        // ===== QUY TU (3 skills) =====

        // 16. Quỷ Ảnh Tàng - Vo hinh
        register(new Skill(plugin, "quyanhtan", "Quỷ Ảnh Tàng",
                "Vô hình trong 6 giây, tấn công đầu tiên gây x2 sát thương", ClassType.QUY_TU, 20000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                player.addPotionEffect(new PotionEffect(ef("invisibility"), 120, 0, false, false, false));
                data.setInvisible(true);
                data.addBuff("stealth_bonus", 8000); // Bonus sát thương khi tan cong tu tang hinh
                // An item khi tang hinh
                player.getEquipment().setHelmet(null);
                player.getEquipment().setChestplate(null);
                player.getEquipment().setLeggings(null);
                player.getEquipment().setBoots(null);
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
                player.sendMessage(plugin.prefix() + "§5👻 Quỷ Ảnh Tàng! §dTàng hình trong 6 giây!");
                return true;
            }
        });

        // 17. Hư Không Đoạt - Teleport den ke thu va burst
        register(new Skill(plugin, "hukhongdoat", "Hư Không Đoạt",
                "Dịch chuyển tức thời đến kẻ thù 10m, gây sát thương lớn", ClassType.QUY_TU, 15000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                LivingEntity target = getNearestEnemy(player, 10.0);
                if (target == null) {
                    player.sendMessage(plugin.prefix() + "§cKhông có mục tiêu trong 10 blocks!");
                    return false;
                }
                // Hieu ung bien mat
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.3, 0.5, 0.3, 0.5);
                player.teleport(target.getLocation().add(0, 0.5, 0));

                double damage = 18.0 * data.calculateDamageMultiplier();
                if (data.hasBuff("stealth_bonus")) {
                    damage *= 2.0; // Burst khi tang hinh
                    player.sendMessage(plugin.prefix() + "§5💀 Hư Không Đoạt! §dTấn công tàng hình gây x2!");
                }
                target.damage(damage, player);

                // Hieu ung xuat hien
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.3, 0.5, 0.3, 0.5);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
                player.sendMessage(plugin.prefix() + "§5⚡ Hư Không Đoạt! §dGây " + (int) damage + " sát thương!");
                return true;
            }
        });

        // 18. Tử Ảnh - Hut sinh mang
        register(new Skill(plugin, "tuanh", "Tử Ảnh",
                "Hút sinh mạng kẻ thù 4m, hồi phục 40% sát thương gây ra", ClassType.QUY_TU, 22000, 3) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                double totalDamage = 0;
                double damage = 10.0 * data.calculateDamageMultiplier();
                for (Entity e : player.getNearbyEntities(4, 4, 4)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(damage, player);
                        totalDamage += damage;
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, le.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                    }
                }
                double heal = totalDamage * 0.4;
                if (heal > 0) {
                    player.setHealth(Math.min(player.getHealth() + heal, player.getMaxHealth()));
                }
                spawnCircleParticles(player.getLocation(), 4, Particle.SOUL_FIRE_FLAME);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 1.5f);
                player.sendMessage(plugin.prefix() + "§5💀 Tử Ảnh! §dHút " + (int) totalDamage + " sát thương, hồi " + (int) heal + " máu!");
                return true;
            }
        });

        // ===== PHONG TU (4 skills) =====

        // 19. Phong Tốc Đột Kích - Lao vao ke thu
        register(new Skill(plugin, "phongtocthiet", "Phong Tốc Đột Kích",
                "Lao về phía trước 6m xuyên qua kẻ, gây sát thương mọi kẻ trúng", ClassType.PHONG_TU, 10000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Vector dir = player.getEyeLocation().getDirection().normalize().multiply(0.8);
                double damage = 10.0 * data.calculateDamageMultiplier();
                Set<UUID> hit = new HashSet<>();

                for (int i = 0; i < 8; i++) {
                    final int step = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setVelocity(dir.clone());
                            Location pos = player.getLocation();
                            player.getWorld().spawnParticle(Particle.CRIT, pos, 3, 0.2, 0.2, 0.2, 0);
                            for (Entity e : player.getNearbyEntities(1.2, 1.5, 1.2)) {
                                if (e instanceof LivingEntity le && e != player && !hit.contains(e.getUniqueId())) {
                                    le.damage(damage, player);
                                    hit.add(e.getUniqueId());
                                }
                            }
                        }
                    }.runTaskLater(plugin, i * 2L);
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.8f);
                player.sendMessage(plugin.prefix() + "§b💨 Phong Tốc Đột Kích!");
                return true;
            }
        });

        // 20. Toàn Phong Né - Dodge aura
        register(new Skill(plugin, "toanphongne", "Toàn Phong Né",
                "Tăng 70% né đòn trong 6 giây (random né)", ClassType.PHONG_TU, 20000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                data.setDodgeBuff(true);
                data.addBuff("dodge", 6000);
                player.addPotionEffect(new PotionEffect(ef("speed"), 120, 2, false, true, true));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1, 1.8f);
                player.sendMessage(plugin.prefix() + "§b🌪 Toàn Phong Né! §7Né đòn trong 6 giây (70% né)!");
                return true;
            }
        });

        // 21. Phong Nhẫn - Luoi gio
        register(new Skill(plugin, "phongnhan", "Phong Nhẫn",
                "Phóng lưỡi gió cắt xa 8m gây sát thương cao", ClassType.PHONG_TU, 12000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                double damage = 14.0 * data.calculateDamageMultiplier();
                Location origin = player.getEyeLocation();
                Vector dir = origin.getDirection().normalize();

                // Phong luoi gio
                for (int i = 1; i <= 16; i++) {
                    final int step = i;
                    final double finalDmg = damage;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location pos = origin.clone().add(dir.clone().multiply(step * 0.5));
                            player.getWorld().spawnParticle(Particle.CLOUD, pos, 4, 0.2, 0.2, 0.2, 0.05);
                            player.getWorld().spawnParticle(Particle.CRIT, pos, 1, 0, 0, 0, 0);
                            for (Entity e : player.getWorld().getNearbyEntities(pos, 0.7, 0.7, 0.7)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(finalDmg, player);
                                    le.setVelocity(dir.clone().multiply(0.5).add(new Vector(0, 0.3, 0)));
                                    cancel();
                                }
                            }
                        }
                    }.runTaskLater(plugin, i * 1L);
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 2.0f);
                player.sendMessage(plugin.prefix() + "§b🌀 Phong Nhẫn! §7Lưỡi gió phóng ra!");
                return true;
            }
        });

        // 22. Cuồng Phong Trận - AoE gio lon
        register(new Skill(plugin, "cuongphongtran", "Cuồng Phong Trận",
                "Tạo xoáy ốc gió lớn AoE 5m, đẩy lên và gây sát thương 4 giây", ClassType.PHONG_TU, 35000, 3) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Location center = player.getLocation();
                double damage = 6.0 * data.calculateDamageMultiplier();
                player.sendMessage(plugin.prefix() + "§b🌪 Cuồng Phong Trận! §7Xoáy ốc gió bùng phát!");
                player.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 1.8f);

                new BukkitRunnable() {
                    int ticks = 0;
                    double angle = 0;
                    @Override
                    public void run() {
                        if (ticks >= 80) { cancel(); return; }
                        ticks++;
                        angle += 15;
                        // Xoan oc particles
                        for (int layer = 1; layer <= 4; layer++) {
                            double rad = Math.toRadians(angle + (layer * 90));
                            Location p = center.clone().add(
                                Math.cos(rad) * layer, layer * 0.3, Math.sin(rad) * layer);
                            center.getWorld().spawnParticle(Particle.CLOUD, p, 2, 0.1, 0.1, 0.1, 0.05);
                        }
                        // Gay sát thương moi 10 ticks
                        if (ticks % 10 == 0) {
                            for (Entity e : center.getWorld().getNearbyEntities(center, 5, 5, 5)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(damage, player);
                                    le.setVelocity(le.getVelocity().add(new Vector(0, 0.4, 0)));
                                }
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            }
        });

        // ===== HOA TU (4 skills) =====

        // 23. Nghiệp Hỏa Thiêu - Dot lua
        register(new Skill(plugin, "nghiephoathieu", "Nghiệp Hỏa Thiêu",
                "Đốt cháy kẻ thù trong 5m, lửa nghiệp gây sát thương DoT 8 giây", ClassType.HOA_TU, 12000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int count = 0;
                for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.setFireTicks(160); // 8 giay
                        le.addPotionEffect(new PotionEffect(ef("wither"), 80, 1, false, true, true));
                        le.damage(8.0 * data.calculateDamageMultiplier(), player);
                        player.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
                        count++;
                    }
                }
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 40, 1, 0.5, 1, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2f, 0.5f);
                player.sendMessage(plugin.prefix() + "§6🔥 Nghiệp Hỏa Thiêu! §7Đốt cháy " + count + " kẻ!");
                return true;
            }
        });

        // 24. Hỏa Long - Phong long lua
        register(new Skill(plugin, "hoalong", "Hỏa Long",
                "Triệu hồi rồng lửa bay thẳng 10m gây sát thương", ClassType.HOA_TU, 18000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                double damage = 16.0 * data.calculateDamageMultiplier();

                player.getWorld().playSound(start, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 1.5f);
                player.sendMessage(plugin.prefix() + "§6🐉 Hỏa Long! §7Long lửa vụt lên!");

                Set<UUID> hit = new HashSet<>();
                new BukkitRunnable() {
                    int step = 0;
                    @Override
                    public void run() {
                        if (step >= 20) { cancel(); return; }
                        Location pos = start.clone().add(dir.clone().multiply(step * 0.5));
                        // Long lua effect
                        player.getWorld().spawnParticle(Particle.FLAME, pos, 8, 0.3, 0.3, 0.3, 0.05);
                        player.getWorld().spawnParticle(Particle.LAVA, pos, 3, 0.2, 0.2, 0.2, 0);
                        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, pos, 3, 0.2, 0.2, 0.2, 0.02);

                        for (Entity e : player.getWorld().getNearbyEntities(pos, 1.2, 1.2, 1.2)) {
                            if (e instanceof LivingEntity le && e != player && !hit.contains(e.getUniqueId())) {
                                le.damage(damage, player);
                                le.setFireTicks(100);
                                hit.add(e.getUniqueId());
                            }
                        }
                        step++;
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            }
        });

        // 25. Phong Hỏa Trận - Tran dia lua
        register(new Skill(plugin, "phonghoa", "Phong Hỏa Trận",
                "Đặt trận địa lửa 6m, đốt cháy liên tục 6 giây", ClassType.HOA_TU, 25000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Location center = player.getLocation();
                double damage = 5.0 * data.calculateDamageMultiplier();
                player.getWorld().playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.7f);
                player.sendMessage(plugin.prefix() + "§6☄ Phong Hỏa Trận! §7Lửa trận lan ra!");

                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 120) { cancel(); return; } // 6 giay
                        ticks += 5;
                        for (double angle = 0; angle < 360; angle += 20) {
                            double rad = Math.toRadians(angle + ticks * 2);
                            Location p = center.clone().add(Math.cos(rad) * 3, 0, Math.sin(rad) * 3);
                            center.getWorld().spawnParticle(Particle.FLAME, p, 2, 0.2, 0.1, 0.2, 0.02);
                        }
                        // Gay sát thương
                        for (Entity e : center.getWorld().getNearbyEntities(center, 6, 2, 6)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.damage(damage, player);
                                le.setFireTicks(60);
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 5);
                return true;
            }
        });

        // ===== SKILL DAC BIET MOI CLASS =====

        // S1. KIEM TU - Kiem Tien Phap: Thu hut Linh Thach tu khong khi
        register(new Skill(plugin, "kiemtienphap", "Kiếm Tiền Pháp",
                "Dùng kiếm khí thu hút Linh Thạch từ linh lực xung quanh (5m, 3 giây)", ClassType.KIEM_TU, 60000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int stageBonus = data.getStage().ordinal() + 1;
                double damage = 6.0 * data.calculateDamageMultiplier();
                final int[] total = {0};

                player.sendMessage(plugin.prefix() + "§6⚔ Kiếm Tiền Pháp! §7Thu hút linh lực...");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);

                new BukkitRunnable() {
                    int tick = 0;
                    @Override
                    public void run() {
                        if (tick >= 6) {
                            // Trao Tien
                            int earned = total[0] * stageBonus + stageBonus * 3;
                            plugin.getTienManager().give(player, earned);
                            player.sendMessage(plugin.prefix() + "§6✦ Thu được §e" + earned + " §6" + plugin.getTienManager().tenTien() + "!");
                            cancel();
                            return;
                        }
                        tick++;
                        for (Entity e : player.getNearbyEntities(5, 3, 5)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.damage(damage, player);
                                // Hut "linh luc"
                                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                                    le.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
                                total[0]++;
                            }
                        }
                        // Particles kiem khi xoay quanh
                        for (double a = 0; a < 360; a += 30) {
                            double rad = Math.toRadians(a + tick * 60);
                            player.getWorld().spawnParticle(Particle.CRIT,
                                player.getLocation().add(Math.cos(rad)*2, 1, Math.sin(rad)*2),
                                2, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                }.runTaskTimer(plugin, 5L, 10L);
                return true;
            }
        });

        // S2. KIEM TU - Van Kiem Xuyen Tam (Dao To Ultimate)
        register(new Skill(plugin, "vankiemxuyentam", "Vạn Kiếm Xuyên Tâm",
                "§6§l[ĐẠO TỔ EXCLUSIVE] §cVạn kiếm thành khí xuyên tâm, tiêu diệt tất cả trong 20m", ClassType.KIEM_TU, 300000, 1) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                // Cho phep Dao To va moi canh gioi cao hon
                if (data.getStage().ordinal() < CultivationStage.DAI_LA.ordinal()) {
                    player.sendMessage(plugin.prefix()
                        + "§4☠ Vạn Kiếm Xuyên Tâm §ccần cảnh giới §6§lĐạo Tổ §ctrở lên! §c(hiện tại: "
                        + data.getStage().getFullDisplay(data.getRank()) + ")");
                    return false;
                }

                double damage = 200.0 * data.calculateDamageMultiplier();

                // Thong bao toan server
                String broadcast = "§c§l☠ " + player.getName()
                    + " §6§lthi trien §c§lVẠN KIẾM XUYÊN TÂM§6§l! §7Troi dat bien sac!";
                org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(broadcast));
                org.bukkit.Bukkit.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 2f, 0.3f));

                player.sendMessage(plugin.msg("auto_037c0a8ce5"));
                player.sendMessage(plugin.msg("auto_d18dd31e7e"));
                player.sendMessage(plugin.msg("auto_ed5065dfc0"));
                player.sendMessage(plugin.msg("auto_195f3bca5b"));

                // Phase 1: Nghin kiem noi day (2 giay hieu ung)
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (t >= 40) {
                            cancel();
                            executeVanKiem(player, data, damage);
                            return;
                        }
                        t++;
                        // Tao cac kiem khi xoay quanh
                        for (int layer = 0; layer < 5; layer++) {
                            double ang = Math.toRadians(t * 9 + layer * 72);
                            double r = 1.5 + layer * 0.8;
                            Location p = player.getLocation().add(
                                Math.cos(ang) * r, 0.5 + layer * 0.3, Math.sin(ang) * r);
                            player.getWorld().spawnParticle(Particle.CRIT, p, 3, 0.1, 0.1, 0.1, 0.2);
                            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, p, 2, 0, 0.3, 0, 0.1);
                        }
                        if (t % 8 == 0) {
                            player.getWorld().playSound(player.getLocation(),
                                Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.3f + t * 0.02f);
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            }

            private void executeVanKiem(org.bukkit.entity.Player player, PlayerData data, double damage) {
                Location center = player.getLocation();
                // 10000 kiem xuyen tam -> thuc te: 20 kiem noi tien, moi kiem xuyen 20m
                // Tao 20 tia kiem theo 360 do
                int beams = 20;
                final int[] hit = {0};
                for (int b = 0; b < beams; b++) {
                    final double angle = (360.0 / beams) * b;
                    final double radians = Math.toRadians(angle);
                    final Vector dir = new Vector(Math.cos(radians), 0, Math.sin(radians));

                    new BukkitRunnable() {
                        int step = 0;
                        @Override
                        public void run() {
                            if (step >= 40) { cancel(); return; } // 20m max
                            step++;
                            Location pos = center.clone().add(dir.clone().multiply(step * 0.5));
                            // Particles kiem khi
                            player.getWorld().spawnParticle(Particle.CRIT, pos, 4, 0.1, 0.2, 0.1, 0.3);
                            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, pos, 2, 0, 0.2, 0, 0.1);

                            for (Entity e : player.getWorld().getNearbyEntities(pos, 0.7, 1, 0.7)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(damage, player);
                                    le.setNoDamageTicks(0); // Bo qua invincibility frame
                                    hit[0]++;
                                    player.getWorld().spawnParticle(Particle.EXPLOSION,
                                        le.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
                                }
                            }
                        }
                    }.runTaskTimer(plugin, b * 1L, 1L);
                }

                // Hieu ung vu no cuoi
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.2f);
                    player.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 2f, 0.5f);
                    for (double a = 0; a < 360; a += 3) {
                        double rad = Math.toRadians(a);
                        Location p = center.clone().add(Math.cos(rad) * 10, 0, Math.sin(rad) * 10);
                        center.getWorld().spawnParticle(Particle.EXPLOSION, p, 1, 0, 0, 0, 0);
                    }
                    // Thuong Tien vi dung biet ky Dao To
                    int ltReward = 5000 + hit[0] * 100;
                    plugin.getTienManager().give(player, ltReward);
                    player.sendMessage(plugin.prefix() + "§6✦ Vạn Kiếm gây sát thương! Nhận §e" + ltReward + " §6" + plugin.getTienManager().tenTien() + "!");
                }, 45L);
            }
        });

        // S3. PHAP TU - Thai Cuc Huyen Phap: Tai Chi AoE heal+damage
        register(new Skill(plugin, "thaicuchuanphap", "Thái Cực Huyền Pháp",
                "Tạo vòng Thái Cực 8m: hồi máu đồng đội, gây sát thương kẻ thù 6 giây", ClassType.PHAP_TU, 45000, 3) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                Location center = player.getLocation();
                double damage = 8.0 * data.calculateDamageMultiplier();
                double healPerTick = player.getMaxHealth() * 0.05;

                player.sendMessage(plugin.prefix() + "§9☯ Thái Cực Huyền Pháp! §7Âm dương hòa hợp!");
                player.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.6f);

                new BukkitRunnable() {
                    int tick = 0;
                    double rotAngle = 0;
                    @Override
                    public void run() {
                        if (tick >= 120) { cancel(); return; } // 6 giay
                        tick++;
                        rotAngle += 3;

                        // Vong Am (xanh - heal)
                        for (double a = 0; a < 180; a += 15) {
                            double rad = Math.toRadians(a + rotAngle);
                            Location p = center.clone().add(Math.cos(rad)*6, 0.1, Math.sin(rad)*6);
                            center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, p, 2, 0.1, 0.1, 0.1, 0);
                        }
                        // Vong Duong (do - damage)
                        for (double a = 180; a < 360; a += 15) {
                            double rad = Math.toRadians(a + rotAngle);
                            Location p = center.clone().add(Math.cos(rad)*6, 0.1, Math.sin(rad)*6);
                            center.getWorld().spawnParticle(Particle.FLAME, p, 2, 0.1, 0.1, 0.1, 0.02);
                        }

                        if (tick % 10 == 0) {
                            for (Entity e : center.getWorld().getNearbyEntities(center, 8, 4, 8)) {
                                if (e instanceof Player ally && e != player) {
                                    // Dong doi: hoi mau
                                    ally.setHealth(Math.min(ally.getHealth() + healPerTick, ally.getMaxHealth()));
                                } else if (e instanceof LivingEntity le && !(e instanceof Player)) {
                                    // Ke thu: sát thương
                                    le.damage(damage, player);
                                }
                            }
                            player.setHealth(Math.min(player.getHealth() + healPerTick, player.getMaxHealth()));
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            }
        });

        // S4. Y TU - Hoi Duong Cuu Nghich: Buff hoi sinh cho dong doi
        register(new Skill(plugin, "hoiduongcuunghich", "Hồi Dương Cứu Nghịch",
                "Trao bùa hộ mệnh cho đồng đội 5m: khi HP về 0 tự hồi phục 50% máu 1 lần", ClassType.Y_TU, 90000, 4) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                int buffed = 0;
                for (Entity e : player.getNearbyEntities(5, 5, 5)) {
                    if (e instanceof Player ally) {
                        ally.addPotionEffect(new PotionEffect(ef("absorption"), 400, 4, false, true, true));
                        ally.addPotionEffect(new PotionEffect(ef("regeneration"), 200, 2, false, true, true));
                        plugin.getDataManager().get(ally).addBuff("hoisinhbuff", 20000);
                        ally.sendMessage(plugin.prefix() + "§a☀ Hồi Dương Cứu Nghịch gia hộ! HP về 0 sẽ hồi phục!");
                        ally.playSound(ally.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0,2,0), 15, 0.5, 0.3, 0.5, 0);
                        buffed++;
                    }
                }
                // Self
                player.addPotionEffect(new PotionEffect(ef("absorption"), 400, 4, false, true, true));
                player.addPotionEffect(new PotionEffect(ef("regeneration"), 200, 2, false, true, true));
                plugin.getDataManager().get(player).addBuff("hoisinhbuff", 20000);
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0,2,0), 20, 0.5, 0.5, 0.5, 0);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
                player.sendMessage(plugin.prefix() + "§a☀ Hồi Dương Cứu Nghịch! §7Bùa hộ mệnh cho " + (buffed+1) + " người!");
                return true;
            }
        });

        // S5. DOC TU - Van Doc Chi The: Bien than thanh doc the
        register(new Skill(plugin, "vandocchithe", "Vạn Độc Chi Thể",
                "Biến thân thành linh độc: miễn dịch mọi hiệu ứng xấu, kẻ gần bị độc, +30% sát thương trong 10s", ClassType.DOC_TU, 60000, 3) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                // Xoa tat ca hieu ung xau truoc
                for (var effect : player.getActivePotionEffects()) {
                    player.removePotionEffect(effect.getType());
                }
                // Buff manh
                player.addPotionEffect(new PotionEffect(ef("poison"), 1, 0, false, false, false)); // Fake - miễn dịch trick
                player.addPotionEffect(new PotionEffect(ef("strength"), 200, 2, false, true, true));
                data.addBuff("vandoc_immunity", 10000);
                data.addBuff("vandoc_aura", 10000);

                player.sendMessage(plugin.prefix() + "§2☠ Vạn Độc Chi Thể! §7Thân thể hóa thành linh độc!");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 1.5f);

                new BukkitRunnable() {
                    int tick = 0;
                    @Override
                    public void run() {
                        if (!data.hasBuff("vandoc_aura") || !player.isOnline()) { cancel(); return; }
                        tick++;
                        // Particles doc toa ra
                        for (double a = 0; a < 360; a += 20) {
                            double rad = Math.toRadians(a + tick*5);
                            Location p = player.getLocation().add(Math.cos(rad)*2, 0.5, Math.sin(rad)*2);
                            player.getWorld().spawnParticle(Particle.WITCH, p, 2, 0.1, 0.1, 0.1, 0);
                        }
                        // Doc nhe ke xung quanh 3m
                        for (Entity e : player.getNearbyEntities(3, 2, 3)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.addPotionEffect(new PotionEffect(ef("poison"), 40, 1, false, true, true));
                                le.addPotionEffect(new PotionEffect(ef("slowness"), 40, 1, false, true, true));
                            }
                        }
                    }
                }.runTaskTimer(plugin, 0, 5);
                return true;
            }
        });

        // S6. THE TU - Kim Cuong Bat Hoai: Bat tu 5 giay
        register(new Skill(plugin, "kimcuongbathoai", "Kim Cương Bất Hoại",
                "Biến thân thành Kim Cương: miễn dịch 100% sát thương trong 5 giây", ClassType.THE_TU, 90000, 4) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                data.addBuff("kimcuong_invincible", 5000);
                player.addPotionEffect(new PotionEffect(ef("resistance"), 100, 255, false, false, false));
                player.addPotionEffect(new PotionEffect(ef("absorption"), 100, 4, false, true, true));

                player.sendMessage(plugin.prefix() + "§7💎 Kim Cương Bất Hoại! §fKim cuong bat hoai trong 5 giay!");
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
                player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1f, 0.8f);

                // Hieu ung kim cuong
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        if (!data.hasBuff("kimcuong_invincible") || t >= 100) {
                            // Het buff: thong bao
                            if (player.isOnline()) player.sendMessage(plugin.prefix() + "§7Kim Cương Bất Hoại đã hết!");
                            cancel(); return;
                        }
                        t++;
                        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                            player.getLocation().add(0, 1, 0), 8, 0.4, 0.6, 0.4, 0.1);
                        player.getWorld().spawnParticle(Particle.ENCHANT,
                            player.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, 1.0);
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            }
        });

        // S7. QUY TU - Quy Vuong Giang The: Trieu hoi quy than
        register(new Skill(plugin, "quyvuonggianthe", "Quỷ Vương Giáng Thế",
                "Triệu hồi Quỷ Vương: triệu 5 phantom tấn công kẻ thù 8 giây", ClassType.QUY_TU, 75000, 4) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                double damage = 12.0 * data.calculateDamageMultiplier();
                player.sendMessage(plugin.prefix() + "§5👻 Quỷ Vương Giáng Thế! §dNgũ quỷ xuất thế!");
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);

                // Tao 5 phantom entities
                for (int i = 0; i < 5; i++) {
                    final int idx = i;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // Spawn phantom o vi tri quanh player
                        double ang = Math.toRadians(idx * 72);
                        Location spawnLoc = player.getLocation().add(Math.cos(ang)*3, 1, Math.sin(ang)*3);
                        Phantom phantom = (Phantom) player.getWorld().spawnEntity(spawnLoc, EntityType.PHANTOM);
                        phantom.setCustomName("§5Quỷ Thần #" + (idx+1));
                        phantom.setCustomNameVisible(true);
                        phantom.setTarget(null); // Se tim muc tieu tu dong

                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, spawnLoc.add(0,1,0), 20, 0.5, 0.5, 0.5, 0.05);

                        // Sau 8 giay: xoa phantom
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            if (phantom.isValid()) phantom.remove();
                        }, 160L);

                        // Kiem soat phantom tan cong ke gan
                        new BukkitRunnable() {
                            int t = 0;
                            @Override
                            public void run() {
                                if (t >= 80 || !phantom.isValid()) { cancel(); return; }
                                t++;
                                if (phantom.getTarget() == null) {
                                    // Tim muc tieu gan nhat
                                    for (Entity e : phantom.getNearbyEntities(8, 4, 8)) {
                                        if (e instanceof LivingEntity le && !(e instanceof Player)) {
                                            phantom.setTarget(le);
                                            break;
                                        }
                                    }
                                }
                                // Gay sát thương cho ke gan
                                for (Entity e : phantom.getNearbyEntities(2, 2, 2)) {
                                    if (e instanceof LivingEntity le && !(e instanceof Player)) {
                                        le.damage(damage * 0.3, player);
                                    }
                                }
                            }
                        }.runTaskTimer(plugin, 5L, 10L);
                    }, idx * 5L);
                }
                return true;
            }
        });

        // S8. PHONG TU - Pha Khong Thuan Di: Dich chuyen nhanh den diem nhin
        register(new Skill(plugin, "phakhongthuandi", "Phá Không Thuấn Di",
                "Xích đến điểm đang nhìn trong 30m tức thì, nổ sát thương AoE tại điểm đến", ClassType.PHONG_TU, 20000, 2) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                // Raycast den vi tri dang nhin
                Location target = player.getTargetBlock(null, 30).getLocation().add(0.5, 1, 0.5);
                Location from = player.getLocation();
                double distance = from.distance(target);

                if (distance < 2) {
                    player.sendMessage(plugin.prefix() + "§cQuá gần! Nhìn xa hơn để dịch chuyển."); return false;
                }

                // Hieu ung o vi tri cu
                player.getWorld().spawnParticle(Particle.CLOUD, from.add(0,1,0), 20, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 2f);

                // Teleport
                player.teleport(target);

                // Hieu ung o vi tri moi + no AoE
                player.getWorld().spawnParticle(Particle.CRIT, target, 15, 0.5, 0.5, 0.5, 0);
                player.getWorld().spawnParticle(Particle.CLOUD, target, 20, 0.5, 0.5, 0.5, 0.2);
                player.getWorld().playSound(target, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 2f);

                double damage = 14.0 * data.calculateDamageMultiplier();
                for (Entity e : player.getNearbyEntities(4, 3, 4)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(damage, player);
                        le.setVelocity(le.getVelocity().add(new Vector(
                            (le.getLocation().getX() - target.getX()) * 0.4,
                            0.4,
                            (le.getLocation().getZ() - target.getZ()) * 0.4)));
                    }
                }
                player.sendMessage(plugin.prefix() + "§b💨 Phá Không Thuấn Di! §7Bay xa §e" + (int)distance + "m!");
                return true;
            }
        });

        // S9. HOA TU - Phuong Hoang Trong Sinh: Tu hoi sinh 1 lan
        register(new Skill(plugin, "phuonghoangtrognsinh", "Phượng Hoàng Trọng Sinh",
                "Khi HP về 0 lần tới, tự động hồi sinh với 80% HP + bắt lửa kẻ thù xung quanh (30 giây buff)", ClassType.HOA_TU, 180000, 5) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                data.addBuff("phuonghoang_rebirth", 30000);
                player.addPotionEffect(new PotionEffect(ef("fire_resistance"), 600, 0, false, true, true));

                // Hieu ung lua bao quanh
                player.getWorld().spawnParticle(Particle.FLAME,
                    player.getLocation().add(0,1,0), 60, 1, 0.5, 1, 0.1);
                player.getWorld().spawnParticle(Particle.LAVA,
                    player.getLocation().add(0,1,0), 20, 0.5, 0.5, 0.5, 0);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1.8f);

                player.sendMessage(plugin.prefix() + "§6🔥 Phượng Hoàng Trọng Sinh! §7Phượng Hoàng linh vũ trở về!");
                player.sendTitle(plugin.msg("auto_0bf6b53f4a"), plugin.msg("auto_2e5b8335b2"), 10, 60, 20);

                // Trong thoi gian buff, moi 5 tick dot khi tat ca ke xung quanh
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!data.hasBuff("phuonghoang_rebirth") || !player.isOnline()) { cancel(); return; }
                        for (Entity e : player.getNearbyEntities(4, 3, 4)) {
                            if (e instanceof LivingEntity le && e != player) {
                                le.setFireTicks(60);
                            }
                        }
                        player.getWorld().spawnParticle(Particle.FLAME,
                            player.getLocation().add(0,0.5,0), 5, 0.5, 0.3, 0.5, 0.05);
                    }
                }.runTaskTimer(plugin, 0, 5);
                return true;
            }
        });

        // 26. Tuyệt Địa Hỏa Hải (ULTIMATE) - Bung no lon nhat
        register(new Skill(plugin, "tuyetdiahoahai", "Tuyệt Địa Hỏa Hải",
                "§c§l[ULTIMATE] §6Biệt kỹ Hỏa Tu! Lửa thiên địa nguyên xuất hiện, tiêu diệt toàn bộ kẻ thù", ClassType.HOA_TU, 120000, 6) {
            @Override
            public boolean execute(org.bukkit.entity.Player player, PlayerData data) {
                // Kiem tra canh gioi toi thieu
                if (data.getStage().ordinal() < 2) {
                    player.sendMessage(plugin.prefix() + "§cCần cảnh giới §eNguyên Anh §ctrở lên để sử dụng Tuyệt Địa!");
                    return false;
                }

                Location center = player.getLocation();
                double damage = 50.0 * data.calculateDamageMultiplier();

                player.sendMessage(plugin.msg("auto_ab53beb0ed"));
                player.sendMessage(plugin.msg("auto_612f2f95e9"));
                player.sendMessage(plugin.msg("auto_ae8e8ccb25"));
                player.sendMessage(plugin.msg("auto_1b6d202d6f"));

                // Phase 1: Giao huong (2 giay)
                new BukkitRunnable() {
                    int tick = 0;
                    @Override
                    public void run() {
                        if (tick >= 40) { cancel(); phase2(player, center, damage); return; }
                        tick++;
                        double r = tick * 0.4;
                        for (double ang = 0; ang < 360; ang += 15) {
                            double rad = Math.toRadians(ang + tick * 5);
                            Location p = center.clone().add(Math.cos(rad) * r, 0, Math.sin(rad) * r);
                            center.getWorld().spawnParticle(Particle.FLAME, p, 3, 0.1, 0.1, 0.1, 0.03);
                        }
                        if (tick % 10 == 0) {
                            center.getWorld().playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.5f + tick * 0.02f);
                        }
                    }
                }.runTaskTimer(plugin, 0, 1);
                return true;
            }

            private void phase2(org.bukkit.entity.Player player, Location center, double damage) {
                // Phase 2: Bung no
                center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2f, 0.5f);
                center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.3f);

                // Tao vu no theo tang song
                for (int wave = 1; wave <= 5; wave++) {
                    final int w = wave;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            double radius = w * 3;
                            // Particles bao no
                            for (double angle = 0; angle < 360; angle += 5) {
                                double rad = Math.toRadians(angle);
                                Location p = center.clone().add(Math.cos(rad) * radius, 0, Math.sin(rad) * radius);
                                center.getWorld().spawnParticle(Particle.EXPLOSION, p, 1, 0, 0, 0, 0);
                                center.getWorld().spawnParticle(Particle.LAVA, p, 5, 0.2, 0.5, 0.2, 0.1);
                                center.getWorld().spawnParticle(Particle.FLAME, p, 8, 0.2, 0.2, 0.2, 0.1);
                            }
                            // Gay sát thương ke thu trong song no
                            double dmgReduced = damage * (1.0 - (w - 1) * 0.15);
                            for (Entity e : center.getWorld().getNearbyEntities(center, radius + 1, 5, radius + 1)) {
                                if (e instanceof LivingEntity le && e != player) {
                                    le.damage(dmgReduced, player);
                                    le.setFireTicks(200);
                                    le.setVelocity(new Vector(
                                        (le.getLocation().getX() - center.getX()) * 0.3,
                                        0.5, (le.getLocation().getZ() - center.getZ()) * 0.3));
                                }
                            }
                            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f - w * 0.1f);
                        }
                    }.runTaskLater(plugin, wave * 5L);
                }
            }
        });
        // Dang ky tat ca combo skills (16 per class)
        registerAllComboSkills();
        plugin.getLogger().info("[SkillManager] Loaded " + skills.size() + " skills.");
    }

    // ==========================================
    //  UTILITY METHODS
    // ==========================================

    private LivingEntity getNearestEnemy(org.bukkit.entity.Player player, double range) {
        LivingEntity nearest = null;
        double nearest_dist = range * range;
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity le && e != player) {
                if (le instanceof Player) continue; // Khong tan cong player khac tru pvp
                double dist = player.getLocation().distanceSquared(le.getLocation());
                if (dist < nearest_dist) {
                    nearest = le;
                    nearest_dist = dist;
                }
            }
        }
        // Neu khong co mob, tim player (pvp)
        if (nearest == null) {
            for (Entity e : player.getNearbyEntities(range, range, range)) {
                if (e instanceof Player target && e != player) {
                    double dist = player.getLocation().distanceSquared(target.getLocation());
                    if (dist < nearest_dist) {
                        nearest = target;
                        nearest_dist = dist;
                    }
                }
            }
        }
        return nearest;
    }

    private void spawnCircleParticles(Location center, double radius, Particle particle) {
        for (double angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            Location p = center.clone().add(Math.cos(rad) * radius, 0.05, Math.sin(rad) * radius);
            center.getWorld().spawnParticle(particle, p, 3, 0.1, 0.1, 0.1, 0);
        }
    }

    // ============================================================
    //  COMBO SKILLS - DAY DU 16 SKILL / CLASS
    // ============================================================

    private void registerAllComboSkills() {
        // Called from registerAll()

        // ===== KIEM TU COMBO =====

        register(new Skill(plugin, "kiemkhiphan", "Kiếm Khí Phản",
                "Phản đòn sát thương - kích hoạt khiên trong 3 giây", ClassType.KIEM_TU, 18000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double m = data.calculateDamageMultiplier();
                data.addBuff("kiem_phan_reflect", 3000);
                player.addPotionEffect(new PotionEffect(ef("resistance"), 60, 1, false, true, true));
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> data.removeBuff("kiem_phan_reflect"), 60L);
                player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, player.getLocation().add(0,1,0), 20, 0.5,0.5,0.5,0.2);
                player.sendMessage(plugin.prefix() + "§b⚔ Kiếm Khí Phản! §7Phan 30% sát thương 3 giay!");
                return true;
            }
        });

        register(new Skill(plugin, "bat_quan_kiem", "Bát Quái Kiếm",
                "Tạo 8 kiếm khí vòng tròn đâm tất cả kẻ xung quanh", ClassType.KIEM_TU, 16000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 14.0 * data.calculateDamageMultiplier();
                int hits = 0;
                for (Entity e : player.getNearbyEntities(4,3,4)) {
                    if (e instanceof LivingEntity le && e != player) { le.damage(dmg,player); hits++; }
                }
                for (double a=0;a<360;a+=45) {
                    double r=Math.toRadians(a);
                    Location p=player.getLocation().add(Math.cos(r)*3,1,Math.sin(r)*3);
                    player.getWorld().spawnParticle(Particle.CRIT,p,5,0.2,0.2,0.2,0.2);
                }
                player.sendMessage(plugin.prefix()+"§c⚔ Bát Quái Kiếm! §7Trung §e"+hits+" §7ke!");
                player.getWorld().playSound(player.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.2f,1f);
                return true;
            }
        });

        register(new Skill(plugin, "kiem_cuong_ba", "Kiếm Cuồng Ba",
                "Cuồng bạo kiếm khí đẩy lùi và gây sát thương lớn", ClassType.KIEM_TU, 20000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 22.0 * data.calculateDamageMultiplier();
                for (Entity e : player.getNearbyEntities(6,3,6)) {
                    if (e instanceof LivingEntity le && e != player) {
                        le.damage(dmg,player);
                        Vector v = le.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(1.5).add(new Vector(0,0.5,0));
                        le.setVelocity(v);
                    }
                }
                player.getWorld().spawnParticle(Particle.CLOUD,player.getLocation().add(0,1,0),30,2,0.5,2,0.15);
                player.sendMessage(plugin.prefix()+"§c⚔ Kiếm Cuồng Ba! §7Đẩy lùi tất cả kẻ xung quanh!");
                return true;
            }
        });

        register(new Skill(plugin, "thien_kiem_trui", "Thiên Kiếm Trụy",
                "Kiếm sáng từ trên trời rơi xuống trên đầu kẻ thù", ClassType.KIEM_TU, 22000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 30.0 * data.calculateDamageMultiplier();
                LivingEntity t = getNearestEnemy(player, 12.0);
                if (t==null){player.sendMessage(plugin.prefix()+"§cKhông có mục tiêu!");return false;}
                Location tLoc = t.getLocation();
                plugin.getServer().getScheduler().runTaskLater(plugin, ()->{
                    t.damage(dmg,player); t.setNoDamageTicks(0);
                    tLoc.getWorld().strikeLightningEffect(tLoc);
                    tLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT,tLoc.add(0,1,0),30,0.5,0.5,0.5,0.2);
                }, 15L);
                // Warning particle
                for(int i=0;i<12;i++){double a=Math.toRadians(i*30);
                    tLoc.getWorld().spawnParticle(Particle.CRIT,tLoc.clone().add(Math.cos(a)*1.5,0.1,Math.sin(a)*1.5),2,0,0,0,0);}
                player.sendMessage(plugin.prefix()+"§c⚔ Thiên Kiếm Trụy! §7Kiem sang se giang xuong!");
                return true;
            }
        });

        register(new Skill(plugin, "van_kiem_tran", "Vạn Kiếm Trận",
                "Đặt trận kiếm quanh người chơi, kẻ xâm nhập bị đâm", ClassType.KIEM_TU, 30000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 12.0 * data.calculateDamageMultiplier();
                Location base = player.getLocation().clone();
                player.sendMessage(plugin.prefix()+"§c⚔ Vạn Kiếm Trận! §7Trận kiếm khởi động 8 giây!");
                new BukkitRunnable(){int t=0;@Override public void run(){
                    if(t++>=160){cancel();return;}
                    if(t%20==0){for(Entity e:base.getWorld().getNearbyEntities(base,4,3,4)){
                        if(e instanceof LivingEntity le&&e!=player){le.damage(dmg,player);
                            le.getWorld().spawnParticle(Particle.CRIT,le.getLocation().add(0,1,0),5,0.2,0.2,0.2,0.2);}}}
                    for(double a=0;a<360;a+=20){double r=Math.toRadians(a+t*4.5);
                        Location p=base.clone().add(Math.cos(r)*3.5,0.5+Math.sin(Math.toRadians(t*3))*0.5,Math.sin(r)*3.5);
                        base.getWorld().spawnParticle(Particle.ENCHANTED_HIT,p,1,0,0,0,0.05);}
                }}.runTaskTimer(plugin,0,1);
                return true;
            }
        });

        register(new Skill(plugin, "nhat_kiem_sat", "Nhất Kiếm Sát",
                "Một kiếm định quyết, sát thương khủng với một kẻ duy nhất", ClassType.KIEM_TU, 25000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 55.0 * data.calculateDamageMultiplier();
                LivingEntity t = getNearestEnemy(player,8.0);
                if(t==null){player.sendMessage(plugin.prefix()+"§cKhông có mục tiêu!");return false;}
                t.damage(dmg,player); t.setNoDamageTicks(0);
                t.getWorld().spawnParticle(Particle.CRIT,t.getLocation().add(0,1,0),40,0.5,0.5,0.5,0.3);
                player.sendMessage(plugin.prefix()+"§c⚔ Nhất Kiếm Sát! §7Gây §e"+(int)dmg+" §7sát thương!");
                player.sendTitle(plugin.msg("auto_da39a3ee5e"), plugin.msg("auto_b440709fc4"),0,20,5);
                return true;
            }
        });

        register(new Skill(plugin, "thien_long_kiem", "Thiên Long Kiếm",
                "Gọi rồng kiếm thiên long tấn công tất cả kẻ trong khu vực", ClassType.KIEM_TU, 35000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 28.0 * data.calculateDamageMultiplier();
                int cnt=0;
                for(Entity e:player.getNearbyEntities(8,4,8)){
                    if(e instanceof LivingEntity le&&e!=player){le.damage(dmg,player);le.setNoDamageTicks(0);cnt++;}
                }
                for(int i=0;i<3;i++){final int d=i*5;plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                    for(double a=0;a<360;a+=15){double r=Math.toRadians(a);
                        Location p=player.getLocation().add(Math.cos(r)*5,1,Math.sin(r)*5);
                        player.getWorld().spawnParticle(Particle.CRIT,p,2,0.2,0.2,0.2,0.1);}
                },d);}
                player.getWorld().playSound(player.getLocation(),Sound.ENTITY_ENDER_DRAGON_GROWL,0.8f,1.5f);
                player.sendMessage(plugin.prefix()+"§c🐉 Thiên Long Kiếm! §7Gây sát thương §e"+cnt+" §7kẻ!");
                return true;
            }
        });

        register(new Skill(plugin, "kiem_dao_vo_thuong", "Kiếm Đạo Vô Thượng",
                "Khai phát kiếm đạo vô thượng, AoE lớn nhất của Kiếm Tu", ClassType.KIEM_TU, 40000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 45.0 * data.calculateDamageMultiplier();
                int cnt=0;
                for(Entity e:player.getNearbyEntities(10,5,10)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.damage(dmg,player);le.setNoDamageTicks(0);
                        le.addPotionEffect(new PotionEffect(ef("weakness"),100,2,false,true,true));cnt++;}
                }
                player.sendTitle(plugin.msg("auto_bb1a964081"),"§e"+cnt+" ke trung chieu!",5,40,10);
                player.getWorld().playSound(player.getLocation(),Sound.ENTITY_ENDER_DRAGON_GROWL,1f,0.5f);
                return true;
            }
        });

        register(new Skill(plugin, "kiem_thoat_the", "Kiếm Thoát Thể",
                "Hóa thân thành kiếm quang, dịch chuyển nhanh và gây sát thương dọc đường", ClassType.KIEM_TU, 20000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 18.0 * data.calculateDamageMultiplier();
                Location start = player.getEyeLocation();
                Vector dir = start.getDirection().normalize();
                java.util.Set<java.util.UUID> hit=new java.util.HashSet<>();
                new BukkitRunnable(){int s=0;@Override public void run(){
                    if(s++>=25){cancel();return;}
                    Location pos=start.clone().add(dir.clone().multiply(s*0.6));
                    player.teleport(pos.clone().subtract(0,1,0));
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,pos,3,0.2,0.2,0.2,0.1);
                    for(Entity e:player.getWorld().getNearbyEntities(pos,1,1.5,1)){
                        if(e instanceof LivingEntity le&&e!=player&&!hit.contains(e.getUniqueId())){
                            le.damage(dmg,player);hit.add(e.getUniqueId());}
                    }
                }}.runTaskTimer(plugin,0,1);
                player.sendMessage(plugin.prefix()+"§b⚔ Kiếm Thoát Thể! §7Bay xuyên qua kẻ thù!");
                return true;
            }
        });

        register(new Skill(plugin, "nguyen_shi_kiem", "Nguyên Thủy Kiếm",
                "Kiếm cổ cực mạnh, phá vỡ mọi phòng thủ DEF giảm 70%", ClassType.KIEM_TU, 30000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 35.0 * data.calculateDamageMultiplier();
                for(Entity e:player.getNearbyEntities(5,3,5)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.damage(dmg,player);le.setNoDamageTicks(0);
                        le.addPotionEffect(new PotionEffect(ef("weakness"),120,4,false,true,true));}
                }
                player.sendMessage(plugin.prefix()+"§6⚔ Nguyên Thủy Kiếm! §7Phá DEF kẻ thù!");
                return true;
            }
        });

        register(new Skill(plugin, "kiem_he_pho_tien", "Kiếm Hệ Phổ Thiên",
                "[ULTIMATE] Kiếm Thánh Giới - hủy diệt toàn bộ kẻ trong 15m", ClassType.KIEM_TU, 60000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 80.0 * data.calculateDamageMultiplier();
                int cnt=0;
                for(Entity e:player.getNearbyEntities(15,8,15)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.damage(dmg,player);le.setNoDamageTicks(0);le.setFireTicks(200);cnt++;}
                }
                final int finalCnt1=cnt;
                Bukkit.getOnlinePlayers().forEach(pp->pp.sendMessage(
                    plugin.prefix()+"§c§l⚡ "+player.getName()+" thi triển §f§lKIẾM HỆ PHỔ THIÊN§c§l! "+finalCnt1+" kẻ bị tiêu diệt!"));
                player.getWorld().playSound(player.getLocation(),Sound.ENTITY_ENDER_DRAGON_DEATH,1.5f,0.3f);
                player.sendTitle(plugin.msg("auto_f445c9d6b1"),"§c"+finalCnt1+" kẻ trúng!",5,60,10);
                return true;
            }
        });

        // ===== PHAP TU COMBO =====

        register(new Skill(plugin, "loi_hoa_phap", "Lôi Hỏa Pháp",
                "Kết hợp lửa và sét, AoE lớn vừa đốt vừa sét đánh", ClassType.PHAP_TU, 18000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 18.0 * data.calculateDamageMultiplier();
                for(Entity e:player.getNearbyEntities(6,3,6)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.damage(dmg,player);le.setFireTicks(60);
                        player.getWorld().strikeLightningEffect(le.getLocation());}
                }
                player.sendMessage(plugin.prefix()+"§e⚡🔥 Lôi Hỏa Pháp! §7Sét và lửa bao quanh!");
                return true;
            }
        });

        register(new Skill(plugin, "thuy_long_phap", "Thủy Long Pháp",
                "Gọi rồng nước tấn công và làm chậm kẻ thù", ClassType.PHAP_TU, 22000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 16.0 * data.calculateDamageMultiplier();
                for(Entity e:player.getNearbyEntities(7,3,7)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.damage(dmg,player);
                        le.addPotionEffect(new PotionEffect(ef("slowness"),80,3,false,true,true));}
                }
                player.getWorld().spawnParticle(Particle.SNOWFLAKE,player.getLocation().add(0,1,0),60,2,1,2,0.1);
                player.sendMessage(plugin.prefix()+"§9🌊 Thủy Long Pháp! §7Rồng nước làm chậm kẻ thù!");
                return true;
            }
        });

        register(new Skill(plugin, "ngu_hanh_tran", "Ngũ Hành Trận",
                "Đặt trận ngũ hành liên tục gây sát thương mọi loại nguyên tố", ClassType.PHAP_TU, 35000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 10.0 * data.calculateDamageMultiplier();
                Location base=player.getLocation().clone();
                player.sendMessage(plugin.prefix()+"§d✦ Ngũ Hành Trận! §7Tran phep 10 giay!");
                new BukkitRunnable(){int t=0;@Override public void run(){
                    if(t++>=200){cancel();return;}
                    if(t%25==0){for(Entity e:base.getWorld().getNearbyEntities(base,5,3,5)){
                        if(e instanceof LivingEntity le&&e!=player){
                            le.damage(dmg,player);
                            le.addPotionEffect(new PotionEffect(ef("poison"),40,1,false,true,true));}}}
                    if(t%5==0){double a=Math.toRadians(t*18);
                        Location p=base.clone().add(Math.cos(a)*4,0.5,Math.sin(a)*4);
                        Particle[] ps={Particle.FLAME,Particle.SNOWFLAKE,Particle.ENCHANTED_HIT,Particle.WITCH,Particle.CRIT};
                        base.getWorld().spawnParticle(ps[t/40%5],p,3,0.2,0.2,0.2,0.05);}
                }}.runTaskTimer(plugin,0,1);
                return true;
            }
        });

        register(new Skill(plugin, "thien_di_phap", "Thiên Địa Pháp",
                "Pháp môn thiên địa, AoE cao nhất khu vực lớn nhất", ClassType.PHAP_TU, 40000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 35.0 * data.calculateDamageMultiplier();
                int cnt=0;
                for(Entity e:player.getNearbyEntities(12,6,12)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.damage(dmg,player);le.setNoDamageTicks(0);cnt++;}
                }
                player.sendTitle(plugin.msg("auto_efa71cf989"),"§7"+cnt+" kẻ trúng!",5,40,10);
                player.getWorld().playSound(player.getLocation(),Sound.ENTITY_LIGHTNING_BOLT_THUNDER,1.5f,0.3f);
                return true;
            }
        });

        register(new Skill(plugin, "linh_vu_bao_phong", "Linh Vũ Bão Phong",
                "Mưa linh pháp vũ bão trút vào kẻ thù liên tục trong 5 giây", ClassType.PHAP_TU, 25000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 8.0 * data.calculateDamageMultiplier();
                player.sendMessage(plugin.prefix()+"§9⚡ Linh Vũ Bão Phong! §7Mưa pháp vũ bắt đầu!");
                new BukkitRunnable(){int t=0;@Override public void run(){
                    if(t++>=100){cancel();return;}
                    if(t%10==0){LivingEntity tgt=getNearestEnemy(player,10.0);
                        if(tgt!=null){tgt.damage(dmg,player);
                            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,tgt.getLocation().add(0,1,0),8,0.3,0.3,0.3,0.1);}}
                }}.runTaskTimer(plugin,0,1);
                return true;
            }
        });

        register(new Skill(plugin, "cuong_loi_phap", "Cuồng Lôi Pháp",
                "Phát 5 tia sét liên tiếp vào kẻ thù gần nhất", ClassType.PHAP_TU, 20000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 20.0 * data.calculateDamageMultiplier();
                for(int i=0;i<5;i++){final int d=i*8;plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                    LivingEntity t=getNearestEnemy(player,12.0);
                    if(t!=null){t.damage(dmg*0.6,player);t.setNoDamageTicks(0);
                        player.getWorld().strikeLightningEffect(t.getLocation());}
                },d);}
                player.sendMessage(plugin.prefix()+"§e⚡ Cuồng Lôi Pháp! §75 tia sét liên tiếp!");
                return true;
            }
        });

        register(new Skill(plugin, "phap_tac_vu", "Pháp Tắc Vũ",
                "Vũ trụ pháp tắc: giảm 40% sát thương kẻ thù trong 6 giây", ClassType.PHAP_TU, 28000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                for(Entity e:player.getNearbyEntities(8,4,8)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.addPotionEffect(new PotionEffect(ef("weakness"),120,2,false,true,true));
                        le.addPotionEffect(new PotionEffect(ef("slowness"),120,1,false,true,true));}
                }
                player.sendMessage(plugin.prefix()+"§9✦ Pháp Tắc Vũ! §7Giảm sát thương kẻ thù 6 giây!");
                return true;
            }
        });

        register(new Skill(plugin, "thien_mang_phap", "Thiên Mang Pháp",
                "Pháp môn thiên mạng, sát thương cao nhất 1 kèm kẻ đơn nhất", ClassType.PHAP_TU, 30000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 70.0 * data.calculateDamageMultiplier();
                LivingEntity t=getNearestEnemy(player,10.0);
                if(t==null){player.sendMessage(plugin.prefix()+"§cKhông có mục tiêu!");return false;}
                t.damage(dmg,player);t.setNoDamageTicks(0);
                player.sendMessage(plugin.prefix()+"§9§l⚡ Thiên Mang Pháp! §7Gây §e"+(int)dmg+" §9sát thương!");
                return true;
            }
        });

        register(new Skill(plugin, "linh_vuc_khai_mo", "Linh Vực Khai Mở",
                "Mở linh vực bao quanh, tăng ATK và DEF cho bản thân 8 giây", ClassType.PHAP_TU, 25000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                player.addPotionEffect(new PotionEffect(ef("strength"),160,2,false,true,true));
                player.addPotionEffect(new PotionEffect(ef("resistance"),160,1,false,true,true));
                player.addPotionEffect(new PotionEffect(ef("speed"),160,1,false,true,true));
                for(double a=0;a<360;a+=12){double r=Math.toRadians(a);
                    Location p=player.getLocation().add(Math.cos(r)*2.5,0.5,Math.sin(r)*2.5);
                    player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,p,2,0.1,0.1,0.1,0.05);}
                player.sendMessage(plugin.prefix()+"§9✦ Linh Vực Khai Mở! §7Triple buff 8 giây!");
                return true;
            }
        });

        register(new Skill(plugin, "phap_tu_thoai_thu", "Pháp Tổ Thoái Thủ",
                "Thoát khỏi vòng vây và tạo màn hình ngăn kẻ thù", ClassType.PHAP_TU, 20000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                Vector backward = player.getLocation().getDirection().multiply(-3);
                backward.setY(0.5);
                player.setVelocity(backward);
                player.addPotionEffect(new PotionEffect(ef("speed"),100,3,false,true,true));
                for(Entity e:player.getNearbyEntities(5,3,5)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.addPotionEffect(new PotionEffect(ef("blindness"),60,0,false,true,true));}
                }
                player.getWorld().spawnParticle(Particle.CLOUD,player.getLocation(),20,1,0.5,1,0.1);
                player.sendMessage(plugin.prefix()+"§9✦ Pháp Tổ Thoái Thủ! §7Thoát & làm mù kẻ thù!");
                return true;
            }
        });

        register(new Skill(plugin, "ngu_sac_shen_lei", "Ngũ Sắc Thần Lôi",
                "[ULTIMATE] Ngũ sắc thần lôi gây sát thương khủng AoE", ClassType.PHAP_TU, 50000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 50.0 * data.calculateDamageMultiplier();
                int cnt=0;
                for(int wave=0;wave<5;wave++){final int w=wave;final double d=dmg*(1-w*0.1);
                    plugin.getServer().getScheduler().runTaskLater(plugin,()->{
                        for(Entity e:player.getNearbyEntities(10,5,10)){
                            if(e instanceof LivingEntity le&&e!=player){le.damage(d,player);le.setNoDamageTicks(0);}}
                        player.getWorld().strikeLightningEffect(player.getLocation().add((Math.random()-0.5)*6,0,(Math.random()-0.5)*6));
                    },wave*8L);}
                player.sendTitle(plugin.msg("auto_84ae2c583d"), plugin.msg("auto_f835c2273a"),5,50,10);
                player.getWorld().playSound(player.getLocation(),Sound.ENTITY_LIGHTNING_BOLT_THUNDER,2f,0.3f);
                return true;
            }
        });

        register(new Skill(plugin, "van_phap_qui_tong", "Vạn Pháp Quy Tông",
                "[ULTIMATE] Hội tụ vạn pháp, AoE 20m cực cao", ClassType.PHAP_TU, 60000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double dmg = 90.0 * data.calculateDamageMultiplier();
                int cnt=0;
                for(Entity e:player.getNearbyEntities(20,10,20)){
                    if(e instanceof LivingEntity le&&e!=player){
                        le.damage(dmg,player);le.setNoDamageTicks(0);cnt++;}
                }
                final int finalCnt2=cnt;
                Bukkit.getOnlinePlayers().forEach(pp->pp.sendMessage(
                    plugin.prefix()+"§9§l✦ "+player.getName()+" thi triển §f§lVẠN PHÁP QUY TÔNG§9§l! "+finalCnt2+" kẻ!"));
                player.sendTitle(plugin.msg("auto_9a9fdb21de"),"§7"+finalCnt2+" kẻ trúng!",5,60,10);
                return true;
            }
        });

        // ===== Y TU COMBO =====

        register(new Skill(plugin, "hoi_sinh_phap", "Hồi Sinh Pháp",
                "Hồi phục 50% HP ngay lập tức", ClassType.Y_TU, 30000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                double heal=player.getMaxHealth()*0.5;
                player.setHealth(Math.min(player.getHealth()+heal,player.getMaxHealth()));
                player.getWorld().spawnParticle(Particle.HEART,player.getLocation().add(0,2,0),15,0.5,0.5,0.5,0);
                player.sendMessage(plugin.prefix()+"§a💊 Hồi Sinh Pháp! §7Hồi phục §e"+(int)heal+" §amáu!");
                return true;
            }
        });

        register(new Skill(plugin, "cam_lo_vu", "Cam Lộ Vũ",
                "Mưa cam lộ hồi phục cả đội trong 5 giây", ClassType.Y_TU, 35000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                player.sendMessage(plugin.prefix()+"§a🌿 Cam Lộ Vũ! §7Mưa hồi phục 5 giây!");
                new BukkitRunnable(){int t=0;@Override public void run(){
                    if(t++>=5){cancel();return;}
                    for(Entity e:player.getNearbyEntities(8,4,8)){
                        if(e instanceof Player ally){double h=ally.getMaxHealth()*0.05;
                            ally.setHealth(Math.min(ally.getHealth()+h,ally.getMaxHealth()));
                            ally.getWorld().spawnParticle(Particle.HEART,ally.getLocation().add(0,2,0),2,0.3,0.3,0.3,0);}}
                    double hs=player.getMaxHealth()*0.05;
                    player.setHealth(Math.min(player.getHealth()+hs,player.getMaxHealth()));
                }}.runTaskTimer(plugin,0,20);
                return true;
            }
        });

        register(new Skill(plugin, "linh_dan_bao_tri", "Linh Đan Bảo Trị",
                "Bọ vào trạng thái bảo vệ, giảm 50% sát thương 5 giây", ClassType.Y_TU, 25000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                player.addPotionEffect(new PotionEffect(ef("resistance"),100,2,false,true,true));
                player.addPotionEffect(new PotionEffect(ef("regeneration"),100,2,false,true,true));
                player.sendMessage(plugin.prefix()+"§a🛡 Linh Đan Bảo Trị! §7Giảm 50% sát thương + regen 5s!");
                return true;
            }
        });

        register(new Skill(plugin, "phuc_hoa_lan", "Phục Hoa Lan",
                "Hồi phục toàn bộ HP cho 1 ally gần nhất", ClassType.Y_TU, 40000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                Player ally=getNearestAlly(player,10.0);
                if(ally==null){double h=player.getMaxHealth()*0.8;
                    player.setHealth(Math.min(player.getHealth()+h,player.getMaxHealth()));
                    player.sendMessage(plugin.prefix()+"§a🌸 Phục Hoa Lan! §7(Tự hồi phục)");return true;}
                ally.setHealth(ally.getMaxHealth());
                ally.addPotionEffect(new PotionEffect(ef("regeneration"),100,2,false,true,true));
                ally.getWorld().spawnParticle(Particle.HEART,ally.getLocation().add(0,2,0),20,0.5,0.5,0.5,0);
                ally.sendMessage(plugin.prefix()+"§a🌸 Phục Hoa Lan cứu bạn! HP đầy lại!");
                player.sendMessage(plugin.prefix()+"§a🌸 Phục Hoa Lan → §e"+ally.getName());
                return true;
            }
        });

        register(new Skill(plugin, "san_sinh_chi_thu", "San Sinh Chi Thuật",
                "Xóa tất cả hiệu ứng xấu + miễn dịch 6 giây", ClassType.Y_TU, 30000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                for(var eff:player.getActivePotionEffects()){
                    boolean bad=java.util.Arrays.asList("poison","wither","slowness","weakness","blindness","hunger").contains(eff.getType().getKey().getKey());
                    if(bad)player.removePotionEffect(eff.getType());
                }
                player.addPotionEffect(new PotionEffect(ef("absorption"),120,3,false,true,true));
                player.sendMessage(plugin.prefix()+"§a✦ San Sinh Chi Thuật! §7Xóa hiệu ứng xấu!");
                return true;
            }
        });

        register(new Skill(plugin, "thien_y_shen_shou", "Thiên Y Thần Thủ",
                "Hồi phục 100% HP cho bản thân + regen III 10 giây", ClassType.Y_TU, 50000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                player.setHealth(player.getMaxHealth());
                player.addPotionEffect(new PotionEffect(ef("regeneration"),200,2,false,true,true));
                player.addPotionEffect(new PotionEffect(ef("absorption"),200,4,false,true,true));
                player.getWorld().spawnParticle(Particle.HEART,player.getLocation().add(0,1.5,0),30,0.8,0.8,0.8,0);
                player.sendMessage(plugin.prefix()+"§a§l✦ Thiên Y Thần Thủ! §7HP day lai + Regen 10 giay!");
                return true;
            }
        });

        register(new Skill(plugin, "linh_y_bao_than", "Linh Y Bảo Thân",
                "Khiên bất tử 3 giây khi HP xuống thấp", ClassType.Y_TU, 60000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                data.addBuff("bachngoc_invincible",3000);
                player.addPotionEffect(new PotionEffect(ef("resistance"),60,255,false,false,false));
                player.addPotionEffect(new PotionEffect(ef("regeneration"),60,4,false,true,true));
                player.sendMessage(plugin.prefix()+"§a§l💊 Linh Y Bảo Thân! §fBất tử + Regen V 3s!");
                return true;
            }
        });

        register(new Skill(plugin, "van_duoc_gui_yuan", "Vạn Dược Quy Nguyên",
                "Hồi phục 30% HP cho tất cả đồng đội trong 15m", ClassType.Y_TU, 45000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                int cnt=0;
                for(Entity e:player.getNearbyEntities(15,8,15)){
                    if(e instanceof Player ally){double h=ally.getMaxHealth()*0.3;
                        ally.setHealth(Math.min(ally.getHealth()+h,ally.getMaxHealth()));
                        ally.getWorld().spawnParticle(Particle.HEART,ally.getLocation().add(0,2,0),8,0.4,0.4,0.4,0);cnt++;}
                }
                player.setHealth(Math.min(player.getHealth()+player.getMaxHealth()*0.3,player.getMaxHealth()));
                player.sendMessage(plugin.prefix()+"§a✦ Vạn Dược Quy Nguyên! §7Hồi phục "+cnt+" đồng đội!");
                return true;
            }
        });

        register(new Skill(plugin, "y_dao_shen_thu", "Y Đạo Thần Thuật",
                "Tăng max HP và heal tốc độ cho toàn đội trong 30 giây", ClassType.Y_TU, 40000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                for(Entity e:player.getNearbyEntities(10,5,10)){
                    if(e instanceof Player ally){
                        ally.addPotionEffect(new PotionEffect(ef("health_boost"),600,3,false,true,true));
                        ally.addPotionEffect(new PotionEffect(ef("regeneration"),600,1,false,true,true));}
                }
                player.addPotionEffect(new PotionEffect(ef("health_boost"),600,3,false,true,true));
                player.addPotionEffect(new PotionEffect(ef("regeneration"),600,1,false,true,true));
                player.sendMessage(plugin.prefix()+"§a✦ Y Đạo Thần Thuật! §7HP max tăng cho toàn đội 30s!");
                return true;
            }
        });

        register(new Skill(plugin, "hua_tuo_zhi_shu", "Hoa Đà Chi Thuật",
                "Hồi phục toàn bộ đồng đội + bỏ tất cả hiệu ứng xấu", ClassType.Y_TU, 55000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                int cnt=0;
                for(Entity e:player.getNearbyEntities(12,6,12)){
                    if(e instanceof Player ally){
                        ally.setHealth(ally.getMaxHealth());
                        for(var eff:ally.getActivePotionEffects()){
                            if(java.util.Arrays.asList("poison","wither","slowness","weakness","blindness").contains(eff.getType().getKey().getKey()))
                                ally.removePotionEffect(eff.getType());}
                        ally.sendMessage(plugin.prefix()+"§a💊 Hoa Đà Chi Thuật cứu sống!");cnt++;}
                }
                player.sendMessage(plugin.prefix()+"§a§l✦ Hoa Đà Chi Thuật! §7Cứu "+cnt+" đồng đội!");
                return true;
            }
        });

        register(new Skill(plugin, "linh_y_zhi_jing", "Linh Y Chi Cảnh",
                "[ULTIMATE] Mở linh y cảnh giới, heal và buff cực mạnh", ClassType.Y_TU, 70000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                for(Entity e:player.getNearbyEntities(20,10,20)){
                    if(e instanceof Player ally){
                        ally.setHealth(ally.getMaxHealth());
                        ally.addPotionEffect(new PotionEffect(ef("regeneration"),600,4,false,true,true));
                        ally.addPotionEffect(new PotionEffect(ef("absorption"),600,4,false,true,true));
                        ally.addPotionEffect(new PotionEffect(ef("resistance"),600,2,false,true,true));
                        ally.addPotionEffect(new PotionEffect(ef("strength"),600,2,false,true,true));
                        ally.sendMessage(plugin.prefix()+"§a§l✦ Linh Y Chi Cảnh! Toàn bộ stat max hóa!");}
                }
                player.sendTitle(plugin.msg("auto_3e2f372b56"), plugin.msg("auto_410569b52b"),5,60,10);
                Bukkit.getOnlinePlayers().forEach(p->p.sendMessage(plugin.prefix()+"§a§l"+player.getName()+" khai triển LINH Y CHI CẢNH!"));
                return true;
            }
        });

        register(new Skill(plugin, "bao_shen_shen_yi", "Bảo Thần Thần Y",
                "[ULTIMATE] Y Tu cao nhất - hồi sinh toàn tập nếu chết trong 30s", ClassType.Y_TU, 80000, 1) {
            @Override public boolean execute(Player player, PlayerData data) {
                data.addBuff("phabao_rebirth",30000);
                player.addPotionEffect(new PotionEffect(ef("absorption"),600,4,false,true,true));
                player.addPotionEffect(new PotionEffect(ef("regeneration"),600,2,false,true,true));
                player.sendMessage(plugin.prefix()+"§a§l💊 Bảo Thần Thần Y! §7Hồi sinh tự động nếu chết trong 30s!");
                return true;
            }
        });

        // ===== DOC TU COMBO =====
        registerDocTuCombo();
        // ===== THE TU COMBO =====
        registerTheTuCombo();
        // ===== QUY TU COMBO =====
        registerQuyTuCombo();
        // ===== PHONG TU COMBO =====
        registerPhongTuCombo();
        // ===== HOA TU COMBO =====
        registerHoaTuCombo();
    }

    private void registerDocTuCombo() {
        register(new Skill(plugin,"doc_vu_phong","Độc Vũ Phong","Mưa độc đổ dồn lên kẻ thù trong 5m",ClassType.DOC_TU,18000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=10*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(5,3,5)){if(e instanceof LivingEntity le&&e!=p){le.addPotionEffect(new PotionEffect(ef("poison"),120,3,false,true,true));le.damage(dmg,p);}}p.getWorld().spawnParticle(Particle.WITCH,p.getLocation().add(0,1,0),30,2,1,2,0.1);p.sendMessage(plugin.prefix()+"§2☠ Độc Vũ Phong!");return true;}});
        register(new Skill(plugin,"thanh_doc_bao","Thanh Độc Bạo","Nổ độc xanh AoE lớn",ClassType.DOC_TU,22000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=22*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(7,4,7)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);le.addPotionEffect(new PotionEffect(ef("poison"),100,3,false,true,true));}}p.sendMessage(plugin.prefix()+"§2☠ Thanh Độc Bạo! §7Nổ độc AoE!");return true;}});
        register(new Skill(plugin,"am_sat_doc_tran","Ám Sát Độc Trận","Đặt trận độc vô hình 10 giây",ClassType.DOC_TU,28000,1){@Override public boolean execute(Player p,PlayerData d){final Location base=p.getLocation().clone();new BukkitRunnable(){int t=0;@Override public void run(){if(t++>=200){cancel();return;}if(t%20==0)for(Entity e:base.getWorld().getNearbyEntities(base,3,2,3)){if(e instanceof LivingEntity le&&e!=p){le.addPotionEffect(new PotionEffect(ef("poison"),120,4,false,true,true));le.addPotionEffect(new PotionEffect(ef("weakness"),60,2,false,true,true));}}if(t%10==0)base.getWorld().spawnParticle(Particle.WITCH,base,2,0.3,0.1,0.3,0);}}.runTaskTimer(plugin,0,1);p.sendMessage(plugin.prefix()+"§2☠ Ám Sát Độc Trận!");return true;}});
        register(new Skill(plugin,"doc_lung_bao_ba","Độc Long Bạo Phát","Gọi rồng độc tấn công",ClassType.DOC_TU,30000,1){@Override public boolean execute(Player p,PlayerData d){final double dmg=25*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(8,4,8)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.addPotionEffect(new PotionEffect(ef("poison"),150,4,false,true,true));}}p.getWorld().spawnParticle(Particle.WITCH,p.getLocation().add(0,1,0),50,2,1,2,0.1);p.sendMessage(plugin.prefix()+"§2🐍 Độc Long Bạo Phát!");return true;}});
        register(new Skill(plugin,"doc_shen_fa","Độc Thần Pháp","Tăng sát thương độc lên 3x trong 10 giây",ClassType.DOC_TU,25000,1){@Override public boolean execute(Player p,PlayerData d){d.addBuff("doc_amplify",10000);p.addPotionEffect(new PotionEffect(ef("speed"),200,2,false,true,true));p.sendMessage(plugin.prefix()+"§2✦ Độc Thần Pháp! §7Độc tăng 3x 10 giây!");return true;}});
        register(new Skill(plugin,"shi_ling_du_gong","Thế Linh Độc Công","Tấn công kết hợp di chuyển nhanh + gây độc",ClassType.DOC_TU,20000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("speed"),100,3,false,true,true));LivingEntity t=getNearestEnemy(p,8.0);if(t!=null){t.damage(18*d.calculateDamageMultiplier(),p);t.addPotionEffect(new PotionEffect(ef("poison"),120,3,false,true,true));}p.sendMessage(plugin.prefix()+"§2⚡ Thế Linh Độc Công!");return true;}});
        register(new Skill(plugin,"doc_zhu_shen_zhen","Độc Chú Thần Trận","Chú nguyền làm kẻ tự gây sát thương cho nhau",ClassType.DOC_TU,40000,1){@Override public boolean execute(Player p,PlayerData d){for(Entity e:p.getNearbyEntities(8,4,8)){if(e instanceof LivingEntity le&&e!=p){le.addPotionEffect(new PotionEffect(ef("glowing"),200,0,false,true,true));le.addPotionEffect(new PotionEffect(ef("poison"),200,4,false,true,true));}}p.sendMessage(plugin.prefix()+"§5✦ Độc Chú Thần Trận! §7Chú nguyền tất cả kẻ thù!");return true;}});
        register(new Skill(plugin,"du_jing_wan_xiang","Độc Kinh Vạn Tượng","Phóng độc kính gây damage toàn vùng",ClassType.DOC_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){final double dmg=15*d.calculateDamageMultiplier();new BukkitRunnable(){int t=0;@Override public void run(){if(t++>=10){cancel();return;}for(Entity e:p.getNearbyEntities(6,3,6)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.addPotionEffect(new PotionEffect(ef("poison"),60,2,false,true,true));}}}}.runTaskTimer(plugin,0,20);p.sendMessage(plugin.prefix()+"§2✦ Độc Kinh Vạn Tượng! §710 sóng độc!");return true;}});
        register(new Skill(plugin,"tian_du_zhi_ti","Thiên Độc Chi Thể","Thể thân độc, thu sát thương lại tăng ATK",ClassType.DOC_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){d.addBuff("doc_body",8000);p.addPotionEffect(new PotionEffect(ef("strength"),160,3,false,true,true));p.addPotionEffect(new PotionEffect(ef("resistance"),160,1,false,true,true));p.sendMessage(plugin.prefix()+"§2✦ Thiên Độc Chi Thể! §7Nhận sát thương = tăng ATK!");return true;}});
        register(new Skill(plugin,"wan_du_gui_yi","Vạn Độc Quy Yi","Thu hút tất cả độc từ mob xung quanh thành lực",ClassType.DOC_TU,30000,1){@Override public boolean execute(Player p,PlayerData d){int cnt=0;for(Entity e:p.getNearbyEntities(10,5,10)){if(e instanceof LivingEntity le&&!(e instanceof Player)){le.damage(20*d.calculateDamageMultiplier(),p);cnt++;}}p.setHealth(Math.min(p.getHealth()+cnt*3,p.getMaxHealth()));p.sendMessage(plugin.prefix()+"§2✦ Vạn Độc Quy Yi! §7Hút lực "+cnt+" mob!");return true;}});
        register(new Skill(plugin,"du_zun_wu_shang","Độc Tôn Vô Thượng","[ULTIMATE] Độc bá tỏa ra gây sát thương kinh hoàng",ClassType.DOC_TU,60000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=60*d.calculateDamageMultiplier();int cnt=0;for(Entity e:p.getNearbyEntities(12,6,12)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);le.addPotionEffect(new PotionEffect(ef("poison"),300,4,false,true,true));le.addPotionEffect(new PotionEffect(ef("wither"),200,3,false,true,true));cnt++;}}p.sendTitle(plugin.msg("auto_d0bd67cac4"),"§7"+cnt+" kẻ trúng!",5,60,10);final int docCnt=cnt;Bukkit.getOnlinePlayers().forEach(pp->pp.sendMessage(plugin.prefix()+"§2§l"+p.getName()+" khai triển ĐỘC TÔN VÔ THƯỢNG! "+docCnt+" kẻ!"));return true;}});
    }

    private void registerTheTuCombo() {
        register(new Skill(plugin,"gang_ti_shen_ti","Cương Thể Thần Thể","Tăng DEF và max HP",ClassType.THE_TU,15000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("resistance"),200,2,false,true,true));p.addPotionEffect(new PotionEffect(ef("health_boost"),200,3,false,true,true));p.sendMessage(plugin.prefix()+"§7⚙ Cương Thể! §7DEF+HP tăng!");return true;}});
        register(new Skill(plugin,"kim_zhong_zhao","Kim Chung Triệu","Tạo khiên kim loại bao bọc",ClassType.THE_TU,20000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("resistance"),120,3,false,true,true));p.addPotionEffect(new PotionEffect(ef("absorption"),120,4,false,true,true));p.sendMessage(plugin.prefix()+"§7🔔 Kim Chung Triệu! §7Khiên kim loại 6s!");return true;}});
        register(new Skill(plugin,"gang_shan_ya","Cương Sơn Áp","Nhảy lên và đè nặng xuống đạp chân kẻ thù",ClassType.THE_TU,22000,1){@Override public boolean execute(Player p,PlayerData d){p.setVelocity(new Vector(0,1.5,0));plugin.getServer().getScheduler().runTaskLater(plugin,()->{for(Entity e:p.getNearbyEntities(4,2,4)){if(e instanceof LivingEntity le&&e!=p){le.damage(30*d.calculateDamageMultiplier(),p);le.setVelocity(new Vector(0,-0.5,0));}}},15L);p.sendMessage(plugin.prefix()+"§7🏔 Cương Sơn Áp!");return true;}});
        register(new Skill(plugin,"tie_bi_jin_zhong","Thiết Bị Kim Chung","Giảm 80% sát thương nhận vào trong 4 giây",ClassType.THE_TU,28000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("resistance"),80,4,false,true,true));p.addPotionEffect(new PotionEffect(ef("absorption"),80,4,false,true,true));p.sendMessage(plugin.prefix()+"§7🔒 Thiết Bị Kim Chung! §7-80% sát thương 4s!");return true;}});
        register(new Skill(plugin,"bu_huai_jin_shen","Bất Hoại Kim Thân","Khiên bất tử 4 giây",ClassType.THE_TU,40000,1){@Override public boolean execute(Player p,PlayerData d){d.addBuff("bachngoc_invincible",4000);p.addPotionEffect(new PotionEffect(ef("resistance"),80,255,false,false,false));p.sendMessage(plugin.prefix()+"§7💎 Bất Hoại Kim Thân! §fBất tử 4s!");return true;}});
        register(new Skill(plugin,"tian_gang_ti","Thiên Cương Thể","Thể thân thiên cương, ATK+DEF tăng cao",ClassType.THE_TU,30000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("strength"),200,2,false,true,true));p.addPotionEffect(new PotionEffect(ef("resistance"),200,2,false,true,true));p.addPotionEffect(new PotionEffect(ef("haste"),200,2,false,true,true));p.sendMessage(plugin.prefix()+"§7⚙ Thiên Cương Thể! §7Triple buff!");return true;}});
        register(new Skill(plugin,"bu_mie_jin_gang","Bất Diệt Kim Cương","Phản đòn 50% sát thương trong 6 giây",ClassType.THE_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){d.addBuff("vanlinh_reflect",6000);p.addPotionEffect(new PotionEffect(ef("absorption"),120,5,false,true,true));p.sendMessage(plugin.prefix()+"§7💎 Bất Diệt Kim Cương! §7Phản 50% sát thương 6s!");return true;}});
        register(new Skill(plugin,"wan_lu_gui_zong","Vạn Lực Quy Tông","Hút lực xé từ mọi hướng đẩy lùi kẻ thù",ClassType.THE_TU,30000,1){@Override public boolean execute(Player p,PlayerData d){for(Entity e:p.getNearbyEntities(8,4,8)){if(e instanceof LivingEntity le&&e!=p){le.damage(20*d.calculateDamageMultiplier(),p);Vector v=le.getLocation().subtract(p.getLocation()).toVector().normalize().multiply(2).add(new Vector(0,0.6,0));le.setVelocity(v);}}p.sendMessage(plugin.prefix()+"§7⚙ Vạn Lực Quy Tông! §7Đẩy lùi tất cả!");return true;}});
        register(new Skill(plugin,"tian_ti_jing","Thiên Thể Cảnh","Vào trạng thái thiên thể, miễn dịch mọi hiệu ứng xấu 8s",ClassType.THE_TU,40000,1){@Override public boolean execute(Player p,PlayerData d){for(var eff:p.getActivePotionEffects()){if(java.util.Arrays.asList("poison","wither","slowness","weakness").contains(eff.getType().getKey().getKey()))p.removePotionEffect(eff.getType());}p.addPotionEffect(new PotionEffect(ef("saturation"),160,5,false,false,false));d.addBuff("thamtam_immune",8000);p.sendMessage(plugin.prefix()+"§7✦ Thiên Thể Cảnh! §7Miễn dịch hiệu ứng xấu 8s!");return true;}});
        register(new Skill(plugin,"jin_ti_wu_shang","Kim Thể Vô Thượng","Tăng max HP +100% trong 15 giây",ClassType.THE_TU,45000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("health_boost"),300,9,false,true,true));p.addPotionEffect(new PotionEffect(ef("absorption"),300,9,false,true,true));p.setHealth(p.getMaxHealth());p.sendMessage(plugin.prefix()+"§7💎 Kim Thể Vô Thượng! §7Max HP tăng 2x 15s!");return true;}});
        register(new Skill(plugin,"bu_huai_wang_shen","Bất Hoại Vương Thần","Sát thương AoE công phá lên kẻ xung quanh",ClassType.THE_TU,40000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=40*d.calculateDamageMultiplier();int cnt=0;for(Entity e:p.getNearbyEntities(6,3,6)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);cnt++;}}p.sendMessage(plugin.prefix()+"§7§l⚙ Bất Hoại Vương Thần! §7Gây "+cnt+" kẻ sát thương lực!");return true;}});
        register(new Skill(plugin,"wu_shang_jin_ti","Vô Thượng Kim Thể","[ULTIMATE] Vô thượng kim thể - bất tử 8 giây + AoE sát thương",ClassType.THE_TU,80000,1){@Override public boolean execute(Player p,PlayerData d){d.addBuff("bachngoc_invincible",8000);p.addPotionEffect(new PotionEffect(ef("resistance"),160,255,false,false,false));p.addPotionEffect(new PotionEffect(ef("strength"),160,4,false,true,true));int cnt=0;for(Entity e:p.getNearbyEntities(8,4,8)){if(e instanceof LivingEntity le&&e!=p){le.damage(50*d.calculateDamageMultiplier(),p);cnt++;}}p.sendTitle(plugin.msg("auto_fd3fb3f60f"),"§fBất tử 8s + "+cnt+" kẻ!",5,60,10);return true;}});
    }

    private void registerQuyTuCombo() {
        register(new Skill(plugin,"quy_hun_fen_shen","Quỷ Hồn Phân Thân","Tạo phân thân ma gây sát thương",ClassType.QUY_TU,20000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=12*d.calculateDamageMultiplier();plugin.getServer().getScheduler().runTaskLater(plugin,()->{for(Entity e:p.getNearbyEntities(5,3,5)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,le.getLocation().add(0,1,0),5,0.3,0.3,0.3,0.05);}}},20L);p.getWorld().spawnParticle(Particle.PORTAL,p.getLocation().add(0,1,0),20,0.3,0.5,0.3,0.3);p.sendMessage(plugin.prefix()+"§5👻 Quỷ Hồn Phân Thân!");return true;}});
        register(new Skill(plugin,"si_wang_zhu_zhou","Tử Vong Chú Nguyền","Chú nguyền chết chậm lên kẻ thù",ClassType.QUY_TU,25000,1){@Override public boolean execute(Player p,PlayerData d){LivingEntity t=getNearestEnemy(p,10.0);if(t==null){p.sendMessage(plugin.prefix()+"§cKhông có mục tiêu!");return false;}t.addPotionEffect(new PotionEffect(ef("wither"),200,3,false,true,true));t.addPotionEffect(new PotionEffect(ef("slowness"),200,4,false,true,true));t.addPotionEffect(new PotionEffect(ef("weakness"),200,3,false,true,true));p.sendMessage(plugin.prefix()+"§5☠ Tử Vong Chú Nguyền! §7Chú nguyền kẻ thù!");return true;}});
        register(new Skill(plugin,"yin_yue_sha_qi","Âm Nguyệt Sát Khí","Bọc sát khí ám tối, tăng ATK ban đêm",ClassType.QUY_TU,20000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("strength"),200,3,false,true,true));p.addPotionEffect(new PotionEffect(ef("night_vision"),300,0,false,false,false));p.sendMessage(plugin.prefix()+"§5🌑 Âm Nguyệt Sát Khí! §7ATK tang + Night Vision!");return true;}});
        register(new Skill(plugin,"ling_hun_huo","Linh Hồn Hỏa","Phóng lửa linh hồn gây sát thương và hút máu",ClassType.QUY_TU,22000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=18*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(6,3,6)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);double heal=dmg*0.2;p.setHealth(Math.min(p.getHealth()+heal,p.getMaxHealth()));p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,le.getLocation().add(0,1,0),8,0.3,0.5,0.3,0.05);}}p.sendMessage(plugin.prefix()+"§5🔥 Linh Hồn Hỏa! §7Hút máu kẻ thù!");return true;}});
        register(new Skill(plugin,"gui_wang_xia_ling","Quỷ Vương Hạ Lệnh","Lệnh kẻ thù đánh nhau trong 5 giây",ClassType.QUY_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){for(Entity e:p.getNearbyEntities(8,4,8)){if(e instanceof LivingEntity le&&e!=p){le.addPotionEffect(new PotionEffect(ef("weakness"),100,3,false,true,true));le.addPotionEffect(new PotionEffect(ef("glowing"),100,0,false,true,true));}}p.sendMessage(plugin.prefix()+"§5👑 Quỷ Vương Hạ Lệnh! §7Kẻ thù bị debuff nặng!");return true;}});
        register(new Skill(plugin,"gui_di_zhi_yan","Quỷ Địa Chi Nhãn","Mở mắt quỷ, phát hiện tất cả kẻ trong 30m",ClassType.QUY_TU,20000,1){@Override public boolean execute(Player p,PlayerData d){int cnt=0;for(Entity e:p.getNearbyEntities(30,15,30)){if(e instanceof LivingEntity le&&e!=p){le.addPotionEffect(new PotionEffect(ef("glowing"),300,0,false,false,true));cnt++;}}p.sendMessage(plugin.prefix()+"§5👁 Quỷ Địa Chi Nhãn! §7Phát hiện §e"+cnt+" §5kẻ trong 30m!");return true;}});
        register(new Skill(plugin,"si_wang_zhi_yuan","Tử Vong Chi Nguyên","Tử Vong Chi Nguyên - hấp thụ linh khí từ kẻ chết",ClassType.QUY_TU,25000,1){@Override public boolean execute(Player p,PlayerData d){int cnt=0;long lk=0;for(Entity e:p.getNearbyEntities(10,5,10)){if(e instanceof LivingEntity le&&!(e instanceof Player)){le.damage(30*d.calculateDamageMultiplier(),p);lk+=3000;cnt++;}}d.addLinhKhi(lk);plugin.getSkillItemManager().updateInfoItem(p,d);p.sendMessage(plugin.prefix()+"§5✦ Tử Vong Chi Nguyên! §7Thu §e"+lk+" §5LK từ §e"+cnt+" §5mob!");return true;}});
        register(new Skill(plugin,"ling_yu_mo_zhen","Linh Vực Ma Trận","Đặt trận ma vương, kẻ bị ma trận làm lộ vị trí",ClassType.QUY_TU,30000,1){@Override public boolean execute(Player p,PlayerData d){final double dmg=10*d.calculateDamageMultiplier();final Location base=p.getLocation().clone();new BukkitRunnable(){int t=0;@Override public void run(){if(t++>=160){cancel();return;}if(t%20==0)for(Entity e:base.getWorld().getNearbyEntities(base,5,3,5)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.addPotionEffect(new PotionEffect(ef("glowing"),40,0,false,true,true));}}if(t%8==0)for(double a=0;a<360;a+=20){double r=Math.toRadians(a+t*3);base.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,base.clone().add(Math.cos(r)*4,0.5,Math.sin(r)*4),1,0.1,0.1,0.1,0.02);}}}.runTaskTimer(plugin,0,1);p.sendMessage(plugin.prefix()+"§5✦ Linh Vực Ma Trận! §78 giây!");return true;}});
        register(new Skill(plugin,"gui_zun_zhi_fa","Quỷ Tôn Chi Pháp","Quyền năng quỷ tôn bộc phát, sát thương cực cao",ClassType.QUY_TU,40000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=50*d.calculateDamageMultiplier();LivingEntity t=getNearestEnemy(p,10.0);if(t==null){p.sendMessage(plugin.prefix()+"§cKhông có mục tiêu!");return false;}t.damage(dmg,p);t.setNoDamageTicks(0);t.addPotionEffect(new PotionEffect(ef("wither"),200,3,false,true,true));p.sendMessage(plugin.prefix()+"§5👑 Quỷ Tôn Chi Pháp! §7Gây §e"+(int)dmg+" §5sát thương!");return true;}});
        register(new Skill(plugin,"wan_gui_chao_zong","Vạn Quỷ Triều Tông","Gọi vạn quỷ AoE lớn nhất Quỷ Tu",ClassType.QUY_TU,50000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=35*d.calculateDamageMultiplier();int cnt=0;for(Entity e:p.getNearbyEntities(12,6,12)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);le.addPotionEffect(new PotionEffect(ef("wither"),150,2,false,true,true));cnt++;}}p.sendTitle(plugin.msg("auto_c14dc4785e"),"§7"+cnt+" kẻ trúng!",5,50,10);return true;}});
        register(new Skill(plugin,"gui_di_po_jun","Quỷ Đế Phá Quân","Phá tất cả phòng thủ, sát thương bán mạng",ClassType.QUY_TU,55000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=60*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(8,4,8)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);le.addPotionEffect(new PotionEffect(ef("weakness"),200,5,false,true,true));}}p.sendTitle(plugin.msg("auto_b402a02dd4"), plugin.msg("auto_d73d91d91b"),5,50,10);return true;}});
        register(new Skill(plugin,"gui_zu_wu_shang","Quỷ Tổ Vô Thượng","[ULTIMATE] Quỷ Tổ uy thế - AoE và chú nguyền đỉnh cao",ClassType.QUY_TU,80000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=80*d.calculateDamageMultiplier();int cnt=0;for(Entity e:p.getNearbyEntities(15,8,15)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);le.addPotionEffect(new PotionEffect(ef("wither"),300,4,false,true,true));le.addPotionEffect(new PotionEffect(ef("blindness"),200,1,false,true,true));cnt++;}}final int quyCnt=cnt;Bukkit.getOnlinePlayers().forEach(pp->pp.sendMessage(plugin.prefix()+"§5§l"+p.getName()+" khai triển QUỶ TỔ VÔ THƯỢNG! "+quyCnt+" kẻ!"));p.sendTitle(plugin.msg("auto_797bc3737e"),"§7"+cnt+" kẻ bị chú nguyền!",5,60,10);return true;}});
    }

    private void registerPhongTuCombo() {
        register(new Skill(plugin,"phong_liu_hua","Phong Lưu Hoa","Bước phong lưu hóa, né đòn 100% trong 2s",ClassType.PHONG_TU,15000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("speed"),40,5,false,true,true));d.addBuff("phong_dodge_100",2000);p.sendMessage(plugin.prefix()+"§b💨 Phong Lưu Hoa! §7Né đòn 100% 2s!");return true;}});
        register(new Skill(plugin,"bao_feng_shan","Bão Phong Trảm","Chém bão phong đẩy lùi tất cả",ClassType.PHONG_TU,20000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=16*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(7,3,7)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);Vector v=le.getLocation().subtract(p.getLocation()).toVector().normalize().multiply(2).add(new Vector(0,0.5,0));le.setVelocity(v);}}p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation().add(0,1,0),30,2,0.5,2,0.15);p.sendMessage(plugin.prefix()+"§b🌪 Bão Phong Trảm!");return true;}});
        register(new Skill(plugin,"feng_shen_bu","Phong Thần Bộ","Tăng tốc độ di chuyển +100% trong 5s",ClassType.PHONG_TU,18000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("speed"),100,4,false,true,true));p.addPotionEffect(new PotionEffect(ef("jump_boost"),100,3,false,true,true));p.sendMessage(plugin.prefix()+"§b💨 Phong Thần Bộ! §7Speed +100% 5s!");return true;}});
        register(new Skill(plugin,"tinh_phong_jie","Tinh Phong Kiếm","Kiếm phong tốc độ nhanh nhất, đánh liên tiếp",ClassType.PHONG_TU,22000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=10*d.calculateDamageMultiplier();new BukkitRunnable(){int t=0;@Override public void run(){if(t++>=5){cancel();return;}LivingEntity tgt=getNearestEnemy(p,8.0);if(tgt!=null){tgt.damage(dmg,p);tgt.setNoDamageTicks(0);p.getWorld().spawnParticle(Particle.CRIT,tgt.getLocation().add(0,1,0),5,0.3,0.3,0.3,0.2);}}}.runTaskTimer(plugin,0,4);p.sendMessage(plugin.prefix()+"§b⚡ Tinh Phong Kiếm! §75 đòn liên tiếp!");return true;}});
        register(new Skill(plugin,"tian_feng_zhan","Thiên Phong Trảm","Tấn công từ trên cao xuống sát thương cao",ClassType.PHONG_TU,25000,1){@Override public boolean execute(Player p,PlayerData d){p.setVelocity(new Vector(0,2,0));plugin.getServer().getScheduler().runTaskLater(plugin,()->{double dmg=35*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(5,3,5)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);}}p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),25,2,0.5,2,0.15);},25L);p.sendMessage(plugin.prefix()+"§b⚔ Thiên Phong Trảm!");return true;}});
        register(new Skill(plugin,"feng_shen_biao","Phong Thần Tiêu","Phong tiêu tốc độ cao nhất, xuyên qua 5 kẻ",ClassType.PHONG_TU,20000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=20*d.calculateDamageMultiplier();Location eye=p.getEyeLocation();Vector dir=eye.getDirection().normalize();java.util.Set<java.util.UUID> hit=new java.util.HashSet<>();new BukkitRunnable(){int s=0;@Override public void run(){if(s++>=35||hit.size()>=5){cancel();return;}Location pos=eye.clone().add(dir.clone().multiply(s*0.5));p.getWorld().spawnParticle(Particle.CLOUD,pos,2,0.1,0.1,0.1,0.02);for(Entity e:p.getWorld().getNearbyEntities(pos,0.8,0.8,0.8)){if(e instanceof LivingEntity le&&e!=p&&!hit.contains(e.getUniqueId())){le.damage(dmg,p);hit.add(e.getUniqueId());}}}}.runTaskTimer(plugin,0,1);p.sendMessage(plugin.prefix()+"§b💨 Phong Thần Tiêu! §7Xuyên "+5+" kẻ!");return true;}});
        register(new Skill(plugin,"kuang_feng_jie","Cuồng Phong Kiếm","Kiếm cuồng phong AoE ngắn liên tục",ClassType.PHONG_TU,30000,1){@Override public boolean execute(Player p,PlayerData d){final double dmg=14*d.calculateDamageMultiplier();new BukkitRunnable(){int t=0;@Override public void run(){if(t++>=60){cancel();return;}if(t%10==0)for(Entity e:p.getNearbyEntities(5,3,5)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);}}}}.runTaskTimer(plugin,0,1);p.sendMessage(plugin.prefix()+"§b🌪 Cuồng Phong Kiếm! §710 đòn trong 3s!");return true;}});
        register(new Skill(plugin,"feng_zu_zhi_bu","Phong Tổ Chi Bộ","Tốc độ tuyệt đối, 3s + né đòn 90%",ClassType.PHONG_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("speed"),60,9,false,true,true));p.addPotionEffect(new PotionEffect(ef("jump_boost"),60,9,false,true,true));d.addBuff("phong_dodge_100",3000);p.sendMessage(plugin.prefix()+"§b⚡ Phong Tổ Chi Bộ! §7Tốc độ tối đa + né 3s!");return true;}});
        register(new Skill(plugin,"tian_feng_yi","Thiên Phong Dực","Cánh phong thiên, tăng tốc độ bay và ATK",ClassType.PHONG_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){if(p.isFlying()){p.setFlySpeed(Math.min(p.getFlySpeed()+0.1f,1.0f));}p.addPotionEffect(new PotionEffect(ef("speed"),200,3,false,true,true));p.addPotionEffect(new PotionEffect(ef("strength"),200,2,false,true,true));p.sendMessage(plugin.prefix()+"§b🦅 Thiên Phong Dực! §7Tốc độ bay tăng!");return true;}});
        register(new Skill(plugin,"jue_feng_shen_su","Tuyệt Phong Thần Tốc","Tốc độ cao nhất, dịch chuyển 20m trong 0.5s",ClassType.PHONG_TU,25000,1){@Override public boolean execute(Player p,PlayerData d){Vector dir=p.getLocation().getDirection().normalize().multiply(20);Location dest=p.getLocation().add(dir);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),15,0.5,0.5,0.5,0.1);p.teleport(dest);p.getWorld().spawnParticle(Particle.CLOUD,dest,15,0.5,0.5,0.5,0.1);p.sendMessage(plugin.prefix()+"§b⚡ Tuyệt Phong Thần Tốc! §720m trong 0.5s!");return true;}});
        register(new Skill(plugin,"wu_ji_feng_shen","Vô Cực Phong Thần","[ULTIMATE] Gió thần tốc độ không giới hạn, AoE toàn phương",ClassType.PHONG_TU,70000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("speed"),400,9,false,true,true));p.addPotionEffect(new PotionEffect(ef("strength"),400,3,false,true,true));p.addPotionEffect(new PotionEffect(ef("resistance"),400,2,false,true,true));d.addBuff("phong_dodge_100",10000);double dmg=40*d.calculateDamageMultiplier();int cnt=0;for(Entity e:p.getNearbyEntities(12,6,12)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setVelocity(new Vector(0,1.5,0));cnt++;}}final int phongCnt=cnt;Bukkit.getOnlinePlayers().forEach(pp->pp.sendMessage(plugin.prefix()+"§b§l"+p.getName()+" khai triển VÔ CỰC PHONG THẦN! "+phongCnt+" kẻ!"));p.sendTitle(plugin.msg("auto_ed55e5daa1"), plugin.msg("auto_dabd65461d"),5,60,10);return true;}});
    }

    private void registerHoaTuCombo() {
        register(new Skill(plugin,"hoa_phen_ba","Hỏa Phẫn Bạo","Nổ hỏa phát nổ lớn AoE",ClassType.HOA_TU,18000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=20*d.calculateDamageMultiplier();for(Entity e:p.getNearbyEntities(6,3,6)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setFireTicks(80);}}p.getWorld().spawnParticle(Particle.FLAME,p.getLocation().add(0,1,0),40,2,1,2,0.1);p.sendMessage(plugin.prefix()+"§6🔥 Hỏa Phẫn Bạo!");return true;}});
        register(new Skill(plugin,"huo_shen_jiang","Hỏa Thần Giáng","Gọi hỏa thần giáng xuất 5 cầu lửa",ClassType.HOA_TU,22000,1){@Override public boolean execute(Player p,PlayerData d){for(int i=0;i<5;i++){final int di=i*5;plugin.getServer().getScheduler().runTaskLater(plugin,()->{LivingEntity t=getNearestEnemy(p,12.0);if(t!=null){t.damage(15*d.calculateDamageMultiplier(),p);t.setFireTicks(60);t.setNoDamageTicks(0);t.getWorld().spawnParticle(Particle.FLAME,t.getLocation().add(0,1,0),10,0.3,0.3,0.3,0.1);}},di);}p.sendMessage(plugin.prefix()+"§6🔥 Hỏa Thần Giáng! §75 cầu lửa!");return true;}});
        register(new Skill(plugin,"yan_long_jue","Viêm Long Quyết","Gọi rồng lửa tấn công 6 giây",ClassType.HOA_TU,28000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=12*d.calculateDamageMultiplier();p.sendMessage(plugin.prefix()+"§6🐉 Viêm Long Quyết! §7Rồng lửa 6 giây!");new BukkitRunnable(){int t=0;@Override public void run(){if(t++>=120){cancel();return;}if(t%20==0){for(Entity e:p.getNearbyEntities(7,4,7)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setFireTicks(80);le.setNoDamageTicks(0);}}}double a=Math.toRadians(t*6);Location lp=p.getLocation().add(Math.cos(a)*4,1,Math.sin(a)*4);p.getWorld().spawnParticle(Particle.FLAME,lp,3,0.2,0.2,0.2,0.05);}}.runTaskTimer(plugin,0,1);return true;}});
        register(new Skill(plugin,"di_huo_fen_bao","Địa Hỏa Phẫn Bạo","Địa hỏa nổ từ dưới đất AoE lớn",ClassType.HOA_TU,30000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=28*d.calculateDamageMultiplier();plugin.getServer().getScheduler().runTaskLater(plugin,()->{for(Entity e:p.getNearbyEntities(8,3,8)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setFireTicks(100);le.setNoDamageTicks(0);}}p.getWorld().spawnParticle(Particle.LAVA,p.getLocation(),30,3,0.5,3,0);p.getWorld().playSound(p.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.5f,0.5f);},15L);p.sendMessage(plugin.prefix()+"§6💥 Địa Hỏa Phẫn Bạo!");return true;}});
        register(new Skill(plugin,"tian_huo_jing","Thiên Hỏa Cảnh","Mở thiên hỏa cảnh, AoE đốt cháy liên tục 10s",ClassType.HOA_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){final double dmg=8*d.calculateDamageMultiplier();final Location base=p.getLocation().clone();p.sendMessage(plugin.prefix()+"§6🔥 Thiên Hỏa Cảnh! §710 giây!");new BukkitRunnable(){int t=0;@Override public void run(){if(t++>=200){cancel();return;}if(t%20==0)for(Entity e:base.getWorld().getNearbyEntities(base,6,3,6)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setFireTicks(60);}}for(double a=0;a<360;a+=15){double r=Math.toRadians(a+t*3);base.getWorld().spawnParticle(Particle.FLAME,base.clone().add(Math.cos(r)*5,0.5,Math.sin(r)*5),1,0.1,0.2,0.1,0.03);}}}.runTaskTimer(plugin,0,1);return true;}});
        register(new Skill(plugin,"huo_zu_zhi_yan","Hỏa Tổ Chi Viêm","Tăng sát thương lửa +100% trong 8 giây",ClassType.HOA_TU,28000,1){@Override public boolean execute(Player p,PlayerData d){d.addBuff("hoa_amplify",8000);p.addPotionEffect(new PotionEffect(ef("strength"),160,3,false,true,true));p.addPotionEffect(new PotionEffect(ef("fire_resistance"),200,0,false,false,false));p.sendMessage(plugin.prefix()+"§6✦ Hỏa Tổ Chi Viêm! §7Lửa x2 + Fire Resist 8s!");return true;}});
        register(new Skill(plugin,"wan_huo_gui_zong","Vạn Hỏa Quy Tông","Hội tụ vạn hỏa, nổ vũ AoE lớn nhất",ClassType.HOA_TU,40000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=40*d.calculateDamageMultiplier();int cnt=0;for(Entity e:p.getNearbyEntities(10,5,10)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setFireTicks(160);le.setNoDamageTicks(0);cnt++;}}p.getWorld().spawnParticle(Particle.LAVA,p.getLocation(),20,3,1,3,0);p.sendTitle(plugin.msg("auto_274e9342c4"),"§7"+cnt+" kẻ trúng!",5,45,10);return true;}});
        register(new Skill(plugin,"huo_di_li","Hỏa Đế Lệ","Lửa đế bao quanh +ATK cao trong 10s",ClassType.HOA_TU,35000,1){@Override public boolean execute(Player p,PlayerData d){p.addPotionEffect(new PotionEffect(ef("strength"),200,4,false,true,true));p.addPotionEffect(new PotionEffect(ef("fire_resistance"),200,0,false,false,false));p.addPotionEffect(new PotionEffect(ef("resistance"),200,1,false,true,true));p.sendMessage(plugin.prefix()+"§6👑 Hỏa Đế Lệ! §7Lửa đế bọc người - ATK max!");return true;}});
        register(new Skill(plugin,"huo_huang_lin","Hỏa Hoàng Lâm","Phát lửa mắt phá vỡ phòng thủ kẻ thù",ClassType.HOA_TU,40000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=50*d.calculateDamageMultiplier();LivingEntity t=getNearestEnemy(p,10.0);if(t==null){p.sendMessage(plugin.prefix()+"§cKhông có mục tiêu!");return false;}t.damage(dmg,p);t.setNoDamageTicks(0);t.setFireTicks(200);t.addPotionEffect(new PotionEffect(ef("weakness"),200,4,false,true,true));p.sendMessage(plugin.prefix()+"§6🔥 Hỏa Hoàng Lâm! §7Gây §e"+(int)dmg+" §6+ phá DEF!");return true;}});
        register(new Skill(plugin,"tian_yan_jue","Thiên Viêm Quyết","Thiên viêm phát hỏa AoE toàn phương hướng",ClassType.HOA_TU,50000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=35*d.calculateDamageMultiplier();int cnt=0;for(int wave=0;wave<3;wave++){final int w=wave;plugin.getServer().getScheduler().runTaskLater(plugin,()->{for(Entity e:p.getNearbyEntities(8+w*2,4,8+w*2)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setFireTicks(100+w*40);le.setNoDamageTicks(0);}for(int i=0;i<3;i++){double a=Math.random()*Math.PI*2;p.getWorld().strikeLightningEffect(p.getLocation().add(Math.cos(a)*6,0,Math.sin(a)*6));}}},w*8L);}p.sendTitle(plugin.msg("auto_e7a4bb0eb8"), plugin.msg("auto_1a10f44c64"),5,50,10);return true;}});
        register(new Skill(plugin,"wu_shang_zhen_huo","Vô Thượng Chân Hỏa","[ULTIMATE] Lửa thật sự vô thượng, đốt cháy toàn thế giới",ClassType.HOA_TU,80000,1){@Override public boolean execute(Player p,PlayerData d){double dmg=100*d.calculateDamageMultiplier();int cnt=0;for(Entity e:p.getNearbyEntities(20,10,20)){if(e instanceof LivingEntity le&&e!=p){le.damage(dmg,p);le.setNoDamageTicks(0);le.setFireTicks(400);cnt++;}}p.setFireTicks(0);p.addPotionEffect(new PotionEffect(ef("fire_resistance"),600,0,false,false,false));final int hoaCnt=cnt;Bukkit.getOnlinePlayers().forEach(pp->pp.sendMessage(plugin.prefix()+"§6§l🔥 "+p.getName()+" khai triển VÔ THƯỢNG CHÂN HỎA! "+hoaCnt+" kẻ bị đốt!"));p.sendTitle(plugin.msg("auto_461ba96250"),"§e"+cnt+" kẻ bị đốt!",5,70,10);p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_DEATH,1f,0.4f);return true;}});
    }

    private Player getNearestAlly(Player player, double range) {
        Player nearest = null; double nd = range*range;
        for(Entity e:player.getNearbyEntities(range,range,range)){
            if(e instanceof Player ally&&e!=player){
                double dist=player.getLocation().distanceSquared(ally.getLocation());
                if(dist<nd){nearest=ally;nd=dist;}}}
        return nearest;
    }
}

