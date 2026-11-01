package com.tutien.element;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Danh sách Dị Hỏa & Thần Lôi Thiên Địa trong truyền thuyết Tu Tiên.
 */
public enum BanMenhElement {

    // === DỊ HỎA (HEAVENLY FLAMES) ===
    THANH_LIEN_DIA_TAM_HOA(
            "thanh_lien",
            "Thanh Liên Địa Tâm Hỏa",
            "§a",
            ElementType.DI_HOA,
            Particle.DUST,
            new Particle.DustOptions(Color.fromRGB(46, 204, 113), 1.5f), // Xanh lục bảo
            Sound.BLOCK_FIRE_AMBIENT,
            Sound.ENTITY_BLAZE_SHOOT,
            "Lửa sinh từ lòng đất sâu nghìn trượng, ngưng tụ 10 vạn năm thành hoa sen ngọc bích.",
            15, // Cần Kết Đan trở lên (ordinal 2)
            0.20, // +20% sát thương chuẩn đốt cháy
            0.15, // +15% tỉ lệ luyện đan thần cấp
            0.12  // +12% tỉ lệ rèn pháp bảo
    ),
    COT_LANH_DU_HOA(
            "cot_lanh",
            "Cốt Lãnh Du Hỏa",
            "§b",
            ElementType.DI_HOA,
            Particle.SOUL_FIRE_FLAME,
            null,
            Sound.BLOCK_LAVA_EXTINGUISH,
            Sound.ENTITY_BLAZE_BURN,
            "Cực hàn hóa cực nhiệt, băng hỏa giao hòa sinh ra nơi sâu thẳm hắc ám vạn năm.",
            18,
            0.28,
            0.22,
            0.18
    ),
    TAM_THIEN_DIEM_VIEM_HOA(
            "tam_thien",
            "Tam Thiên Diễm Viêm Hỏa",
            "§5",
            ElementType.DI_HOA,
            Particle.DUST,
            new Particle.DustOptions(Color.fromRGB(155, 89, 182), 1.6f), // Tím tinh tú
            Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
            Sound.ENTITY_BLAZE_SHOOT,
            "Hình thành nơi hư không vũ trụ, nuốt chửng ánh sáng tinh tú, sinh mệnh bất tử.",
            22,
            0.35,
            0.30,
            0.25
    ),
    TINH_LAM_CUU_U_HOA(
            "tinh_lam",
            "Tịnh Lam Cửu U Hỏa",
            "§9",
            ElementType.DI_HOA,
            Particle.SCULK_SOUL,
            null,
            Sound.BLOCK_FIRE_AMBIENT,
            Sound.ENTITY_BLAZE_SHOOT,
            "Ngọn lửa U Minh thanh tẩy linh hồn, đốt cháy từ tận căn cơ chân nguyên của kẻ thù.",
            25,
            0.42,
            0.35,
            0.30
    ),
    HAC_DIEM_TU_THAN(
            "hac_diem",
            "Hắc Diễm Thôn Thiên",
            "§8§l",
            ElementType.DI_HOA,
            Particle.LARGE_SMOKE,
            null,
            Sound.ENTITY_WITHER_AMBIENT,
            Sound.ENTITY_WITHER_SHOOT,
            "Hắc hỏa tà thần thôn phệ vạn vật thiên địa, thiêu rụi cả thần hồn lẫn nguyên anh.",
            28,
            0.50,
            0.40,
            0.38
    ),

    // === THẦN LÔI (DIVINE THUNDERS) ===
    CUU_THIEN_HUYEN_LOI(
            "cuu_thien",
            "Cửu Thiên Huyền Lôi",
            "§d",
            ElementType.THAN_LOI,
            Particle.DUST,
            new Particle.DustOptions(Color.fromRGB(186, 85, 211), 1.6f), // Tím sấm sét
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            Sound.ITEM_TRIDENT_THUNDER,
            "Sấm sét trên chín tầng mây, đánh nát tà ma chướng khí, tốc độ sấm chớp giật liên hoàn.",
            16,
            0.22,
            0.12,
            0.20
    ),
    NGU_SAC_THAN_LOI(
            "ngu_sac",
            "Ngũ Sắc Thần Lôi",
            "§e",
            ElementType.THAN_LOI,
            Particle.FIREWORK,
            null,
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
            "Tập hợp nguyên lực ngũ hành âm dương bộc phá, sấm chớp ngũ sắc xé toạc hư không.",
            20,
            0.30,
            0.18,
            0.28
    ),
    TU_TIEU_THAN_LOI(
            "tu_tieu",
            "Tử Tiêu Thần Lôi",
            "§5§l",
            ElementType.THAN_LOI,
            Particle.DUST,
            new Particle.DustOptions(Color.fromRGB(128, 0, 128), 1.8f), // Tím đậm chí tôn
            Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
            Sound.ENTITY_WARDEN_SONIC_BOOM,
            "Thiên lôi bản nguyên của Thiên Đạo chí tôn, trừng phạt nghịch thiên chi nhân.",
            26,
            0.45,
            0.25,
            0.35
    );

    public enum ElementType {
        DI_HOA("Dị Hỏa", "🔥", "§6"),
        THAN_LOI("Thần Lôi", "⚡", "§d");

        private final String name;
        private final String icon;
        private final String color;

        ElementType(String name, String icon, String color) {
            this.name = name;
            this.icon = icon;
            this.color = color;
        }

        public String getName() { return name; }
        public String getIcon() { return icon; }
        public String getColor() { return color; }
    }

    private final String id;
    private final String displayName;
    private final String color;
    private final ElementType type;
    private final Particle particle;
    private final Particle.DustOptions dustOptions;
    private final Sound ambientSound;
    private final Sound triggerSound;
    private final String description;
    private final int minStageRequired; // Cảnh giới tối thiểu để dung hợp
    private final double damageMultiplier;
    private final double alchemyBonus;
    private final double forgeBonus;

    BanMenhElement(String id, String displayName, String color, ElementType type,
                   Particle particle, Particle.DustOptions dustOptions,
                   Sound ambientSound, Sound triggerSound, String description,
                   int minStageRequired, double damageMultiplier,
                   double alchemyBonus, double forgeBonus) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.type = type;
        this.particle = particle;
        this.dustOptions = dustOptions;
        this.ambientSound = ambientSound;
        this.triggerSound = triggerSound;
        this.description = description;
        this.minStageRequired = minStageRequired;
        this.damageMultiplier = damageMultiplier;
        this.alchemyBonus = alchemyBonus;
        this.forgeBonus = forgeBonus;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public ElementType getType() { return type; }
    public Particle getParticle() { return particle; }
    public Particle.DustOptions getDustOptions() { return dustOptions; }
    public Sound getAmbientSound() { return ambientSound; }
    public Sound getTriggerSound() { return triggerSound; }
    public String getDescription() { return description; }
    public int getMinStageRequired() { return minStageRequired; }
    public double getDamageMultiplier() { return damageMultiplier; }
    public double getAlchemyBonus() { return alchemyBonus; }
    public double getForgeBonus() { return forgeBonus; }

    public static BanMenhElement fromId(String id) {
        if (id == null) return null;
        for (BanMenhElement el : values()) {
            if (el.getId().equalsIgnoreCase(id)) return el;
        }
        return null;
    }
}
