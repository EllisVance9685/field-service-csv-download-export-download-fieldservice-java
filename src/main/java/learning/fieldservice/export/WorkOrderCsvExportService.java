package learning.fieldservice.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class WorkOrderCsvExportService {
    private static final String CSV_TYPE = "text/csv";

    private final InfraiStorageClient storage;
    private final Clock clock;

    public WorkOrderCsvExportService(InfraiStorageClient storage, Clock clock) {
        this.storage = storage;
        this.clock = clock;
    }

    public ExportReceipt export(List<WorkOrder> orders, String requestId) throws IOException, InterruptedException {
        String fileName = "work-orders-" + LocalDate.now(clock) + ".csv";
        String key = "reports/" + requestId + "/" + fileName;
        byte[] csv = renderCsv(orders).getBytes(StandardCharsets.UTF_8);

        storage.requireBucket();
        String uploadUrl = storage.presignPut(key, CSV_TYPE, csv.length, requestId + "-put");
        storage.upload(uploadUrl, csv, CSV_TYPE);
        String downloadUrl = storage.presignGet(key, fileName, requestId + "-get");
        return new ExportReceipt(fileName, downloadUrl, orders.size());
    }

    static String renderCsv(List<WorkOrder> orders) {
        StringBuilder csv = new StringBuilder(
                "work_order,customer,dispatch_status,photo_count,technician_follow_up,follow_up_required\n");
        for (WorkOrder order : orders) {
            csv.append(cell(order.number())).append(',')
                    .append(cell(order.customer())).append(',')
                    .append(order.dispatchStatus()).append(',')
                    .append(order.photoKeys().size()).append(',')
                    .append(cell(order.technicianFollowUp())).append(',')
                    .append(order.requiresFollowUp()).append('\n');
        }
        return csv.toString();
    }

    private static String cell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    public record ExportReceipt(String fileName, String downloadUrl, int exportedOrders) {}
}
