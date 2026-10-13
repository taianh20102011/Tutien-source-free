package com.tutien.tongmon;

import com.tutien.TuTienPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Bilateral sect diplomacy: invite/accept, mutual PvP consent and mutual territory access. */
public final class TongMonAllianceManager implements Listener {
    private final TuTienPlugin plugin;
    private final TongMonManager tongMonManager;
    private final File file;
    private final Map<String, Alliance> alliances = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<String>> pendingByTarget = new LinkedHashMap<>();
    private final Map<UUID, Long> lastPvpNoticeAt = new HashMap<>();

    public TongMonAllianceManager(TuTienPlugin plugin, TongMonManager tongMonManager) {
        this.plugin = plugin;
        this.tongMonManager = tongMonManager;
        this.file = new File(plugin.getDataFolder(), "tongmon-lienminh.yml");
        load();
    }

    public boolean invite(Player sender, String targetReference) {
        TongMon source = tongMonManager.getTongMon(sender.getUniqueId());
        if (source == null || !source.canManageAlliance(sender.getUniqueId())) {
            sender.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ/Phó Tông Chủ mới gửi lời mời liên minh."); return false;
        }
        TongMon target = resolveTongMon(targetReference);
        if (target == null) { sender.sendMessage(plugin.prefix() + "§cKhông tìm thấy Tông Môn: " + targetReference); return false; }
        if (source.getId().equals(target.getId())) { sender.sendMessage(plugin.prefix() + "§cKhông thể liên minh với chính mình."); return false; }
        if (find(source.getId(), target.getId()) != null) { sender.sendMessage(plugin.prefix() + "§eHai Tông Môn đã liên minh."); return false; }
        LinkedHashSet<String> list = pendingByTarget.computeIfAbsent(target.getId(), ignored -> new LinkedHashSet<>());
        if (!list.add(source.getId())) { sender.sendMessage(plugin.prefix() + "§eLời mời đã được gửi trước đó."); return false; }
        save();
        sender.sendMessage(plugin.prefix() + "§aĐã gửi lời mời liên minh đến §e" + target.getName() + "§a.");
        for (UUID member : target.getMembers().keySet()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null && target.canManageAlliance(member)) {
                online.sendMessage(plugin.prefix() + "§6Tông Môn §e" + source.getName() + " §6muốn lập liên minh với bạn.");
                online.sendMessage("§a/tongmon lienminh chap " + source.getId() + " §7| §c/tongmon lienminh tuchoi " + source.getId());
            }
        }
        return true;
    }

    public boolean accept(Player receiver, String sourceId) {
        TongMon target = tongMonManager.getTongMon(receiver.getUniqueId());
        if (target == null || !target.canManageAlliance(receiver.getUniqueId())) {
            receiver.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ/Phó Tông Chủ mới chấp nhận liên minh."); return false;
        }
        TongMon source = tongMonManager.getTongMonById(sourceId);
        LinkedHashSet<String> pending = pendingByTarget.get(target.getId());
        if (source == null || pending == null || !pending.contains(sourceId)) {
            receiver.sendMessage(plugin.prefix() + "§cKhông có lời mời liên minh tương ứng."); return false;
        }
        String key = pairKey(source.getId(), target.getId());
        alliances.putIfAbsent(key, new Alliance(source.getId(), target.getId()));
        pending.remove(sourceId);
        if (pending.isEmpty()) pendingByTarget.remove(target.getId());
        save();
        tongMonManager.notifyMembers(source, plugin.prefix() + "§aLiên minh với §e" + target.getName() + " §ađã được chấp thuận.");
        tongMonManager.notifyMembers(target, plugin.prefix() + "§aĐã chấp thuận liên minh với §e" + source.getName() + "§a.");
        return true;
    }

    public boolean decline(Player receiver, String sourceId) {
        TongMon target = tongMonManager.getTongMon(receiver.getUniqueId());
        if (target == null || !target.canManageAlliance(receiver.getUniqueId())) {
            receiver.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ/Phó Tông Chủ mới xử lý lời mời."); return false;
        }
        LinkedHashSet<String> pending = pendingByTarget.get(target.getId());
        if (pending == null || !pending.remove(sourceId)) {
            receiver.sendMessage(plugin.prefix() + "§cKhông tìm thấy lời mời."); return false;
        }
        if (pending.isEmpty()) pendingByTarget.remove(target.getId());
        save();
        receiver.sendMessage(plugin.prefix() + "§7Đã từ chối lời mời liên minh.");
        return true;
    }

    public boolean setConsent(Player player, String otherId, String agreementType, boolean enabled) {
        TongMon own = tongMonManager.getTongMon(player.getUniqueId());
        if (own == null || !own.canManageAlliance(player.getUniqueId())) {
            player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ và Phó Tông Chủ được đổi thỏa thuận liên minh."); return false;
        }
        TongMon other = resolveTongMon(otherId);
        if (other == null) { player.sendMessage(plugin.prefix() + "§cKhông tìm thấy Tông Môn kia."); return false; }
        Alliance alliance = find(own.getId(), other.getId());
        if (alliance == null) { player.sendMessage(plugin.prefix() + "§cHai Tông Môn chưa phải đồng minh."); return false; }
        boolean isA = own.getId().equals(alliance.a);
        String type = agreementType.toLowerCase(Locale.ROOT);
        if (type.equals("pvp")) {
            if (isA) alliance.pvpA = enabled; else alliance.pvpB = enabled;
        } else if (type.equals("dat") || type.equals("land") || type.equals("lanhtho")) {
            if (isA) alliance.landA = enabled; else alliance.landB = enabled;
        } else {
            player.sendMessage(plugin.prefix() + "§cThỏa thuận hợp lệ: pvp hoặc dat."); return false;
        }
        save();
        boolean effective = type.equals("pvp") ? alliance.pvpA && alliance.pvpB : alliance.landA && alliance.landB;
        String label = type.equals("pvp") ? "PvP liên minh" : "Quyền vào lãnh địa";
        String state = effective ? "§aĐã được CẢ HAI bên đồng ý" : "§eChưa có hiệu lực — cần cả hai bên đồng ý";
        player.sendMessage(plugin.prefix() + "§7Bạn đặt " + label + " của Tông Môn mình thành " + (enabled ? "§aBẬT" : "§cTẮT") + "§7. " + state);
        tongMonManager.notifyMembers(other, plugin.prefix() + "§6Thỏa thuận liên minh với §e" + own.getName() + "§6 đã thay đổi: " + label + " — " + state);
        return true;
    }

    /** True only when both allied sects have explicitly enabled territory access. */
    public boolean hasMutualLandAccess(String firstTongMonId, String secondTongMonId) {
        if (firstTongMonId == null || secondTongMonId == null || firstTongMonId.equals(secondTongMonId)) return false;
        Alliance alliance = find(firstTongMonId, secondTongMonId);
        return alliance != null && alliance.landA && alliance.landB;
    }

    /** True only when both allied sects have explicitly enabled alliance PvP. */
    public boolean hasMutualPvpConsent(String firstTongMonId, String secondTongMonId) {
        if (firstTongMonId == null || secondTongMonId == null || firstTongMonId.equals(secondTongMonId)) return true;
        Alliance alliance = find(firstTongMonId, secondTongMonId);
        return alliance == null || (alliance.pvpA && alliance.pvpB);
    }

    private Player resolvePlayerAttacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAlliancePvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolvePlayerAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;
        TongMon attackerSect = tongMonManager.getTongMon(attacker.getUniqueId());
        TongMon victimSect = tongMonManager.getTongMon(victim.getUniqueId());
        if (attackerSect == null || victimSect == null || attackerSect.getId().equals(victimSect.getId())) return;
        Alliance alliance = find(attackerSect.getId(), victimSect.getId());
        if (alliance != null && !(alliance.pvpA && alliance.pvpB)) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            long previous = lastPvpNoticeAt.getOrDefault(attacker.getUniqueId(), 0L);
            if (now - previous >= 1500L) {
                lastPvpNoticeAt.put(attacker.getUniqueId(), now);
                attacker.sendMessage(plugin.prefix() + "§cPvP giữa hai Tông Môn đồng minh đang bị khóa vì chưa có đồng thuận từ cả hai bên.");
            }
        }
    }

    public void openGui(Player player) {
        TongMon own = tongMonManager.getTongMon(player.getUniqueId());
        if (own == null) { player.sendMessage(plugin.prefix() + "§cBạn chưa gia nhập Tông Môn."); return; }
        AllianceHolder holder = new AllianceHolder(player.getUniqueId(), "LIST", null);
        Inventory inv = Bukkit.createInventory(holder, 54, "§0Liên Minh Tông Môn");
        holder.inventory = inv;
        fill(inv);
        // Incoming invitations occupy the first middle row. Left-click accepts, right-click declines.
        LinkedHashSet<String> incoming = pendingByTarget.getOrDefault(own.getId(), new LinkedHashSet<>());
        int inviteSlot = 10;
        if (own.canManageAlliance(player.getUniqueId())) {
            for (String sourceId : incoming) {
                if (inviteSlot > 16) break;
                TongMon source = tongMonManager.getTongMonById(sourceId);
                if (source == null) continue;
                inv.setItem(inviteSlot, item(Material.WRITABLE_BOOK, "§eLời mời: " + source.getName(),
                        "§7Tông Môn gửi lời mời liên minh.", "§aChuột trái: chấp nhận", "§cChuột phải: từ chối"));
                holder.pendingAtSlot.put(inviteSlot, sourceId);
                inviteSlot++;
            }
        }
        int slot = 19;
        for (Alliance alliance : alliances.values()) {
            if (!alliance.contains(own.getId()) || slot > 43) continue;
            String otherId = alliance.other(own.getId());
            TongMon other = tongMonManager.getTongMonById(otherId);
            if (other == null) continue;
            inv.setItem(slot, item(Material.WHITE_BANNER, "§e" + other.getName() + " §7[" + other.getTag() + "]",
                    "§7PvP hiệu lực: " + yesNo(alliance.pvpA && alliance.pvpB),
                    "§7Vào lãnh địa: " + yesNo(alliance.landA && alliance.landB),
                    "§aNhấn để quản lý thỏa thuận"));
            holder.allianceAtSlot.put(slot, otherId);
            slot += slot % 9 == 7 ? 3 : 1;
        }
        inv.setItem(4, item(Material.PAPER, "§bLời mời / Liên minh", "§7Chuột trái lời mời để chấp nhận.", "§7Chuột phải để từ chối.", "§7Liên minh hiện có nằm ở các hàng dưới."));
        inv.setItem(49, item(Material.BARRIER, "§cĐóng", "§7Đóng giao diện"));
        player.openInventory(inv);
    }

    private void openSettings(Player player, String allianceKey) {
        TongMon own = tongMonManager.getTongMon(player.getUniqueId());
        Alliance alliance = alliances.get(allianceKey);
        if (own == null || alliance == null || !alliance.contains(own.getId())) { openGui(player); return; }
        boolean canEdit = own.canManageAlliance(player.getUniqueId());
        boolean isA = own.getId().equals(alliance.a);
        boolean ownPvp = isA ? alliance.pvpA : alliance.pvpB;
        boolean otherPvp = isA ? alliance.pvpB : alliance.pvpA;
        boolean ownLand = isA ? alliance.landA : alliance.landB;
        boolean otherLand = isA ? alliance.landB : alliance.landA;
        TongMon other = tongMonManager.getTongMonById(alliance.other(own.getId()));
        AllianceHolder holder = new AllianceHolder(player.getUniqueId(), "SETTINGS", allianceKey);
        Inventory inv = Bukkit.createInventory(holder, 45, "§0Thỏa Thuận Liên Minh");
        holder.inventory = inv;
        fill(inv);
        inv.setItem(13, item(Material.NAME_TAG, "§e" + own.getName() + " §7↔ §b" + (other == null ? "?" : other.getName()),
                "§7Chỉ thỏa thuận được áp dụng khi hai bên cùng bật."));
        inv.setItem(20, item(ownPvp ? Material.DIAMOND_SWORD : Material.WOODEN_SWORD,
                "§cPvP liên minh: " + (ownPvp ? "§aBẬT" : "§7TẮT"),
                "§7Phía bạn: " + yesNo(ownPvp), "§7Phía đồng minh: " + yesNo(otherPvp),
                "§7Hiệu lực: " + yesNo(ownPvp && otherPvp), canEdit ? "§eNhấn để đổi lựa chọn của tông bạn" : "§8Chỉ Môn Chủ/Phó Tông Chủ được đổi"));
        inv.setItem(24, item(Material.OAK_FENCE_GATE,
                "§6Quyền vào lãnh địa: " + (ownLand ? "§aBẬT" : "§7TẮT"),
                "§7Phía bạn: " + yesNo(ownLand), "§7Phía đồng minh: " + yesNo(otherLand),
                "§7Hiệu lực: " + yesNo(ownLand && otherLand), canEdit ? "§eNhấn để đổi lựa chọn của tông bạn" : "§8Chỉ Môn Chủ/Phó Tông Chủ được đổi"));
        inv.setItem(40, item(Material.ARROW, "§e← Quay lại", "§7Danh sách liên minh"));
        player.openInventory(inv);
    }

    @EventHandler public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof AllianceHolder holder)) return;
        event.setCancelled(true);
        if (!holder.viewer.equals(player.getUniqueId()) || event.getRawSlot() < 0 || event.getRawSlot() >= holder.inventory.getSize()) return;
        int slot = event.getRawSlot();
        if (slot == 49 || slot == 40) { if (holder.mode.equals("SETTINGS")) openGui(player); else player.closeInventory(); return; }
        if (holder.mode.equals("LIST")) {
            String pendingSourceId = holder.pendingAtSlot.get(slot);
            if (pendingSourceId != null) {
                TongMon own = tongMonManager.getTongMon(player.getUniqueId());
                if (own == null || !own.canManageAlliance(player.getUniqueId())) {
                    player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ/Phó Tông Chủ mới xử lý lời mời liên minh."); return;
                }
                if (event.isRightClick()) decline(player, pendingSourceId);
                else accept(player, pendingSourceId);
                openGui(player);
                return;
            }
            String otherId = holder.allianceAtSlot.get(slot);
            if (otherId == null) return;
            TongMon own = tongMonManager.getTongMon(player.getUniqueId());
            Alliance alliance = own == null ? null : find(own.getId(), otherId);
            if (alliance != null) openSettings(player, pairKey(alliance.a, alliance.b));
            return;
        }
        Alliance alliance = holder.allianceKey == null ? null : alliances.get(holder.allianceKey);
        TongMon own = tongMonManager.getTongMon(player.getUniqueId());
        if (alliance == null || own == null || !alliance.contains(own.getId())) return;
        if (!own.canManageAlliance(player.getUniqueId())) {
            player.sendMessage(plugin.prefix() + "§cChỉ Môn Chủ/Phó Tông Chủ được đổi thỏa thuận."); return;
        }
        boolean isA = own.getId().equals(alliance.a);
        if (slot == 20) { if (isA) alliance.pvpA = !alliance.pvpA; else alliance.pvpB = !alliance.pvpB; }
        else if (slot == 24) { if (isA) alliance.landA = !alliance.landA; else alliance.landB = !alliance.landB; }
        else return;
        save();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);
        openSettings(player, holder.allianceKey);
    }

    @EventHandler public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AllianceHolder) event.setCancelled(true);
    }

    /** Removes all pending requests and agreements for a sect that is being disbanded. */
    public void removeTongMon(String tongId) {
        if (tongId == null || tongId.isBlank()) return;
        boolean changed = alliances.entrySet().removeIf(e -> e.getValue().contains(tongId));
        if (pendingByTarget.remove(tongId) != null) changed = true;
        for (LinkedHashSet<String> pending : pendingByTarget.values()) changed |= pending.remove(tongId);
        if (pendingByTarget.entrySet().removeIf(e -> e.getValue().isEmpty())) changed = true;
        if (changed) save();
    }

    public boolean isSameSect(UUID first, UUID second) {
        TongMon a = tongMonManager.getTongMon(first), b = tongMonManager.getTongMon(second);
        return a != null && b != null && a.getId().equals(b.getId());
    }

    /** Friendly-fire protection: same sect is always safe; allies need both PvP consents enabled. */
    public boolean canDamagePlayer(UUID attacker, UUID victim) {
        TongMon a = tongMonManager.getTongMon(attacker), b = tongMonManager.getTongMon(victim);
        if (a == null || b == null) return true;
        if (a.getId().equals(b.getId())) return false;
        Alliance alliance = find(a.getId(), b.getId());
        if (alliance == null) return true;
        return alliance.pvpA && alliance.pvpB;
    }

    public boolean canEnterTerritory(String landOwnerTongId, UUID playerId) {
        TongMon own = tongMonManager.getTongMon(playerId);
        if (own == null) return false;
        if (own.getId().equals(landOwnerTongId)) return true;
        Alliance alliance = find(landOwnerTongId, own.getId());
        return alliance != null && alliance.landA && alliance.landB;
    }

    public List<Alliance> getAlliancesFor(String tongId) {
        List<Alliance> out = new ArrayList<>();
        for (Alliance a : alliances.values()) if (a.contains(tongId)) out.add(a);
        return Collections.unmodifiableList(out);
    }

    public TongMon resolveTongMon(String reference) {
        if (reference == null) return null;
        TongMon exact = tongMonManager.getTongMonById(reference);
        if (exact != null) return exact;
        for (TongMon tm : tongMonManager.getAllTongMons()) {
            if (tm.getTag().equalsIgnoreCase(reference) || tm.getName().equalsIgnoreCase(reference)) return tm;
        }
        return null;
    }

    private Alliance find(String a, String b) { return alliances.get(pairKey(a, b)); }
    private String pairKey(String a, String b) { return a.compareToIgnoreCase(b) <= 0 ? a + "--" + b : b + "--" + a; }

    private void load() {
        if (!file.isFile()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection as = cfg.getConfigurationSection("alliances");
        if (as != null) for (String key : as.getKeys(false)) {
            ConfigurationSection s = as.getConfigurationSection(key); if (s == null) continue;
            String a = s.getString("a", ""), b = s.getString("b", "");
            if (tongMonManager.getTongMonById(a) == null || tongMonManager.getTongMonById(b) == null || a.equals(b)) continue;
            Alliance al = new Alliance(a, b);
            al.pvpA = s.getBoolean("pvp-a", false); al.pvpB = s.getBoolean("pvp-b", false);
            al.landA = s.getBoolean("land-a", false); al.landB = s.getBoolean("land-b", false);
            alliances.put(pairKey(a, b), al);
        }
        ConfigurationSection rs = cfg.getConfigurationSection("requests");
        if (rs != null) for (String targetId : rs.getKeys(false)) {
            LinkedHashSet<String> from = new LinkedHashSet<>();
            for (String src : rs.getStringList(targetId)) if (tongMonManager.getTongMonById(src) != null) from.add(src);
            if (tongMonManager.getTongMonById(targetId) != null && !from.isEmpty()) pendingByTarget.put(targetId, from);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Alliance> entry : alliances.entrySet()) {
            String p = "alliances." + entry.getKey(); Alliance a = entry.getValue();
            cfg.set(p + ".a", a.a); cfg.set(p + ".b", a.b);
            cfg.set(p + ".pvp-a", a.pvpA); cfg.set(p + ".pvp-b", a.pvpB);
            cfg.set(p + ".land-a", a.landA); cfg.set(p + ".land-b", a.landB);
        }
        for (Map.Entry<String, LinkedHashSet<String>> entry : pendingByTarget.entrySet())
            cfg.set("requests." + entry.getKey(), new ArrayList<>(entry.getValue()));
        try { cfg.save(file); } catch (IOException e) { plugin.getLogger().warning("Không thể lưu lienminh.yml: " + e.getMessage()); }
    }

    private void fill(Inventory inv) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < inv.getSize(); i++) if (i < 9 || i >= inv.getSize() - 9 || i % 9 == 0 || i % 9 == 8) inv.setItem(i, pane);
    }
    private String yesNo(boolean value) { return value ? "§aCó" : "§cKhông"; }
    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name); if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.values()); stack.setItemMeta(meta); return stack;
    }

    private static final class AllianceHolder implements InventoryHolder {
        private final UUID viewer; private final String mode; private final String allianceKey;
        private final Map<Integer, String> allianceAtSlot = new HashMap<>();
        private final Map<Integer, String> pendingAtSlot = new HashMap<>();
        private Inventory inventory;
        private AllianceHolder(UUID viewer, String mode, String allianceKey) { this.viewer = viewer; this.mode = mode; this.allianceKey = allianceKey; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public static final class Alliance {
        public final String a, b; private boolean pvpA, pvpB, landA, landB;
        private Alliance(String a, String b) { this.a = a; this.b = b; }
        public boolean contains(String id) { return a.equals(id) || b.equals(id); }
        public String other(String id) { return a.equals(id) ? b : a; }
        public boolean isPvpEnabled() { return pvpA && pvpB; }
        public boolean isLandAccessEnabled() { return landA && landB; }
    }
}
