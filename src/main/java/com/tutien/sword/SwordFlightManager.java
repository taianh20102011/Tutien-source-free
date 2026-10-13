package com.tutien.sword;

import com.tutien.TuTienPlugin;
import com.tutien.data.ClassType;
import com.tutien.data.PlayerData;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SwordFlightManager V3
 *
 * NGỰ KIẾM PHI HÀNH
 *
 * Mục tiêu:
 * - Player đứng thẳng trên thanh kiếm.
 * - Kiếm nằm ngang theo hướng nhìn / hướng bay của player.
 * - Mũi kiếm hướng về phía trước.
 * - Không dùng ArmorStand.
 * - Dùng ItemDisplay + Transformation để kiểm soát chính xác rotation.
 * - Particle nhẹ, tập trung quanh kiếm và phía sau kiếm.
 *
 * Yêu cầu:
 * - Paper/Spigot API có ItemDisplay (Minecraft 1.19.4+).
 */
public class SwordFlightManager implements Listener {

    private final TuTienPlugin plugin;

    private final Set<UUID> enabled = new HashSet<>();
    private final Map<UUID, ItemDisplay> mounts = new HashMap<>();

    private static final int MIN_ORD = 10; // BAN_THANH trong he 25 canh gioi moi

    // ============================================================
    // ⚔️ CẤU HÌNH VỊ TRÍ
    // ============================================================

    /**
     * Độ cao của tâm ItemDisplay so với vị trí chân player.
     *
     * Giá trị càng GẦN 0 (vd -0.20)  → kiếm càng SÁT chân.
     * Giá trị càng ÂM sâu (vd -0.80) → kiếm càng thấp, xa chân hơn.
     *
     * Đã chỉnh sát chân hơn theo yêu cầu (cũ: -0.54).
     * Nếu vẫn muốn sát hơn nữa: chỉnh dần về -0.20 / -0.15.
     * Nếu bị lún quá xuống đất/chân lún vào kiếm: lùi về -0.40 / -0.45.
     */
    private static final double Y_OFFSET = -0.30;

    /**
     * Đưa kiếm tiến/lùi theo hướng player.
     */
    private static final double FORWARD_OFFSET = 0.0;

    /**
     * Độ cao player đứng trên kiếm.
     *
     * Đây là khoảng cách visual giữa tâm kiếm và chân.
     */
    private static final double FOOT_LIFT = 0.02;

    // ============================================================
    // ⚔️ KÍCH THƯỚC
    // ============================================================

    /**
     * Scale thanh kiếm.
     *
     * 1.0 = kích thước ItemDisplay bình thường.
     */
    private static final float SWORD_SCALE = 1.20f;

    /**
     * Xoay nhẹ mũi kiếm xuống.
     *
     * 0 = hoàn toàn nằm ngang.
     */
    private static final float SWORD_PITCH = 0.0f;

    /**
     * Góc nghiêng ngang của kiếm.
     *
     * 0 = thẳng theo hướng bay.
     */
    private static final float SWORD_ROLL = 0.0f;

    // ============================================================
    // ✨ ANIMATION
    // ============================================================

    private static final float BOB_HEIGHT = 0.018f;
    private static final float BOB_SPEED = 0.16f;

    /**
     * Không xoay thân kiếm khi bay.
     * Player đứng ổn định trên sống kiếm.
     */
    private static final float ROLL_ANIMATION = 0.0f;

    // ============================================================
    // 🌌 TRAIL
    // ============================================================

    private static final double TRAIL_LENGTH = 2.2;
    private static final int TRAIL_POINTS = 6;

    private static final Color ENERGY_BLUE =
            Color.fromRGB(80, 220, 255);

    private static final Color ENERGY_PURPLE =
            Color.fromRGB(175, 90, 255);

    private static final Color ENERGY_WHITE =
            Color.fromRGB(235, 250, 255);

