package com.tutien.sword;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SwordFlightManager V4 (Hoàn Thiện & Nâng Cấp)
 *
 * NGỰ KIẾM PHI HÀNH - TIÊN HIỆP ĐỈNH CAO
 *
 * Tính năng chính:
 * - Player đứng vững chãi, uy nghi trên phi kiếm.
 * - Mũi kiếm xoay chuẩn xác 100% theo hướng nhìn 3D (Yaw xoay trái/phải, Pitch ngẩng lên/chúc xuống).
 * - Khắc phục hoàn toàn lỗi kiếm không quay đầu theo hướng nhìn và lỗi xoay vòng tròn 360 độ quanh trục X.
 * - Hệ thống hiệu ứng Kiếm Khí (Sword Aura), Vệt Đuôi Khí Động Học 3D (Dual Spiral Trail),
 *   Linh Trận Bàn Chân (Foot Lotus Array), cùng hiệu ứng Triệu Hồi / Thu Kiếm tráng lệ.
 * - Màu sắc khí tức và loại phi kiếm tự động tiến hóa theo Cảnh Giới tu tiên của người chơi.
 */
public class SwordFlightManager implements Listener {

    private final TuTienPlugin plugin;

    private final Set<UUID> enabled = new HashSet<>();
    private final Map<UUID, ItemDisplay> mounts = new ConcurrentHashMap<>();
    private BukkitTask loopTask;

    private static final int MIN_ORD = 10; // BÁN_THÁNH trong hệ thống 25 cảnh giới

    // ============================================================
    // ⚔️ CẤU HÌNH VỊ TRÍ & KÍCH THƯỚC
    // ============================================================

    /**
     * Vị trí tâm ItemDisplay so với chân player.
     * -0.30 đặt tâm kiếm ngay dưới đế giày của player.
     */
    private static final double Y_OFFSET = -0.30;

    /**
     * Nâng visual kiếm lên một khoảng nhỏ để đế giày tiếp xúc tự nhiên với sống kiếm.
     */
    private static final double FOOT_LIFT = 0.02;

    /**
     * Kích thước phi kiếm (1.30f tạo cảm giác cự kiếm / phi kiếm tiên gia oai phong, rộng rãi).
     */
    private static final float SWORD_SCALE = 1.30f;

    /**
     * Tinh chỉnh góc pitch mặc định của kiếm nếu muốn chúc nhẹ.
     */
    private static final float SWORD_PITCH = 0.0f;

    // ============================================================
    // ✨ ANIMATION BỒNG BỀNH
    // ============================================================

    private static final float BOB_HEIGHT = 0.016f;
    private static final float BOB_SPEED = 0.14f;

    // ============================================================
    // 🎨 HỆ THỐNG KHÍ TỨC CẢNH GIỚI (STAGE AURA)
    // ============================================================

    private static class StageAura {
        final Color primary;
        final Color secondary;
        final Particle accentParticle;
        final String swordTitle;
        final Material defaultMaterial;

        StageAura(Color primary, Color secondary, Particle accentParticle, String swordTitle, Material defaultMaterial) {
            this.primary = primary;
            this.secondary = secondary;
            this.accentParticle = accentParticle;
            this.swordTitle = swordTitle;
            this.defaultMaterial = defaultMaterial;
        }
    }

