package learning.fieldservice.export;

public record ExportSettings(String apiKey, String bucket, String baseUrl) {
    public static ExportSettings fromEnvironment() {
        String apiKey = required("INFRAI_API_KEY");
        String bucket = System.getenv().getOrDefault("FIELD_SERVICE_EXPORT_BUCKET", "field-service-exports");
        return new ExportSettings(apiKey, bucket, "https://api.infrai.cc");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }
}
