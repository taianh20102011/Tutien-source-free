package com.tutien;

import com.tutien.PlatformUtil;
import com.tutien.license.LicenseManager;
import com.tutien.breakthrough.BreakthroughManager;
import com.tutien.breakthrough.TribulationBossArena;
import com.tutien.commands.TuTienCommand;
import com.tutien.commands.AdminCommand;
import com.tutien.commands.GuideCommand;
import com.tutien.aura.AuraManager;
import com.tutien.meditation.MeditationSystem;
import com.tutien.sword.SwordSummon;
import com.tutien.sword.PetSummon;
import com.tutien.tienthao.TienThao;
import com.tutien.uyap.UyApSystem;
import com.tutien.herb.HerbManager;
import com.tutien.skills.FlyingSwordManager;
import com.tutien.uyap.UyApManager;
import com.tutien.zone.ZoneManager;
import com.tutien.party.PartyManager;
import com.tutien.bloodmoon.BloodMoonManager;
import com.tutien.arena.ArenaManager;
import com.tutien.trade.TradeManager;
import com.tutien.meditation.MeditationManager;
import com.tutien.bag.TuTienBag;
import com.tutien.bag.TuTienBagCommand;
import com.tutien.alchemy.AlchemyManager;
import com.tutien.alchemy.AlchemyCommand;
import com.tutien.alchemy.AlchemyGUI;
import com.tutien.nametag.NametagManager;
import com.tutien.data.PlayerData;
import com.tutien.data.PlayerDataManager;
import com.tutien.economy.TienManager;
import com.tutien.lang.LangManager;
import com.tutien.effects.EffectEngine;
import com.tutien.lingcan.LinhCanManager;
import com.tutien.listeners.CombatListener;
import com.tutien.listeners.FlightListener;
import com.tutien.listeners.PlayerListener;
import com.tutien.phabao.PhaBaoListener;
import com.tutien.phabao.PhaBaoManager;
import com.tutien.phabao.PhaBaoEnhanceManager;
import com.tutien.phabao.PhaBaoEnhanceGUI;
import com.tutien.phabao.PhaBaoEnhanceCommand;
import com.tutien.phabao.PhaBaoSocketManager;
import com.tutien.phabao.PhaBaoSocketGUI;
import com.tutien.phabao.PhaBaoSocketCommand;
import com.tutien.phabao.PhaBaoFusionManager;
import com.tutien.phabao.PhaBaoFusionGUI;
import com.tutien.phabao.PhaBaoFusionCommand;
import com.tutien.phabao.PhaBaoLinhThucManager;
import com.tutien.phabao.PhaBaoLinhThucGUI;
import com.tutien.phabao.PhaBaoLinhThucCommand;
import com.tutien.phabao.PhaBaoKhiLinhManager;
import com.tutien.phabao.PhaBaoKhiLinhGUI;
import com.tutien.phabao.PhaBaoKhiLinhCommand;
import com.tutien.phabao.PhaBaoDoatBaoListener;
import com.tutien.skills.SkillItemManager;
import com.tutien.skills.SkillManager;
import com.tutien.skills.PassiveSkillManager;
import com.tutien.toc.TocManager;
import com.tutien.tongmon.TongMonCommand;
import com.tutien.tongmon.TongMonManager;
import com.tutien.tongmon.TongMonAllianceManager;
import com.tutien.tongmon.TongMonLandManager;
import com.tutien.congphap.ConGPhapManager;
import com.tutien.data.DaoPath;
import com.tutien.phabao.PhaBaoType;
import com.tutien.gui.HubItemFactory;
import com.tutien.gui.SkillSettingsGUI;
import com.tutien.hooks.MythicLibHook;
import com.tutien.hooks.MythicMobsHook;
import org.bukkit.plugin.java.JavaPlugin;

public class TuTienPlugin extends JavaPlugin {

    private static TuTienPlugin instance;

