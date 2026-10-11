package com.tutien.meditation;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Thien dinh: /thiendinh hoac /td
 */
public class MeditationSystem implements Listener {

    private final TuTienPlugin plugin;

    private final Set<UUID> meditators = new HashSet<>();
    private final Map<UUID, Location> lastLoc = new HashMap<>();
    private final Map<UUID, ArmorStand> seatEntities = new HashMap<>();

    // Đã căn chỉnh ghế vừa vặn sát mặt đất
    private static final double SEAT_Y_OFFSET = -0.7;

    private final List<LinhMachZone> linhMachZones = new ArrayList<>();

    private final Map<UUID, Integer> ticksUntilChallenge = new HashMap<>();
    private final Set<UUID> inChallenge = new HashSet<>();
    private final Map<UUID, Integer> challengeAnswer = new HashMap<>();
    private final Random challengeRandom = new Random();

    private static final int CHALLENGE_MIN_TICKS = 20; // 60s
    private static final int CHALLENGE_MAX_TICKS = 30; // 90s
    private static final long CHALLENGE_TIMEOUT_TICKS = 120L; // 6s

    private record TapNiemQuestion(String question, String a, String b, String c, int correctIdx) {}

    private static final List<TapNiemQuestion> TAP_NIEM_QUESTIONS = List.of(
            new TapNiemQuestion("§e\"Một tiếng động vang lên phá vỡ tĩnh lặng...\"",
                    "§aGiữ tâm bất động", "§7Mở mắt xem là gì", "§cHoảng loạn đứng dậy", 0),
            new TapNiemQuestion("§e\"Cơn đói bụng réo lên giữa lúc thiền...\"",
                    "§7Dừng lại ăn ngay", "§aMặc kệ, tiếp tục", "§cBực bội cáu gắt", 1),
            new TapNiemQuestion("§e\"Ký ức đau buồn bỗng ùa về...\"",
                    "§aBuông bỏ, để nó trôi qua", "§7Chìm đắm trong đó", "§cCố quên bằng vũ lực", 0),
            new TapNiemQuestion("§e\"Có tiếng thì thầm dụ dỗ luyện tắt...\"",
                    "§7Nghe theo cho nhanh", "§cTò mò lại gần", "§aPhớt lờ, giữ vững đạo tâm", 2),
            new TapNiemQuestion("§e\"Cảm giác ngứa ngáy khắp người...\"",
                    "§aTĩnh tâm chịu đựng", "§7Gãi ngay lập tức", "§cBỏ cuộc đứng dậy", 0),
            new TapNiemQuestion("§e\"Một giọng nói vang lên: 'Ngươi sẽ thất bại thôi'...\"",
                    "§cTin lời, nản chí", "§aCười nhạt, kệ nó", "§7Tranh cãi với giọng nói", 1)
    );

    private static final String CHALLENGE_GUI_TITLE = "§5☯ Tạp Niệm Xâm Nhập";

    public MeditationSystem(TuTienPlugin plugin) {
        this.plugin = plugin;
        startMeditationLoop();
        startAuraLoop();
    }

    // ========================
    //  START / STOP
    // ========================

    public void startMeditate(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cCần chọn class trước!");
            return;
        }
        if (data.isMeditating()) {
            stopMeditate(player, true);
            return;
        }

        data.setMeditating(true);
        meditators.add(player.getUniqueId());

        ticksUntilChallenge.put(player.getUniqueId(),
                CHALLENGE_MIN_TICKS + challengeRandom.nextInt(CHALLENGE_MAX_TICKS - CHALLENGE_MIN_TICKS + 1));

        // Mount ghế vô hình cho người chơi
        mountSeat(player);

        // Cập nhật vị trí CHÍNH XÁC sau khi đã mount để tránh bị lùi tọa độ gây ngắt thiền
        lastLoc.put(player.getUniqueId(), player.getLocation().clone());

