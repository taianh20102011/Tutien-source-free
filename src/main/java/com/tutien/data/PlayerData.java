package com.tutien.data;

import com.tutien.lingcan.LinhCanElement;
import com.tutien.lingcan.LinhCanGrade;
import com.tutien.toc.TocType;
import com.tutien.economy.TienManager;
import com.tutien.TuTienPlugin;

import java.util.*;

public class PlayerData {

    private final UUID uuid;
    private String playerName;

    // === CULTIVATION ===
    private ClassType classType;
    private DaoPath daoPath;
    private CultivationStage stage;
    private int rank;
    private long linhKhi;
    private long totalLinhKhiEarned;

    // === LINH CAN ===
    private List<LinhCanElement> linhCan = new ArrayList<>();
    private boolean linhCanRevealed = false;

    private double btAtkBonus = 0.0; // Thien Long Kinh accumulated bonus

    // === TOC (Race) ===
    private TocType toc = null;
    private boolean tocRevealed = false;

    // === BREAKTHROUGH STATE ===
    private boolean pendingBreakthrough = false;
    private boolean inBreakthroughTrial = false;
    private boolean inThienDinh         = false;  // Dang trong buoc 1: Thien Dinh (Vuot Tam Ma)
    private boolean pendingDotPha        = false;  // Thien Dinh thanh cong, cho buoc 2 / Dot Pha

    // === STATS ===
    private int kills, deaths, bossKills, breakthroughSuccess, breakthroughFail;
    // Rieng cho leaderboard: pvpKills CHI tinh kill nguoi choi khac (kills o tren dang gop
    // ca kill quai thuong), bossToiThuongKills CHI tinh rieng con Boss Toi Thuong (bossKills
    // o tren dang gop moi loai boss noi chung - Wither, Ender Dragon, quai dat ten rieng...).
    private int pvpKills, bossToiThuongKills;
    // Legacy field kept only for backwards-compatible data migration. Vault is the source of truth.
    private long tien = 0L;
    private static TienManager economyManager;

    // === SKILL COOLDOWNS ===
    private final Map<String, Long> skillCooldowns = new HashMap<>();
    // === ACTIVE BUFFS (id -> expire timestamp ms) ===
    private final Map<String, Long> activeBuffs = new HashMap<>();

    // === AURA TOGGLE ===
    private boolean auraEnabled = true;

    // === COMBO SKILL: active sub-skill index per slot (0-3) ===
    private final int[] activeSubSkill = {0, 0, 0, 0};

    // === FLAGS ===
    // === Thien dinh ===
    private boolean meditating = false;
    private long    meditateStart = 0L;

    // === Daily Quest ===
    private long lastDailyReset = 0L;
    private int  dailyKills = 0;
    private boolean dailyKillClaimed = false;
    private boolean dailyBtClaimed   = false;

    // === Trao doi thao duoc ===
    private int thaoDuocCount = 0;

    // === Dong tu ===
    private long lastGroupBonusTime = 0L;

    private boolean isInvisible = false;
    private boolean hasCounterBuff = false;
    private boolean hasDodgeBuff = false;

    public static void bindEconomy(TienManager manager) { economyManager = manager; }