    public SwordFlightManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startFollowLoop();
    }

    // ============================================================
    // TOGGLE
    // ============================================================

    public void toggle(Player player) {

        PlayerData data =
                plugin.getDataManager().get(player);

        if (data == null || !data.hasClass()) {

            player.sendMessage(
                    plugin.prefix()
                            + "§cCần chọn class trước! /tutien chon"
            );

            return;
        }

        if (data.getClassType() != ClassType.KIEM_TU) {

            player.sendMessage(
                    plugin.prefix()
                            + "§cChỉ §eKiếm Tu §cmới có thể Ngự Kiếm Phi Hành!"
            );

            return;
        }

        if (!data.getStage().canFly()
                || data.getStage().ordinal() < MIN_ORD) {

            player.sendMessage(
                    plugin.prefix()
                            + "§cCần đạt §fThánh Giới §c(Bán Thánh) trở lên để bay và ngự kiếm!"
            );

            return;
        }

        UUID uid =
                player.getUniqueId();

        // ========================================================
        // TẮT
        // ========================================================

        if (enabled.contains(uid)) {

            enabled.remove(uid);

            removeMount(
                    uid,
                    true
            );

            player.sendMessage(
                    plugin.prefix()
                            + "§7Đã thu hồi §bPhi Kiếm§7."
            );

            player.playSound(
                    player.getLocation(),
                    Sound.BLOCK_BEACON_DEACTIVATE,
                    0.7f,
                    1.25f
            );

            return;
        }

        // ========================================================
        // BẬT
        // ========================================================

        enabled.add(uid);

        player.sendMessage(
                plugin.prefix()
                        + "§b✦ §fNgự Kiếm Phi Hành §bđã bật!"
                        + " §7Bay lên §e(Space x2) §7để đứng lên kiếm."
        );

        player.playSound(
                player.getLocation(),
                Sound.ITEM_TRIDENT_RIPTIDE_1,
                0.75f,
                1.35f
        );

        if (player.isFlying()) {
            spawnMount(player);
        }
    }

    // ============================================================
    // ⚔️ TẠO PHI KIẾM
    // ============================================================

    private void spawnMount(Player player) {

        UUID uid =
                player.getUniqueId();

        ItemDisplay existing =
                mounts.get(uid);

        if (existing != null
                && !existing.isDead()) {

            return;
        }

        mounts.remove(uid);

        try {

            Location loc =
                    getSwordLocation(player);

            ItemDisplay display =
                    player.getWorld()
                            .spawn(
                                    loc,
                                    ItemDisplay.class
                            );

            // ====================================================
            // ITEM
            // ====================================================

            display.setItemStack(
                    createSword()
            );

            /*
             * NONE = không tự áp dụng transform kiểu GUI/GROUND.
             *
             * Toàn bộ hướng kiếm được điều khiển bằng
             * Transformation bên dưới.
             */
            display.setItemDisplayTransform(
                    ItemDisplay.ItemDisplayTransform.NONE
            );

            display.setBillboard(
                    Display.Billboard.FIXED
            );

            display.setGravity(false);
            display.setInvulnerable(true);
            display.setPersistent(false);
            display.setSilent(true);

            display.setInterpolationDuration(1);
            display.setInterpolationDelay(0);

            // ====================================================
            // ROTATION
            // ====================================================

            applySwordTransformation(
                    display,
                    player,
                    0
            );

            mounts.put(
                    uid,
                    display
            );

            // ====================================================
            // SUMMON EFFECT
            // ====================================================

            spawnSummonEffect(
                    player
            );

        } catch (Exception e) {

            plugin.getLogger().warning(
                    "[SwordFlight] Không thể tạo Phi Kiếm cho "
                            + player.getName()
                            + ": "
                            + e.getMessage()
            );
        }
    }

    // ============================================================
    // ⚔️ ITEM
    // ============================================================

    private ItemStack createSword() {

        ItemStack sword =
                new ItemStack(
                        Material.NETHERITE_SWORD
                );

        ItemMeta meta =
                sword.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(plugin.msg("auto_b473af6861"));

            meta.addItemFlags(
                    org.bukkit.inventory.ItemFlag.values()
            );

            sword.setItemMeta(meta);
        }

        return sword;
    }

    // ============================================================
    // 🔄 TRANSFORMATION
    // ============================================================

    private void applySwordTransformation(
            ItemDisplay display,
            Player player,
            long tick
    ) {

        Location pLoc =
                player.getLocation();

        /*
         * Minecraft yaw:
         * 0   = South
         * 90  = West
         * 180 = North
         * -90 = East
         */

        float yaw =
                (float) Math.toRadians(
                        -pLoc.getYaw()
                );

        float pitch =
                pLoc.getPitch();

        float bob =
                (float)
                        Math.sin(
                                tick * BOB_SPEED
                        ) * BOB_HEIGHT;

        /*
         * ĐÃ KHẮC PHỤC LỖI KIẾM DỰNG THẲNG:
         * ItemDisplayTransform.NONE giữ nguyên không gian gốc của model Netherite Sword (vốn dĩ nằm dọc theo trục Y/Z tùy phiên bản).
         * Để kiếm nằm ngang song song mặt đất và mũi kiếm hướng thẳng về trước theo hướng nhìn của người chơi:
         * 1. Xoay 90 độ quanh trục X để lật kiếm từ trạng thái dọc sang nằm ngang.
         * 2. Kết hợp với góc pitch từ hướng nhìn của người chơi.
         */
        Quaternionf rotation =
                new Quaternionf()
                        .rotateY(yaw)
                        .rotateX(
                                (float)
                                        Math.toRadians(
                                                90.0f + pitch + SWORD_PITCH
                                        )
                        );

        Transformation transformation =
                new Transformation(

                        // Translation
                        new Vector3f(
                                0f,
                                (float) (FOOT_LIFT + bob),
                                0f
                        ),

                        // Left rotation
                        rotation,

                        // Scale
                        new Vector3f(
                                SWORD_SCALE,
                                SWORD_SCALE,
                                SWORD_SCALE
                        ),

                        // Right rotation
                        new Quaternionf()
                );

        display.setInterpolationDelay(0);
        display.setTransformation(
                transformation
        );
    }

    // ============================================================
    // 📍 VỊ TRÍ
    // ============================================================

    private Location getSwordLocation(
            Player player
    ) {

        Location pLoc =
                player.getLocation();

        Vector dir =
                pLoc.getDirection()
                        .setY(0);

        if (dir.lengthSquared()
                > 0.0001) {

            dir.normalize();
        }

        Location loc =
                pLoc.clone()
                        .add(
                                dir.multiply(
                                        FORWARD_OFFSET
                                )
                        )
                        .add(
                                0,
                                Y_OFFSET,
                                0
                        );

        loc.setYaw(
                pLoc.getYaw()
        );

        loc.setPitch(0);

        return loc;
    }

    // ============================================================
    // 🔄 FOLLOW LOOP
    // ============================================================

    private void startFollowLoop() {

        new BukkitRunnable() {

            long tick = 0;

            @Override
            public void run() {

                tick++;

                for (UUID uid :
                        new HashSet<>(
                                mounts.keySet()
                        )) {

                    Player player =
                            Bukkit.getPlayer(uid);

                    ItemDisplay display =
                            mounts.get(uid);

                    // ==========================================
                    // PLAYER KHÔNG TỒN TẠI
                    // ==========================================

                    if (player == null
                            || !player.isOnline()) {

                        removeMount(uid);

                        continue;
                    }

                    // ==========================================
                    // KHÔNG CÒN BAY
                    // ==========================================

                    if (!player.isFlying()) {

                        removeMount(
                                uid,
                                true
                        );

                        continue;
                    }

                    // ==========================================
                    // DISPLAY HỎNG
                    // ==========================================

                    if (display == null
                            || display.isDead()) {

                        mounts.remove(uid);

                        spawnMount(
                                player
                        );

                        continue;
                    }

                    // ==========================================
                    // FOLLOW PLAYER
                    // ==========================================

                    Location target =
                            getSwordLocation(
                                    player
                            );

                    display.teleport(
                            target
                    );

                    // ==========================================
                    // ROTATION
                    // ==========================================

                    applySwordTransformation(
                            display,
                            player,
                            tick
                    );

                    // ==========================================
                    // PARTICLES
                    // ==========================================

                    spawnFlightParticles(
                            player,
                            display,
                            tick
                    );
                }
            }

        }.runTaskTimer(
                plugin,
                1L,
                1L
        );
    }

    // ============================================================
    // ✨ PARTICLES
    // ============================================================

    private void spawnFlightParticles(
            Player player,
            ItemDisplay sword,
            long tick
    ) {

        Location base =
                sword.getLocation();

        World world =
                player.getWorld();

        Vector forward =
                getHorizontalDirection(
                        player
                );

        Vector backward =
                forward.clone()
                        .multiply(-1);

        // ========================================================
        // 1. LINH KHÍ SÁT THÂN KIẾM
        // ========================================================

        if (tick % 2 == 0) {

            for (int i = 0; i < 2; i++) {

                Location p =
                        base.clone()
                                .add(
                                        (Math.random() - 0.5)
                                                * 0.75,
                                        (Math.random() - 0.5)
                                                * 0.12,
                                        (Math.random() - 0.5)
                                                * 0.75
                                );

                world.spawnParticle(
                        Particle.END_ROD,
                        p,
                        1,
                        0,
                        0,
                        0,
                        0
                );
            }
        }

        // ========================================================
        // 2. TRAIL SAU PHI KIẾM
        // ========================================================

        if (tick % 2 == 0) {

            for (int i = 1;
                 i <= TRAIL_POINTS;
                 i++) {

                double progress =
                        i / (double)
                                TRAIL_POINTS;

                double distance =
                        progress
                                * TRAIL_LENGTH;

                double wave =
                        Math.sin(
                                tick * 0.18
                                        + i * 0.8
                        )
                                * 0.08;

                Location trail =
                        base.clone()
                                .add(
                                        backward
                                                .clone()
                                                .multiply(
                                                        distance
                                                )
                                )
                                .add(
                                        0,
                                        wave,
                                        0
                                );

                Particle.DustOptions dust =
                        new Particle.DustOptions(
                                i % 3 == 0
                                        ? ENERGY_PURPLE
                                        : ENERGY_BLUE,
                                0.55f
                        );

                world.spawnParticle(
                        Particle.DUST,
                        trail,
                        1,
                        0.025,
                        0.025,
                        0.025,
                        0,
                        dust
                );

                if (i == 2
                        || i == 5) {

                    world.spawnParticle(
                            Particle.END_ROD,
                            trail,
                            1,
                            0.02,
                            0.02,
                            0.02,
                            0
                    );
                }
            }
        }

        // ========================================================
        // 3. VÒNG LINH LỰC NHỎ
        // ========================================================

        if (tick % 3 == 0) {

            double radius =
                    0.45;

            for (int i = 0;
                 i < 8;
                 i++) {

                double angle =
                        tick * 0.18
                                + (
                                Math.PI * 2
                                        * i
                                        / 8
                        );

                Location p =
                        base.clone()
                                .add(
                                        Math.cos(angle)
                                                * radius,
                                        0.02,
                                        Math.sin(angle)
                                                * radius
                                );

                world.spawnParticle(
                        Particle.DUST,
                        p,
                        1,
                        0,
                        0,
                        0,
                        0,
                        new Particle.DustOptions(
                                i % 2 == 0
                                        ? ENERGY_BLUE
                                        : ENERGY_PURPLE,
                                0.45f
                        )
                );
            }
        }

        // ========================================================
        // 4. SPARKS
        // ========================================================

        if (tick % 7 == 0) {

            Location spark =
                    base.clone()
                            .add(
                                    backward
                                            .clone()
                                            .multiply(
                                                    0.5
                                            )
                            )
                            .add(
                                    (Math.random()
                                            - 0.5)
                                            * 0.5,
                                    Math.random()
                                            * 0.25,
                                    (Math.random()
                                            - 0.5)
                                            * 0.5
                            );

            world.spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    spark,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.01
            );
        }
    }

    // ============================================================
    // 🧭 HƯỚNG NGANG
    // ============================================================

    private Vector getHorizontalDirection(
            Player player
    ) {

        Vector dir =
                player.getLocation()
                        .getDirection()
                        .setY(0);

        if (dir.lengthSquared()
                < 0.0001) {

            return new Vector(
                    0,
                    0,
                    1
            );
        }

        return dir.normalize();
    }

    // ============================================================
    // 🌟 SUMMON
    // ============================================================

    private void spawnSummonEffect(
            Player player
    ) {

        Location loc =
                getSwordLocation(
                        player
                );

        World world =
                player.getWorld();

        // ========================================================
        // VÒNG TRIỆU HỒI
        // ========================================================

        for (int i = 0;
             i < 18;
             i++) {

            double angle =
                    Math.PI * 2
                            * i
                            / 18;

            Location p =
                    loc.clone()
                            .add(
                                    Math.cos(angle)
                                            * 0.65,
                                    0,
                                    Math.sin(angle)
                                            * 0.65
                            );

            world.spawnParticle(
                    Particle.DUST,
                    p,
                    1,
                    0,
                    0,
                    0,
                    0,
                    new Particle.DustOptions(
                            i % 2 == 0
                                    ? ENERGY_BLUE
                                    : ENERGY_PURPLE,
                            0.6f
                    )
            );
        }

        world.spawnParticle(
                Particle.END_ROD,
                loc,
                8,
                0.45,
                0.05,
                0.45,
                0.01
        );

        world.spawnParticle(
                Particle.ENCHANT,
                loc,
                12,
                0.45,
                0.08,
                0.45,
                0.04
        );

        world.playSound(
                loc,
                Sound.BLOCK_BEACON_POWER_SELECT,
                0.7f,
                1.5f
        );

        world.playSound(
                loc,
                Sound.ITEM_TRIDENT_RIPTIDE_1,
                0.55f,
                1.65f
        );
    }

    // ============================================================
    // 💨 DISAPPEAR
    // ============================================================

    private void spawnDisappearEffect(
            Location loc
    ) {

        World world =
                loc.getWorld();

        if (world == null) {
            return;
        }

        world.spawnParticle(
                Particle.END_ROD,
                loc,
                8,
                0.25,
                0.06,
                0.25,
                0.01
        );

        world.spawnParticle(
                Particle.CLOUD,
                loc,
                6,
                0.22,
                0.04,
                0.22,
                0.015
        );

        world.spawnParticle(
                Particle.ENCHANT,
                loc,
                10,
                0.25,
                0.05,
                0.25,
                0.03
        );

        world.playSound(
                loc,
                Sound.BLOCK_BEACON_DEACTIVATE,
                0.5f,
                1.5f
        );
    }

    // ============================================================
    // 🧹 REMOVE
    // ============================================================

    private void removeMount(
            UUID uid
    ) {

        removeMount(
                uid,
                false
        );
    }

    private void removeMount(
            UUID uid,
            boolean effect
    ) {

        ItemDisplay display =
                mounts.remove(uid);

        if (display == null) {
            return;
        }

        if (!display.isDead()) {

            if (effect) {

                spawnDisappearEffect(
                        display.getLocation()
                );
            }

            display.remove();
        }
    }

    // ============================================================
    // EVENT: DOUBLE SPACE
    // ============================================================

    @EventHandler
    public void onToggleFlight(
            PlayerToggleFlightEvent event
    ) {

        Player player =
                event.getPlayer();

        UUID uid =
                player.getUniqueId();

        if (!enabled.contains(uid)) {
            return;
        }

        if (event.isFlying()) {

            Bukkit.getScheduler()
                    .runTask(
                            plugin,
                            () -> {

                                if (player.isOnline()
                                        && player.isFlying()
                                        && enabled.contains(uid)) {

                                    spawnMount(
                                            player
                                    );
                                }
                            }
                    );

        } else {

            removeMount(
                    uid,
                    true
            );
        }
    }

    // ============================================================
    // EVENT: QUIT
    // ============================================================

    @EventHandler
    public void onQuit(
            PlayerQuitEvent event
    ) {

        UUID uid =
                event.getPlayer()
                        .getUniqueId();

        enabled.remove(uid);

        removeMount(
                uid
        );
    }
}