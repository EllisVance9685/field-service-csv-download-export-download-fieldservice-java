package learning.fieldservice.export;

import java.util.List;

public record WorkOrder(
        String number,
        String customer,
        DispatchStatus dispatchStatus,
        List<String> photoKeys,
        String technicianFollowUp) {

    public enum DispatchStatus {
        SCHEDULED, EN_ROUTE, COMPLETED
    }

    public boolean requiresFollowUp() {
        return dispatchStatus == DispatchStatus.COMPLETED
                && (photoKeys.isEmpty() || technicianFollowUp == null || technicianFollowUp.isBlank());
    }
}