    public PlayerData(UUID uuid) {
        this(uuid, "Unknown");
    }

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid; this.playerName = playerName;
        this.stage = CultivationStage.LUYEN_KHI;
        this.rank  = 1; this.linhKhi = 0;
    }

    // ====== LINH KHI ======

    public void addLinhKhi(long amount) {
        if (classType == ClassType.PHAP_TU)  amount = (long)(amount * 1.30);
        if (!linhCan.isEmpty()) amount = (long)(amount * LinhCanGrade.combinedLkBonus(linhCan) * LinhCanGrade.fromCount(linhCan.size()).getLkMultiplier());
        if (daoPath != null) amount = (long) (amount * daoPath.getLkMult());
        com.tutien.TuTienPlugin plugin = com.tutien.TuTienPlugin.getInstance();
        if (plugin != null && plugin.getConGPhapManager() != null) {
            amount = (long) (amount * plugin.getConGPhapManager().getLinhKhiMultiplier(this));
        }
        // Ban Menh Chau: +50%/cap, toi da 10 cap (+500%)
        int chauLvl = getExtraInt("banmenh_chau_lvl", 0);
        if (chauLvl > 0) amount = (long)(amount * (1.0 + chauLvl * 0.5));
        // Season: class manh nhat mua nay tu luyen nhanh hon
        com.tutien.season.SeasonManager sm = season();
        if (sm != null) amount = (long)(amount * sm.getClassCultivationMultiplier(classType));

        long max = getMaxLinhKhi();
        // CHAN LK O MAX - khong cho vuot qua, tranh tran so o canh gioi cao
        // (trước đây không có giới hạn này khiến thành LK "mất kiểm soát" khi thiền định lâu)
        if (max > 0) {
            linhKhi = Math.min(max, linhKhi + amount);
        } else {
            linhKhi += amount;
        }
        totalLinhKhiEarned += Math.max(0, amount);
    }

    public long getMaxLinhKhi() { return stage.getMaxLinhKhiForRank(rank); }

    public boolean spendLinhKhi(long amount) {
        if (amount <= 0) return true;
        if (linhKhi < amount) return false;
        linhKhi -= amount;
        return true;
    }

    public boolean doBreakthrough() {
        if (linhKhi < getMaxLinhKhi()) return false;
        linhKhi -= getMaxLinhKhi();
        if (stage.hasNextRank(rank)) { rank++; }
        else {
            CultivationStage next = stage.nextStage();
            if (next == null) return false;
            stage = next; rank = 1;
        }
        breakthroughSuccess++;
        return true;
    }

    public void failBreakthrough() {
        breakthroughFail++;
        TuTienPlugin p = TuTienPlugin.getInstance();
        int rankLoss = p != null
                ? Math.max(1, p.getConfig().getInt("cultivation.breakthrough.fail-rank-loss", 1))
                : 1;
        for (int i = 0; i < rankLoss; i++) {
            if (rank > 1) { rank--; }
            else {
                CultivationStage prev = stage.prevStage();
                if (prev != null) { stage = prev; rank = prev.getMaxRank(); }
                else break; // da o day thap nhat, khong the tut them
            }
        }
        linhKhi = 0;
    }

    // ====== SEASON (meta mua) ======

    /** SeasonManager hien tai, null-safe (vd goi truoc khi plugin enable xong). */
    private com.tutien.season.SeasonManager season() {
        TuTienPlugin p = TuTienPlugin.getInstance();
        return p != null ? p.getSeasonManager() : null;
    }

    // ====== DAMAGE CALC ======

    public double calculateDamageMultiplier() {
        double m = stage.getAttackMultiplier(rank);
        if (classType == ClassType.KIEM_TU) m *= 1.5;
        if (daoPath != null) m *= daoPath.getAtkBonus();
        com.tutien.TuTienPlugin plugin = com.tutien.TuTienPlugin.getInstance();
        if (plugin != null && plugin.getConGPhapManager() != null) m *= plugin.getConGPhapManager().getAttackMultiplier(this);
        if (!linhCan.isEmpty()) m *= LinhCanGrade.combinedAtkBonus(linhCan);
        // Ban Menh Kiem: +5%/cap, toi da 20 cap (+100%)
        int kiemLvl = getExtraInt("banmenh_kiem_lvl", 0);
        if (kiemLvl > 0) m *= (1.0 + kiemLvl * 0.05);
        if (hasPassive("ATK")) m *= (1.0 + passiveOrdinalBonus());
        // Season: class/dao manh nhat mua nay (xem SeasonManager, "/ttadmin season set")
        com.tutien.season.SeasonManager sm = season();
        if (sm != null) {
            m *= sm.getClassAtkMultiplier(classType);
            m *= sm.getDaoAtkMultiplier(daoPath);
        }
        return m;
    }

    public double calculateDefenseMultiplier() {
        double m = 1.0 + (stage.ordinal() * 0.15);
        if (classType == ClassType.THE_TU && isModuleEnabled("the_chat")) m *= 1.5;
        if (hasBuff("thietbich"))          m *= 1.8;
        if (daoPath != null)               m *= daoPath.getDefBonus();
        com.tutien.TuTienPlugin plugin = com.tutien.TuTienPlugin.getInstance();
        if (plugin != null && plugin.getConGPhapManager() != null) m *= plugin.getConGPhapManager().getDefenseMultiplier(this);
        if (!linhCan.isEmpty()) m *= LinhCanGrade.combinedDefBonus(linhCan);
        // Ban Menh Giap: +3%/cap, toi da 20 cap (+60%)
        int giapLvl = getExtraInt("banmenh_giap_lvl", 0);
        if (giapLvl > 0) m *= (1.0 + giapLvl * 0.03);
        if (hasPassive("DEF")) m *= (1.0 + passiveOrdinalBonus());
        // Season: class/dao manh nhat mua nay
        com.tutien.season.SeasonManager sm = season();
        if (sm != null) {
            m *= sm.getClassDefMultiplier(classType);
            m *= sm.getDaoDefMultiplier(daoPath);
        }
        return m;
    }

    public double calculateMaxHealth() {
        double hp = 20.0 * stage.getHealthMultiplier(rank);
        if (classType == ClassType.THE_TU && isModuleEnabled("the_chat")) hp *= 10.0;
        if (hasPassive("HP")) hp *= (1.0 + passiveOrdinalBonus());
        return Math.min(hp, 2048);
    }

    public double getBreakthroughDifficulty() {
        double base = stage.getBreakthroughDifficulty();
        if (daoPath != null) base *= daoPath.getBreakDifficultyMult();
        return Math.min(base, 0.99);
    }

    // ====== SKILLS ======

    public boolean isSkillOnCooldown(String id) {
        Long cd = skillCooldowns.get(id);
        return cd != null && cd > System.currentTimeMillis();
    }

    public long getSkillCooldownRemaining(String id) {
        Long cd = skillCooldowns.get(id);
        return cd == null ? 0 : Math.max(0, cd - System.currentTimeMillis());
    }

    public void setSkillCooldown(String id, long ms) {
        skillCooldowns.put(id, System.currentTimeMillis() + ms);
    }

    // ====== BUFFS ======

    public void addBuff(String id, long ms) {
        activeBuffs.put(id, System.currentTimeMillis() + ms);
    }

    public boolean hasBuff(String id) {
        Long exp = activeBuffs.get(id);
        if (exp == null) return false;
        if (exp <= System.currentTimeMillis()) { activeBuffs.remove(id); return false; }
        return true;
    }

    public long getBuffExpire(String id) {
        return activeBuffs.getOrDefault(id, 0L);
    }

    public void removeBuff(String id) { activeBuffs.remove(id); }

    // ====== GETTERS / SETTERS ======

    public UUID   getUuid()         { return uuid; }
    public String getPlayerName()   { return playerName; }
    public void   setPlayerName(String n) { this.playerName = n; }

    public ClassType getClassType() { return classType; }
    public void setClassType(ClassType c) { this.classType = c; }
    public boolean hasClass()       { return classType != null; }

    public DaoPath getDaoPath()     { return daoPath; }
    public void setDaoPath(DaoPath d) { this.daoPath = d; }
    public boolean hasDaoPath()     { return daoPath != null; }

    public CultivationStage getStage()      { return stage; }
    public void setStage(CultivationStage s){ this.stage = s; }

    public int  getRank()               { return rank; }
    public void setRank(int r)          { this.rank = Math.max(1, Math.min(stage != null ? stage.getMaxRank() : 9, r)); }

    public long getLinhKhi()            { return linhKhi; }
    public void setLinhKhi(long v)      { this.linhKhi = v; }
    public long getTotalLinhKhiEarned() { return totalLinhKhiEarned; }

    // Linh Can
    public List<LinhCanElement> getLinhCan()         { return linhCan; }
    public void setLinhCan(List<LinhCanElement> lc)  { this.linhCan = lc; }
    public boolean hasLinhCan()                      { return !linhCan.isEmpty(); }
    public boolean isLinhCanRevealed()               { return linhCanRevealed; }
    public void setLinhCanRevealed(boolean b)        { this.linhCanRevealed = b; }

    public int  getKills()     { return kills; }
    public void addKill()      { kills++; }
    public void setKills(int v){ kills = v; }
    public int  getDeaths()    { return deaths; }
    public void addDeath()     { deaths++; }
    public void setDeaths(int v){ deaths = v; }
    public int  getBossKills() { return bossKills; }
    public void addBossKill()  { bossKills++; }
    public void setBossKills(int v){ bossKills = v; }
    public int  getPvpKills()  { return pvpKills; }
    public void addPvpKill()   { pvpKills++; }
    public void setPvpKills(int v) { pvpKills = v; }
    public int  getBossToiThuongKills()  { return bossToiThuongKills; }
    public void addBossToiThuongKill()   { bossToiThuongKills++; }
    public void setBossToiThuongKills(int v) { bossToiThuongKills = v; }

    public int  getBreakthroughSuccess() { return breakthroughSuccess; }
    public int  getBreakthroughFail()    { return breakthroughFail; }

    public boolean isPendingBreakthrough()           { return pendingBreakthrough; }
    public void setPendingBreakthrough(boolean b)    { this.pendingBreakthrough = b; }
    public boolean isInBreakthroughTrial()           { return inBreakthroughTrial; }
    public void setInBreakthroughTrial(boolean b)    { this.inBreakthroughTrial = b; }
    public boolean isInThienDinh()                   { return inThienDinh; }
    public void setInThienDinh(boolean b)            { this.inThienDinh = b; }
    public boolean isPendingDotPha()                 { return pendingDotPha; }
    public void setPendingDotPha(boolean b)          { this.pendingDotPha = b; }

    public long getTien() {
        return economyManager != null ? economyManager.getBalance(this) : Math.max(0L, tien);
    }

    public long getLegacyTien() { return Math.max(0L, tien); }

    public void setTien(long v) {
        long target = Math.max(0L, v);
        if (economyManager != null) economyManager.setBalance(this, target);
        this.tien = target; // legacy cache for old files
    }

    public void addTien(long v) {
        if (v <= 0) return;
        if (economyManager != null) {
            OfflinePlayerAdapter.deposit(economyManager, this, v);
        } else {
            this.tien += v;
        }
    }

    public boolean spendTien(long v) {
        if (v <= 0) return true;
        if (economyManager != null) return OfflinePlayerAdapter.withdraw(economyManager, this, v);
        if (tien < v) return false;
        tien -= v;
        return true;
    }

    /**
     * Tiny adapter avoids exposing Vault types from PlayerData's public API.
     */
    private static final class OfflinePlayerAdapter {
        static void deposit(TienManager manager, PlayerData data, long amount) {
            manager.give(org.bukkit.Bukkit.getOfflinePlayer(data.getUuid()).getPlayer(), amount);
            // Vault providers may support offline deposits; use the direct bridge if player is offline.
            if (org.bukkit.Bukkit.getPlayer(data.getUuid()) == null) {
                manager.giveOffline(data, amount);
            }
        }
        static boolean withdraw(TienManager manager, PlayerData data, long amount) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(data.getUuid());
            return p != null ? manager.spend(p, amount) : manager.spendOffline(data, amount);
        }
    }

    public double getBtAtkBonus()            { return btAtkBonus; }
    public void setBtAtkBonus(double v)      { this.btAtkBonus = v; }

    public TocType getToc()                  { return toc; }
    public void setToc(TocType t)            { this.toc = t; }
    public boolean hasToc()                  { return toc != null; }
    public boolean isTocRevealed()           { return tocRevealed; }
    public void setTocRevealed(boolean b)    { this.tocRevealed = b; }

    public void clearAllCooldowns() {
        skillCooldowns.clear();
    }

    public boolean isAuraEnabled()        { return auraEnabled; }
    public void setAuraEnabled(boolean b) { this.auraEnabled = b; }

    public int  getActiveSubSkill(int slot) { return slot >= 0 && slot < 4 ? activeSubSkill[slot] : 0; }
    public void setActiveSubSkill(int slot, int idx) { if (slot >= 0 && slot < 4) activeSubSkill[slot] = idx; }
    public void cycleSubSkill(int slot, int maxIdx) {
        if (slot < 0 || slot >= 4) return;
        activeSubSkill[slot] = maxIdx <= 0 ? 0 : (activeSubSkill[slot] + 1) % maxIdx;
    }

    // ========================
    //  TUI DO TU TIEN (GUI bag storage)
    // ========================
    private final org.bukkit.inventory.ItemStack[] bagStorage = new org.bukkit.inventory.ItemStack[18];

    public org.bukkit.inventory.ItemStack[] getBagStorage() { return bagStorage; }

    public boolean isMeditating()             { return meditating; }
    public void    setMeditating(boolean b)   { this.meditating = b; if(b) meditateStart=System.currentTimeMillis(); }
    public long    getMeditateStart()         { return meditateStart; }
    public long    getLastDailyReset()        { return lastDailyReset; }
    public void    setLastDailyReset(long v)  { this.lastDailyReset = v; }
    public int     getDailyKills()            { return dailyKills; }
    public void    addDailyKill()             { this.dailyKills++; }
    public boolean isDailyKillClaimed()       { return dailyKillClaimed; }
    public void    setDailyKillClaimed(boolean v){ this.dailyKillClaimed = v; }
    public boolean isDailyBtClaimed()         { return dailyBtClaimed; }
    public void    setDailyBtClaimed(boolean v){ this.dailyBtClaimed = v; }
    public int     getThaoDuoc()              { return thaoDuocCount; }
    public void    addThaoDuoc(int n)         { this.thaoDuocCount = Math.max(0,thaoDuocCount+n); }
    public long    getLastGroupBonus()        { return lastGroupBonusTime; }
    public void    setLastGroupBonus(long v)  { this.lastGroupBonusTime = v; }
    public void setInvisible(boolean b)   { isInvisible = b; }
    public boolean hasCounterBuff()       { return hasCounterBuff; }
    public void setCounterBuff(boolean b) { hasCounterBuff = b; }
    public boolean hasDodgeBuff()         { return hasDodgeBuff; }
    public void setDodgeBuff(boolean b)   { hasDodgeBuff = b; }

    // ══════════════════════════════════════════════════════
    //  EXTRA DATA MAP — dùng cho tất cả hệ thống mở rộng
    //  (Quest, Title, Destiny, SuPhu, v.v.)
    // ══════════════════════════════════════════════════════
    // === PASSIVE SKILLS (sach hoc ky nang bi dong - toi da 3 cai, nang cap theo canh gioi) ===
    public static final int MAX_PASSIVES = 3;
    private final java.util.Set<String> learnedPassives = new java.util.LinkedHashSet<>();

    public java.util.Set<String> getLearnedPassives() { return learnedPassives; }
    public void setLearnedPassives(java.util.Collection<String> keys) {
        learnedPassives.clear();
        if (keys != null) learnedPassives.addAll(keys);
    }
    public boolean hasPassive(String key) { return isModuleEnabled("noi_tinh") && learnedPassives.contains(key); }
    /** Per-player GUI toggle; a missing setting defaults to enabled for backwards compatibility. */
    public boolean isModuleEnabled(String key) { return getExtraBool("tt.skill-module." + key, true); }
    /** Hoc them 1 bi dong. False neu da hoc roi hoac da du 3 cai. */
    public boolean learnPassive(String key) {
        if (learnedPassives.contains(key)) return false;
        if (learnedPassives.size() >= MAX_PASSIVES) return false;
        learnedPassives.add(key);
        return true;
    }
    public boolean forgetPassive(String key) { return learnedPassives.remove(key); }
    /** He so tang them cho bi dong da hoc, tang dan theo canh gioi (ordinal 0-24, toi da +12%). */
    public double passiveOrdinalBonus() { return stage.ordinal() * 0.005; }

    private final java.util.Map<String, Object> extraData = new java.util.HashMap<>();

    // Dùng Number thay vì ép kiểu Long/Integer cứng: YAML có thể nạp lại một số nhỏ
    // dưới dạng Integer dù đã setExtraLong với giá trị Long -> tránh mất dữ liệu khi load.
    public long   getExtraLong(String key, long def)     { Object v = extraData.get(key); return v instanceof Number n ? n.longValue() : def; }
    public int    getExtraInt (String key, int def)      { Object v = extraData.get(key); return v instanceof Number n ? n.intValue()  : def; }
    public String getExtraStr (String key, String def)   { Object v = extraData.get(key); return v instanceof String  s ? s : def; }
    public boolean getExtraBool(String key, boolean def) { Object v = extraData.get(key); return v instanceof Boolean b ? b : def; }

    public void setExtraLong (String key, long val)    { extraData.put(key, val); }
    public void setExtraInt  (String key, int val)     { extraData.put(key, val); }
    public void setExtraStr  (String key, String val)  { if (val == null) extraData.remove(key); else extraData.put(key, val); }
    public void setExtraBool (String key, boolean val) { extraData.put(key, val); }
    public void addExtraLong (String key, long delta)  { extraData.put(key, getExtraLong(key, 0L) + delta); }
    public java.util.Map<String,Object> getExtraData() { return extraData; }
    /** Dùng khi nạp lại từ file: đổ thẳng dữ liệu đã lưu vào map, không qua từng setter. */
    public void loadExtraData(java.util.Map<String,Object> saved) {
        extraData.clear();
        if (saved != null) extraData.putAll(saved);
    }
}
