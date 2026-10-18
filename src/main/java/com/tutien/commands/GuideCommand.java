package com.tutien.commands;

import com.tutien.TuTienPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

/**
 * /guide - Hướng dẫn chi tiết cho người mới chơi Tu Tiên Plugin
 * /hd <chu_de>
 */
public class GuideCommand implements CommandExecutor, TabCompleter {

    private final TuTienPlugin plugin;

    public GuideCommand(TuTienPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(plugin.msg("auto_6df645165c")); return true; }

        // /guide is owned by TuTienPlugin. When DeluxeMenus is installed,
        // hand the main guide over to DeluxeMenus instead of printing the old text menu.
        if (args.length == 0) {
            if (openDeluxeMenusGuide(player)) {
                return true;
            }
            showMainMenu(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "batdau","start"       -> { showBatDau(player);     yield true; }
            case "canh_gioi","level"    -> { showCanhGioi(player);   yield true; }
            case "class"                -> { showClass(player);       yield true; }
            case "dao"                  -> { showDao(player);         yield true; }
            case "linh_can","linhcan"   -> { showLinhCan(player);    yield true; }
            case "toc"                  -> { showToc(player);         yield true; }
            case "skill"                -> { showSkill(player);       yield true; }
            case "phap_bao","phabao"    -> { showPhaBao(player);     yield true; }
            case "dot_pha","dotpha"     -> { showDotPha(player);     yield true; }
            case "linh_dan","luyen"     -> { showLinhDan(player);    yield true; }
            case "tien","lt"            -> { showTien(player);       yield true; }
            case "tong_mon","tongmon"   -> { showTongMon(player);    yield true; }
            case "lenh","commands"      -> { showLenh(player);        yield true; }
            case "meo","tip"            -> { showMeo(player);         yield true; }
            case "thiendinh","td"       -> { showThienDinh(player); yield true; }
            case "kiemdao","sword"      -> { showKiemDao(player);   yield true; }
            case "thao","herb"          -> { showThao(player);      yield true; }
            case "zone"                 -> { showZone(player);      yield true; }
            default                     -> { showMainMenu(player);   yield true; }
        };
    }