    private PlayerDataManager    dataManager;
    private com.tutien.career.BachNgheManager bachNgheManager;
    private com.tutien.coduyen.CoDuyenManager coDuyenManager;
    private SkillManager         skillManager;
    private PassiveSkillManager  passiveSkillManager;
    private SkillItemManager     skillItemManager;
    private CombatListener       combatListener;
    private BreakthroughManager  breakthroughManager;
    private TienManager          tienManager;
    private LangManager          langManager;
    private LinhCanManager       linhCanManager;
    private PhaBaoManager        phaBaoManager;
    private PhaBaoEnhanceManager phaBaoEnhanceManager;
    private PhaBaoEnhanceGUI     phaBaoEnhanceGUI;
    private PhaBaoSocketManager  phaBaoSocketManager;
    private PhaBaoSocketGUI      phaBaoSocketGUI;
    private PhaBaoFusionManager  phaBaoFusionManager;
    private PhaBaoFusionGUI      phaBaoFusionGUI;
    private PhaBaoLinhThucManager phaBaoLinhThucManager;
    private PhaBaoLinhThucGUI    phaBaoLinhThucGUI;
    private PhaBaoKhiLinhManager phaBaoKhiLinhManager;
    private PhaBaoKhiLinhGUI     phaBaoKhiLinhGUI;
    private FlightListener       flightListener;
    private EffectEngine         effectEngine;
    private TocManager           tocManager;
    private TongMonManager       tongMonManager;
    private TongMonAllianceManager tongMonAllianceManager;
    private TongMonLandManager tongMonLandManager;
    private ConGPhapManager conGPhapManager;
    private HubItemFactory hubItemFactory;
    private SkillSettingsGUI skillSettingsGUI;
    private AuraManager          auraManager;
    private AlchemyManager       alchemyManager;
    private AdminCommand         adminCommand;
    private NametagManager       nametagManager;
    private TuTienBag            tuTienBag;
    private PartyManager         partyManager;
    private BloodMoonManager     bloodMoonManager;
    private ArenaManager         arenaManager;
    private TradeManager         tradeManager;
    private AlchemyGUI           alchemyGUI;
    private MeditationSystem     meditationSystem;
    private MeditationManager    meditationManager;
    private ZoneManager          zoneManager;
    private UyApManager          uyApManager;
    private FlyingSwordManager   flyingSwordManager;
    private HerbManager          herbManager;
    private com.tutien.commands.LinhChungShopCommand linhChungShopCommand;
    private SwordSummon          swordSummon;
    private PetSummon            petSummon;
    private com.tutien.data.PetTierManager petTierManager;
    private com.tutien.sword.SwordFlightManager swordFlightManager;
    private com.tutien.gui.TuTienPetGUI petGUI;
    private TienThao             tienThao;
    private UyApSystem           uyApSystem;

    // ── External Plugin Hooks (optional, null nếu plugin không cài) ──
    private MythicLibHook  mythicLibHook;
    private com.tutien.hooks.LuckPermsHook luckPermsHook;
    private MythicMobsHook mythicMobsHook;
    private com.tutien.worldboss.WorldBossManager worldBossManager;
    private com.tutien.season.SeasonManager       seasonManager;
    private com.tutien.banboard.BanBoardManager banBoardManager;
    private com.tutien.hooks.ItemsAdderEffectHook itemsAdderEffectHook;

    // ── New v4 systems ──
    private com.tutien.karma.KarmaManager    karmaManager;
    private com.tutien.suphudedo.SuPhuDeDoManager suPhuDeDoManager;
    private com.tutien.tamma.TamMaManager    tamMaManager;
    private com.tutien.quest.QuestManager    questManager;
    private com.tutien.gui.TuTienGuideGUI    guideGUI;

    private String prefixStr;
    private LicenseManager licenseManager;
    private volatile boolean debugEnabled;

