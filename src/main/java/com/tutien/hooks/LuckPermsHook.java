package com.tutien.hooks;

import com.tutien.TuTienPlugin;
import com.tutien.data.CultivationStage;
import com.tutien.data.PlayerData;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LuckPermsHook {

    private final TuTienPlugin plugin;
    private boolean available = false;
    private LuckPerms api;

    private static final int PREFIX_PRIORITY = 100;
    private static final String GROUP_PREFIX = "tt_realm_";

    // Cache trạng thái đã đồng bộ gần nhất cho mỗi người chơi trong phiên chơi hiện tại.
    // Mục đích: KHÔNG set/unset lại LuckPerms nếu cảnh giới không đổi -> tránh ghi
    // dữ liệu (I/O) hàng loạt khi nhiều người chơi online cùng lúc (ví dụ lúc restart).
    // Cache chỉ tồn tại trong RAM nên không ảnh hưởng tới việc "chết vẫn giữ đúng":
    // nếu server crash, cache mất -> lần join/breakthrough kế tiếp sẽ tự đối chiếu
    // lại với dữ liệu thật trong LuckPerms và tự sửa nếu bị lệch (self-heal).
    private final java.util.Map<UUID, String> lastSynced = new ConcurrentHashMap<>();

    private static final List<String> ALL_STAGE_PERMS = Arrays.asList(
            "tutien.luyenkhi",
            "tutien.trucco",
            "tutien.ketdan",
            "tutien.nguyenanh",
            "tutien.hoathan",
            "tutien.detu",
            "tutien.dehau",
            "tutien.devuong",
            "tutien.deton",
            "tutien.dechu",
            "tutien.banthanh",
            "tutien.chuanthanh",
            "tutien.daithanh",
            "tutien.thanhton",
            "tutien.thanhde",
            "tutien.chantien",
            "tutien.kimtien",
            "tutien.thaiat",
            "tutien.daila",
            "tutien.tiende",
            "tutien.thienton",
            "tutien.thiendao",
            "tutien.thiende",
            "tutien.honnguyen",
            "tutien.vothuongchiton"
    );

    public LuckPermsHook(TuTienPlugin plugin) {
        this.plugin = plugin;
        tryLoad();
    }

    private void tryLoad() {
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
                plugin.getLogger().info("[Hook] LuckPerms không tìm thấy — bỏ qua đồng bộ group/prefix.");
                return;
            }
            api = LuckPermsProvider.get();
            available = true;
            plugin.getLogger().info("[Hook] ✅ LuckPerms kết nối — tự động đồng bộ group + prefix + permission theo cảnh giới.");
        } catch (IllegalStateException | NoClassDefFoundError e) {
            plugin.getLogger().info("[Hook] LuckPerms chưa sẵn sàng — bỏ qua đồng bộ group/prefix.");
            available = false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Đồng bộ group/prefix/permission theo cảnh giới hiện tại của người chơi.
     * Thiết kế "chỉ ghi khi thật sự đổi":
     *   1. Nếu cache trong RAM cho biết lần đồng bộ gần nhất đã đúng y hệt trạng thái
     *      cần có -> bỏ qua ngay, KHÔNG gọi LuckPerms (0 I/O). Đây là trường hợp
     *      thường gặp nhất khi server đông người (breakthrough của người này không
     *      đụng tới ai khác, và không có thao tác "set/unset hàng loạt" nào cả).
     *   2. Nếu chưa chắc (mới join, server vừa restart, cache trống...) -> đọc trạng
     *      thái THẬT trong LuckPerms rồi so sánh. Nếu đã khớp -> chỉ cập nhật cache,
     *      không ghi gì thêm. Nếu lệch (ví dụ do crash giữa chừng lần trước) -> mới
     *      thực sự set lại đúng 1 lần cho người chơi đó -> đây chính là cơ chế
     *      "tự chữa lành" (self-heal) đảm bảo dù server có chết đột ngột thì lần
     *      đồng bộ kế tiếp vẫn đưa permission về đúng như cảnh giới thật trong dữ
     *      liệu người chơi (PlayerData), mà không phải set/unset lại cho toàn bộ
     *      người chơi đang online.
     * Toàn bộ việc đọc/ghi LuckPerms chạy bất đồng bộ (async), không đụng main thread.
     */
    public void syncPlayer(Player player, PlayerData data) {
        if (!available || player == null || data == null) return;
        UUID uuid = player.getUniqueId();
        CultivationStage stage = data.getStage();
        String targetGroup = GROUP_PREFIX + slug(stage.getRealm());
        String prefix = "[" + stage.getColor() + stage.getDisplayName() + "§r]";
        String targetPerm = stageToPermission(stage);
        String cacheKey = targetGroup + '|' + prefix + '|' + targetPerm;

        if (cacheKey.equals(lastSynced.get(uuid))) return; // đã đúng từ trước, không có gì để set/unset

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                User user = api.getUserManager().getUser(uuid);
                if (user != null && isInSync(user, targetGroup, prefix, targetPerm)) {
                    lastSynced.put(uuid, cacheKey);
                    return;
                }
                api.getUserManager().modifyUser(uuid, u -> applyStage(u, targetGroup, prefix, targetPerm));
                lastSynced.put(uuid, cacheKey);
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "[LuckPermsHook] syncPlayer lỗi: " + e.getMessage());
            }
        });
    }

    /** So sánh dữ liệu đang có trên user với trạng thái mục tiêu — không sửa gì cả. */
    private boolean isInSync(User user, String targetGroup, String prefix, String targetPerm) {
        boolean groupOk = false, prefixOk = false, permOk = (targetPerm == null);
        int stagePermCount = 0;
        for (Node node : user.data().toCollection()) {
            if (node instanceof InheritanceNode in) {
                String g = in.getGroupName();
                if (g.toLowerCase(Locale.ROOT).startsWith(GROUP_PREFIX)) {
                    if (!g.equalsIgnoreCase(targetGroup)) return false;
                    groupOk = true;
                }
            } else if (node instanceof PrefixNode pn) {
                if (pn.getPriority() == PREFIX_PRIORITY) {
                    if (!pn.getMetaValue().equals(prefix)) return false;
                    prefixOk = true;
                }
            } else if (node instanceof PermissionNode perm) {
                String key = perm.getKey().toLowerCase(Locale.ROOT);
                if (ALL_STAGE_PERMS.contains(key)) {
                    stagePermCount++;
                    if (targetPerm == null || !key.equals(targetPerm.toLowerCase(Locale.ROOT)) || !perm.getValue())
                        return false;
                    permOk = true;
                }
            }
        }
        if (targetPerm != null && stagePermCount != 1) return false; // thừa/thiếu permission cảnh giới
        return groupOk && prefixOk && permOk;
    }

    /** Set đúng 1 lần: xoá group/prefix/permission cũ của hệ thống, gắn cái mới. */
    private void applyStage(User user, String targetGroup, String prefix, String targetPerm) {
        user.data().clear(NodeType.INHERITANCE.predicate(node ->
                node.getGroupName().toLowerCase(Locale.ROOT).startsWith(GROUP_PREFIX)));
        user.data().add(InheritanceNode.builder(targetGroup).build());

        user.data().clear(NodeType.PREFIX.predicate(node ->
                node.getPriority() == PREFIX_PRIORITY));
        user.data().add(PrefixNode.builder(prefix, PREFIX_PRIORITY).build());

        user.data().clear(NodeType.PERMISSION.predicate(node ->
                ALL_STAGE_PERMS.contains(node.getKey().toLowerCase(Locale.ROOT))));
        if (targetPerm != null) {
            user.data().add(PermissionNode.builder(targetPerm.toLowerCase(Locale.ROOT)).value(true).build());
        }
    }

    public void clearPlayerData(UUID uuid) {
        if (!available || uuid == null) return;
        lastSynced.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                api.getUserManager().modifyUser(uuid, user -> {
                    user.data().clear(NodeType.INHERITANCE.predicate(node ->
                            node.getGroupName().toLowerCase(Locale.ROOT).startsWith(GROUP_PREFIX)));

                    user.data().clear(NodeType.PREFIX.predicate(node ->
                            node.getPriority() == PREFIX_PRIORITY));

                    user.data().clear(NodeType.PERMISSION.predicate(node ->
                            ALL_STAGE_PERMS.contains(node.getKey().toLowerCase(Locale.ROOT))));
                });
            } catch (Exception ignored) {}
        });
    }

    /** Gọi khi người chơi rời server, dọn cache RAM để tránh rò rỉ bộ nhớ theo thời gian. */
    public void clearCache(UUID uuid) {
        if (uuid != null) lastSynced.remove(uuid);
    }

    @Deprecated
    public void syncRealmGroup(Player player, PlayerData data) { syncPlayer(player, data); }

    @Deprecated
    public void syncPrefix(Player player, PlayerData data) { syncPlayer(player, data); }

    private String slug(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", "_");
    }

    private String stageToPermission(CultivationStage stage) {
        if (stage == null) return null;
        int ord = stage.ordinal();
        if (ord >= 0 && ord < ALL_STAGE_PERMS.size()) {
            return ALL_STAGE_PERMS.get(ord);
        }
        return null;
    }
}