    private StageAura getStageAura(CultivationStage stage) {
        int ord = (stage != null) ? stage.ordinal() : MIN_ORD;
        if (ord >= 20) {
            // Thiên Giới & Chí Tôn: Hoàng Kim Xích Diễm Chí Tôn
            return new StageAura(
                    Color.fromRGB(255, 215, 0),    // Vàng Kim
                    Color.fromRGB(255, 60, 0),     // Xích Hỏa
                    Particle.SOUL_FIRE_FLAME,
                    "§6✦ §eThái Sơ Hỗn Nguyên Phi Kiếm §6✦",
                    Material.NETHERITE_SWORD
            );
        } else if (ord >= 15) {
            // Tiên Giới: Tử Tiêu U Minh Huyền Khí
            return new StageAura(
                    Color.fromRGB(180, 80, 255),   // Tím Huyền Ảo
                    Color.fromRGB(90, 230, 255),   // Lam Ngọc Tiên Giới
                    Particle.PORTAL,
                    "§d✦ §5Tử Tiêu Cửu U Phi Kiếm §d✦",
                    Material.NETHERITE_SWORD
            );
        } else {
            // Thánh Giới: Bích Hải Thanh Phong Băng Lôi
            return new StageAura(
                    Color.fromRGB(0, 235, 255),    // Lam Băng Sáng
                    Color.fromRGB(240, 250, 255),  // Bạch Kim Tinh Khiết
                    Particle.ELECTRIC_SPARK,
                    "§b✦ §fBích Hải Thanh Phong Phi Kiếm §b✦",
                    Material.DIAMOND_SWORD
            );
        }
    }

