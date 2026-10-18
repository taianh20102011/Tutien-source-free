package com.tutien.banboard;

import com.tutien.TuTienPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BanBoardManager — doc truc tiep file banned-players.json cua server (file
 * ban CHUAN cua Bukkit) de lay danh sach nguoi choi moi bi ban gan day nhat,
 * roi phoi qua PlaceholderAPI cho hologram/GUI hien thi.
 *
 * VI SAO DOC FILE THAY VI HOOK EVENT: ca lenh /ban vanilla LAN /ban cua
 * EssentialsX (mac dinh, khi khong dung plugin punishment rieng) deu ghi
 * vao CUNG 1 file banned-players.json nay thong qua Bukkit BanList - nen
 * doc file la cach chac chan hoat dong voi CA HAI, khong phu thuoc server
 * dang dung lenh ban cua plugin nao. Doc dinh ky (khong hook event) de
 * don gian, ben, khong lo bo sot neu event thay doi giua cac ban Paper.
 *
 * KHONG dung AJLeaderboards duoc cho tinh nang nay: day la danh sach
 * (list) sap theo THOI GIAN, khong phai bang xep hang theo 1 con so -
 * AJLeaderboards chi ho tro truong hop thu 2.
 */
public class BanBoardManager {

    /** Thread-safe parser for dates written by Bukkit into banned-players.json. */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss Z", Locale.ROOT);

    // Regex tach tung object {...} trong mang JSON, va tach tung truong
    // "key": "value" ben trong - du dung cho cau truc PHANG (khong long
    // nhau) cua banned-players.json, tranh phai them dependency JSON.
    private static final Pattern OBJECT_PATTERN = Pattern.compile("\\{[^{}]*}");
    private static final Pattern FIELD_PATTERN  = Pattern.compile(
            "\"(name|created|source|expires|reason)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final TuTienPlugin plugin;
    private final AtomicReference<List<BanEntry>> cache = new AtomicReference<>(Collections.emptyList());
    private BukkitTask task;

    public BanBoardManager(TuTienPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("ban-board.enabled", true)) return;

        long intervalTicks = Math.max(5, plugin.getConfig().getInt("ban-board.update-interval-seconds", 30)) * 20L;

        // Doc file bat dong bo ngay khi khoi dong (khong cho 30s dau tien
        // hologram trong), roi lap lai theo chu ky da cau hinh.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::refresh);
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refresh, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    /** Danh sach da cache, sap theo thoi gian ban moi nhat truoc. Luon an toan de doc tu bat ky thread nao. */
    public List<BanEntry> getTop() {
        return cache.get();
    }

    private void refresh() {
        try {
            // banned-players.json nam o THU MUC GOC server (cung cap voi
            // server.jar, plugins/, ...) - la thu muc lam viec (working
            // directory) cua tien trinh server, nen dung duong dan tuong
            // doi la du, khong can biet duong dan tuyet doi.
            File file = new File("banned-players.json");
            if (!file.exists()) {
                cache.set(Collections.emptyList());
                return;
            }

            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            List<BanEntry> parsed = parse(content);

            parsed.sort(Comparator.comparingLong(BanEntry::createdMillis).reversed());

            int top = Math.max(1, plugin.getConfig().getInt("ban-board.top-count", 10));
            if (parsed.size() > top) parsed = new ArrayList<>(parsed.subList(0, top));

            cache.set(parsed);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[BanBoard] Khong doc duoc banned-players.json", e);
        } catch (Exception e) {
            // Bat rong: file co the dang duoc ghi do (mid-write) dung luc doc,
            // hoac dinh dang bi hong. Bo qua 1 chu ky, thu lai o lan sau,
            // khong lam crash tac vu dinh ky.
            plugin.getLogger().log(Level.WARNING, "[BanBoard] Loi khi phan tich banned-players.json, bo qua chu ky nay", e);
        }
    }

    private List<BanEntry> parse(String content) {
        List<BanEntry> result = new ArrayList<>();
        Matcher objMatcher = OBJECT_PATTERN.matcher(content);

        while (objMatcher.find()) {
            String obj = objMatcher.group();

            String name = null, created = null, expires = null, reason = null;
            Matcher fieldMatcher = FIELD_PATTERN.matcher(obj);
            while (fieldMatcher.find()) {
                String key = fieldMatcher.group(1);
                String value = unescape(fieldMatcher.group(2));
                switch (key) {
                    case "name"    -> name = value;
                    case "created" -> created = value;
                    case "expires" -> expires = value;
                    case "reason"  -> reason = value;
                    default -> { /* source: khong can dung */ }
                }
            }

            if (name == null) continue; // entry hong/thieu ten -> bo qua

            long createdMillis;
            try {
                createdMillis = created != null ? OffsetDateTime.parse(created, DATE_FORMAT).toInstant().toEpochMilli() : 0L;
            } catch (DateTimeParseException e) {
                createdMillis = 0L;
            }

            result.add(new BanEntry(
                    name,
                    (reason == null || reason.isBlank()) ? "Khong ro ly do" : reason,
                    expires,
                    createdMillis
            ));
        }

        return result;
    }

    private String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    /** 1 dong trong banned-players.json, da rut gon con nhung truong can hien thi. */
    public record BanEntry(String name, String reason, String expiresRaw, long createdMillis) {

        /** Chuoi thoi han hien thi cho nguoi choi: "Vinh vien" / con lai bao lau / da het han. */
        public String durationDisplay() {
            if (expiresRaw == null || expiresRaw.equalsIgnoreCase("forever")) {
                return "§cVĩnh viễn";
            }
            try {
                long expiresMillis = OffsetDateTime.parse(expiresRaw, DATE_FORMAT).toInstant().toEpochMilli();
                long remaining = expiresMillis - System.currentTimeMillis();
                if (remaining <= 0) return "§7Đã hết hạn";
                return "§e" + formatDuration(remaining) + " còn lại";
            } catch (DateTimeParseException e) {
                return "§7Không rõ";
            }
        }

        private static String formatDuration(long millis) {
            long days = millis / 86_400_000L;
            long hours = (millis % 86_400_000L) / 3_600_000L;
            long minutes = (millis % 3_600_000L) / 60_000L;

            if (days > 0)  return days + " ngày " + hours + " giờ";
            if (hours > 0) return hours + " giờ " + minutes + " phút";
            return Math.max(1, minutes) + " phút";
        }
    }
}
