package com.tutien.license;

import com.tutien.TuTienPlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Release license preflight. The authoritative host IP is observed by the remote
 * license API, not trusted from a client-supplied field. DEV builds remain usable
 * without a license endpoint; FREE/PREMIUM release flags are compiled into the jar.
 */
public final class LicenseManager {
    private static final Pattern JSON_OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*true");
    private static final Pattern JSON_STRING = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern JSON_EXPIRY = Pattern.compile("\\\"expiresAt\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|(null))");

    private final TuTienPlugin plugin;
    private final HttpClient http;
    private final Path instanceFile;
    private final Path cacheFile;
    private final String edition;
    private final boolean required;
    private final String instanceId;
    private volatile boolean licensed;
    private volatile boolean premiumAccess;
    private volatile String plan = "NONE";
    private BukkitTask refreshTask;

    public LicenseManager(TuTienPlugin plugin) {
        this.plugin = plugin;
        this.edition = readBuildProperty("edition", "DEV").toUpperCase(java.util.Locale.ROOT);
        this.required = Boolean.parseBoolean(readBuildProperty("license.required", "false"));
        this.instanceFile = plugin.getDataFolder().toPath().resolve("instance-id.txt");
        this.cacheFile = plugin.getDataFolder().toPath().resolve("license-cache.properties");
        this.instanceId = loadOrCreateInstanceId();
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Returns false only when a release build has no valid license or offline grace. */
    public boolean checkAtStartup() {
        if ("DEV".equals(edition)) {
            licensed = true;
            premiumAccess = true;
            plan = "DEV";
            plugin.getLogger().info("[License] DEV build — remote license check is disabled.");
            return true;
        }

        String key = plugin.getConfig().getString("licensing.key", "").trim();

        // 1. Nếu có nhập key (đặc biệt là key Premium TTP- hoặc bất kỳ key bản quyền nào)
        if (!key.isEmpty() && !"none".equalsIgnoreCase(key)) {
            plugin.getLogger().info("[License] Đang xác thực License Key: " + maskKey(key) + "...");
            ValidationResult result = requestValidation(true);
            if (result.valid()) {
                applyValid(result.plan(), result.expiresAt());
                plugin.getLogger().info("[License] License hợp lệ: plan=" + plan + ", expires=" + (result.expiresAt() == null ? "lifetime" : result.expiresAt()) + ".");
                return true;
            }

            if (result.networkFailure() && restoreOfflineGrace()) {
                plugin.getLogger().warning("[License] Không kết nối được license server; tiếp tục bằng offline grace (" + plan + ").");
                return true;
            }

            // Nếu đây là bản compile riêng cho PREMIUM, bắt buộc phải có key hợp lệ
            if ("PREMIUM".equals(edition)) {
                licensed = false;
                premiumAccess = false;
                plugin.getLogger().severe("[License] Không thể xác thực license Premium: " + safeReason(result.reason()));
                plugin.getLogger().severe("[License] Plugin sẽ tắt để bảo vệ bản quyền Premium.");
                return false;
            }

            // Nếu bản Free mà người dùng nhập key không hợp lệ, thông báo và fallback về FREE
            plugin.getLogger().warning("[License] Không thể kích hoạt key (" + safeReason(result.reason()) + "). Tự động chuyển về chế độ FREE vĩnh viễn.");
        }

        // 2. Không nhập key hoặc để trống key:
        if ("PREMIUM".equals(edition)) {
            licensed = false;
            premiumAccess = false;
            plugin.getLogger().severe("[License] Chưa nhập licensing.key. Bản PREMIUM yêu cầu License Key trong config.yml!");
            return false;
        }

        // Bản FREE vĩnh viễn: không cần key
        licensed = true;
        premiumAccess = false;
        plan = "FREE";
        plugin.getLogger().info("[License] Bản TuTiên FREE — Kích hoạt thành công, miễn phí vĩnh viễn không cần License Key!");
        return true;
    }

    /** Start periodic revalidation after the plugin has fully initialized. */
    public void startPeriodicValidation() {
        if ("DEV".equals(edition) || refreshTask != null) return;
        // Chỉ revalidate nếu đang chạy bản quyền PREMIUM
        if (!this.premiumAccess || !"PREMIUM".equalsIgnoreCase(this.plan)) return;

        long ticks = 20L * 60L * 60L * 6L; // every six hours
        refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            ValidationResult result = requestValidation(false);
            if (result.valid()) {
                applyValid(result.plan(), result.expiresAt());
                return;
            }
            if (result.networkFailure() && restoreOfflineGrace()) return;
            licensed = false;
            premiumAccess = false;
            plugin.getLogger().severe("[License] License Premium bị thu hồi/hết hạn hoặc không còn hợp lệ: " + safeReason(result.reason()));
            if ("PREMIUM".equals(edition)) {
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().disablePlugin(plugin));
            } else {
                licensed = true;
                plan = "FREE";
                plugin.getLogger().warning("[License] Đã hạ cấp về bản FREE.");
            }
        }, ticks, ticks);
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public boolean isLicensed() { return licensed; }
    public boolean hasPremiumAccess() { return premiumAccess; }
    public String getEdition() { return edition; }
    public String getPlan() { return plan; }
    public String getInstanceId() { return instanceId; }

    private ValidationResult requestValidation(boolean activate) {
        String base = plugin.getConfig().getString("licensing.api-url", "http://127.0.0.1:8787").trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String endpoint = base + (activate ? "/api/activate" : "/api/validate");
        String key = plugin.getConfig().getString("licensing.key", "").trim();
        String serverName = Bukkit.getServer().getName();
        String requestEdition = key.toUpperCase(java.util.Locale.ROOT).startsWith("TTP-") ? "PREMIUM" : edition;
        String body = "{\"licenseKey\":\"" + jsonEscape(key) + "\","
                + "\"instanceId\":\"" + jsonEscape(instanceId) + "\","
                + "\"edition\":\"" + jsonEscape(requestEdition) + "\","
                + "\"serverName\":\"" + jsonEscape(serverName) + "\"}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body() == null ? "" : response.body();
            if (response.statusCode() >= 500 || response.statusCode() == 429) {
                return new ValidationResult(false, true, "License API tạm thời lỗi (HTTP " + response.statusCode() + ")", null, null);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300 || !JSON_OK.matcher(responseBody).find()) {
                return new ValidationResult(false, false, extractMessage(responseBody, "API từ chối license (HTTP " + response.statusCode() + ")"), null, null);
            }
            String returnedPlan = extract(responseBody, "plan");
            if (returnedPlan == null) returnedPlan = "NONE";
            returnedPlan = returnedPlan.toUpperCase(java.util.Locale.ROOT);
            if ("PREMIUM".equals(edition) && !"PREMIUM".equals(returnedPlan)) {
                return new ValidationResult(false, false, "Bản Premium cần license Premium", returnedPlan, null);
            }
            Instant expiry = extractExpiry(responseBody);
            return new ValidationResult(true, false, "OK", returnedPlan, expiry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ValidationResult(false, true, "Xác thực bị gián đoạn", null, null);
        } catch (Exception e) {
            return new ValidationResult(false, true, "Không kết nối license API (" + e.getClass().getSimpleName() + ")", null, null);
        }
    }

    private void applyValid(String validatedPlan, Instant expiresAt) {
        this.plan = validatedPlan == null ? "NONE" : validatedPlan.toUpperCase(java.util.Locale.ROOT);
        this.licensed = true;
        this.premiumAccess = "PREMIUM".equalsIgnoreCase(this.plan);
        Properties p = new Properties();
        p.setProperty("plan", this.plan);
        p.setProperty("validatedAt", Long.toString(System.currentTimeMillis()));
        p.setProperty("expiresAt", expiresAt == null ? "0" : Long.toString(expiresAt.toEpochMilli()));
        storePropertiesAtomically(cacheFile, p);
    }

    private boolean restoreOfflineGrace() {
        Properties p = loadProperties(cacheFile);
        long validatedAt = parseLong(p.getProperty("validatedAt"), 0L);
        long expiresAt = parseLong(p.getProperty("expiresAt"), -1L);
        String cachedPlan = p.getProperty("plan", "NONE");
        long graceHours = Math.max(0L, plugin.getConfig().getLong("licensing.offline-grace-hours", 72L));
        long graceMs = Math.min(graceHours, 168L) * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();
        if (validatedAt <= 0L || now - validatedAt > graceMs) return false;
        if (expiresAt < 0L || (expiresAt > 0L && now >= expiresAt)) return false;
        this.plan = cachedPlan.toUpperCase(java.util.Locale.ROOT);
        this.licensed = true;
        this.premiumAccess = "PREMIUM".equalsIgnoreCase(this.plan);
        return true;
    }

    private static String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "-****-" + key.substring(key.length() - 4);
    }

    private String readBuildProperty(String key, String fallback) {
        try (InputStream in = plugin.getResource("edition.properties")) {
            if (in == null) return fallback;
            Properties p = new Properties();
            p.load(in);
            return p.getProperty(key, fallback).trim();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[License] Cannot read edition.properties", e);
            return fallback;
        }
    }

    private String loadOrCreateInstanceId() {
        try {
            Files.createDirectories(instanceFile.getParent());
            if (Files.exists(instanceFile)) {
                String existing = Files.readString(instanceFile, StandardCharsets.UTF_8).trim();
                if (existing.matches("[0-9a-fA-F-]{36}")) return existing;
            }
            String created = UUID.randomUUID().toString();
            Files.writeString(instanceFile, created + System.lineSeparator(), StandardCharsets.UTF_8);
            return created;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[License] Cannot read/create instance ID", e);
            return UUID.randomUUID().toString();
        }
    }

    private static Properties loadProperties(Path path) {
        Properties p = new Properties();
        if (!Files.exists(path)) return p;
        try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        catch (IOException ignored) { }
        return p;
    }

    private static void storePropertiesAtomically(Path path, Properties props) {
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(temp)) { props.store(out, "TuTien license cache; no license key is stored here"); }
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException ignored) { /* Cache is optional; online validation still succeeds. */ }
    }

    private static String extract(String json, String key) {
        Pattern p = Pattern.compile(String.format(JSON_STRING.pattern(), Pattern.quote(key)));
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static Instant extractExpiry(String json) {
        Matcher m = JSON_EXPIRY.matcher(json);
        if (!m.find() || m.group(2) != null || m.group(1) == null || m.group(1).isBlank()) return null;
        try { return Instant.parse(m.group(1)); } catch (Exception ignored) { return null; }
    }

    private static String extractMessage(String json, String fallback) {
        String msg = extract(json, "message");
        return msg == null || msg.isBlank() ? fallback : msg.replaceAll("[\\r\\n]", " ");
    }

    private static String safeReason(String reason) {
        if (reason == null || reason.isBlank()) return "Lỗi không xác định";
        return reason.length() > 220 ? reason.substring(0, 220) : reason;
    }

    private static String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static long parseLong(String value, long fallback) {
        try { return Long.parseLong(value); } catch (Exception ignored) { return fallback; }
    }

    private record ValidationResult(boolean valid, boolean networkFailure, String reason, String plan, Instant expiresAt) { }
}