    /**
     * Opens the DeluxeMenus menu "tutien_guide" through its admin command.
     * Console dispatch is used so normal members do not need deluxemenus.open.others.
     */
    private boolean openDeluxeMenusGuide(Player player) {
        if (plugin.getServer().getPluginManager().getPlugin("DeluxeMenus") == null) {
            return false;
        }

        try {
            String command = "dm open tutien_guide " + player.getName();
            boolean ok = plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    command
            );

            if (!ok) {
                plugin.getLogger().warning("Không thể mở DeluxeMenus menu tutien_guide cho " + player.getName());
            }
            return ok;
        } catch (Exception ex) {
            plugin.getLogger().warning("Lỗi khi mở DeluxeMenus tutien_guide: " + ex.getMessage());
            return false;
        }
    }

    // ========================
    //  MENU CHÍNH
    // ========================

    private void showMainMenu(Player player) {
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_e2666b3719"));
        player.sendMessage(plugin.msg("auto_57fd58df39"));
        player.sendMessage(plugin.msg("auto_415b23d63e"));
        player.sendMessage(plugin.msg("auto_cf3c014412"));
        player.sendMessage(plugin.msg("auto_8d79d2cf3a"));
        player.sendMessage(plugin.msg("auto_0647212cfd"));
        player.sendMessage(plugin.msg("auto_3a4649a39a"));
        player.sendMessage(plugin.msg("auto_9b3fab2287"));
        player.sendMessage(plugin.msg("auto_b80821e59c"));
        player.sendMessage(plugin.msg("auto_a2a9ef10a2"));
        player.sendMessage(plugin.msg("auto_c8fbc22302"));
        player.sendMessage(plugin.msg("auto_4ee3006447"));
        player.sendMessage(plugin.msg("auto_fb03960a54"));
        player.sendMessage(plugin.msg("auto_5f34b048c8"));
        player.sendMessage(plugin.msg("auto_6b6999a6ab"));
        player.sendMessage(plugin.msg("auto_aaa55c0013"));
        player.sendMessage(plugin.msg("auto_af793931b0"));
        player.sendMessage(plugin.msg("auto_69eef8845d"));
        player.sendMessage(plugin.msg("auto_ccfc9ea068"));
    }

    // ========================
    //  BẮT ĐẦU (Người mới)
    // ========================

    private void showBatDau(Player player) {
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_e2666b3719"));
        player.sendMessage(plugin.msg("auto_5189f84122"));
        player.sendMessage(plugin.msg("auto_415b23d63e"));
        player.sendMessage(plugin.msg("auto_0b1c5cefa9"));
        player.sendMessage(plugin.msg("auto_cd6853f448"));
        player.sendMessage(plugin.msg("auto_b9a892e397"));
        player.sendMessage(plugin.msg("auto_3722a62cef"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_546b589ea8"));
        player.sendMessage(plugin.msg("auto_eb0f79aea2"));
        player.sendMessage(plugin.msg("auto_20467a56d7"));
        player.sendMessage(plugin.msg("auto_901ceb5983"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_6f57bf9991"));
        player.sendMessage(plugin.msg("auto_4a4867162a"));
        player.sendMessage(plugin.msg("auto_933cad9960"));
        player.sendMessage(plugin.msg("auto_8bee24f8a8"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_37996cbf52"));
        player.sendMessage(plugin.msg("auto_cf0b24f4b8"));
        player.sendMessage(plugin.msg("auto_3590cf511b"));
        player.sendMessage(plugin.msg("auto_5d1b91b51a"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_4051e370ee"));
        player.sendMessage(plugin.msg("auto_c1e57fe7a9"));
        player.sendMessage(plugin.msg("auto_a3662d8bdd"));
        player.sendMessage(plugin.msg("auto_74a7ce0f36"));
        player.sendMessage(plugin.msg("auto_cf7f7f6137"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_b110cd233d"));
        player.sendMessage(plugin.msg("auto_da189f166f"));
        player.sendMessage(plugin.msg("auto_b0b9a378dc"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_ce3e839a45"));
        player.sendMessage(plugin.msg("auto_a7ab6cd5bf"));
        player.sendMessage(plugin.msg("auto_69eef8845d"));
    }

    // ========================
    //  CẢNH GIỚI
    // ========================

    private void showCanhGioi(Player player) {
        player.sendMessage(plugin.msg("auto_3e4a34df5c"));
        player.sendMessage(plugin.msg("auto_00a22778f9"));
        player.sendMessage(plugin.msg("auto_e03366e85e"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_135ce01263"));
        player.sendMessage(plugin.msg("auto_86ee20a34e"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_a2e7c5a6cd"));
        player.sendMessage(plugin.msg("auto_d85c501f9c"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_21ccf49c1e"));
        player.sendMessage(plugin.msg("auto_0f706b1846"));
        player.sendMessage(plugin.msg("auto_87178cfd02"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_29c66ab794"));
        player.sendMessage(plugin.msg("auto_079248c6a4"));
        player.sendMessage(plugin.msg("auto_1ab64f84b9"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_8204ba4f09"));
        player.sendMessage(plugin.msg("auto_9ea55ce169"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_3e83b80669"));
        player.sendMessage(plugin.msg("auto_c9d0588637"));
        player.sendMessage(plugin.msg("auto_ddb6eb52a2"));
    }

    // ========================
    //  CLASS
    // ========================

    private void showClass(Player player) {
        player.sendMessage(plugin.msg("auto_a672ea8859"));
        player.sendMessage(plugin.msg("auto_976fbaf0e2"));
        player.sendMessage(plugin.msg("auto_d7d221d3e4"));
        player.sendMessage(plugin.msg("auto_dbc256f40c"));
        player.sendMessage(plugin.msg("auto_049b0922f8"));
        player.sendMessage(plugin.msg("auto_c3b63dcced"));
        player.sendMessage(plugin.msg("auto_b8cbc6bf2e"));
        player.sendMessage(plugin.msg("auto_0cb9b2c7ee"));
        player.sendMessage(plugin.msg("auto_1ab7b39726"));
        player.sendMessage(plugin.msg("auto_9e8070e26b"));
        player.sendMessage(plugin.msg("auto_f93a6a6c73"));
        player.sendMessage(plugin.msg("auto_11127ec38f"));
        player.sendMessage(plugin.msg("auto_d906ee281a"));
        player.sendMessage(plugin.msg("auto_0c71d5592d"));
        player.sendMessage(plugin.msg("auto_8ceff0057c"));
        player.sendMessage(plugin.msg("auto_282988ce2f"));
        player.sendMessage(plugin.msg("auto_cca74da5b8"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_f2ad6d3580"));
        player.sendMessage(plugin.msg("auto_2e6e72c7f2"));
        player.sendMessage(plugin.msg("auto_5b9f0a9ea8"));
    }

    // ========================
    //  ĐẠO
    // ========================

    private void showDao(Player player) {
        player.sendMessage(plugin.msg("auto_6bdb128406"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_6da031b9e1"));
        player.sendMessage(plugin.msg("auto_7a3581cc05"));
        player.sendMessage(plugin.msg("auto_0a13a7a66a"));
        player.sendMessage(plugin.msg("auto_362b867dab"));
        player.sendMessage(plugin.msg("auto_706d4a9edd"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_90d1bd4fda"));
        player.sendMessage(plugin.msg("auto_0ccea91651"));
        player.sendMessage(plugin.msg("auto_1ca2cd3ad2"));
        player.sendMessage(plugin.msg("auto_e3eb8a3f76"));
        player.sendMessage(plugin.msg("auto_53ae14329d"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_bc6815518a"));
        player.sendMessage(plugin.msg("auto_5b9f0a9ea8"));
    }

    // ========================
    //  ĐỘT PHÁ
    // ========================

    private void showDotPha(Player player) {
        player.sendMessage(plugin.msg("auto_2ef10632be"));
        player.sendMessage(plugin.msg("auto_6139ea22fe"));
        player.sendMessage(plugin.msg("auto_dc1ac344b4"));
        player.sendMessage(plugin.msg("auto_2b66973fa3"));
        player.sendMessage(plugin.msg("auto_bd494e937b"));
        player.sendMessage(plugin.msg("auto_7c35565400"));
        player.sendMessage(plugin.msg("auto_9c9967f885"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_4518312ae1"));
        player.sendMessage(plugin.msg("auto_0e0939a896"));
        player.sendMessage(plugin.msg("auto_ba90f4ef64"));
        player.sendMessage(plugin.msg("auto_a3ff223b16"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_998757159d"));
        player.sendMessage(plugin.msg("auto_788c25b929"));
        player.sendMessage(plugin.msg("auto_22c93b57aa"));
        player.sendMessage(plugin.msg("auto_8818a56154"));
        player.sendMessage(plugin.msg("auto_55b950ddb6"));
        player.sendMessage(plugin.msg("auto_5e5fbb2641"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_d3af5505e4"));
        player.sendMessage(plugin.msg("auto_2edb48bb8d"));
        player.sendMessage(plugin.msg("auto_9376933f8d"));
    }

    // ========================
    //  SKILL
    // ========================

    private void showSkill(Player player) {
        player.sendMessage(plugin.msg("auto_c3aa5360b0"));
        player.sendMessage(plugin.msg("auto_2c807dfab3"));
        player.sendMessage(plugin.msg("auto_acf82cee69"));
        player.sendMessage(plugin.msg("auto_f29b327261"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_0cc04b59ec"));
        player.sendMessage(plugin.msg("auto_ac8ca8ac93"));
        player.sendMessage(plugin.msg("auto_36f64e4f65"));
        player.sendMessage(plugin.msg("auto_9664d73254"));
        player.sendMessage(plugin.msg("auto_70f264b341"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_017211abeb"));
        player.sendMessage(plugin.msg("auto_3751b7f842"));
        player.sendMessage(plugin.msg("auto_56d8485e82"));
        player.sendMessage(plugin.msg("auto_bdb8a523b3"));
        player.sendMessage(plugin.msg("auto_a0d7c48c6c"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_65280bc25c"));
        player.sendMessage(plugin.msg("auto_42ca0ec765"));
        player.sendMessage(plugin.msg("auto_e52b567b44"));
        player.sendMessage(plugin.msg("auto_5b9f0a9ea8"));
    }

    // ========================
    //  LINH ĐAN (Alchemy)
    // ========================

    private void showLinhDan(Player player) {
        player.sendMessage(plugin.msg("auto_b095c63eb9"));
        player.sendMessage(plugin.msg("auto_a4dcfb40e4"));
        player.sendMessage(plugin.msg("auto_9b9ad162ac"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_c0d2086aa5"));
        player.sendMessage(plugin.msg("auto_46bb887e39"));
        player.sendMessage(plugin.msg("auto_ce0ed55b78"));
        player.sendMessage(plugin.msg("auto_1cd9b17c41"));
        player.sendMessage(plugin.msg("auto_dc014ce825"));
        player.sendMessage(plugin.msg("auto_8e5e9c04a6"));
        player.sendMessage(plugin.msg("auto_ede1a07eb5"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_ad578097e9"));
        player.sendMessage(plugin.msg("auto_49dc1aa671"));
        player.sendMessage(plugin.msg("auto_3ebbc245e4"));
        player.sendMessage(plugin.msg("auto_fbc0669c52"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_4befc25269"));
        player.sendMessage(plugin.msg("auto_1bca9ccf7d"));
        player.sendMessage(plugin.msg("auto_9270648988"));
        player.sendMessage(plugin.msg("auto_e3d2fd7a32"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_faae212753"));
        player.sendMessage(plugin.msg("auto_9883818a23"));
        player.sendMessage(plugin.msg("auto_de0860aebf"));
        player.sendMessage(plugin.msg("auto_734f6b8934"));
        player.sendMessage(plugin.msg("auto_2ac02f613e"));
        player.sendMessage(plugin.msg("auto_f6fc33c05d"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_28c0a1d4a0"));
        player.sendMessage(plugin.msg("auto_6c7da6fc19"));
        player.sendMessage(plugin.msg("auto_c90456f64e"));
        player.sendMessage(plugin.msg("auto_d5d3b83f24"));
        player.sendMessage(plugin.msg("auto_3f968367e0"));
        player.sendMessage(plugin.msg("auto_ffea016d2d"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_2060ed23e0"));
        player.sendMessage(plugin.msg("auto_9a32e9dcd6"));
        player.sendMessage(plugin.msg("auto_78670bc9db"));
    }

    // ========================
    //  PHÁP BẢO
    // ========================

    private void showPhaBao(Player player) {
        player.sendMessage(plugin.msg("auto_657187e610"));
        player.sendMessage(plugin.msg("auto_c2e9b0d540"));
        player.sendMessage(plugin.msg("auto_b801230baa"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_da4bf19807"));
        player.sendMessage(plugin.msg("auto_f7e04bb775"));
        player.sendMessage(plugin.msg("auto_fbc78b421a"));
        player.sendMessage(plugin.msg("auto_acad1deb86"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_58da2b37f7"));
        player.sendMessage(plugin.msg("auto_37b4419c8d"));
        player.sendMessage(plugin.msg("auto_51986fb740"));
        player.sendMessage(plugin.msg("auto_3759ff3495"));
        player.sendMessage(plugin.msg("auto_b7b1dda2fe"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_b707d88a76"));
        player.sendMessage(plugin.msg("auto_69eef8845d"));
    }

    // ========================
    //  TIỀN
    // ========================

    private void showTien(Player player) {
        player.sendMessage(plugin.msg("auto_7b6e548b5a"));
        player.sendMessage(plugin.msg("auto_8a94065de9"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_9947aca8f3"));
        player.sendMessage(plugin.msg("auto_a93af8ded5"));
        player.sendMessage(plugin.msg("auto_a7e2ddf80b"));
        player.sendMessage(plugin.msg("auto_a46f341f79"));
        player.sendMessage(plugin.msg("auto_e289e2e113"));
        player.sendMessage(plugin.msg("auto_331aee445a"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_f003e8168f"));
        player.sendMessage(plugin.msg("auto_f04872d426"));
        player.sendMessage(plugin.msg("auto_587d913927"));
        player.sendMessage(plugin.msg("auto_a495c01eb1"));
        player.sendMessage(plugin.msg("auto_f0388e1ebd"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_6da2418e7a"));
        player.sendMessage(plugin.msg("auto_b1a90c95de"));
        player.sendMessage(plugin.msg("auto_ddb6eb52a2"));
    }

    // ========================
    //  LINH CĂN
    // ========================

    private void showLinhCan(Player player) {
        player.sendMessage(plugin.msg("auto_6d4fd21dd4"));
        player.sendMessage(plugin.msg("auto_93064a0b14"));
        player.sendMessage(plugin.msg("auto_c74f7810b2"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_20f3c5be96"));
        player.sendMessage(plugin.msg("auto_0c3f53c023"));
        player.sendMessage(plugin.msg("auto_250f8ca489"));
        player.sendMessage(plugin.msg("auto_cae0471aba"));
        player.sendMessage(plugin.msg("auto_27b6009fb8"));
        player.sendMessage(plugin.msg("auto_5d44b207e9"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_3ab4c99a16"));
        player.sendMessage(plugin.msg("auto_bfdf0936d5"));
        player.sendMessage(plugin.msg("auto_ddb6eb52a2"));
    }

    // ========================
    //  TỘC
    // ========================

    private void showToc(Player player) {
        player.sendMessage(plugin.msg("auto_051ba2576b"));
        player.sendMessage(plugin.msg("auto_194ff4b664"));
        player.sendMessage(plugin.msg("auto_c14580619c"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_1e9bb0ee1c"));
        player.sendMessage(plugin.msg("auto_055e8bee31"));
        player.sendMessage(plugin.msg("auto_8f201ee4ce"));
        player.sendMessage(plugin.msg("auto_c8afe648e2"));
        player.sendMessage(plugin.msg("auto_c2ea7c8767"));
        player.sendMessage(plugin.msg("auto_3bcaa4e619"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_2abc93ed55"));
        player.sendMessage(plugin.msg("auto_ddb6eb52a2"));
    }

    // ========================
    //  TÔNG MÔN
    // ========================

    private void showTongMon(Player player) {
        player.sendMessage(plugin.msg("auto_0a060c7b77"));
        player.sendMessage(plugin.msg("auto_777f6977fd"));
        player.sendMessage(plugin.msg("auto_de288e469c"));
        player.sendMessage(plugin.msg("auto_1ee2113477"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_8b40a35313"));
        player.sendMessage(plugin.msg("auto_8766137980"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_29bcfed7f9"));
        player.sendMessage(plugin.msg("auto_4b6423b2ba"));
        player.sendMessage(plugin.msg("auto_0a9e1c0de0"));
        player.sendMessage(plugin.msg("auto_da39a3ee5e"));
        player.sendMessage(plugin.msg("auto_9693b461a9"));
        player.sendMessage(plugin.msg("auto_426ae88127"));
        player.sendMessage(plugin.msg("auto_63597b0ce1"));
        player.sendMessage(plugin.msg("auto_b0f4b5318a"));
        player.sendMessage(plugin.msg("auto_f9555e414b"));
        player.sendMessage(plugin.msg("auto_ddb6eb52a2"));
    }

    // ========================
    //  LỆNH
    // ========================

    private void showLenh(Player player) {
        player.sendMessage(plugin.msg("auto_63b2b75f9c"));
        player.sendMessage(plugin.msg("auto_bdddfc941b"));
        player.sendMessage(plugin.msg("auto_309c4f4db7"));
        player.sendMessage(plugin.msg("auto_d55d848473"));
        player.sendMessage(plugin.msg("auto_1a7d0fb339"));
        player.sendMessage(plugin.msg("auto_cefa3e0d50"));
        player.sendMessage(plugin.msg("auto_e2cb8f1588"));
        player.sendMessage(plugin.msg("auto_7658025872"));
        player.sendMessage(plugin.msg("auto_10ba840c9a"));
        player.sendMessage(plugin.msg("auto_a052214978"));
        player.sendMessage(plugin.msg("auto_53b872fce3"));
        player.sendMessage(plugin.msg("auto_2cc93473d4"));
        player.sendMessage(plugin.msg("auto_825d264ad5"));
        player.sendMessage(plugin.msg("auto_4ebcd2f5a2"));
        player.sendMessage(plugin.msg("auto_ebb38c1514"));
        player.sendMessage(plugin.msg("auto_2950416a88"));
    }

    // ========================
    //  MẸO
    // ========================

    private void showMeo(Player player) {
        player.sendMessage(plugin.msg("auto_81e4658bfc"));
        player.sendMessage(plugin.msg("auto_db9be31cba"));
        player.sendMessage(plugin.msg("auto_efe8731ad9"));
        player.sendMessage(plugin.msg("auto_f6232a962b"));
        player.sendMessage(plugin.msg("auto_f56394eca1"));
        player.sendMessage(plugin.msg("auto_d32c820fa8"));
        player.sendMessage(plugin.msg("auto_a7413dc46b"));
        player.sendMessage(plugin.msg("auto_d9037b6881"));
        player.sendMessage(plugin.msg("auto_105eeafd83"));
        player.sendMessage(plugin.msg("auto_edf5e06899"));
        player.sendMessage(plugin.msg("auto_371709a4e4"));
        player.sendMessage(plugin.msg("auto_98bf4777b9"));
        player.sendMessage(plugin.msg("auto_f58cbd3333"));
        player.sendMessage(plugin.msg("auto_ddb6eb52a2"));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1)
            return Arrays.asList("batdau","canh_gioi","class","dao","linh_can","toc",
                    "skill","phap_bao","dot_pha","linh_dan","tien",
                    "tong_mon","lenh","meo");
        return Collections.emptyList();
    }

    private void showThienDinh(Player player) {
        player.sendMessage(plugin.msg("auto_3614d3508f"));
        player.sendMessage(plugin.msg("auto_e6025239d4"));
        player.sendMessage(plugin.msg("auto_141ca2eeaf"));
        player.sendMessage(plugin.msg("auto_10be12e4aa"));
        player.sendMessage(plugin.msg("auto_a58f87ecf9"));
        player.sendMessage(plugin.msg("auto_c0c74ffb84"));
        player.sendMessage(plugin.msg("auto_f3b265e7cc"));
        player.sendMessage(plugin.msg("auto_69eef8845d"));
    }

    private void showKiemDao(Player player) {
        player.sendMessage(plugin.msg("auto_456d129100"));
        player.sendMessage(plugin.msg("auto_0709b180f6"));
        player.sendMessage(plugin.msg("auto_b0bfd90c80"));
        player.sendMessage(plugin.msg("auto_009d81ca84"));
        player.sendMessage(plugin.msg("auto_16a7c50d1b"));
        player.sendMessage(plugin.msg("auto_070c2aee88"));
        player.sendMessage(plugin.msg("auto_e5aa51968e"));
        player.sendMessage(plugin.msg("auto_42ab840bae"));
        player.sendMessage(plugin.msg("auto_69eef8845d"));
    }

    private void showThao(Player player) {
        player.sendMessage(plugin.msg("auto_7e6b3f468d"));
        player.sendMessage(plugin.msg("auto_55acc2f171"));
        player.sendMessage(plugin.msg("auto_943d288217"));
        player.sendMessage(plugin.msg("auto_c319921e7c"));
        player.sendMessage(plugin.msg("auto_a9566f23ea"));
        player.sendMessage(plugin.msg("auto_22fa98d525"));
        player.sendMessage(plugin.msg("auto_dffc95dcf1"));
        player.sendMessage(plugin.msg("auto_dc29ec6d36"));
        player.sendMessage(plugin.msg("auto_e110222104"));
        player.sendMessage(plugin.msg("auto_5b9f0a9ea8"));
    }

    private void showZone(Player player) {
        player.sendMessage(plugin.msg("auto_41cf52a361"));
        player.sendMessage(plugin.msg("auto_32acbd77ef"));
        player.sendMessage(plugin.msg("auto_2f428baabc"));
        player.sendMessage(plugin.msg("auto_f1103fa8d0"));
        player.sendMessage(plugin.msg("auto_2b80216a24"));
        player.sendMessage(plugin.msg("auto_b7364fdfbf"));
        player.sendMessage(plugin.msg("auto_f91daf9941"));
        player.sendMessage(plugin.msg("auto_616cc61300"));
    }
}