    @Override
    public void onEnable() {
        instance = this;
        PlatformUtil.init();  // Detect Geyser/Bedrock
        saveDefaultConfig();
        ensureConfigResource("dao.yml");
        ensureConfigResource("congphap.yml");
        ensureConfigResource("bach-nghe-tu-tien.yml");
        ensureConfigResource("co-duyen-kim-thu-chi.yml");
        ensureConfigResource("tongmon-config.yml");
        ensureConfigResource("phabao.yml");
        ensureConfigResource("hubs.yml");
        ensureConfigResource("custom-canh-gioi.yml");
        com.tutien.data.CultivationStage.loadCustomizations(
                new java.io.File(getDataFolder(), "custom-canh-gioi.yml"), getLogger());
        // Config-driven systems must load before player data and item managers.
        DaoPath.loadFrom(new java.io.File(getDataFolder(), "dao.yml"), getLogger());
        PhaBaoType.loadCustomizations(new java.io.File(getDataFolder(), "phabao.yml"), getLogger());
        licenseManager = new LicenseManager(this);
        if (!licenseManager.checkAtStartup()) {
            getLogger().severe("TuTien không khởi động do license không hợp lệ.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        debugEnabled = getConfig().getBoolean("debug.enabled", false);
        prefixStr = getConfig().getString("messages.prefix", "&8[&6Tu Tiên&8] &r")
                .replace("&", "§");
        langManager = new LangManager(this);

        // Init order matters
        tienManager         = new TienManager(this); // Vault-backed economy must be ready before player data loads
        dataManager         = new PlayerDataManager(this);
        bachNgheManager    = new com.tutien.career.BachNgheManager(this);
        coDuyenManager     = new com.tutien.coduyen.CoDuyenManager(this);
        getServer().getPluginManager().registerEvents(bachNgheManager, this);
        getServer().getPluginManager().registerEvents(coDuyenManager, this);
        conGPhapManager     = new ConGPhapManager(this);
        hubItemFactory      = new HubItemFactory(this);
        skillSettingsGUI    = new SkillSettingsGUI(this);
        getServer().getScheduler().runTask(this, () -> tienManager.refreshVault());
        skillManager        = new SkillManager(this);
        passiveSkillManager = new PassiveSkillManager(this);
        skillItemManager    = new SkillItemManager(this);
        combatListener      = new CombatListener(this);
        breakthroughManager = new BreakthroughManager(this);
        linhCanManager      = new LinhCanManager(this);
        phaBaoManager       = new PhaBaoManager(this);
        phaBaoEnhanceManager = new PhaBaoEnhanceManager(this);
        phaBaoEnhanceGUI    = new PhaBaoEnhanceGUI(this);
        phaBaoSocketManager = new PhaBaoSocketManager(this);
        phaBaoSocketGUI     = new PhaBaoSocketGUI(this);
        // ── Dung Hợp / Linh Thức / Khí Linh (tính năng nâng cao cho Pháp Bảo) ──
        phaBaoFusionManager   = new PhaBaoFusionManager(this);
        phaBaoFusionGUI       = new PhaBaoFusionGUI(this);
        phaBaoLinhThucManager = new PhaBaoLinhThucManager(this);
        phaBaoLinhThucGUI     = new PhaBaoLinhThucGUI(this);
        phaBaoKhiLinhManager  = new PhaBaoKhiLinhManager(this);
        phaBaoKhiLinhGUI      = new PhaBaoKhiLinhGUI(this);
        flightListener      = new FlightListener(this);
        effectEngine        = new EffectEngine(this);
        tocManager          = new TocManager(this);
        tongMonManager      = new TongMonManager(this);
        tongMonAllianceManager = new TongMonAllianceManager(this, tongMonManager);
        tongMonLandManager = new TongMonLandManager(this, tongMonManager);
        auraManager         = new AuraManager(this);
        alchemyManager      = new AlchemyManager(this);
        nametagManager      = new NametagManager(this);
        tuTienBag           = new TuTienBag(this);
        partyManager        = new PartyManager(this);
        bloodMoonManager    = new BloodMoonManager(this);

        // ── Season: meta mùa (class/đạo mạnh nhất), xem "/ttadmin season" ──
        seasonManager       = new com.tutien.season.SeasonManager(this);

        // ── Init optional hooks AFTER core systems ──
        mythicLibHook = new MythicLibHook(this);

        // LuckPerms is an optional integration. Do not load its API-facing hook
        // unless the LuckPerms plugin is present; the API is provided by LuckPerms
        // at runtime and is intentionally not shaded into this plugin.
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                luckPermsHook = new com.tutien.hooks.LuckPermsHook(this);
            } catch (LinkageError | RuntimeException e) {
                luckPermsHook = null;
                getLogger().warning("[Hook] Không thể nạp LuckPerms API — bỏ qua đồng bộ group/prefix/permission. "
                        + "Chi tiết: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } else {
            luckPermsHook = null;
            getLogger().info("[Hook] LuckPerms không tìm thấy — bỏ qua đồng bộ group/prefix/permission.");
        }

        mythicMobsHook = new MythicMobsHook(this);
        itemsAdderEffectHook = new com.tutien.hooks.ItemsAdderEffectHook(this);

        // ── Boss Tối Thượng (world boss tự động, cần MythicMobsHook ở trên) ──
        worldBossManager = new com.tutien.worldboss.WorldBossManager(this);
        worldBossManager.start();

        // ── Bảng "Người chơi đã bị ban" (đọc banned-players.json định kỳ) ──
        banBoardManager = new com.tutien.banboard.BanBoardManager(this);
        banBoardManager.start();

        // ── PlaceholderAPI hook ──
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.tutien.hooks.TuTienPlaceholderExpansion(this).register();
            getLogger().info("[Hook] ✅ PlaceholderAPI — 30 placeholder đã đăng ký.");
        } else {
            getLogger().info("[Hook] PlaceholderAPI không tìm thấy — bỏ qua.");
        }

        // ── New v4 systems ──
        karmaManager   = new com.tutien.karma.KarmaManager(this);
        suPhuDeDoManager = new com.tutien.suphudedo.SuPhuDeDoManager(this);
        if (getCommand("suphu")  != null) getCommand("suphu").setExecutor(suPhuDeDoManager);
        if (getCommand("dedoan") != null) { getCommand("dedoan").setExecutor(suPhuDeDoManager); getCommand("dedoan").setTabCompleter(suPhuDeDoManager); }
        tamMaManager   = new com.tutien.tamma.TamMaManager(this);
        questManager   = new com.tutien.quest.QuestManager(this);

        // Register new listeners
        getServer().getPluginManager().registerEvents(karmaManager,  this);
        getServer().getPluginManager().registerEvents(questManager,  this);
        getServer().getPluginManager().registerEvents(suPhuDeDoManager, this);
        arenaManager        = null; // VoDai da bi vo hieu hoa
        tradeManager        = new TradeManager(this);
        alchemyGUI          = new AlchemyGUI(this);
        meditationSystem    = new MeditationSystem(this);
        swordSummon         = new SwordSummon(this);
        petTierManager      = new com.tutien.data.PetTierManager(this);
        petSummon           = new PetSummon(this);
        tienThao            = new TienThao(this);
        uyApSystem          = new UyApSystem(this);
        if (getCommand("uyap") != null) {
            getCommand("uyap").setExecutor(uyApSystem);
            getCommand("uyap").setTabCompleter(uyApSystem);
        }
        meditationManager   = new MeditationManager(this);
        zoneManager         = new ZoneManager(this);
        uyApManager         = new UyApManager(this);
        flyingSwordManager  = new FlyingSwordManager(this);
        swordFlightManager  = new com.tutien.sword.SwordFlightManager(this);
        petGUI              = new com.tutien.gui.TuTienPetGUI(this);
        guideGUI            = new com.tutien.gui.TuTienGuideGUI(this);
        herbManager         = new HerbManager(this);
        linhChungShopCommand = new com.tutien.commands.LinhChungShopCommand(this);
        if (getCommand("linhchungshop") != null) getCommand("linhchungshop").setExecutor(linhChungShopCommand);

        // Commands
        TuTienCommand  cmd    = new TuTienCommand(this);
        GuideCommand   guide  = new GuideCommand(this);
        TongMonCommand tmCmd  = new TongMonCommand(this);

        if (getCommand("tutien") != null) getCommand("tutien").setExecutor(cmd);    if (getCommand("tutien") != null) getCommand("tutien").setTabCompleter(cmd);
        if (getCommand("skill") != null) { getCommand("skill").setExecutor(cmd); getCommand("skill").setTabCompleter(cmd); }
        if (getCommand("linhkhi") != null) getCommand("linhkhi").setExecutor(cmd);
        if (getCommand("vuottamma") != null) getCommand("vuottamma").setExecutor(cmd);
        if (getCommand("dotpha") != null) getCommand("dotpha").setExecutor(cmd);
        if (getCommand("boqua") != null) getCommand("boqua").setExecutor(cmd);
        if (getCommand("tongmon") != null) { getCommand("tongmon").setExecutor(tmCmd); getCommand("tongmon").setTabCompleter(tmCmd); }
        if (getCommand("mochieumo") != null) { getCommand("mochieumo").setExecutor(tmCmd); getCommand("mochieumo").setTabCompleter(tmCmd); }
        if (getCommand("congphap") != null) { getCommand("congphap").setExecutor(conGPhapManager); getCommand("congphap").setTabCompleter(conGPhapManager); }
        if (getCommand("kinang") != null) getCommand("kinang").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) { sender.sendMessage("Lệnh này chỉ dùng trong game."); return true; }
            skillSettingsGUI.open(player);
            return true;
        });
        if (getCommand("toc") != null) getCommand("toc").setExecutor(cmd);
        if (getCommand("guide") != null) getCommand("guide").setExecutor(guide);   if (getCommand("guide") != null) getCommand("guide").setTabCompleter(guide);
        AlchemyCommand alchCmd = new AlchemyCommand(this);
        if (getCommand("luyen") != null) getCommand("luyen").setExecutor(alchCmd); if (getCommand("luyen") != null) getCommand("luyen").setTabCompleter(alchCmd);
        com.tutien.forge.RenDucCommand renDucCmd = new com.tutien.forge.RenDucCommand(this);
        if (getCommand("renduc") != null) getCommand("renduc").setExecutor(renDucCmd);
        if (getCommand("cuonghoa") != null) {
            PhaBaoEnhanceCommand cuongHoaCmd = new PhaBaoEnhanceCommand(this);
            getCommand("cuonghoa").setExecutor(cuongHoaCmd);
            getCommand("cuonghoa").setTabCompleter(cuongHoaCmd);
        }
        if (getCommand("khamnam") != null) {
            PhaBaoSocketCommand khamNamCmd = new PhaBaoSocketCommand(this);
            getCommand("khamnam").setExecutor(khamNamCmd);
            getCommand("khamnam").setTabCompleter(khamNamCmd);
        }
        if (getCommand("dunghop") != null) getCommand("dunghop").setExecutor(new PhaBaoFusionCommand(this));
        if (getCommand("linhthuc") != null) getCommand("linhthuc").setExecutor(new PhaBaoLinhThucCommand(this));
        if (getCommand("khilinh") != null) getCommand("khilinh").setExecutor(new PhaBaoKhiLinhCommand(this));
        if (getCommand("aura") != null) getCommand("aura").setExecutor(cmd);

        // ── New v4 commands ──
        if (getCommand("karma")   != null) getCommand("karma").setExecutor(cmd);
        if (getCommand("quest")   != null) getCommand("quest").setExecutor(cmd);
        if (getCommand("nhiemvu") != null) getCommand("nhiemvu").setExecutor(cmd);
        if (getCommand("bachnghe") != null) { getCommand("bachnghe").setExecutor(bachNgheManager); getCommand("bachnghe").setTabCompleter(bachNgheManager); }
        if (getCommand("coduyen") != null) { getCommand("coduyen").setExecutor(coDuyenManager); getCommand("coduyen").setTabCompleter(coDuyenManager); }
        adminCommand = new AdminCommand(this);
        if (getCommand("ttadmin") != null) getCommand("ttadmin").setExecutor(adminCommand);
        if (getCommand("ttadmin") != null) getCommand("ttadmin").setTabCompleter(adminCommand);
        if (getCommand("debug") != null) {
            com.tutien.commands.DebugCommand debugCommand = new com.tutien.commands.DebugCommand(this);
            getCommand("debug").setExecutor(debugCommand);
            getCommand("debug").setTabCompleter(debugCommand);
        }

        // Tui Do
        var bagCmd2 = new com.tutien.bag.TuTienBagCommand(this);
        if (getCommand("tuitutien") != null) getCommand("tuitutien").setExecutor(bagCmd2);
        if (getCommand("ttbag") != null)     getCommand("ttbag").setExecutor(bagCmd2);

        // Trade + Tang
        if (getCommand("trade") != null) getCommand("trade").setExecutor((sender, _trc, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) return true;
            tradeManager.handleTradeCommand(p, args); return true;
        });
        if (getCommand("tang") != null) getCommand("tang").setExecutor((sender, _tgc, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) return true;
            tradeManager.handleGiftCommand(p, args); return true;
        });
        // Arena / Vo Dai da bi xoa bo
        // (chuc nang vodai khong con duoc su dung)

        // Blood Moon
        if (getCommand("bloodmoon") != null) getCommand("bloodmoon").setExecutor((sender, _bmc, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("start")) bloodMoonManager.startBloodMoon();
            else if (args.length > 0 && args[0].equalsIgnoreCase("stop")) bloodMoonManager.stopBloodMoon();
            else sender.sendMessage(prefix() + "§7/bloodmoon start | stop | status");
            return true;
        });
        if (getCommand("binhxl") != null) getCommand("binhxl").setExecutor((sender, _bxc, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) return true;
            bloodMoonManager.handleBinhCommand(p, args); return true;
        });
        // Party + Group: both command names must reach the same manager.
        org.bukkit.command.CommandExecutor partyExecutor = (sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(prefix() + "§cLệnh này chỉ dành cho người chơi.");
                return true;
            }
            partyManager.handleCommand(player, args);
            return true;
        };
        if (getCommand("party") != null) getCommand("party").setExecutor(partyExecutor);
        if (getCommand("group") != null) getCommand("group").setExecutor(partyExecutor);

        // New system commands
        if (getCommand("thiendinh") != null)
            getCommand("thiendinh").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                PlayerData _pd = dataManager.get(p);
                if (_pd != null && _pd.isMeditating()) meditationSystem.stopMeditate(p, true);
                else meditationSystem.startMeditate(p); return true;
            });
        if (getCommand("trieukiem") != null)
            getCommand("trieukiem").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                if (args.length > 0 && (args[0].equalsIgnoreCase("mode") || args[0].equalsIgnoreCase("chedo"))) {
                    swordSummon.cycleMode(p);
                } else {
                    swordSummon.toggleSwords(p);
                }
                return true;
            });
        if (getCommand("trieuthu") != null)
            getCommand("trieuthu").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                if (args.length > 0 && (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("trieuhoi") || args[0].equalsIgnoreCase("thuhoi"))) {
                    petSummon.togglePet(p);
                } else {
                    petGUI.open(p);
                }
                return true;
            });
        if (getCommand("hocbidong") != null)
            getCommand("hocbidong").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                if (args.length < 1) { p.sendMessage(prefix() + "§c/hocbidong <atk|hp|def|speed|luck|regen>"); return true; }
                passiveSkillManager.learnFromHand(p, args[0]); return true;
            });
        if (getCommand("bidong") != null)
            getCommand("bidong").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                passiveSkillManager.showLearned(p); return true;
            });
        if (getCommand("doiky") != null)
            getCommand("doiky").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                if (args.length < 1 || args[0].length() != 1 || "abcd".indexOf(Character.toLowerCase(args[0].charAt(0))) < 0) {
                    p.sendMessage(prefix() + "§c/doiky <a|b|c|d>"); return true;
                }
                int slot = Character.toLowerCase(args[0].charAt(0)) - 'a';
                skillItemManager.cycleMergedSlot(p, slot);
                return true;
            });
        if (getCommand("ngukiem") != null)
            getCommand("ngukiem").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                swordFlightManager.toggle(p); return true;
            });
        if (getCommand("thao") != null)
            getCommand("thao").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                herbManager.showHerbInfo(p); return true;
            });
        if (getCommand("addlinhzone") != null)
            getCommand("addlinhzone").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof org.bukkit.entity.Player p)) return true;
                if (!p.hasPermission("tutien.admin")) { p.sendMessage(prefix() + "§cKhông có quyền!"); return true; }
                String name = args.length > 0 ? args[0] : "zone_" + System.currentTimeMillis();
                double radius = 20.0;
                if (args.length > 1) {
                    try {
                        radius = Double.parseDouble(args[1]);
                    } catch (NumberFormatException ex) {
                        p.sendMessage(prefix() + "§cBán kính phải là số từ 1 đến 200.");
                        return true;
                    }
                }
                if (!Double.isFinite(radius) || radius < 1.0 || radius > 200.0) {
                    p.sendMessage(prefix() + "§cBán kính phải nằm trong khoảng 1 đến 200.");
                    return true;
                }
                meditationSystem.addZone(p.getLocation(), radius, name);
                p.sendMessage(prefix() + "§aĐã tạo Linh Mạch §e" + name + " §abán kính §e" + radius + "m §a(LK x1.5)");
                return true;
            });

        // Events
        var pm = getServer().getPluginManager();
        pm.registerEvents(combatListener,           this);
        pm.registerEvents(tongMonAllianceManager,   this);
        pm.registerEvents(tongMonLandManager,       this);
        pm.registerEvents(skillSettingsGUI,         this);
        pm.registerEvents(zoneManager,              this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(skillItemManager,         this);
        pm.registerEvents(flightListener,           this);
        pm.registerEvents(new PhaBaoListener(this), this);
        pm.registerEvents(phaBaoEnhanceGUI,         this);
        pm.registerEvents(phaBaoSocketGUI,          this);
        pm.registerEvents(phaBaoFusionManager,      this); // proc dong an khi danh trung
        pm.registerEvents(phaBaoFusionGUI,          this);
        pm.registerEvents(phaBaoLinhThucManager,    this); // ho the / phan kich / hoa canh
        pm.registerEvents(phaBaoLinhThucGUI,        this);
        pm.registerEvents(phaBaoKhiLinhGUI,         this);
        pm.registerEvents(new PhaBaoDoatBaoListener(this), this); // doat bao PVP khi chet
        pm.registerEvents(tocManager,               this);
        pm.registerEvents(tuTienBag,                this);
        pm.registerEvents(partyManager,             this);
        pm.registerEvents(bloodMoonManager,         this);

        // Fix lỗi tại đây: Chỉ đăng ký nếu không null
        if (arenaManager != null) {
            pm.registerEvents(arenaManager,         this);
        }

        pm.registerEvents(tradeManager,             this);
        pm.registerEvents(alchemyGUI,               this);
        pm.registerEvents(meditationSystem,         this);
        pm.registerEvents(uyApSystem,               this);
        pm.registerEvents(herbManager,              this);
        pm.registerEvents(swordSummon,              this);
        pm.registerEvents(petSummon,                this);
        pm.registerEvents(petGUI,                   this);
        pm.registerEvents(passiveSkillManager,      this);
        pm.registerEvents(swordFlightManager,       this);
        pm.registerEvents(guideGUI,                 this);
        pm.registerEvents(breakthroughManager,      this);

        // Kiểm tra an toàn cho Boss Arena
        if (breakthroughManager != null && breakthroughManager.getBossArena() != null) {
            pm.registerEvents(breakthroughManager.getBossArena(), this);
        }

        // Dot pha qua NPC Citizens (xem config "breakthrough-npc"). CHI dang
        // ky khi Citizens THAT SU dang chay tren server — neu khong, class
        // com.tutien.breakthrough.BreakthroughNpcListener (tham chieu
        // NPCRightClickEvent cua Citizens) se KHONG BAO GIO duoc JVM load,
        // tranh loi NoClassDefFoundError khi thieu Citizens.
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            if (getConfig().getBoolean("breakthrough-npc.enabled", false)) {
                pm.registerEvents(new com.tutien.breakthrough.BreakthroughNpcListener(this), this);
                getLogger().info("[TuTien] Da phat hien Citizens -> bat tinh nang Dot Pha qua NPC.");
            } else {
                getLogger().info("[TuTien] Citizens da duoc cai nhung \"breakthrough-npc.enabled\" dang false "
                        + "-> Dot Pha qua NPC KHONG duoc bat.");
            }
        } else if (getConfig().getBoolean("breakthrough-npc.enabled", false)) {
            getLogger().warning("[TuTien] config \"breakthrough-npc.enabled: true\" nhung KHONG tim thay "
                    + "plugin Citizens tren server -> tinh nang Dot Pha qua NPC se KHONG hoat dong "
                    + "(lenh /vuottamma, /dotpha se van bi khoa neu \"world\" khop, nhung khong co cach nao dot pha o world do)!");
        }

        // Auto-save 5 min
        getServer().getScheduler().runTaskTimer(this, () -> {
            // PlayerDataManager and TongMonManager hold mutable HashMaps and Bukkit-backed state.
            // Saving from an async timer raced with joins/quits and could corrupt or lose data.
            dataManager.saveAll();
            tongMonManager.saveAll();
            if (tongMonAllianceManager != null) tongMonAllianceManager.save();
            if (tongMonLandManager != null) tongMonLandManager.save();
        }, 6000L, 6000L);
        licenseManager.startPeriodicValidation();

        getLogger().info("╔═══════════════════════════════════════════╗");
        getLogger().info("║    TU TIÊN PLUGIN v" + getDescription().getVersion() + "                  ║");
        getLogger().info("║ 100+ cảnh giới | 83 Pháp Bảo | Karma+Quest║");
        getLogger().info("║  6 Tộc (0.0001% Long Tộc) | Tông Môn      ║");
        getLogger().info("║  6 cấp Pháp Bảo | Linh Căn | Hiệu Ứng     ║");
        getLogger().info("╚═══════════════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        if (bachNgheManager != null) bachNgheManager.save();
        if (coDuyenManager != null) coDuyenManager.save();
        if (dataManager   != null) dataManager.saveAll();
        if (tongMonManager != null) tongMonManager.saveAll();
        if (tongMonAllianceManager != null) tongMonAllianceManager.save();
        if (tongMonLandManager != null) tongMonLandManager.save();
        if (banBoardManager != null) banBoardManager.stop();
        if (mythicLibHook != null) mythicLibHook.shutdown();
        if (zoneManager != null) zoneManager.saveZones();
        if (swordFlightManager != null) swordFlightManager.cleanup();
        if (petSummon != null) petSummon.cleanup();
        if (licenseManager != null) licenseManager.shutdown();
        instance = null;
        getLogger().info("TuTien đã tắt. Đã lưu dữ liệu.");
    }

    public String prefix()                               { return prefixStr; }
    public LicenseManager getLicenseManager()             { return licenseManager; }
    public boolean isDebugEnabled()                      { return debugEnabled; }
    public void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        getConfig().set("debug.enabled", enabled);
        saveConfig();
    }
    /** Refreshes config-backed runtime flags after an admin reload. */
    public void refreshConfigState() {
        debugEnabled = getConfig().getBoolean("debug.enabled", false);
        prefixStr = getConfig().getString("messages.prefix", "&8[&6Tu Tiên&8] &r").replace("&", "§");
    }
    public void debug(String message) {
        if (debugEnabled) getLogger().info("[DEBUG] " + message);
    }
    private void ensureConfigResource(String name) {
        java.io.File target = new java.io.File(getDataFolder(), name);
        if (!target.exists() && getResource(name) != null) {
            try { saveResource(name, false); }
            catch (IllegalArgumentException ex) { getLogger().warning("Không thể tạo cấu hình " + name + ": " + ex.getMessage()); }
        }
    }

    public static TuTienPlugin getInstance()             { return instance; }
    public PlayerDataManager    getDataManager()         { return dataManager; }
    public com.tutien.career.BachNgheManager getBachNgheManager() { return bachNgheManager; }
    public com.tutien.coduyen.CoDuyenManager getCoDuyenManager() { return coDuyenManager; }
    public SkillManager         getSkillManager()        { return skillManager; }
    public PassiveSkillManager  getPassiveSkillManager() { return passiveSkillManager; }
    public SkillItemManager     getSkillItemManager()    { return skillItemManager; }
    public CombatListener       getCombatListener()      { return combatListener; }
    public BreakthroughManager  getBreakthroughManager() { return breakthroughManager; }
    public TienManager          getTienManager()         { return tienManager; }
    public LangManager          getLangManager()         { return langManager; }
    public String msg(String key)                         { return langManager.msg(key); }
    public LinhCanManager       getLinhCanManager()      { return linhCanManager; }
    public PhaBaoManager        getPhaBaoManager()       { return phaBaoManager; }
    public PhaBaoEnhanceManager getPhaBaoEnhanceManager(){ return phaBaoEnhanceManager; }
    public PhaBaoEnhanceGUI     getPhaBaoEnhanceGUI()    { return phaBaoEnhanceGUI; }
    public PhaBaoSocketManager  getPhaBaoSocketManager() { return phaBaoSocketManager; }
    public PhaBaoSocketGUI      getPhaBaoSocketGUI()     { return phaBaoSocketGUI; }
    public PhaBaoFusionManager   getPhaBaoFusionManager()   { return phaBaoFusionManager; }
    public PhaBaoFusionGUI       getPhaBaoFusionGUI()       { return phaBaoFusionGUI; }
    public PhaBaoLinhThucManager getPhaBaoLinhThucManager() { return phaBaoLinhThucManager; }
    public PhaBaoLinhThucGUI     getPhaBaoLinhThucGUI()     { return phaBaoLinhThucGUI; }
    public PhaBaoKhiLinhManager  getPhaBaoKhiLinhManager()  { return phaBaoKhiLinhManager; }
    public PhaBaoKhiLinhGUI      getPhaBaoKhiLinhGUI()      { return phaBaoKhiLinhGUI; }
    public FlightListener       getFlightListener()      { return flightListener; }
    public EffectEngine         getEffectEngine()        { return effectEngine; }
    public TocManager           getTocManager()          { return tocManager; }
    public TongMonManager       getTongMonManager()      { return tongMonManager; }
    public TongMonAllianceManager getTongMonAllianceManager() { return tongMonAllianceManager; }
    public TongMonLandManager getTongMonLandManager() { return tongMonLandManager; }
    public ConGPhapManager getConGPhapManager() { return conGPhapManager; }
    public HubItemFactory getHubItemFactory() { return hubItemFactory; }
    public SkillSettingsGUI getSkillSettingsGUI() { return skillSettingsGUI; }
    public AuraManager          getAuraManager()         { return auraManager; }
    public AlchemyManager       getAlchemyManager()      { return alchemyManager; }
    public NametagManager       getNametagManager()      { return nametagManager; }
    public TuTienBag            getTuTienBag()           { return tuTienBag; }
    public PartyManager         getPartyManager()        { return partyManager; }
    public BloodMoonManager     getBloodMoonManager()    { return bloodMoonManager; }
    public ArenaManager         getArenaManager()        { return arenaManager; }
    public TradeManager         getTradeManager()        { return tradeManager; }
    public AlchemyGUI           getAlchemyGUI()          { return alchemyGUI; }
    public MeditationSystem     getMeditationSystem()    { return meditationSystem; }
    public SwordSummon          getSwordSummon()         { return swordSummon; }
    public PetSummon            getPetSummon()           { return petSummon; }
    public com.tutien.data.PetTierManager getPetTierManager() { return petTierManager; }
    public com.tutien.gui.TuTienPetGUI getPetGUI() { return petGUI; }
    public com.tutien.sword.SwordFlightManager getSwordFlightManager() { return swordFlightManager; }
    public TienThao             getTienThao()            { return tienThao; }
    public UyApSystem           getUyApSystem()          { return uyApSystem; }
    public MeditationManager    getMeditationManager()   { return meditationManager; }
    public ZoneManager          getZoneManager()         { return zoneManager; }
    public UyApManager          getUyApManager()         { return uyApManager; }
    public FlyingSwordManager   getFlyingSwordManager()  { return flyingSwordManager; }
    public HerbManager          getHerbManager()         { return herbManager; }

    // ── Hook getters (null-safe: trả null nếu plugin không được cài) ──
    public MythicLibHook  getMythicLibHook()  { return mythicLibHook; }
    public com.tutien.hooks.LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public MythicMobsHook getMythicMobsHook() { return mythicMobsHook; }
    public com.tutien.worldboss.WorldBossManager getWorldBossManager() { return worldBossManager; }
    public com.tutien.season.SeasonManager       getSeasonManager()    { return seasonManager; }
    public com.tutien.banboard.BanBoardManager getBanBoardManager() { return banBoardManager; }
    public com.tutien.hooks.ItemsAdderEffectHook getItemsAdderEffectHook() { return itemsAdderEffectHook; }
    public boolean hasMythicLib()  { return mythicLibHook  != null && mythicLibHook.isAvailable(); }
    public boolean hasMythicMobs() { return mythicMobsHook != null && mythicMobsHook.isAvailable(); }

    // ── New v4 system getters ──
    public com.tutien.karma.KarmaManager    getKarmaManager()    { return karmaManager; }
    public com.tutien.suphudedo.SuPhuDeDoManager getSuPhuDeDoManager() { return suPhuDeDoManager; }
    public com.tutien.tamma.TamMaManager    getTamMaManager()    { return tamMaManager; }
    public com.tutien.quest.QuestManager    getQuestManager()    { return questManager; }
    public com.tutien.gui.TuTienGuideGUI    getGuideGUI()        { return guideGUI; }
}
