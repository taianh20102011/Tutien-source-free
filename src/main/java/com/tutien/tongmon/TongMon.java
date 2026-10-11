package com.tutien.tongmon;

import java.util.*;

/**
 * Tông Môn - hệ thống bang hội cho tu tiên.
 * Người chơi tạo tông môn, có tên, logo, thành viên, cấp bậc, kho tông môn.
 */
public class TongMon {

    // ========================
    //  HANG VI TRONG TONG MON
    // ========================
    public enum Rank {
        MON_CHU   ("Môn Chủ",    "§6§l", 0),      // Người lập ra
        TRUONG_LAO("Trưởng Lão", "§e§l", 1),      // Elder
        GIAM_SO   ("Giám Sự",    "§b",   2),      // Overseer
        NGOAI_MON ("Ngoại Môn",  "§7",   3),      // Outer disciple
        MON_DO    ("Môn Đồ",     "§f",   4);      // New member

        private final String display;
        private final String color;
        private final int level;
        Rank(String d, String c, int l) { display = d; color = c; level = l; }
        public String getDisplay()      { return display; }
        public String getColored()      { return color + display; }
        public int    getLevel()        { return level; }
    }

    private final String id;           // unique ID (UUID-based)
    private String name;               // Ten tong mon
    private String tag;                // [TAG] hien thi ben canh ten
    private String description;        // Mo ta
    private String color;              // Mau sac chu de §x
    private UUID   owner;              // Mon Chu UUID
    private final Map<UUID, Rank> members = new LinkedHashMap<>();
    private long tienKho = 0;    // Kho tien chung
    private long createdAt;
    private int   level = 1;          // Cap tong mon (1-10)
    private long  totalExp = 0;       // EXP tong mon (tang khi thanh vien dot pha)
    private String announcement = "";  // Thong bao tong mon

    // Bonuses cho thanh vien
    private double lkBonus   = 0.05;  // 5% LK speed bon
    private double atkBonus  = 0.02;  // 2% ATK bonus
    private double defBonus  = 0.02;  // 2% DEF bonus

    public TongMon(String id, String name, String tag, UUID owner) {
        this.id = id;
        this.name = name;
        this.tag = tag.toUpperCase();
        this.owner = owner;
        this.color = "§e";
        this.createdAt = System.currentTimeMillis();
        this.members.put(owner, Rank.MON_CHU);
    }

    // ========================
    //  THANH VIEN
    // ========================

    public void addMember(UUID uuid) {
        members.put(uuid, Rank.MON_DO);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public Rank getRank(UUID uuid) {
        return members.getOrDefault(uuid, Rank.MON_DO);
    }

    public void setRank(UUID uuid, Rank rank) {
        if (members.containsKey(uuid)) members.put(uuid, rank);
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean canManage(UUID uuid) {
        Rank r = getRank(uuid);
        return r == Rank.MON_CHU || r == Rank.TRUONG_LAO;
    }

    // ========================
    //  LEVEL & EXP
    // ========================

    public void addExp(long amount) {
        totalExp += amount;
        // Cap up formula: level N requires N * 50000 exp
        while (level < 10 && totalExp >= getExpNeeded(level + 1)) {
            totalExp -= getExpNeeded(level + 1);
            level++;
            updateBonuses();
        }
    }

    private long getExpNeeded(int lvl) { return lvl * 50_000L; }

    private void updateBonuses() {
        // Moi cap tang them bonus
        lkBonus  = 0.05 + (level - 1) * 0.02;  // cap 10 = 23% LK
        atkBonus = 0.02 + (level - 1) * 0.01;  // cap 10 = 11% ATK
        defBonus = 0.02 + (level - 1) * 0.01;  // cap 10 = 11% DEF
    }

    // ========================
    //  KHO LINH THACH
    // ========================

    public boolean depositKho(long amount) {
        tienKho += amount;
        return true;
    }

    public boolean withdrawKho(long amount) {
        if (tienKho < amount) return false;
        tienKho -= amount;
        return true;
    }

    // ========================
    //  GETTERS / SETTERS
    // ========================

    public String getId()           { return id; }
    public String getName()         { return name; }
    public void   setName(String n) { this.name = n; }
    public String getTag()          { return tag; }
    public void   setTag(String t)  { this.tag = t.toUpperCase(); }
    public String getDescription()  { return description; }
    public void   setDescription(String d) { this.description = d; }
    public String getColor()        { return color; }
    public void   setColor(String c){ this.color = c; }
    public UUID   getOwner()        { return owner; }
    public void   setOwner(UUID u)  { this.owner = u; }
    public Map<UUID, Rank> getMembers() { return Collections.unmodifiableMap(members); }
    public int    getMemberCount()  { return members.size(); }
    public long   getTienKho() { return tienKho; }
    public long   getCreatedAt()    { return createdAt; }
    public void   setCreatedAt(long t){ createdAt = t; }
    public int    getLevel()        { return level; }
    public void   setLevel(int l)   { this.level = Math.max(1, Math.min(10, l)); updateBonuses(); }
    public long   getTotalExp()     { return totalExp; }
    public void   setTotalExp(long e){ this.totalExp = e; }
    public String getAnnouncement() { return announcement; }
    public void   setAnnouncement(String a) { this.announcement = a; }
    public double getLkBonus()      { return lkBonus; }
    public double getAtkBonus()     { return atkBonus; }
    public double getDefBonus()     { return defBonus; }
    public void   setTienKho(long v) { this.tienKho = v; }

    public String getColoredName()  { return color + name; }
    public String getFullTag()      { return color + "[" + tag + "]"; }
}
