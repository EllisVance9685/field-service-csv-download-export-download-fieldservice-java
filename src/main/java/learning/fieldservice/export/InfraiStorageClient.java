package learning.fieldservice.export;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiStorageClient {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern URL = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern MESSAGE = Pattern.compile("\\\"(?:message|hint)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final int MAX_ATTEMPTS = 4;

    private final HttpClient http;
    private final ExportSettings settings;

    public InfraiStorageClient(ExportSettings settings) {
        this.settings = settings;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void requireBucket() throws IOException, InterruptedException {
        String path = "/v1/storage/bucket/get/" + segment(settings.bucket());
        call("GET", path, null);
    }

    public String presignPut(String key, String contentType, long maxBytes, String requestId)
            throws IOException, InterruptedException {
        String body = "{\"op\":\"put\",\"expires_seconds\":600,\"content_type\":\""
                + json(contentType) + "\",\"max_bytes\":" + maxBytes
                + ",\"idempotency_key\":\"" + json(requestId) + "\"}";
        // Canonical REST wrapper call: infrai.storage.object.presign
        return requireUrl(call("POST", objectPresignPath(key), body));
    }

    public String presignGet(String key, String downloadName, String requestId)
            throws IOException, InterruptedException {
        String disposition = "attachment; filename=\\\"" + downloadName + "\\\"";
        String body = "{\"op\":\"get\",\"expires_seconds\":900,\"response_disposition\":\""
                + json(disposition) + "\",\"idempotency_key\":\"" + json(requestId) + "\"}";
        return requireUrl(call("POST", objectPresignPath(key), body));
    }

    public void upload(String signedUrl, byte[] csv, String contentType) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(signedUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", contentType)
                .method("PUT", HttpRequest.BodyPublishers.ofByteArray(csv))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Signed upload returned HTTP " + response.statusCode());
        }
    }

    private String objectPresignPath(String key) {
        return "/v1/storage/object/presign/" + segment(settings.bucket()) + "/" + segment(key);
    }

    private String call(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(settings.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .header("Content-Type", "application/json");
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body);
            HttpResponse<String> response = http.send(
                    builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());

            Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
            if (!envelope.ok()) {
                if (response.statusCode() == 429 && attempt + 1 < MAX_ATTEMPTS) {
                    Thread.sleep(retryDelayMillis(response, attempt));
                    continue;
                }
                throw new InfraiException(response.statusCode(), envelope.code(), envelope.message());
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Storage transport returned HTTP " + response.statusCode());
            }
            return response.body();
        }
        throw new IOException("Retry attempts exhausted");
    }

    private static Envelope decodeEnvelope(String body, int statusCode) throws IOException {
        Matcher ok = OK.matcher(body);
        if (!ok.find()) {
            throw new IOException("Storage response was not an envelope (HTTP " + statusCode + ")");
        }
        boolean accepted = Boolean.parseBoolean(ok.group(1));
        return new Envelope(accepted, match(CODE, body, "UNKNOWN"), match(MESSAGE, body, "Request rejected"));
    }

    private static String requireUrl(String envelope) throws IOException {
        Matcher matcher = URL.matcher(envelope);
        if (!matcher.find()) {
            throw new IOException("Successful storage response did not contain a URL");
        }
        return unescape(matcher.group(1));
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Math.max(1L, Long.parseLong(retryAfter)) * 1000L;
        } catch (NumberFormatException ignored) {
            return 250L * (1L << attempt);
        }
    }

    private static String match(Pattern pattern, String input, String fallback) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? unescape(matcher.group(1)) : fallback;
    }

    private static String segment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String value) {
        return value.replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private record Envelope(boolean ok, String code, String message) {}
}
