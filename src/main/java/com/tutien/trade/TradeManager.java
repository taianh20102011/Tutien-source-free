package com.tutien.trade;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;

public class TradeManager implements Listener {

    private final TuTienPlugin plugin;
    private static final String TITLE = "§6⚔ Giao Dịch";

    public static class TradeSession {
        public final String id;
        public final UUID playerA, playerB;
        public final ItemStack[] itemsA = new ItemStack[9];
        public final ItemStack[] itemsB = new ItemStack[9];
        public long ltA = 0, ltB = 0;
        public boolean readyA = false, readyB = false;
        public boolean cancelled = false;
        public TradeSession(String id, UUID a, UUID b) { this.id=id; this.playerA=a; this.playerB=b; }
        public boolean bothReady() { return readyA && readyB; }
    }

    private final Map<UUID, TradeSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> requests  = new HashMap<>(); // requested → requester
    private int idCount = 1;

    public TradeManager(TuTienPlugin plugin) { this.plugin = plugin; }

    // ─── COMMANDS ───────────────────────────────────────────

    public void handleTradeCommand(Player p, String[] args) {
        if (args.length == 0) { p.sendMessage(plugin.prefix()+"§7/trade <player>  /trade cancel"); return; }
        String sub = args[0].toLowerCase();
        if (sub.equals("cancel") || sub.equals("huy")) { cancelTrade(p,"§7bạn hủy."); return; }
        if (sub.equals("lt") || sub.equals("linh")) {
            if (args.length < 2) { p.sendMessage(plugin.prefix()+"§c/trade lt <số>"); return; }
            try { setLT(p, Long.parseLong(args[1])); } catch (NumberFormatException e) { p.sendMessage(plugin.prefix()+"§cSố không hợp lệ!"); }
            return;
        }
        // Check if accepting a request
        UUID pendingFrom = requests.get(p.getUniqueId());
        if (pendingFrom != null) {
            Player requester = Bukkit.getPlayer(pendingFrom);
            if (requester != null && requester.getName().equalsIgnoreCase(args[0])) {
                acceptTrade(p, requester); return;
            }
        }
        requestTrade(p, args[0]);
    }

