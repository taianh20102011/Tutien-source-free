package com.tutien.party;

import com.tutien.TuTienPlugin;
import com.tutien.data.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PartyManager implements Listener {

    private final TuTienPlugin plugin;

    public static class Party {
        public final UUID leader;
        public final List<UUID> members = new ArrayList<>();
        public Party(UUID leader) { this.leader=leader; members.add(leader); }
    }

    private final Map<UUID, Party>  partyOf   = new HashMap<>(); // member → party
    private final Map<UUID, UUID>   invites   = new HashMap<>(); // invitee → leader
    private static final int MAX_SIZE = 5;
    private static final double RANGE = 30.0;

    public PartyManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        startBonusLoop();
        startEffectLoop();
    }

    public void handleCommand(Player p, String[] args) {
        if(args.length==0){showInfo(p);return;}
        switch(args[0].toLowerCase()){
            case "create"          -> create(p);
            case "invite"          -> { if(args.length<2){p.sendMessage(plugin.prefix()+"§c/party invite <player>");return;}invite(p,args[1]);}
            case "accept"          -> accept(p);
            case "decline"         -> decline(p);
            case "leave"           -> leave(p);
            case "kick"            -> { if(args.length<2){p.sendMessage(plugin.prefix()+"§c/party kick <player>");return;}kick(p,args[1]);}
            case "disband"         -> disband(p);
            case "info"            -> showInfo(p);
            default                -> showHelp(p);
        }
    }

    private void create(Player p) {
        if(partyOf.containsKey(p.getUniqueId())){p.sendMessage(plugin.prefix()+"§cBạn đã trong nhóm! /party leave để rời.");return;}
        Party party=new Party(p.getUniqueId());
        partyOf.put(p.getUniqueId(),party);
        p.sendMessage(plugin.prefix()+"§6✦ Đã tạo nhóm Tu Tiên! /party invite <player> để mời.");
        p.playSound(p.getLocation(),Sound.BLOCK_BEACON_POWER_SELECT,1f,1.5f);
    }

    private void invite(Player leader, String targetName) {
        Party party=partyOf.get(leader.getUniqueId());
        if(party==null){leader.sendMessage(plugin.prefix()+"§cBạn không có nhóm! /party create");return;}
        if(!party.leader.equals(leader.getUniqueId())){leader.sendMessage(plugin.prefix()+"§cChỉ trưởng nhóm mới mời được!");return;}
        if(party.members.size()>=MAX_SIZE){leader.sendMessage(plugin.prefix()+"§cNhóm đầy ("+MAX_SIZE+"/"+MAX_SIZE+")!");return;}
        Player target=Bukkit.getPlayer(targetName);
        if(target==null){leader.sendMessage(plugin.prefix()+"§cKhông tìm thấy §e"+targetName);return;}
        if(target.equals(leader)){leader.sendMessage(plugin.prefix()+"§cKhông mời chính mình!");return;}
        if(partyOf.containsKey(target.getUniqueId())){leader.sendMessage(plugin.prefix()+"§e"+target.getName()+" §cđang trong nhóm khác!");return;}
        invites.put(target.getUniqueId(),leader.getUniqueId());
        leader.sendMessage(plugin.prefix()+"§7Đã mời §e"+target.getName()+"§7.");
        target.sendMessage(plugin.prefix()+"§6✦ §e"+leader.getName()+" §6mời bạn vào nhóm Tu Tiên!");
        target.sendMessage(plugin.prefix()+"§a/party accept §7chấp nhận  §c/party decline §7từ chối");
        target.playSound(target.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,0.8f,1.3f);
        plugin.getServer().getScheduler().runTaskLater(plugin,()->{
            if(invites.remove(target.getUniqueId(),leader.getUniqueId())){
                if(target.isOnline())target.sendMessage(plugin.prefix()+"§7Lời mời từ §e"+leader.getName()+" §7hết hạn.");
                if(leader.isOnline())leader.sendMessage(plugin.prefix()+"§e"+target.getName()+" §7không phản hồi.");
            }},1200L);
    }

    private void accept(Player p) {
        UUID lid=invites.remove(p.getUniqueId());
        if(lid==null){p.sendMessage(plugin.prefix()+"§cKhông có lời mời nào!");return;}
        Player leader=Bukkit.getPlayer(lid);
        if(leader==null||!leader.isOnline()){p.sendMessage(plugin.prefix()+"§cNgười mời đã offline!");return;}
        Party party=partyOf.get(lid);
        if(party==null){p.sendMessage(plugin.prefix()+"§cNhóm không còn tồn tại.");return;}
        if(party.members.size()>=MAX_SIZE){p.sendMessage(plugin.prefix()+"§cNhóm đã đầy!");return;}
        if(partyOf.containsKey(p.getUniqueId())){p.sendMessage(plugin.prefix()+"§cBạn đang trong nhóm, rời trước!");return;}
        party.members.add(p.getUniqueId());
        partyOf.put(p.getUniqueId(),party);
        broadcast(party,plugin.prefix()+"§a✦ §e"+p.getName()+" §a§ltham gia nhóm! §8("+party.members.size()+"/"+MAX_SIZE+")");
        p.sendTitle(plugin.msg("auto_e412fe0fc2"), plugin.msg("auto_44b0edfb5a"),5,50,10);
        p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.8f,1.2f);
    }

    private void decline(Player p){UUID lid=invites.remove(p.getUniqueId());if(lid==null){p.sendMessage(plugin.prefix()+"§cKhông có lời mời.");return;}p.sendMessage(plugin.prefix()+"§7Đã từ chối.");Player l=Bukkit.getPlayer(lid);if(l!=null)l.sendMessage(plugin.prefix()+"§e"+p.getName()+" §7từ chối.");}

    private void leave(Player p){
        if(!partyOf.containsKey(p.getUniqueId())){p.sendMessage(plugin.prefix()+"§cBạn không trong nhóm nào!");return;}
        Party party=partyOf.get(p.getUniqueId());
        if(party.leader.equals(p.getUniqueId())){disband(p);return;}
        party.members.remove(p.getUniqueId());partyOf.remove(p.getUniqueId());
        p.sendMessage(plugin.prefix()+"§7Đã rời nhóm.");broadcast(party,plugin.prefix()+"§e"+p.getName()+" §7đã rời nhóm.");
    }

    private void kick(Player leader, String targetName){
        Party party=partyOf.get(leader.getUniqueId());
        if(party==null||!party.leader.equals(leader.getUniqueId())){leader.sendMessage(plugin.prefix()+"§cChỉ trưởng nhóm mới kick được!");return;}
        Player target=Bukkit.getPlayer(targetName);
        if(target==null||!party.members.contains(target.getUniqueId())){leader.sendMessage(plugin.prefix()+"§e"+targetName+" §ckhông trong nhóm!");return;}
        party.members.remove(target.getUniqueId());partyOf.remove(target.getUniqueId());
        target.sendMessage(plugin.prefix()+"§cBạn bị kick khỏi nhóm!");broadcast(party,plugin.prefix()+"§e"+target.getName()+" §7bị kick.");
    }

    private void disband(Player leader){
        Party party=partyOf.get(leader.getUniqueId());
        if(party==null||!party.leader.equals(leader.getUniqueId())){leader.sendMessage(plugin.prefix()+"§cBạn không phải trưởng nhóm!");return;}
        broadcast(party,plugin.prefix()+"§c✦ Nhóm đã giải tán!");
        for(UUID uid:new ArrayList<>(party.members))partyOf.remove(uid);
    }

    public void showInfo(Player p){
        if(!partyOf.containsKey(p.getUniqueId())){p.sendMessage(plugin.prefix()+"§7Bạn không trong nhóm nào. §e/party create §7để tạo nhóm.");showHelp(p);return;}
        Party party=partyOf.get(p.getUniqueId());
        Player leader=Bukkit.getPlayer(party.leader);
        p.sendMessage(plugin.msg("auto_21990548cc"));
        p.sendMessage(plugin.msg("auto_93879b2373"));
        p.sendMessage(plugin.msg("auto_7bf18676dc"));
        p.sendMessage("  §7Trưởng nhóm: §6"+(leader!=null?leader.getName():"?")+"  §7Thành viên: §e"+party.members.size()+"/"+MAX_SIZE);
        p.sendMessage(plugin.msg("auto_7bf18676dc"));
        int nearby=0;
        for(UUID uid:party.members){Player m=Bukkit.getPlayer(uid);if(m==null)continue;boolean near=m.getWorld().equals(p.getWorld())&&m.getLocation().distance(p.getLocation())<=RANGE;if(near)nearby++;PlayerData data=plugin.getDataManager().get(m);String stage=data!=null?data.getStage().getDisplayName():"?";p.sendMessage("  "+(uid.equals(party.leader)?"§6★ ":"§7▸ ")+"§f"+m.getName()+" "+(near?"§a[Gần]":"§7[Xa]")+" §8["+stage+"]");}
        double bonus=getBonus(nearby);
        p.sendMessage(plugin.msg("auto_7bf18676dc"));
        p.sendMessage("  §7Gần nhau: §e"+nearby+" §7→ Bonus LK: §a+"+(int)(bonus*100)+"%");
        p.sendMessage(plugin.msg("auto_081989b9d7"));
    }

    private void showHelp(Player p){
        p.sendMessage(plugin.msg("auto_f9e969b693"));
        p.sendMessage(plugin.msg("auto_bc5b564b1a"));
        p.sendMessage(plugin.msg("auto_6c9f9d1e64"));
        p.sendMessage(plugin.msg("auto_0308713c62"));
    }

    // ─── BONUS LK ─────────────────────────────────────────

    private void startBonusLoop(){
        new BukkitRunnable(){@Override public void run(){
            Set<Party> seen=new HashSet<>();
            for(Party party:new HashSet<>(partyOf.values())){
                if(!seen.add(party))continue;
                if(party.members.size()<2)continue;
                List<Player> near=getNear(party);
                if(near.size()<2)continue;
                double bonus=getBonus(near.size());
                for(Player p:near){PlayerData d=plugin.getDataManager().get(p);if(d==null)continue;long lkBonus=(long)(d.getMaxLinhKhi()*0.001*bonus);d.addLinhKhi(lkBonus);if(d.getLinhKhi()>=d.getMaxLinhKhi())plugin.getBreakthroughManager().notifyBreakthroughReady(p,d);plugin.getSkillItemManager().updateInfoItem(p,d);}
            }
        }}.runTaskTimer(plugin,100L,100L);
    }

    private List<Player> getNear(Party party){
        List<Player> list=new ArrayList<>();Player ref=null;
        for(UUID uid:party.members){Player p=Bukkit.getPlayer(uid);if(p==null||!p.isOnline())continue;if(ref==null){ref=p;list.add(p);}else if(p.getWorld().equals(ref.getWorld())&&p.getLocation().distance(ref.getLocation())<=RANGE)list.add(p);}
        return list;
    }

    public void onBreakthrough(Player p){
        if(!partyOf.containsKey(p.getUniqueId()))return;
        Party party=partyOf.get(p.getUniqueId());
        for(UUID uid:party.members){if(uid.equals(p.getUniqueId()))continue;Player m=Bukkit.getPlayer(uid);if(m==null||!m.isOnline())continue;m.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.getByKey(NamespacedKey.minecraft("regeneration")),200,1,false,true,true));m.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.getByKey(NamespacedKey.minecraft("strength")),200,0,false,true,true));m.sendMessage(plugin.prefix()+"§6✦ §e"+p.getName()+" §6đột phá! Bạn nhận buff hỗ trợ!");}
        broadcast(party,plugin.prefix()+"§6§l✦ "+p.getName()+" §e§lđột phá thành công! §7Cả nhóm nhận buff!");
    }

    // ─── EFFECTS ─────────────────────────────────────────

    private void startEffectLoop(){
        new BukkitRunnable(){int t=0;@Override public void run(){t++;if(t%20!=0)return;
            Set<Party> seen=new HashSet<>();
            for(Party party:new HashSet<>(partyOf.values())){
                if(!seen.add(party))continue;
                List<Player> online=new ArrayList<>();for(UUID uid:party.members){Player p=Bukkit.getPlayer(uid);if(p!=null&&p.isOnline())online.add(p);}
                for(int i=0;i<online.size()-1;i++){Player a=online.get(i),b=online.get(i+1);if(!a.getWorld().equals(b.getWorld()))continue;if(a.getLocation().distance(b.getLocation())>RANGE)continue;for(double tt=0.2;tt<1.0;tt+=0.25){Location mid=a.getLocation().clone().add(b.getLocation().clone().subtract(a.getLocation()).multiply(tt));mid.add(0,1.2,0);a.getWorld().spawnParticle(Particle.DUST,mid,1,0.05,0.05,0.05,0,new Particle.DustOptions(Color.fromRGB(100,200,255),0.8f));}}
            }
        }}.runTaskTimer(plugin,5L,1L);
    }

    // ─── HELPERS ─────────────────────────────────────────

    private double getBonus(int n){if(n==2)return 0.20;if(n==3)return 0.40;if(n==4)return 0.60;return 0.80;}
    private void broadcast(Party party,String msg){for(UUID uid:party.members){Player p=Bukkit.getPlayer(uid);if(p!=null&&p.isOnline())p.sendMessage(msg);}}
    public boolean isInParty(Player p){return partyOf.containsKey(p.getUniqueId());}

    @EventHandler public void onQuit(PlayerQuitEvent e){
        Player p=e.getPlayer();
        if(!partyOf.containsKey(p.getUniqueId()))return;
        Party party=partyOf.get(p.getUniqueId());
        if(party.leader.equals(p.getUniqueId()))disband(p);else leave(p);
    }
}
