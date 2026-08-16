package learning.fieldservice.export;

import java.util.List;

public final class WorkOrderCsvDecisionTest {
    public static void main(String[] args) {
        List<WorkOrder> orders = List.of(
                new WorkOrder("WO-2001", "Algebra Studio", WorkOrder.DispatchStatus.COMPLETED,
                        List.of("photos/proof.jpg"), "Teacher confirmed the repair."),
                new WorkOrder("WO-2002", "Science Annex", WorkOrder.DispatchStatus.COMPLETED,
                        List.of(), "Technician will return Thursday."),
                new WorkOrder("WO-2003", "Language Center", WorkOrder.DispatchStatus.EN_ROUTE,
                        List.of(), "Arrival expected at 14:00."));

        String csv = WorkOrderCsvExportService.renderCsv(orders);
        require(csv.contains("\"WO-2001\",\"Algebra Studio\",COMPLETED,1,\"Teacher confirmed the repair.\",false"));
        require(csv.contains("\"WO-2002\",\"Science Annex\",COMPLETED,0,\"Technician will return Thursday.\",true"));
        require(csv.contains("\"WO-2003\",\"Language Center\",EN_ROUTE,0,\"Arrival expected at 14:00.\",false"));
        System.out.println("CSV follow-up decision passed");
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError("Unexpected CSV decision");
        }
    }
}