    public void handleGiftCommand(Player p, String[] args) {
        if (args.length == 0) { p.sendMessage(plugin.prefix()+"§c/tang <player>"); return; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { p.sendMessage(plugin.prefix()+"§cKhông tìm thấy §e"+args[0]); return; }
        if (target.equals(p)) { p.sendMessage(plugin.prefix()+"§cKhông tặng cho chính mình!"); return; }
        ItemStack gift = p.getInventory().getItemInMainHand();
        if (gift.getType() == Material.AIR) { p.sendMessage(plugin.prefix()+"§cCầm item muốn tặng!"); return; }
        String name = gift.hasItemMeta() && gift.getItemMeta().hasDisplayName() ? gift.getItemMeta().getDisplayName() : gift.getType().name();
        p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        var left = target.getInventory().addItem(gift.clone());
        if (!left.isEmpty()) target.getWorld().dropItemNaturally(target.getLocation(), gift.clone());
        p.sendMessage(plugin.prefix()+"§7Đã tặng §f"+name+" §7cho §e"+target.getName());
        target.sendMessage(plugin.prefix()+"§6§l✦ §e"+p.getName()+" §6tặng bạn §f"+name+"§6!");
        target.sendTitle(plugin.msg("auto_ce595522f9"), "§e"+p.getName()+" tặng §f"+name, 5, 50, 10);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0,2,0), 15,0.5,0.5,0.5,0, new Particle.DustOptions(Color.fromRGB(255,200,0),1.5f));
    }

    public void setLT(Player p, long amount) {
        TradeSession s = sessions.get(p.getUniqueId());
        if (s == null) { p.sendMessage(plugin.prefix()+"§cKhông có phiên giao dịch!"); return; }
        PlayerData d = plugin.getDataManager().get(p);
        if (d != null && amount > d.getTien()) { p.sendMessage(plugin.prefix()+"§cKhông đủ "+plugin.getTienManager().tenTien()+"! Có: §e"+d.getTien()); return; }
        boolean isA = p.getUniqueId().equals(s.playerA);
        if (isA) s.ltA = Math.max(0,amount); else s.ltB = Math.max(0,amount);
        p.sendMessage(plugin.prefix()+"§7Đặt §e"+amount+" LT §7vào giao dịch.");
        refreshBoth(s);
    }

    // ─── REQUEST / ACCEPT ─────────────────────────────────

    private void requestTrade(Player from, String targetName) {
        Player to = Bukkit.getPlayer(targetName);
        if (to == null) { from.sendMessage(plugin.prefix()+"§cKhông tìm thấy §e"+targetName); return; }
        if (to.equals(from)) { from.sendMessage(plugin.prefix()+"§cKhông tự giao dịch!"); return; }
        if (sessions.containsKey(from.getUniqueId())) { from.sendMessage(plugin.prefix()+"§cBạn đang giao dịch!"); return; }
        if (sessions.containsKey(to.getUniqueId())) { from.sendMessage(plugin.prefix()+"§e"+to.getName()+" §cđang giao dịch!"); return; }
        requests.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage(plugin.prefix()+"§7Đã gửi yêu cầu đến §e"+to.getName());
        to.sendMessage(plugin.prefix()+"§6⚔ §e"+from.getName()+" §6muốn giao dịch!");
        to.sendMessage(plugin.prefix()+"§a/trade "+from.getName()+" §7để chấp nhận  §c/trade cancel §7để từ chối");
        to.playSound(to.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (requests.remove(to.getUniqueId(), from.getUniqueId())) {
                if (from.isOnline()) from.sendMessage(plugin.prefix()+"§7Yêu cầu giao dịch hết hạn.");
                if (to.isOnline()) to.sendMessage(plugin.prefix()+"§7Yêu cầu từ §e"+from.getName()+" §7hết hạn.");
            }
        }, 600L);
    }

    private void acceptTrade(Player who, Player from) {
        requests.remove(who.getUniqueId());
        if (sessions.containsKey(who.getUniqueId()) || sessions.containsKey(from.getUniqueId())) {
            who.sendMessage(plugin.prefix()+"§cMột bên đang bận!"); return;
        }
        TradeSession s = new TradeSession("T"+(idCount++), from.getUniqueId(), who.getUniqueId());
        sessions.put(from.getUniqueId(), s);
        sessions.put(who.getUniqueId(), s);
        openGUI(from, s);
        openGUI(who, s);
    }

    // ─── GUI ──────────────────────────────────────────────

    private static final int[] SLOTS_A = {10,11,12,19,20,21,28,29,30};
    private static final int[] SLOTS_B = {14,15,16,23,24,25,32,33,34};

    private void openGUI(Player p, TradeSession s) {
        boolean isA = p.getUniqueId().equals(s.playerA);
        String otherName = getOtherName(p, s);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE+" "+otherName));
        // Fill background
        ItemStack bg = makePane(Material.GRAY_STAINED_GLASS_PANE,"§0");
        for(int i=0;i<54;i++) inv.setItem(i,bg);
        // Labels
        inv.setItem(0, makePane(Material.LIME_STAINED_GLASS_PANE, "§a§l📦 Hàng của bạn\n§7Drag items vào slot dưới"));
        inv.setItem(9, makePane(Material.RED_STAINED_GLASS_PANE,  "§c§l📦 Hàng §e"+otherName+"\n§7(Chỉ đọc)"));
        // Clear item slots
        for(int slot: SLOTS_A) inv.setItem(slot, makePane(Material.LIGHT_GRAY_STAINED_GLASS_PANE,"§8[Kéo item vào đây]"));
        // Load items
        ItemStack[] mine = isA?s.itemsA:s.itemsB, theirs = isA?s.itemsB:s.itemsA;
        int[] mySlots = isA?SLOTS_A:SLOTS_B, theirSlots = isA?SLOTS_B:SLOTS_A;
        // Wait - both A and B see same side labels
        for(int i=0;i<9;i++){
            if(mine[i]!=null)   inv.setItem(SLOTS_A[i], mine[i].clone());
            if(theirs[i]!=null) inv.setItem(SLOTS_B[i], theirs[i].clone());
        }
        // LT displays
        inv.setItem(4,  makeBtn(Material.GOLD_NUGGET, "§6LT Của Bạn: §e"+(isA?s.ltA:s.ltB)+" LT", "§7/trade lt <số> để thay đổi"));
        inv.setItem(13, makeBtn(Material.GOLD_INGOT,  "§6LT §e"+otherName+": §e"+(isA?s.ltB:s.ltA)+" LT", "§8(Chỉ đọc)"));
        // Ready buttons
        boolean myReady = isA?s.readyA:s.readyB, theirReady = isA?s.readyB:s.readyA;
        inv.setItem(22, makeBtn(myReady?Material.LIME_CONCRETE:Material.RED_CONCRETE, myReady?"§a§l✓ SẴN SÀNG":"§7○ Chưa Sẵn Sàng", "§7Click để toggle"));
        inv.setItem(31, makeBtn(theirReady?Material.LIME_CONCRETE:Material.ORANGE_CONCRETE, theirReady?"§a✓ "+otherName+" SẴN SÀNG":"§7○ "+otherName+" chưa sẵn sàng", ""));
        inv.setItem(49, makeBtn(Material.BARRIER,"§c✘ Hủy Giao Dịch",""));
        p.openInventory(inv);
    }

    private void refreshBoth(TradeSession s) {
        Player a=Bukkit.getPlayer(s.playerA), b=Bukkit.getPlayer(s.playerB);
        if(a!=null&&a.isOnline()&&isTradeGUI(a)){a.closeInventory();openGUI(a,s);}
        if(b!=null&&b.isOnline()&&isTradeGUI(b)){b.closeInventory();openGUI(b,s);}
    }

    // ─── EVENTS ───────────────────────────────────────────

    @EventHandler(priority=EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked()instanceof Player p)) return;
        if(!isTradeGUI(p)) return;
        e.setCancelled(true);
        if(e.getRawSlot()>=54){e.setCancelled(false);return;}
        TradeSession s=sessions.get(p.getUniqueId());
        if(s==null||s.cancelled){p.closeInventory();return;}
        boolean isA=p.getUniqueId().equals(s.playerA);
        int slot=e.getRawSlot();
        if(slot==22){toggleReady(p,s,isA);return;}
        if(slot==49){cancelTrade(p,"§e"+p.getName()+" §7hủy.");return;}
        // Item slots (only mine side = SLOTS_A visually for both)
        for(int i=0;i<9;i++){
            if(slot==SLOTS_A[i]){
                if(isA&&s.readyA||!isA&&s.readyB){if(isA)setReadyA(s,false);else setReadyB(s,false);p.sendMessage(plugin.prefix()+"§7Hủy sẵn sàng để chỉnh sửa.");}
                ItemStack cur=e.getCursor(), existing=isA?s.itemsA[i]:s.itemsB[i];
                if(cur!=null&&cur.getType()!=Material.AIR){if(isA)s.itemsA[i]=cur.clone();else s.itemsB[i]=cur.clone();e.getView().setCursor(existing!=null?existing.clone():new ItemStack(Material.AIR));}
                else if(existing!=null&&existing.getType()!=Material.AIR){e.getView().setCursor(existing.clone());if(isA)s.itemsA[i]=null;else s.itemsB[i]=null;}
                refreshBoth(s);return;
            }
        }
    }

    @EventHandler public void onDrag(InventoryDragEvent e){if(e.getWhoClicked()instanceof Player p&&isTradeGUI(p))e.setCancelled(true);}

    @EventHandler public void onClose(InventoryCloseEvent e) {
        if(!(e.getPlayer()instanceof Player p)) return;
        if(!isTradeGUI(p)) return;
        TradeSession s=sessions.get(p.getUniqueId());
        if(s==null||s.cancelled) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if(sessions.containsKey(p.getUniqueId())&&!isTradeGUI(p)) cancelTrade(p,"§e"+p.getName()+" §7đóng cửa sổ.");
        }, 2L);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e){
        Player p=e.getPlayer();
        if(sessions.containsKey(p.getUniqueId())) cancelTrade(p,"§e"+p.getName()+" §7thoát game.");
        requests.remove(p.getUniqueId());
    }

    // ─── READY / COMPLETE ─────────────────────────────────

    private void setReadyA(TradeSession s, boolean v){s.readyA=v;}
    private void setReadyB(TradeSession s, boolean v){s.readyB=v;}

    private void toggleReady(Player p, TradeSession s, boolean isA) {
        if(isA) s.readyA=!s.readyA; else s.readyB=!s.readyB;
        boolean myReady=isA?s.readyA:s.readyB;
        p.sendMessage(plugin.prefix()+(myReady?"§a✓ Sẵn sàng!":"§7Hủy sẵn sàng."));
        Player other=Bukkit.getPlayer(isA?s.playerB:s.playerA);
        if(other!=null) other.sendMessage(plugin.prefix()+(myReady?"§a✓ §e"+p.getName()+" §aĐÃ SẴN SÀNG!":"§7§e"+p.getName()+" §7hủy sẵn sàng."));
        refreshBoth(s);
        if(s.bothReady()) startConfirm(s);
    }

    private void startConfirm(TradeSession s) {
        Player a=Bukkit.getPlayer(s.playerA),b=Bukkit.getPlayer(s.playerB);
        if(a!=null)a.sendTitle(plugin.msg("auto_f29e8ab052"), plugin.msg("auto_094bf966c6"),0,22,5);if(b!=null)b.sendTitle(plugin.msg("auto_f29e8ab052"), plugin.msg("auto_094bf966c6"),0,22,5);
        new BukkitRunnable(){int cd=2;@Override public void run(){
            if(!s.bothReady()||s.cancelled){cancel();return;}
            Player pa=Bukkit.getPlayer(s.playerA),pb=Bukkit.getPlayer(s.playerB);
            if(pa==null||pb==null||!pa.isOnline()||!pb.isOnline()){cancelTrade(s,"§7Offline.");cancel();return;}
            if(cd>0){pa.sendTitle("§e§l"+cd+"...", plugin.msg("auto_da39a3ee5e"),0,22,5);pb.sendTitle("§e§l"+cd+"...", plugin.msg("auto_da39a3ee5e"),0,22,5);cd--;}
            else{cancel();completeTrade(s);}
        }}.runTaskTimer(plugin,20L,20L);
    }

    private void completeTrade(TradeSession s) {
        if(s.cancelled)return;
        s.cancelled=true;
        sessions.remove(s.playerA);sessions.remove(s.playerB);
        Player a=Bukkit.getPlayer(s.playerA),b=Bukkit.getPlayer(s.playerB);
        if(a!=null)a.closeInventory();if(b!=null)b.closeInventory();
        // Give items
        giveItems(a,s.playerA,s.itemsB,s.ltB);giveItems(b,s.playerB,s.itemsA,s.ltA);
        // Deduct LT
        if(a!=null){PlayerData da=plugin.getDataManager().get(a);if(da!=null&&s.ltA>0){da.spendTien(s.ltA);}}
        if(b!=null){PlayerData db=plugin.getDataManager().get(b);if(db!=null&&s.ltB>0){db.spendTien(s.ltB);}}
        String na=a!=null?a.getName():"?",nb=b!=null?b.getName():"?";
        if(a!=null){a.sendTitle(plugin.msg("auto_e6fa89cb20"),"§7Nhận hàng từ §e"+nb,5,60,15);a.sendMessage(plugin.prefix()+"§6✦ Giao dịch thành công!");a.playSound(a.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,1f,1.2f);}
        if(b!=null){b.sendTitle(plugin.msg("auto_e6fa89cb20"),"§7Nhận hàng từ §e"+na,5,60,15);b.sendMessage(plugin.prefix()+"§6✦ Giao dịch thành công!");b.playSound(b.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,1f,1.2f);}
    }

    private void giveItems(Player p,UUID uuid,ItemStack[]items,long lt){
        if(p==null)return;
        for(ItemStack it:items){if(it==null||it.getType()==Material.AIR)continue;var left=p.getInventory().addItem(it.clone());if(!left.isEmpty())p.getWorld().dropItemNaturally(p.getLocation(),it.clone());}
        if(lt>0){PlayerData d=plugin.getDataManager().get(p);if(d!=null){d.addTien(lt);}}
    }

    private void cancelTrade(Player p, String reason) {
        TradeSession s=sessions.get(p.getUniqueId());
        if(s==null){requests.remove(p.getUniqueId());p.sendMessage(plugin.prefix()+"§7Không có giao dịch nào.");return;}
        cancelTrade(s,reason);
    }
    private void cancelTrade(TradeSession s, String reason) {
        if(s.cancelled)return;s.cancelled=true;
        sessions.remove(s.playerA);sessions.remove(s.playerB);
        returnItems(s.playerA,s.itemsA,s.ltA);returnItems(s.playerB,s.itemsB,s.ltB);
        Player a=Bukkit.getPlayer(s.playerA),b=Bukkit.getPlayer(s.playerB);
        if(a!=null){a.closeInventory();a.sendMessage(plugin.prefix()+"§c⚔ Giao dịch bị hủy: "+reason);}
        if(b!=null){b.closeInventory();b.sendMessage(plugin.prefix()+"§c⚔ Giao dịch bị hủy: "+reason);}
    }
    private void returnItems(UUID uuid,ItemStack[]items,long lt){
        Player p=Bukkit.getPlayer(uuid);if(p==null)return;
        for(ItemStack it:items){if(it==null||it.getType()==Material.AIR)continue;var left=p.getInventory().addItem(it.clone());if(!left.isEmpty())p.getWorld().dropItemNaturally(p.getLocation(),it.clone());}
        if(lt>0){PlayerData d=plugin.getDataManager().get(p);if(d!=null){d.addTien(lt);}}
    }

    // ─── HELPERS ──────────────────────────────────────────

    private boolean isTradeGUI(Player p) {
        try{return p.getOpenInventory()!=null&&LegacyComponentSerializer.legacySection().serialize(p.getOpenInventory().title()).startsWith(TITLE);}
        catch(Exception e){return false;}
    }
    private String getOtherName(Player self, TradeSession s) {
        UUID other=self.getUniqueId().equals(s.playerA)?s.playerB:s.playerA;
        Player op=Bukkit.getPlayer(other);return op!=null?op.getName():"?";
    }
    private ItemStack makePane(Material m,String name){ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName(name);meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_ADDITIONAL_TOOLTIP,ItemFlag.HIDE_ENCHANTS);it.setItemMeta(meta);return it;}
    private ItemStack makeBtn(Material m,String name,String lore){ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName(name);if(!lore.isEmpty())meta.setLore(java.util.Arrays.asList(lore.split("\n")));meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_ADDITIONAL_TOOLTIP,ItemFlag.HIDE_ENCHANTS);it.setItemMeta(meta);return it;}
}
