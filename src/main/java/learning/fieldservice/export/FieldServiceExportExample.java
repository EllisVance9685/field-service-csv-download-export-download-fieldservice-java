package learning.fieldservice.export;

import java.time.Clock;
import java.util.List;

public final class FieldServiceExportExample {
    private FieldServiceExportExample() {}

    public static void main(String[] args) throws Exception {
        ExportSettings settings = ExportSettings.fromEnvironment();
        WorkOrderCsvExportService service = new WorkOrderCsvExportService(
                new InfraiStorageClient(settings), Clock.systemUTC());

        List<WorkOrder> lessonOrders = List.of(
                new WorkOrder("WO-1042", "Northside Academy", WorkOrder.DispatchStatus.COMPLETED,
                        List.of("photos/WO-1042-panel.jpg"), "Panel tested; instructor notified."),
                new WorkOrder("WO-1043", "Riverside Learning Lab", WorkOrder.DispatchStatus.COMPLETED,
                        List.of(), "Return visit scheduled."),
                new WorkOrder("WO-1044", "Oak Street Courses", WorkOrder.DispatchStatus.EN_ROUTE,
                        List.of(), "Technician is travelling."));

        String requestId = "course-ops-2026-08-16";
        WorkOrderCsvExportService.ExportReceipt receipt = service.export(lessonOrders, requestId);
        System.out.println("Exported " + receipt.exportedOrders() + " work orders");
        System.out.println("Download: " + receipt.downloadUrl());
    }
}