    public SwordFlightManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startFollowLoop();
    }

    // ============================================================
    // TOGGLE BẬT / TẮT
    // ============================================================

    public void toggle(Player player) {
        PlayerData data = plugin.getDataManager().get(player);

        if (data == null || !data.hasClass()) {
            player.sendMessage(plugin.prefix() + "§cCần chọn class trước! /tutien chon");
            return;
        }

        if (data.getClassType() != ClassType.KIEM_TU) {
            player.sendMessage(plugin.prefix() + "§cChỉ §eKiếm Tu §cmới có thể Ngự Kiếm Phi Hành!");
            return;
        }

        if (!data.getStage().canFly() || data.getStage().ordinal() < MIN_ORD) {
            player.sendMessage(plugin.prefix() + "§cCần đạt §fThánh Giới §c(Bán Thánh) trở lên để bay và ngự kiếm!");
            return;
        }

        UUID uid = player.getUniqueId();

        // Tắt ngự kiếm
        if (enabled.contains(uid)) {
            enabled.remove(uid);
            removeMount(uid, true);
            player.sendMessage(plugin.prefix() + "§7Đã thu hồi §bPhi Kiếm§7.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.7f, 1.25f);
            return;
        }

        // Bật ngự kiếm
        enabled.add(uid);
        if (!player.getAllowFlight() && data.getStage().canFly()) {
            player.setAllowFlight(true);
        }
        player.sendMessage(
                plugin.prefix()
                        + "§b✦ §fNgự Kiếm Phi Hành §bđã kích hoạt!"
                        + " §7Bay lên §e(Space x2) §7để đạp kiếm ngao du thiên hạ."
        );
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.35f);

        if (player.isFlying()) {
            spawnMount(player);
        }
    }

    // ============================================================
    // ⚔️ TẠO PHI KIẾM
    // ============================================================

    private void spawnMount(Player player) {
        UUID uid = player.getUniqueId();
        ItemDisplay existing = mounts.get(uid);

        if (existing != null && !existing.isDead()) {
            return;
        }

        mounts.remove(uid);

        try {
            Location loc = getSwordLocation(player);
            ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class);

            // Item kiếm tương thích với tay cầm hoặc cảnh giới
            display.setItemStack(createSword(player));

            // NONE để giữ model gốc, toàn bộ góc xoay được kiểm soát qua Transformation
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setBillboard(Display.Billboard.FIXED);
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setPersistent(false);
            display.setSilent(true);

            // Nội suy góc quay 1 tick siêu mượt trên client
            display.setInterpolationDuration(1);
            display.setInterpolationDelay(0);

            // Áp dụng góc quay chuẩn xác theo hướng nhìn người chơi
            applySwordTransformation(display, player, 0);

            mounts.put(uid, display);

            // Hiệu ứng triệu hoán oai linh
            spawnSummonEffect(player);

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[SwordFlight] Không thể tạo Phi Kiếm cho " + player.getName() + ": " + e.getMessage()
            );
        }
    }

    // ============================================================
    // ⚔️ KHỞI TẠO ITEM KIẾM
    // ============================================================

    private ItemStack createSword(Player player) {
        PlayerData data = plugin.getDataManager().get(player);
        CultivationStage stage = (data != null) ? data.getStage() : null;
        StageAura aura = getStageAura(stage);

        ItemStack handItem = player.getInventory().getItemInMainHand();
        ItemStack sword;

        if (handItem != null && handItem.getType().name().endsWith("_SWORD")) {
            sword = handItem.clone();
            sword.setAmount(1);
        } else {
            sword = new ItemStack(aura.defaultMaterial);
            ItemMeta meta = sword.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(aura.swordTitle);
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.values());
                sword.setItemMeta(meta);
            }
        }

        return sword;
    }

    // ============================================================
    // 🔄 TÍNH TOÁN TRANSFORMATION (XOAY ĐẦU KIẾM CHUẨN XÁC 100%)
    // ============================================================

    private void applySwordTransformation(
            ItemDisplay display,
            Player player,
            long tick
    ) {
        Location pLoc = player.getLocation();
        float yaw = pLoc.getYaw();
        float pitch = pLoc.getPitch();

        // Khống chế góc ngẩng/chúc [-75, +75] độ để phi kiếm bổ nhào / thăng thiên đẹp mắt
        float effectivePitch = Math.max(-75.0f, Math.min(75.0f, pitch + SWORD_PITCH));

        // Nhấp nhô bồng bềnh thanh thoát
        float bob = (float) Math.sin(tick * BOB_SPEED) * BOB_HEIGHT;

        /*
         * CƠ CHẾ XOAY 3D CHUẨN XÁC (FIX BUG XOAY 360 VÀ SAI HƯỚNG):
         *
         * Model 2D của kiếm trong ItemDisplayTransform.NONE nằm chéo 45 độ trong mặt phẳng XY:
         *   - Chuôi kiếm ở góc (-0.5, -0.5, 0).
         *   - Mũi kiếm hướng chéo lên ở góc (+0.5, +0.5, 0).
         *
         * Bước 1 (qBase):
         *   - Xoay quanh trục X: -90 độ.
         *   - Xoay quanh trục Z: -135 độ.
         *   -> Chuyển đổi model nằm ngang song song mặt đất, mũi kiếm hướng chuẩn về trục +Z (hướng Nam Minecraft),
         *      sống kiếm nằm ngang và mặt phẳng kiếm hướng thẳng lên (+Y) làm bệ đứng vững chắc.
         *
         * Bước 2 (Look Rotation):
         *   - Xoay quanh trục Y: (-yaw) đưa mũi kiếm quay theo góc nhìn ngang của người chơi.
         *   - Xoay quanh trục X: (effectivePitch) ngẩng/chúc đầu kiếm theo góc nhìn dọc của người chơi.
         *
         * Vì Display entity giữ yaw=0, pitch=0 ở tọa độ thế giới, ma trận biến đổi này kiểm soát
         * góc quay trực tiếp không bị cộng dồn hay triệt tiêu góc yaw như bản cũ.
         */
        Quaternionf qBase = new Quaternionf()
                .rotateX((float) Math.toRadians(-90.0))
                .rotateZ((float) Math.toRadians(-135.0));

        Quaternionf rotation = new Quaternionf()
                .rotateY((float) -Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(effectivePitch))
                .mul(qBase);

        Transformation transformation = new Transformation(
                new Vector3f(0f, (float) (FOOT_LIFT + bob), 0f),
                rotation,
                new Vector3f(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE),
                new Quaternionf()
        );

        display.setInterpolationDelay(0);
        display.setTransformation(transformation);
    }

    // ============================================================
    // 📍 VỊ TRÍ GỐC CỦA DISPLAY ENTITY
    // ============================================================

    private Location getSwordLocation(Player player) {
        Location pLoc = player.getLocation();
        Location loc = pLoc.clone().add(0, Y_OFFSET, 0);

        // Đặt yaw=0, pitch=0 cố định để Translation & Rotation trong Transformation
        // kiểm soát tuyệt đối và mượt mà góc xoay 3D
        loc.setYaw(0f);
        loc.setPitch(0f);

        return loc;
    }

    // ============================================================
    // 🔄 VÒNG LẶP CẬP NHẬT (FOLLOW LOOP)
    // ============================================================

    private void startFollowLoop() {
        if (loopTask != null) {
            loopTask.cancel();
        }

        loopTask = new BukkitRunnable() {
            long tick = 0;

            @Override
            public void run() {
                tick++;

                for (UUID uid : new HashSet<>(enabled)) {
                    Player player = Bukkit.getPlayer(uid);

                    // Player offline / không tồn tại
                    if (player == null || !player.isOnline()) {
                        enabled.remove(uid);
                        removeMount(uid, false);
                        continue;
                    }

                    // Không còn trong trạng thái bay
                    if (!player.isFlying()) {
                        removeMount(uid, true);
                        continue;
                    }

                    ItemDisplay display = mounts.get(uid);

                    // Display entity bị xóa hoặc khác thế giới hoặc chưa khởi tạo
                    if (display == null || display.isDead() || !display.getWorld().equals(player.getWorld())) {
                        removeMount(uid, false);
                        spawnMount(player);
                        display = mounts.get(uid);
                        if (display == null) continue;
                    }

                    // Cập nhật vị trí bám theo người chơi
                    Location target = getSwordLocation(player);
                    display.teleport(target);

                    // Cập nhật góc quay theo hướng nhìn
                    applySwordTransformation(display, player, tick);

                    // Cập nhật đồng bộ model phi kiếm nếu đổi vũ khí cầm tay (mỗi 10 tick)
                    if (tick % 10 == 0) {
                        display.setItemStack(createSword(player));
                    }

                    // Hiệu ứng kiếm khí và vệt đuôi 3D
                    spawnFlightParticles(player, display, tick);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ============================================================
    // ✨ HIỆU ỨNG KIẾM KHÍ & VỆT ĐUÔI 3D NÂNG CẤP
    // ============================================================

    private void spawnFlightParticles(
            Player player,
            ItemDisplay sword,
            long tick
    ) {
        World world = player.getWorld();
        Location pLoc = player.getLocation();
        PlayerData data = plugin.getDataManager().get(player);
        CultivationStage stage = (data != null) ? data.getStage() : null;
        StageAura aura = getStageAura(stage);

        float yaw = pLoc.getYaw();
        float pitch = pLoc.getPitch();
        float effectivePitch = Math.max(-75.0f, Math.min(75.0f, pitch + SWORD_PITCH));

        Quaternionf qBase = new Quaternionf()
                .rotateX((float) Math.toRadians(-90.0))
                .rotateZ((float) Math.toRadians(-135.0));

        Quaternionf rotation = new Quaternionf()
                .rotateY((float) -Math.toRadians(yaw))
                .rotateX((float) Math.toRadians(effectivePitch))
                .mul(qBase);

        // Vector 3D trực chuẩn chính xác tuyệt đối theo frame xoay của thanh kiếm
        Vector3f tipV = new Vector3f(1, 1, 0).normalize();
        Vector3f edgeV = new Vector3f(-1, 1, 0).normalize();
        Vector3f normV = new Vector3f(0, 0, 1).normalize();

        rotation.transform(tipV);
        rotation.transform(edgeV);
        rotation.transform(normV);

        Vector lookDir = new Vector(tipV.x, tipV.y, tipV.z);
        Vector side = new Vector(edgeV.x, edgeV.y, edgeV.z);
        Vector orthoUp = new Vector(normV.x, normV.y, normV.z);
        Vector backward = lookDir.clone().multiply(-1);

        Location swordCenter = sword.getLocation().clone().add(0, FOOT_LIFT, 0);
        Location tipLoc = swordCenter.clone().add(lookDir.clone().multiply(0.78 * SWORD_SCALE));
        Location hiltLoc = swordCenter.clone().add(backward.clone().multiply(0.65 * SWORD_SCALE));

        boolean moving = player.getVelocity().lengthSquared() > 0.005;

        // 1. MŨI KIẾM (SWORD TIP) - Điểm phá không, lôi quang xé gió
        if (tick % 2 == 0) {
            world.spawnParticle(aura.accentParticle, tipLoc, 1, 0.02, 0.02, 0.02, 0.01);
            world.spawnParticle(Particle.DUST, tipLoc, 1, 0.01, 0.01, 0.01, 0,
                    new Particle.DustOptions(aura.secondary, 0.85f));
            if (moving && tick % 6 == 0) {
                world.spawnParticle(Particle.SWEEP_ATTACK, tipLoc, 1, 0, 0, 0, 0);
            }
        }

        // 2. RÌA LƯỠI KIẾM (BLADE EDGES) - Kiếm cương hộ thể 2 bên thân
        if (tick % 2 == 0) {
            double edgeDist = 0.16 * SWORD_SCALE;
            Location leftEdge = swordCenter.clone().add(side.clone().multiply(edgeDist));
            Location rightEdge = swordCenter.clone().add(side.clone().multiply(-edgeDist));

            Particle.DustOptions edgeDust = new Particle.DustOptions(aura.primary, 0.65f);
            world.spawnParticle(Particle.DUST, leftEdge, 1, 0.02, 0.01, 0.02, 0, edgeDust);
            world.spawnParticle(Particle.DUST, rightEdge, 1, 0.02, 0.01, 0.02, 0, edgeDust);
        }

        // 3. LINH TỰ & CỔ VĂN (SWIRLING RUNES)
        if (tick % 4 == 0) {
            world.spawnParticle(Particle.ENCHANT, swordCenter, 2, 0.3, 0.08, 0.3, 0.3);
        }

        // 4. LINH TRẬN BÀN CHÂN (FOOT LOTUS FORMATION)
        if (tick % 3 == 0) {
            double ringRadius = 0.42;
            int ringPoints = 8;
            for (int i = 0; i < ringPoints; i++) {
                double angle = (tick * 0.12) + (i * Math.PI * 2.0 / ringPoints);
                Location footPt = swordCenter.clone()
                        .add(side.clone().multiply(Math.cos(angle) * ringRadius))
                        .add(orthoUp.clone().multiply(Math.sin(angle) * ringRadius * 0.4))
                        .add(lookDir.clone().multiply(Math.sin(angle) * ringRadius * 0.6));

                world.spawnParticle(Particle.DUST, footPt, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(i % 2 == 0 ? aura.primary : aura.secondary, 0.5f));
            }
        }

        // 5. VỆT ĐUÔI KHÍ ĐỘNG HỌC 3D (3D DUAL SPIRAL TRAIL)
        int trailPoints = moving ? 9 : 5;
        double trailLength = moving ? 3.4 : 1.8;

        for (int i = 1; i <= trailPoints; i++) {
            double progress = i / (double) trailPoints;
            double dist = progress * trailLength;
            Location trailBase = hiltLoc.clone().add(backward.clone().multiply(dist));

            double spiralRadius = 0.12 + (progress * 0.24);
            double angle1 = (tick * 0.28) + (i * 0.7);
            double angle2 = angle1 + Math.PI;

            Location pt1 = trailBase.clone()
                    .add(side.clone().multiply(Math.cos(angle1) * spiralRadius))
                    .add(orthoUp.clone().multiply(Math.sin(angle1) * spiralRadius));

            Location pt2 = trailBase.clone()
                    .add(side.clone().multiply(Math.cos(angle2) * spiralRadius))
                    .add(orthoUp.clone().multiply(Math.sin(angle2) * spiralRadius));

            world.spawnParticle(Particle.DUST, pt1, 1, 0.01, 0.01, 0.01, 0,
                    new Particle.DustOptions(aura.primary, (float) (0.75 - progress * 0.35)));
            world.spawnParticle(Particle.DUST, pt2, 1, 0.01, 0.01, 0.01, 0,
                    new Particle.DustOptions(aura.secondary, (float) (0.65 - progress * 0.35)));

            // Đốm tinh tú lấp lánh xen kẽ
            if (i == 2 || (moving && i == 6)) {
                world.spawnParticle(Particle.END_ROD, trailBase, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // 6. TIẾNG KIẾM REO BỒNG BỀNH
        if (tick % 38 == 0) {
            player.playSound(pLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.16f, 1.75f);
        }
        if (moving && tick % 25 == 0) {
            player.playSound(pLoc, Sound.ITEM_ELYTRA_FLYING, 0.10f, 1.6f);
        }
    }

    // ============================================================
    // 🌟 HIỆU ỨNG TRIỆU HOÁN (SUMMON)
    // ============================================================

    private void spawnSummonEffect(Player player) {
        Location pLoc = player.getLocation();
        World world = player.getWorld();
        PlayerData data = plugin.getDataManager().get(player);
        CultivationStage stage = (data != null) ? data.getStage() : null;
        StageAura aura = getStageAura(stage);

        // Vòng kiếm hoàn 36 hạt tỏa rộng
        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2 * i) / 36.0;
            double radius = 1.15;
            Location p = pLoc.clone().add(Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(i % 2 == 0 ? aura.primary : aura.secondary, 0.85f));
        }

        // Cột kiếm quang bắn vút từ dưới lên
        for (double y = 0; y <= 2.0; y += 0.25) {
            Location col = pLoc.clone().add(0, y, 0);
            world.spawnParticle(Particle.END_ROD, col, 2, 0.15, 0.05, 0.15, 0.02);
            world.spawnParticle(Particle.DUST, col, 2, 0.2, 0.05, 0.2, 0,
                    new Particle.DustOptions(aura.secondary, 0.7f));
        }

        world.spawnParticle(Particle.FIREWORK, pLoc.clone().add(0, 0.2, 0), 12, 0.4, 0.1, 0.4, 0.05);
        world.spawnParticle(Particle.ENCHANT, pLoc.clone().add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.8);

        world.playSound(pLoc, Sound.ITEM_TRIDENT_RIPTIDE_2, 0.85f, 1.2f);
        world.playSound(pLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.75f, 1.5f);
        world.playSound(pLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 0.7f);
    }

    // ============================================================
    // 💨 HIỆU ỨNG THU KIẾM (DISAPPEAR)
    // ============================================================

    private void spawnDisappearEffect(Location loc, CultivationStage stage) {
        World world = loc.getWorld();
        if (world == null) return;
        StageAura aura = getStageAura(stage);

        world.spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.2, 0), 8, 0.25, 0.1, 0.25, 0.03);
        world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.1, 0), 10, 0.35, 0.1, 0.35, 0.02);
        world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 0.3, 0), 16, 0.4, 0.15, 0.4, 0.6);

        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 * i) / 16.0;
            Location p = loc.clone().add(Math.cos(angle) * 0.65, 0.05, Math.sin(angle) * 0.65);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(aura.primary, 0.75f));
        }

        world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.4f);
        world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 0.5f, 1.6f);
    }

    // ============================================================
    // 🧹 DỌN DẸP PHI KIẾM
    // ============================================================

    private void removeMount(UUID uid, boolean effect) {
        ItemDisplay display = mounts.remove(uid);
        if (display == null) return;

        if (!display.isDead()) {
            if (effect) {
                Player p = Bukkit.getPlayer(uid);
                CultivationStage stage = null;
                if (p != null) {
                    PlayerData data = plugin.getDataManager().get(p);
                    if (data != null) stage = data.getStage();
                }
                spawnDisappearEffect(display.getLocation(), stage);
            }
            display.remove();
        }
    }

    /**
     * Dọn dẹp toàn bộ khi plugin tắt / reload
     */
    public void cleanup() {
        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
        }
        for (UUID uid : new HashSet<>(mounts.keySet())) {
            removeMount(uid, false);
        }
        mounts.clear();
        enabled.clear();
    }

    // ============================================================
    // EVENTS
    // ============================================================

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();

        if (!enabled.contains(uid)) {
            return;
        }

        if (event.isFlying()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && player.isFlying() && enabled.contains(uid)) {
                    spawnMount(player);
                }
            });
        } else {
            player.setFallDistance(0);
            removeMount(uid, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        enabled.remove(uid);
        removeMount(uid, false);
    }
}
