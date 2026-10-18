package com.tutien.arena;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ArenaManager implements Listener {

    private final TuTienPlugin plugin;

    public enum MatchType { GIAO_DAU, TU_DO, SINH_TU }

    public static class Match {
        public final String id;
        public final UUID p1, p2;
        public final MatchType type;
        public final long bet;
        public boolean active = true;
        public final Location ret1, ret2;
        public long startTime;
        public Match(String id, UUID p1, UUID p2, MatchType t, long bet, Location r1, Location r2) {
            this.id=id; this.p1=p1; this.p2=p2; this.type=t; this.bet=bet;
            this.ret1=r1.clone(); this.ret2=r2.clone(); this.startTime=System.currentTimeMillis();
        }
    }

    private final Map<UUID, Match> inMatch = new HashMap<>();
    private final Map<UUID, UUID> pending  = new HashMap<>(); // challenged → challenger
    private final Map<UUID, Integer> elo   = new HashMap<>();
    private final Map<UUID, Integer> wins  = new HashMap<>();
    private final Map<UUID, Integer> losses= new HashMap<>();
    private int idCount = 1;

    public ArenaManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startTimeoutCheck();
    }

    public void handleCommand(Player p, String[] args) {
        if (args.length == 0) { showMenu(p); return; }
        switch (args[0].toLowerCase()) {
            case "giao","giaodau" -> {
                if (args.length < 2) { p.sendMessage(plugin.prefix()+"§c/vodai giao <player>"); return; }
                sendChallenge(p, args[1], MatchType.GIAO_DAU, 0);
            }
            case "tudo","tu_do" -> {
                if (args.length < 2) { p.sendMessage(plugin.prefix()+"§c/vodai tudo <player>"); return; }
                sendChallenge(p, args[1], MatchType.TU_DO, 0);
            }
            case "sinhtu","sinhtuz","sinh_tu" -> {
                if (args.length < 3) { p.sendMessage(plugin.prefix()+"§c/vodai sinhtu <player> <LT>"); return; }
                try { sendChallenge(p, args[1], MatchType.SINH_TU, Long.parseLong(args[2])); }
                catch (NumberFormatException e) { p.sendMessage(plugin.prefix()+"§cSố LT không hợp lệ!"); }
            }
            case "chap","accept"  -> acceptChallenge(p);
            case "tuchoi","decline" -> declineChallenge(p);
            case "roi","leave"    -> forfeit(p);
            case "xephang","rank" -> showRank(p);
            default -> showMenu(p);
        }
    }

    private void sendChallenge(Player challenger, String targetName, MatchType type, long bet) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) { challenger.sendMessage(plugin.prefix()+"§cKhông tìm thấy "+targetName); return; }
        if (target.equals(challenger)) { challenger.sendMessage(plugin.prefix()+"§cKhông tự thách đấu!"); return; }
        if (inMatch.containsKey(challenger.getUniqueId())) { challenger.sendMessage(plugin.prefix()+"§cBạn đang đấu!"); return; }
        if (inMatch.containsKey(target.getUniqueId()))     { challenger.sendMessage(plugin.prefix()+"§e"+target.getName()+" §cđang đấu!"); return; }
        if (type == MatchType.SINH_TU) {
            if (bet < 100) { challenger.sendMessage(plugin.prefix()+"§cCược tối thiểu 100 LT!"); return; }
            PlayerData cd = plugin.getDataManager().get(challenger);
            if (cd == null || cd.getTien() < bet) { challenger.sendMessage(plugin.prefix()+"§cKhông đủ LT!"); return; }
        }
        pending.put(target.getUniqueId(), challenger.getUniqueId());
        String typeStr = type==MatchType.GIAO_DAU?"§eGiao Tranh":type==MatchType.SINH_TU?"§cSinh Tử ("+bet+" LT)":"§7Tự Do";
        challenger.sendMessage(plugin.prefix()+"§7Gửi thách đấu "+typeStr+" §7đến §e"+target.getName());
        target.sendMessage(plugin.msg("auto_da39a3ee5e"));
        target.sendMessage(plugin.prefix()+"§6⚔ §e"+challenger.getName()+" §6thách đấu! ["+typeStr+"§6]");
        target.sendMessage(plugin.prefix()+"§a/vodai chap §7chấp nhận  §c/vodai tuchoi §7từ chối");
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pending.remove(target.getUniqueId(), challenger.getUniqueId())) {
                if (challenger.isOnline()) challenger.sendMessage(plugin.prefix()+"§7Thách đấu hết hạn.");
                if (target.isOnline()) target.sendMessage(plugin.prefix()+"§7Thách đấu từ §e"+challenger.getName()+" §7hết hạn.");
            }
        }, 600L);
    }

    private void acceptChallenge(Player p) {
        UUID challengerUUID = pending.remove(p.getUniqueId());
        if (challengerUUID == null) { p.sendMessage(plugin.prefix()+"§cKhông có thách đấu nào!"); return; }
        Player challenger = Bukkit.getPlayer(challengerUUID);
        if (challenger == null || !challenger.isOnline()) { p.sendMessage(plugin.prefix()+"§cĐối thủ offline!"); return; }
        // Determine type from existing challenge (we'll reconstruct from context)
        startMatch(challenger, p, MatchType.TU_DO, 0);
    }

    private void declineChallenge(Player p) {
        UUID id = pending.remove(p.getUniqueId());
        if (id == null) { p.sendMessage(plugin.prefix()+"§cKhông có thách đấu."); return; }
        p.sendMessage(plugin.prefix()+"§7Đã từ chối.");
        Player c = Bukkit.getPlayer(id);
        if (c != null) c.sendMessage(plugin.prefix()+"§e"+p.getName()+" §7từ chối thách đấu.");
    }

    private void startMatch(Player p1, Player p2, MatchType type, long bet) {
        // Deduct bet for SINH_TU
        if (type == MatchType.SINH_TU) {
            PlayerData d1 = plugin.getDataManager().get(p1), d2 = plugin.getDataManager().get(p2);
            if (d1 == null || d2 == null || d1.getTien() < bet || d2.getTien() < bet) {
                p1.sendMessage(plugin.prefix()+"§cMột bên không đủ LT!"); p2.sendMessage(plugin.prefix()+"§cMột bên không đủ LT!"); return;
            }
            d1.spendTien(bet); d2.spendTien(bet);
        }
        Match match = new Match("M"+(idCount++), p1.getUniqueId(), p2.getUniqueId(), type, bet, p1.getLocation(), p2.getLocation());
        inMatch.put(p1.getUniqueId(), match); inMatch.put(p2.getUniqueId(), match);
        // Full heal
        p1.setHealth(p1.getMaxHealth()); p2.setHealth(p2.getMaxHealth());
        String typeStr = type==MatchType.GIAO_DAU?"Giao Tranh":type==MatchType.SINH_TU?"Sinh Tử":"Tự Do";
        // Countdown
        new BukkitRunnable() { int cd=5; @Override public void run() {
            if (!match.active){cancel();return;}
            if (cd>0){p1.sendTitle("§e"+cd, plugin.msg("auto_1920aedea1"),0,22,0);p2.sendTitle("§e"+cd, plugin.msg("auto_1920aedea1"),0,22,0);cd--;}
            else {cancel();p1.sendTitle(plugin.msg("auto_f3f2715a0a"),"§7["+typeStr+"]",0,40,10);p2.sendTitle(plugin.msg("auto_f3f2715a0a"),"§7["+typeStr+"]",0,40,10);
                p1.sendMessage(plugin.prefix()+"§c⚔ Trận bắt đầu vs §e"+p2.getName());p2.sendMessage(plugin.prefix()+"§c⚔ Trận bắt đầu vs §e"+p1.getName());}
        }}.runTaskTimer(plugin,0L,20L);
    }

    private void endMatch(Match match, UUID winnerUUID, String reason) {
        if (!match.active) return;
        match.active = false;
        inMatch.remove(match.p1); inMatch.remove(match.p2);
        Player p1=Bukkit.getPlayer(match.p1), p2=Bukkit.getPlayer(match.p2);
        Player winner=winnerUUID!=null?Bukkit.getPlayer(winnerUUID):null;
        Player loser=winner!=null?(winner.getUniqueId().equals(match.p1)?p2:p1):null;
        if (p1!=null&&p1.isOnline()){p1.setHealth(p1.getMaxHealth());if(match.ret1!=null)p1.teleport(match.ret1);}
        if (p2!=null&&p2.isOnline()){p2.setHealth(p2.getMaxHealth());if(match.ret2!=null)p2.teleport(match.ret2);}
        if (winner==null){
            if(match.type==MatchType.SINH_TU){PlayerData d1=plugin.getDataManager().get(match.p1),d2=plugin.getDataManager().get(match.p2);if(d1!=null)d1.addTien(match.bet);if(d2!=null)d2.addTien(match.bet);}
            if(p1!=null)p1.sendMessage(plugin.prefix()+"§7Hòa: "+reason);if(p2!=null)p2.sendMessage(plugin.prefix()+"§7Hòa: "+reason);
            return;
        }
        winner.sendTitle(plugin.msg("auto_60137bb8ed"),"§7"+reason,10,80,20);
        if(loser!=null)loser.sendTitle(plugin.msg("auto_f661d0f8dd"),"§7"+reason,10,80,20);
        if(match.type==MatchType.GIAO_DAU){updateElo(winner,loser);winner.sendMessage(plugin.prefix()+"§6+ELO! Thắng Giao Tranh!");}
        if(match.type==MatchType.SINH_TU){PlayerData wd=plugin.getDataManager().get(winner);if(wd!=null){wd.addTien(match.bet*2);}winner.sendMessage(plugin.prefix()+"§c§l✦ THẮNG SINH TỬ! §6+"+(match.bet*2)+" LT!");}
        wins.merge(winner.getUniqueId(),1,Integer::sum);
        if(loser!=null)losses.merge(loser.getUniqueId(),1,Integer::sum);
    }

    public void forfeit(Player p) {
        Match m=inMatch.get(p.getUniqueId());
        if(m==null){p.sendMessage(plugin.prefix()+"§cBạn không trong trận nào!");return;}
        UUID op=m.p1.equals(p.getUniqueId())?m.p2:m.p1;
        endMatch(m,op,p.getName()+" đầu hàng!");
    }

    private void updateElo(Player w, Player l) {
        if(w==null||l==null)return;
        int we=elo.getOrDefault(w.getUniqueId(),1000),le=elo.getOrDefault(l.getUniqueId(),1000);
        double exp=1.0/(1.0+Math.pow(10,(le-we)/400.0));
        elo.put(w.getUniqueId(),(int)(we+32*(1-exp)));
        elo.put(l.getUniqueId(),(int)(le+32*(0-exp)));
    }

    private void showMenu(Player p) {
        int myElo=elo.getOrDefault(p.getUniqueId(),1000),w=wins.getOrDefault(p.getUniqueId(),0),l=losses.getOrDefault(p.getUniqueId(),0);
        p.sendMessage(plugin.msg("auto_21990548cc"));
        p.sendMessage(plugin.msg("auto_d548f19811"));
        p.sendMessage(plugin.msg("auto_7bf18676dc"));
        p.sendMessage("  §7ELO: §e"+myElo+" §8| §7Thắng: §a"+w+" §8| §7Thua: §c"+l);
        p.sendMessage(plugin.msg("auto_e4949e7649"));
        p.sendMessage(plugin.msg("auto_648e2936d5"));
        p.sendMessage(plugin.msg("auto_10e8ed72b5"));
        p.sendMessage(plugin.msg("auto_4f21a442cc"));
        p.sendMessage(plugin.msg("auto_9bd3cefe04"));
        p.sendMessage(plugin.msg("auto_081989b9d7"));
    }

    private void showRank(Player p) {
        p.sendMessage(plugin.msg("auto_e8d5bc31a4"));
        List<Map.Entry<UUID,Integer>> sorted=new ArrayList<>(elo.entrySet());
        sorted.sort((a,b)->b.getValue()-a.getValue());
        int rank=1;
        for(Map.Entry<UUID,Integer> e:sorted){
            if(rank>10)break;
            String name=Optional.ofNullable(Bukkit.getOfflinePlayer(e.getKey()).getName()).orElse("?");
            int w2=wins.getOrDefault(e.getKey(),0),l2=losses.getOrDefault(e.getKey(),0);
            p.sendMessage("  §e#"+rank+" §f"+name+" §7ELO:§e"+e.getValue()+" §8["+w2+"W/"+l2+"L]"); rank++;
        }
        if(elo.isEmpty()) p.sendMessage(plugin.msg("auto_a21286e051"));
    }

    private void startTimeoutCheck() {
        new BukkitRunnable() { @Override public void run() {
            long now=System.currentTimeMillis();
            for(Match m:new ArrayList<>(inMatch.values())){
                if(!m.active)continue;
                if(now-m.startTime>300_000L) endMatch(m,null,"Hết giờ - Hòa!");
            }
        }}.runTaskTimer(plugin,200L,200L);
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent e) {
        if(!(e.getEntity()instanceof Player victim))return;
        Player attacker=null;
        if(e.getDamager()instanceof Player p)attacker=p;
        else if(e.getDamager()instanceof org.bukkit.entity.Projectile proj&&proj.getShooter()instanceof Player p)attacker=p;
        if(attacker==null)return;
        Match vm=inMatch.get(victim.getUniqueId()),am=inMatch.get(attacker.getUniqueId());
        // VoDai da bi xoa bo - PvP tu do khong con bi chan
        if(vm==null||am==null||!vm.id.equals(am.id)) return; // chi xu ly match dang dien
        double finalHp=victim.getHealth()-e.getFinalDamage();
        if(finalHp<=0.5){e.setCancelled(true);victim.setHealth(0.5);Player fa=attacker;plugin.getServer().getScheduler().runTaskLater(plugin,()->endMatch(vm,fa.getUniqueId(),"§e"+fa.getName()+" §6hạ gục §e"+victim.getName()+"§6!"),1L);}
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) {
        Player p=e.getPlayer();
        Match m=inMatch.get(p.getUniqueId());
        if(m!=null&&m.active){UUID op=m.p1.equals(p.getUniqueId())?m.p2:m.p1;endMatch(m,op,p.getName()+" thoát game!");}
        pending.remove(p.getUniqueId());
    }

    public void addArenaSpawn(Location loc){}
    public int getArenaSpawnCount(){return 0;}
}