        player.sendMessage(plugin.prefix() + "§9✦ Vào thiền định... §7(nhấn Shift để thoát)");
        player.sendTitle(plugin.msg("auto_5751fa450a"), plugin.msg("auto_fd8faff34f"), 5, 40, 10);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.5f);

        // Particle hiệu ứng
        for (int i = 0; i < 20; i++) {
            final int t = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || !meditators.contains(player.getUniqueId())) return;
                double a = Math.toRadians(t * 18);
                Location p = player.getLocation().add(Math.cos(a)*1.5, 0.3, Math.sin(a)*1.5);
                player.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(80,160,255), 1.2f));
            }, t * 1L);
        }
    }

    public void stopMeditate(Player player, boolean manual) {
        PlayerData data = plugin.getDataManager().get(player);
        if (data != null) data.setMeditating(false);
        meditators.remove(player.getUniqueId());
        lastLoc.remove(player.getUniqueId());
        ticksUntilChallenge.remove(player.getUniqueId());
        unmountSeat(player);

        if (inChallenge.remove(player.getUniqueId())) {
            player.closeInventory();
        }

        if (manual) {
            player.sendMessage(plugin.prefix() + "§7Thoát khỏi thiền định.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 1.2f);
        } else {
            player.sendActionBar(LegacyComponentSerializer.legacySection()
                    .deserialize("§c⚠ Thiền định bị gián đoạn!"));
        }
    }

    // ========================
    //  MEDITATION LOOP
    // ========================

    private void startMeditationLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Set<UUID> toStop = new HashSet<>();
                for (UUID uid : new HashSet<>(meditators)) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p == null || !p.isOnline()) {
                        toStop.add(uid);
                        continue;
                    }

                    // Kiểm tra gián đoạn do rời khỏi ghế hoặc chuyển vị trí quá xa (> 0.5 block)
                    Location prev = lastLoc.get(uid);
                    if (prev != null) {
                        Location cur = p.getLocation();
                        if (!cur.getWorld().equals(prev.getWorld()) || cur.distanceSquared(prev) > 0.25) {
                            toStop.add(uid);
                            p.sendMessage(plugin.prefix() + "§c⚠ Thiền định bị gián đoạn do di chuyển!");
                            continue;
                        }
                    }

                    PlayerData data = plugin.getDataManager().get(p);
                    if (data == null) {
                        toStop.add(uid);
                        continue;
                    }

                    long maxLK = data.getMaxLinhKhi();
                    int ord = data.getStage().ordinal();

                    double pct = Math.min(0.005 + ord * 0.0008, 0.03);

                    boolean inZone = isInLinhMach(p.getLocation());
                    if (inZone) pct *= 2.0;

                    int nearbyMeditators = countNearbyMeditators(p);
                    if (nearbyMeditators >= 1) pct *= (1.0 + nearbyMeditators * 0.15);

                    long gain = (long)(maxLK * pct);
                    if (gain <= 0) gain = 1;
                    data.addLinhKhi(gain);

                    if (plugin.getSuPhuDeDoManager() != null) {
                        plugin.getSuPhuDeDoManager().onDeTuGainLK(p, data, gain);
                    }

                    if (data.getLinhKhi() >= maxLK) {
                        plugin.getBreakthroughManager().notifyBreakthroughReady(p, data);
                    }
                    plugin.getSkillItemManager().updateInfoItem(p, data);

                    String bonus = inZone ? " §6(Linh Mạch x2)" : "";
                    if (nearbyMeditators >= 1) bonus += " §a(Đồng Tu +" + (nearbyMeditators*15) + "%)";
                    String msg = "§9⛩ Thiền Định §8| §e+" + fmt(gain) + " LK" + bonus;
                    p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(msg));

                    if (!inChallenge.contains(uid)) {
                        int left = ticksUntilChallenge.getOrDefault(uid, CHALLENGE_MIN_TICKS) - 1;
                        if (left <= 0) {
                            triggerChallenge(p, data);
                            ticksUntilChallenge.put(uid,
                                    CHALLENGE_MIN_TICKS + challengeRandom.nextInt(CHALLENGE_MAX_TICKS - CHALLENGE_MIN_TICKS + 1));
                        } else {
                            ticksUntilChallenge.put(uid, left);
                        }
                    }
                }

                for (UUID uid : toStop) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p != null) {
                        PlayerData d = plugin.getDataManager().get(p);
                        if(d != null) d.setMeditating(false);
                    }
                    meditators.remove(uid);
                    lastLoc.remove(uid);
                }
            }
        }.runTaskTimer(plugin, 60L, 60L);
    }

    // ========================
    //  GHẾ NGOÀI (ArmorStand)
    // ========================

    private void mountSeat(Player player) {
        unmountSeat(player);

        try {
            Location seatLoc = player.getLocation().clone().add(0, SEAT_Y_OFFSET, 0);
            ArmorStand seat = (ArmorStand) player.getWorld().spawnEntity(seatLoc, EntityType.ARMOR_STAND);

            seat.setVisible(false);
            seat.setInvisible(true);
            seat.setGravity(false);
            seat.setInvulnerable(true);
            seat.setSilent(true);
            seat.setSmall(true);
            seat.setBasePlate(false);
            seat.setMarker(true); // Marker = true giúp loại bỏ hitbox hoàn toàn
            seat.setPersistent(false);

            seat.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "meditate_seat"), PersistentDataType.STRING, player.getUniqueId().toString());

            seat.addPassenger(player);
            seatEntities.put(player.getUniqueId(), seat);
        } catch (Exception ignored) {
        }
    }

    private void unmountSeat(Player player) {
        ArmorStand seat = seatEntities.remove(player.getUniqueId());
        if (seat != null && !seat.isDead()) {
            seat.eject();
            seat.remove();
        }
    }

    /**
     * Bấm Shift để rời ghế -> Tự động HỦY THIỀN định mượt mà
     */
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!meditators.contains(p.getUniqueId())) return;

        // Hủy thiền ngay lập tức khi rời ghế ngồi
        Bukkit.getScheduler().runTask(plugin, () -> stopMeditate(p, true));
    }

    // ========================
    //  VÒNG LINH KHÍ
    // ========================

    private void startAuraLoop() {
        new BukkitRunnable() {
            double tick = 0;
            @Override
            public void run() {
                tick += 0.18;
                for (UUID uid : new HashSet<>(meditators)) {
                    Player p = Bukkit.getPlayer(uid);
                    if (p == null || !p.isOnline()) continue;
                    spawnAura(p, tick, isInLinhMach(p.getLocation()));
                }
            }
        }.runTaskTimer(plugin, 5L, 4L);
    }

    private void spawnAura(Player p, double tick, boolean inZone) {
        Location base = p.getLocation().add(0, 0.15, 0);
        World w = p.getWorld();
        Color col = inZone ? Color.fromRGB(255, 200, 50) : Color.fromRGB(90, 170, 255);

        for (int i = 0; i < 4; i++) {
            double a = tick + i * (Math.PI / 2);
            double r = 0.9;
            Location pt = base.clone().add(Math.cos(a) * r, 0.3 + Math.sin(a * 3) * 0.15, Math.sin(a) * r);
            w.spawnParticle(Particle.DUST, pt, 1, 0, 0, 0, 0, new Particle.DustOptions(col, 1.1f));
        }
        for (int i = 0; i < 6; i++) {
            double a = -tick * 0.6 + i * (Math.PI / 3);
            double r = 1.6;
            Location pt = base.clone().add(Math.cos(a) * r, 0.8 + Math.sin(tick + i) * 0.25, Math.sin(a) * r);
            w.spawnParticle(Particle.END_ROD, pt, 1, 0, 0, 0, 0.001);
        }
    }

    // ========================
    //  EVENTS
    // ========================

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!meditators.contains(p.getUniqueId())) return;

        stopMeditate(p, false);
        p.sendMessage(plugin.prefix() + "§c⚠ Thiền định bị phá! (bị tấn công)");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopMeditate(event.getPlayer(), false);
    }

    @EventHandler
    public void onChallengeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uid = player.getUniqueId();
        if (!inChallenge.contains(uid)) return;

        String title = LegacyComponentSerializer.legacySection().serialize(event.getView().title());
        if (!title.contains("Tạp Niệm")) return;

        event.setCancelled(true);
        int slot = event.getSlot();
        if (slot != 11 && slot != 13 && slot != 15) return;

        int chosenIdx = slot == 11 ? 0 : slot == 13 ? 1 : 2;
        int correctIdx = challengeAnswer.getOrDefault(uid, -1);

        inChallenge.remove(uid);
        challengeAnswer.remove(uid);
        player.closeInventory();

        PlayerData data = plugin.getDataManager().get(player);
        if (data == null) return;

        if (chosenIdx == correctIdx) resolveChallengeSuccess(player, data);
        else resolveChallengeFail(player, data);
    }

    // ========================
    //  LINH MẠCH ZONES
    // ========================

    public void addZone(Location center, double radius, String name) {
        linhMachZones.add(new LinhMachZone(center, radius, name));
    }

    public boolean isInLinhMach(Location loc) {
        for (LinhMachZone z : linhMachZones) {
            if (z.contains(loc)) return true;
        }
        return false;
    }

    public List<LinhMachZone> getZones() { return linhMachZones; }

    private int countNearbyMeditators(Player self) {
        int count = 0;
        for (UUID uid : meditators) {
            if (uid.equals(self.getUniqueId())) continue;
            Player other = Bukkit.getPlayer(uid);
            if (other != null && other.getWorld().equals(self.getWorld())
                    && other.getLocation().distanceSquared(self.getLocation()) <= 100) count++;
        }
        return count;
    }

    private String fmt(long n) {
        if (n >= 1_000_000L) return String.format("%.1fM", n/1_000_000.0);
        if (n >= 1_000L)     return String.format("%.1fK", n/1_000.0);
        return String.valueOf(n);
    }

    // ========================
    //  TẠP NIỆM XÂM NHẬP
    // ========================

    private void triggerChallenge(Player player, PlayerData data) {
        UUID uid = player.getUniqueId();
        inChallenge.add(uid);

        TapNiemQuestion q = TAP_NIEM_QUESTIONS.get(challengeRandom.nextInt(TAP_NIEM_QUESTIONS.size()));
        challengeAnswer.put(uid, q.correctIdx());

        Inventory inv = Bukkit.createInventory(null, 27,
                LegacyComponentSerializer.legacySection().deserialize(CHALLENGE_GUI_TITLE));

        inv.setItem(4, makeItem(Material.ENDER_EYE, "§5§l" + q.question(),
                List.of("§7Trả lời trong §e6 giây§7!", "§aĐúng: §f+LK thưởng", "§cSai: §c-HP -LK +Đói")));
        inv.setItem(11, makeItem(Material.LIME_CONCRETE,  q.a(), List.of("§7Đáp án A")));
        inv.setItem(13, makeItem(Material.YELLOW_CONCRETE, q.b(), List.of("§7Đáp án B")));
        inv.setItem(15, makeItem(Material.RED_CONCRETE,   q.c(), List.of("§7Đáp án C")));

        ItemStack border = makeItem(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) if (inv.getItem(i) == null) inv.setItem(i, border);

        player.openInventory(inv);
        player.sendTitle(plugin.msg("auto_0740d98cda"), plugin.msg("auto_9f95cdb18a"), 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.7f, 1.3f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!inChallenge.contains(uid)) return;
                inChallenge.remove(uid);
                challengeAnswer.remove(uid);
                if (player.isOnline()) {
                    player.closeInventory();
                    PlayerData d = plugin.getDataManager().get(player);
                    if (d != null) resolveChallengeFail(player, d);
                }
            }
        }.runTaskLater(plugin, CHALLENGE_TIMEOUT_TICKS);
    }

    private void resolveChallengeSuccess(Player player, PlayerData data) {
        long reward = Math.max(100L, (long) (data.getMaxLinhKhi() * 0.01));
        data.addLinhKhi(reward);

        player.sendMessage(plugin.prefix() + "§a§l✓ Vượt qua tạp niệm! §7Đạo tâm vững vàng. §b+" + fmt(reward) + " LK");
        player.sendTitle(plugin.msg("auto_09d4706b95"), "§b+" + fmt(reward) + " Linh Khí!", 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0);

        if (data.getLinhKhi() >= data.getMaxLinhKhi()) {
            plugin.getBreakthroughManager().notifyBreakthroughReady(player, data);
        }
    }

    private void resolveChallengeFail(Player player, PlayerData data) {
        long lkLoss = Math.max(50L, (long) (data.getMaxLinhKhi() * 0.005));
        data.setLinhKhi(Math.max(0, data.getLinhKhi() - lkLoss));

        double hpLoss = player.getMaxHealth() * 0.12;
        double newHp = Math.max(1.0, player.getHealth() - hpLoss);
        player.setHealth(newHp);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.getByKey(NamespacedKey.minecraft("hunger")), 400, 1, false, true, true));

        player.sendMessage(plugin.prefix() + "§c§l✗ Tạp niệm xâm chiếm tâm trí! §7Mất tập trung.");
        player.sendMessage(plugin.prefix() + "  §c-" + fmt(lkLoss) + " LK  §7| §c-" + String.format("%.1f", hpLoss)
                + " HP §7| §6Đói bụng 20s");
        player.sendTitle(plugin.msg("auto_21ae04e6fd"), plugin.msg("auto_a2d256ada0"), 5, 40, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 0.8f, 0.7f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0),
                25, 0.4, 0.4, 0.4, 0.08);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        List<net.kyori.adventure.text.Component> loreCmp = new ArrayList<>();
        for (String l : lore) loreCmp.add(LegacyComponentSerializer.legacySection().deserialize(l));
        meta.lore(loreCmp);
        item.setItemMeta(meta);
        return item;
    }

    public record LinhMachZone(Location center, double radius, String name) {
        public boolean contains(Location loc) {
            return loc.getWorld().equals(center.getWorld())
                    && loc.distanceSquared(center) <= radius * radius;
        }
    }
}